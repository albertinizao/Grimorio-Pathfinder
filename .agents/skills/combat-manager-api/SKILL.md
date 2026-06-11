---
name: combat-manager-api
description: Inspect and fetch raw monster data from the local Combat Manager API at http://localhost:12457, including discovery by name, list lookups, and full monster payloads. Use when importing, auditing, or reverse-engineering Combat Manager monster records and when the source JSON shape may vary between creatures or versions.
---

# Combat Manager API

## Overview

Use this skill to read Combat Manager as the source system, not as the app contract. The goal is to discover monster IDs, fetch raw monster JSON, and preserve the source payload accurately enough for later normalization.

## Core workflow

1. Search the local API for candidate monsters by name.
2. Select the correct monster ID from the list response.
3. Fetch the full raw monster payload with `getregular`.
4. Treat the payload as unstable source data: verify the fields that matter, do not assume every creature has the same shape.
5. Preserve unknown or unexpected fields in notes for the transform step.
6. When multiple candidates are tied on name match and source quality, prefer the official Bestiary/PFRPG entry over adventure path, companion, or variant records.

## API usage rules

- Base URL: `http://localhost:12457`
- Discovery endpoint: `POST /api/monster/list`
- Full payload endpoint: `GET /api/monster/getregular/{id}` in this local API instance; if a variant instance rejects GET, re-check the local server before assuming POST.
- Use the list endpoint first when only the name is known.
- Prefer the returned `ID` over the display name for downstream work.
- If several candidates share the same name, prefer the one whose source is an official Bestiary or equivalent core PFRPG source when the evidence is otherwise equal.
- Do not invent missing fields; if the payload omits data, keep that omission visible.
- If the API shape differs across creatures, record the exact field names and continue with a conservative parse.

## What to capture from the source

- canonical name
- monster ID
- alignment
- size
- type and subtypes
- summon level or source-level equivalent
- allowed templates or template-like flags
- AC / HP / saves
- speeds
- attacks and damage components
- special defenses
- abilities and full stat block text

## Output expectations

When asked to inspect the API, return:
- the selected monster ID(s)
- the raw source shape if needed
- any important quirks that affect transformation
- which source was preferred when multiple matches existed, especially if an official Bestiary entry was chosen over a campaign or companion variant
- a short recommendation for the next transform step

## References

See `references/api-endpoints.md` for the exact request/response pattern and handling notes.
