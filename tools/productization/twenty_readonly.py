#!/usr/bin/env python3
"""GET-only Twenty custom Products readback; no device data, contacts or CRM writes."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import re
import sys
from pathlib import Path
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import URLError, HTTPError

from export_public_product_candidate import build_public_candidate

PRODUCT_RE = re.compile(r"[a-z0-9][a-z0-9-]{2,79}\Z")
SHA_RE = re.compile(r"[a-f0-9]{64}\Z")
UUID_RE = re.compile(r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\Z")
MAX_BYTES = 256_000

class TwentyReadbackError(ValueError):
    """Fixed code only; never carries secrets or HTTP response content."""

class _NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise TwentyReadbackError("TWENTY_REDIRECT_BLOCKED")

def validate_instance_url(value: str) -> str:
    if not isinstance(value, str) or not value:
        raise TwentyReadbackError("TWENTY_INSTANCE_NOT_CONFIGURED")
    try:
        u = urlsplit(value)
        if (u.scheme != "https" or not u.hostname or u.username or u.password
                or u.port or u.path not in ("", "/") or u.query or u.fragment
                or not re.fullmatch(r"[a-zA-Z0-9.-]+", u.hostname)
                or u.hostname.lower() in ("localhost", "127.0.0.1", "::1")
                or u.hostname.lower().endswith((".localhost", ".local"))):
            raise TwentyReadbackError("TWENTY_ORIGIN_UNSAFE")
    except ValueError:
        raise TwentyReadbackError("TWENTY_ORIGIN_UNSAFE") from None
    return "https://" + u.hostname.lower()

def candidate_from_reviewed(raw: dict) -> dict:
    """Validate the offline exported envelope, not live device state."""
    if (not isinstance(raw, dict) or raw.get("contractVersion") != "PRODUCT_SALES_CANDIDATE@1"
            or raw.get("projectId") != "DEVICE_CARE_KI"
            or raw.get("dataClassification") != "PUBLIC_PRODUCT_METADATA"
            or not isinstance(raw.get("productId"), str)
            or not PRODUCT_RE.fullmatch(raw["productId"])
            or not isinstance(raw.get("productName"), str)
            or not 3 <= len(raw["productName"]) <= 120
            or not isinstance(raw.get("sourceSha256"), str)
            or not SHA_RE.fullmatch(raw["sourceSha256"])):
        raise TwentyReadbackError("TWENTY_PRODUCT_CANDIDATE_INVALID")
    allowed = {
        "contractVersion", "projectId", "productId", "productName",
        "valueProposition", "audience", "differentiators", "canonicalUrl",
        "channels", "sourceSha256", "sourceEvidenceRef", "rightsStatus",
        "rightsEvidenceRef", "economics", "dataClassification",
    }
    required = {
        "valueProposition", "audience", "differentiators", "channels",
        "sourceEvidenceRef", "rightsStatus", "economics",
    }
    if set(raw) - allowed or required - set(raw):
        raise TwentyReadbackError("TWENTY_PRODUCT_CANDIDATE_FIELDS_INVALID")
    public = {k: v for k, v in raw.items() if k in {
        "productId", "productName", "valueProposition", "audience",
        "differentiators", "canonicalUrl", "channels", "sourceEvidenceRef",
        "rightsStatus", "rightsEvidenceRef", "economics",
    }}
    try:
        build_public_candidate(json.dumps(public, sort_keys=True).encode("utf-8"))
    except (ValueError, TypeError, KeyError):
        raise TwentyReadbackError("TWENTY_PRODUCT_CANDIDATE_FIELDS_INVALID") from None
    return raw

def _read_http_get(url: str, token: str) -> dict:
    opener = build_opener(_NoRedirect)
    request = Request(url, method="GET", headers={
        "Authorization": "Bearer " + token, "Accept": "application/json",
    })
    try:
        with opener.open(request, timeout=10) as response:
            if response.status != 200:
                raise TwentyReadbackError("TWENTY_READBACK_HTTP_FAILED")
            payload = response.read(MAX_BYTES + 1)
    except TwentyReadbackError:
        raise
    except (HTTPError, URLError, TimeoutError, OSError):
        raise TwentyReadbackError("TWENTY_READBACK_HTTP_FAILED") from None
    if len(payload) > MAX_BYTES:
        raise TwentyReadbackError("TWENTY_READBACK_TOO_LARGE")
    try:
        result = json.loads(payload)
    except (UnicodeDecodeError, ValueError):
        raise TwentyReadbackError("TWENTY_READBACK_INVALID_JSON") from None
    if not isinstance(result, dict):
        raise TwentyReadbackError("TWENTY_READBACK_INVALID_JSON")
    return result

def read_product(candidate: dict, *, base_url: str, token: str, transport=None) -> dict:
    candidate = candidate_from_reviewed(candidate)
    origin = validate_instance_url(base_url)
    if not isinstance(token, str) or not 10 <= len(token) <= 4096:
        raise TwentyReadbackError("TWENTY_TOKEN_NOT_CONFIGURED")
    key = "DEVICE_CARE_KI:" + candidate["productId"]
    params = urlencode({
        "limit": "2", "depth": "0",
        "fields": "id,name,externalKey,sourceSha256",
        "filter": 'externalKey[eq]:"' + key + '"',
    })
    endpoint = origin + "/rest/products?" + params
    raw = (transport or _read_http_get)(endpoint, token)
    data, info = raw.get("data"), raw.get("pageInfo")
    if not isinstance(data, dict) or not isinstance(info, dict) or info.get("hasNextPage") is not False:
        raise TwentyReadbackError("TWENTY_READBACK_PAGINATION_NOT_PROVEN")
    rows = data.get("products")
    if not isinstance(rows, list) or len(rows) > 2:
        raise TwentyReadbackError("TWENTY_READBACK_SHAPE_INVALID")
    if len(rows) == 2:
        raise TwentyReadbackError("TWENTY_EXTERNAL_KEY_DUPLICATE")
    if not rows:
        return {
            "schema": "DEVICECARE_TWENTY_PRODUCT_READBACK@1",
            "provider": "TWENTY", "scope": "DEVICE_CARE_KI", "status": "NOT_FOUND",
            "productKey": key, "crmRecordId": None, "writeAllowed": False,
            "contentLinkStatus": "HOLD", "deviceState": "NOT_PROVEN",
        }
    row = rows[0]
    if not isinstance(row, dict) or not isinstance(row.get("id"), str) or not UUID_RE.fullmatch(row["id"]):
        raise TwentyReadbackError("TWENTY_RECORD_ID_INVALID")
    if row.get("externalKey") != key or row.get("name") != candidate["productName"]:
        raise TwentyReadbackError("TWENTY_PRODUCT_IDENTITY_MISMATCH")
    if row.get("sourceSha256") != candidate["sourceSha256"]:
        raise TwentyReadbackError("TWENTY_PRODUCT_SOURCE_HASH_MISMATCH")
    digest = hashlib.sha256(json.dumps({
        "externalKey": key, "id": row["id"], "name": row["name"],
        "sourceSha256": row["sourceSha256"],
    }, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    return {
        "schema": "DEVICECARE_TWENTY_PRODUCT_READBACK@1",
        "provider": "TWENTY", "scope": "DEVICE_CARE_KI", "status": "MATCHED",
        "productKey": key, "crmRecordId": row["id"],
        "sourceSha256": candidate["sourceSha256"],
        "readbackProjectionSha256": digest,
        "writeAllowed": False, "contentLinkStatus": "HOLD_EDITORIAL_RIGHTS_AND_ASSET_PROOF",
        "deviceState": "NOT_PROVEN",
    }

def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--input", type=Path, required=True)
    opts = ap.parse_args()
    try:
        raw = json.loads(opts.input.read_text(encoding="utf-8"))
        result = read_product(
            raw, base_url=os.getenv("TWENTY_BASE_URL", ""),
            token=os.getenv("TWENTY_READONLY_API_KEY", ""),
        )
        print(json.dumps(result, indent=2, sort_keys=True))
    except (OSError, json.JSONDecodeError, TwentyReadbackError, ValueError):
        print("DEVICECARE_TWENTY_READBACK=BLOCKED_OR_NOT_PROVEN", file=sys.stderr)
        raise SystemExit(1) from None

if __name__ == "__main__":
    main()
