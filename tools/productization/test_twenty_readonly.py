"""Synthetic, no-network contract/security tests for the Twenty read-only adapter."""
import json
import unittest
from urllib.parse import parse_qs, urlsplit
from twenty_readonly import (
    read_product, validate_instance_url, candidate_from_reviewed, TwentyReadbackError,
)
from export_public_product_candidate import build_public_candidate

class TwentyReadonlyTests(unittest.TestCase):
    def setUp(self):
        product = {
            "productId": "device-health", "productName": "Device Health",
            "valueProposition": "Approved public device health overview for owners",
            "audience": ["Android device owners"], "differentiators": [],
            "channels": ["WEBSITE"], "sourceEvidenceRef": "synthetic:test",
            "rightsStatus": "NOT_PROVEN", "economics": {},
        }
        self.candidate = build_public_candidate(json.dumps(product).encode())
        self.id = "9f42e44f-8740-47d4-b7d2-6d9f455ef2d7"
        self.row = {
            "id": self.id, "name": "Device Health",
            "externalKey": "DEVICE_CARE_KI:device-health",
            "sourceSha256": self.candidate["sourceSha256"],
        }

    def read(self, data=None, candidate=None):
        calls = []
        def transport(url, token):
            calls.append((url, token))
            return data if data is not None else {
                "data": {"products": [self.row]},
                "pageInfo": {"hasNextPage": False},
            }
        result = read_product(candidate or self.candidate,
            base_url="https://api.twenty.com",
            token="synthetic-token-not-secret", transport=transport)
        return result, calls

    def test_exact_public_product_readback(self):
        result, calls = self.read()
        self.assertEqual(result["status"], "MATCHED")
        self.assertFalse(result["writeAllowed"])
        self.assertEqual(result["contentLinkStatus"], "HOLD_EDITORIAL_RIGHTS_AND_ASSET_PROOF")
        self.assertEqual(len(calls), 1)
        parsed = urlsplit(calls[0][0])
        self.assertEqual(parsed.path, "/rest/products")
        self.assertEqual(parsed.netloc, "api.twenty.com")
        query = parse_qs(parsed.query)
        self.assertEqual(query["limit"], ["2"])
        self.assertEqual(query["fields"], ["id,name,externalKey,sourceSha256"])
        self.assertEqual(query["filter"], ['externalKey[eq]:"DEVICE_CARE_KI:device-health"'])
        self.assertNotIn("Authorization", result)

    def test_not_found_and_duplicate_fail_closed(self):
        result, _ = self.read({"data": {"products": []}, "pageInfo": {"hasNextPage": False}})
        self.assertEqual(result["status"], "NOT_FOUND")
        with self.assertRaisesRegex(TwentyReadbackError, "DUPLICATE"):
            self.read({"data": {"products": [self.row, self.row]}, "pageInfo": {"hasNextPage": False}})

    def test_incomplete_pages_and_hash_mismatches(self):
        for payload in (
            {"data": {"products": [self.row]}},
            {"data": {"products": [self.row]}, "pageInfo": {"hasNextPage": True}},
            {"data": {"products": [self.row] * 3}, "pageInfo": {"hasNextPage": False}},
            {"data": {"products": [{**self.row, "sourceSha256": "f"*64}]}, "pageInfo": {"hasNextPage": False}},
            {"data": {"products": [{**self.row, "name": "Wrong"}]}, "pageInfo": {"hasNextPage": False}},
        ):
            with self.subTest(payload=payload), self.assertRaises(TwentyReadbackError):
                self.read(payload)

    def test_reject_unsafe_urls_and_foreign_project_fields(self):
        for url in (
            "http://crm.example.com", "https://localhost", "https://crm.example.com:8443",
            "https://user:pass@crm.example.com", "https://crm.example.com/path",
            "https://crm.example.com?token=x",
            "https://crm.devicecare.example",
            "https://api.twenty.com.evil.test",
        ):
            with self.subTest(url=url), self.assertRaises(TwentyReadbackError):
                validate_instance_url(url)
        for mutation in (
            {"projectId": "CAPITAL_AI"},
            {"email": "person@example.invalid"},
            {"sourceSha256": "bad"},
            {"productId": "x' AND '1"},
        ):
            with self.subTest(mutation=mutation), self.assertRaises(TwentyReadbackError):
                candidate_from_reviewed({**self.candidate, **mutation})

    def test_deterministic_digest_no_device_or_crm_writes(self):
        first, _ = self.read()
        second, _ = self.read()
        self.assertEqual(first["readbackProjectionSha256"], second["readbackProjectionSha256"])
        self.assertEqual(first["sourceSha256"], self.candidate["sourceSha256"])
        self.assertEqual(first["deviceState"], "NOT_PROVEN")

if __name__ == "__main__":
    unittest.main()
