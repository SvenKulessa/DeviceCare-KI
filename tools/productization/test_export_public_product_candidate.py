"""Privacy and contract negative tests; no Android or network access."""
import json
import unittest
from export_public_product_candidate import build_public_candidate


def sample():
    return {
        "productId": "device-health-report",
        "productName": "Device Health Report",
        "valueProposition": "Erstellt nachvollziehbare, nutzerinitiierte Hinweise zur Gerätepflege.",
        "audience": ["Privatnutzer und IT-Administratoren"],
        "differentiators": ["Lokal vorbereiteter Bericht", "Kein ungefragter Upload"],
        "channels": ["EMAIL"],
        "sourceEvidenceRef": "docs/productization/PRODUCT_CANDIDATE_SOURCE.md",
        "rightsStatus": "NOT_PROVEN",
    }


def export(value):
    return build_public_candidate(json.dumps(value, sort_keys=True).encode("utf-8"))


class ProductCandidateTests(unittest.TestCase):
    def test_export_is_deterministic_and_separate(self):
        first = export(sample())
        self.assertEqual(first, export(sample()))
        self.assertEqual(first["projectId"], "DEVICE_CARE_KI")
        self.assertEqual(first["dataClassification"], "PUBLIC_PRODUCT_METADATA")
        self.assertEqual(len(first["sourceSha256"]), 64)
        self.assertNotIn("customer", first)
        self.assertNotIn("device", first)
        self.assertNotIn("crmConnection", first)

    def test_unknown_personal_or_authority_fields_rejected(self):
        for field, value in [
            ("contactEmail", "someone@example.test"),
            ("deviceSerial", "not-allowed"),
            ("token", "secret"),
            ("publicPublishAllowed", True),
            ("sourceSha256", "fake"),
            ("projectId", "CAPITAL_AI"),
        ]:
            with self.subTest(field=field):
                self.assertRaises(ValueError, export, {**sample(), field: value})

    def test_rights_and_unverified_urls_fail_closed(self):
        self.assertRaises(ValueError, export, {**sample(), "rightsStatus": "VERIFIED"})
        for url in ["http://example.test/", "https://user:pass@example.test/",
                    "https://example.test/?token=secret", "https://example.test:9999/"]:
            with self.subTest(url=url):
                self.assertRaises(ValueError, export, {**sample(), "canonicalUrl": url})

    def test_no_synthetic_profit(self):
        result = export(sample())
        self.assertIsNone(result["economics"]["priceEurCents"])
        self.assertIsNone(result["economics"]["variableCostEurCents"])


if __name__ == "__main__":
    unittest.main()
