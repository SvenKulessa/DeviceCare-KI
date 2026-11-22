#!/usr/bin/env python3
"""Offline product marketing candidate export. Never reads Android/device state.

Input is an explicitly prepared PUBLIC PRODUCT metadata JSON only. No CRM,
network, provider, customer, analytics or cross-repository action is performed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from pathlib import Path
from urllib.parse import urlsplit

ALLOWED = frozenset({
    "productId", "productName", "valueProposition", "audience",
    "differentiators", "canonicalUrl", "channels", "sourceEvidenceRef",
    "rightsStatus", "rightsEvidenceRef", "economics",
})
REQUIRED = frozenset({
    "productId", "productName", "valueProposition", "audience",
    "differentiators", "channels", "sourceEvidenceRef", "rightsStatus",
})
CHANNELS = frozenset({"WEBSITE", "LINKEDIN", "EMAIL", "X", "INSTAGRAM"})


def _text(value: object, name: str, minimum: int, maximum: int) -> str:
    if not isinstance(value, str) or not minimum <= len(value.strip()) <= maximum:
        raise ValueError(name + "_INVALID")
    return value.strip()


def _strings(value: object, name: str, maximum: int, max_item: int) -> list[str]:
    if not isinstance(value, list) or len(value) > maximum:
        raise ValueError(name + "_INVALID")
    return [_text(item, name, 3, max_item) for item in value]


def _money(value: object, name: str) -> int | None:
    if value is None:
        return None
    if isinstance(value, bool) or not isinstance(value, int) or value < 0:
        raise ValueError(name + "_INVALID")
    return value


def build_public_candidate(raw_bytes: bytes) -> dict:
    document = json.loads(raw_bytes)
    if not isinstance(document, dict) or set(document) - ALLOWED or REQUIRED - set(document):
        raise ValueError("PUBLIC_METADATA_FIELDS_INVALID")

    product_id = _text(document["productId"], "PRODUCT_ID", 3, 80)
    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,79}", product_id):
        raise ValueError("PRODUCT_ID_INVALID")
    product_name = _text(document["productName"], "PRODUCT_NAME", 3, 120)
    proposition = _text(document["valueProposition"], "VALUE_PROPOSITION", 10, 400)
    audience = _strings(document["audience"], "AUDIENCE", 5, 120)
    differentiators = _strings(document["differentiators"], "DIFFERENTIATORS", 5, 240)
    if not audience:
        raise ValueError("AUDIENCE_REQUIRED")

    channels = document["channels"]
    if not isinstance(channels, list) or not 1 <= len(channels) <= 5 or any(
        not isinstance(c, str) or c not in CHANNELS for c in channels
    ):
        raise ValueError("CHANNELS_INVALID")

    rights = document["rightsStatus"]
    if rights not in ("VERIFIED", "NOT_PROVEN", "BLOCKED"):
        raise ValueError("RIGHTS_INVALID")
    rights_ref = document.get("rightsEvidenceRef")
    if rights_ref is not None:
        rights_ref = _text(rights_ref, "RIGHTS_REF", 3, 240)
    if rights == "VERIFIED" and not rights_ref:
        raise ValueError("RIGHTS_EVIDENCE_REQUIRED")

    source_ref = _text(document["sourceEvidenceRef"], "SOURCE_REF", 3, 240)
    canonical_url = document.get("canonicalUrl")
    if canonical_url is not None:
        canonical_url = _text(canonical_url, "CANONICAL_URL", 10, 2048)
        parsed = urlsplit(canonical_url)
        if (parsed.scheme != "https" or not parsed.hostname or parsed.username
                or parsed.password or parsed.port or parsed.query or parsed.fragment):
            raise ValueError("CANONICAL_URL_UNSAFE")

    economics = document.get("economics", {})
    if not isinstance(economics, dict) or set(economics) - {
        "priceEurCents", "variableCostEurCents", "evidenceRef"
    }:
        raise ValueError("ECONOMICS_INVALID")
    price = _money(economics.get("priceEurCents"), "PRICE")
    cost = _money(economics.get("variableCostEurCents"), "VARIABLE_COST")
    economic_ref = economics.get("evidenceRef")
    if economic_ref is not None:
        economic_ref = _text(economic_ref, "ECONOMICS_REF", 3, 240)

    result = {
        "contractVersion": "PRODUCT_SALES_CANDIDATE@1",
        "projectId": "DEVICE_CARE_KI",
        "productId": product_id,
        "productName": product_name,
        "valueProposition": proposition,
        "audience": audience,
        "differentiators": differentiators,
        "channels": channels,
        "sourceSha256": hashlib.sha256(raw_bytes).hexdigest(),
        "sourceEvidenceRef": source_ref,
        "rightsStatus": rights,
        "economics": {
            "priceEurCents": price,
            "variableCostEurCents": cost,
            "evidenceRef": economic_ref,
        },
        "dataClassification": "PUBLIC_PRODUCT_METADATA",
    }
    if canonical_url is not None:
        result["canonicalUrl"] = canonical_url
    if rights_ref is not None:
        result["rightsEvidenceRef"] = rights_ref
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True, help="Owner-reviewed product metadata JSON")
    parser.add_argument("--output", type=Path, required=True, help="Explicit local JSON export target")
    args = parser.parse_args()
    # Full validation before any write. Exclusive-create avoids replacing owner files.
    result = build_public_candidate(args.input.read_bytes())
    output = json.dumps(result, sort_keys=True, ensure_ascii=False, indent=2) + "\n"
    flags = os.O_WRONLY | os.O_CREAT | os.O_EXCL
    if hasattr(os, "O_NOFOLLOW"):
        flags |= os.O_NOFOLLOW
    fd = os.open(args.output, flags, 0o600)
    with os.fdopen(fd, "w", encoding="utf-8") as stream:
        stream.write(output)
    print("SCRIPT_EXECUTED_LOCAL_EXPORT_ONLY")
    print("FILE=" + args.output.name)
    print("SHA256=" + hashlib.sha256(output.encode("utf-8")).hexdigest())
    print("TRANSFER=NOT_PROVEN")
    print("DEVICE_SYNC=NOT_PROVEN")
    print("DEVICE_EXECUTION=NOT_PROVEN")


if __name__ == "__main__":
    main()
