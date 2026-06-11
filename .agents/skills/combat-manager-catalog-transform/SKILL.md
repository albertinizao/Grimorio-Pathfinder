---
name: combat-manager-catalog-transform
description: Transform raw Combat Manager monster JSON into the project’s static catalog format and Pathfinder 1e creature template JSON. Use when normalizing source payloads into the app’s local catalog, resolving uncertain fields, or generating the files consumed by the backend catalog repository.
---

# Combat Manager Catalog Transform

## Overview

Use this skill after raw Combat Manager payloads have been collected. The objective is to normalize unstable source JSON into the app’s static catalog format, not to preserve Combat Manager’s schema verbatim.

## Target output

Generate the catalog JSON consumed by the project’s repository layer, keeping the current local convention used in `src/main/resources/catalog/*.json`.

## Core workflow

1. Read the raw Combat Manager payload.
2. Identify the app fields that are directly supported.
3. Map source values to the project’s catalog schema.
4. Keep unknown source data visible instead of inventing missing structure.
5. Write one normalized JSON file per creature.
6. Verify the output can be loaded by the existing repository parser.
7. If multiple source records could represent the same creature, prefer the official Bestiary/PFRPG entry when evidence is otherwise equal.

## Normalization rules

- Preserve the creature’s identity with a stable `id`.
- Convert names and enums to the project’s expected values.
- Keep `fullStatBlock` if available.
- Keep attacks, defenses, speeds, abilities, and templates as explicit data.
- Do not flatten distinct damage components into a single undifferentiated damage string if the source separates them.
- Do not fabricate rules or template permissions that the source does not support.
- If the source omits a field, leave the catalog field empty or use the smallest safe default.
- If the source schema changes, adapt the mapping conservatively and note the discrepancy.
- If multiple Combat Manager records match the same creature name, prefer the official Bestiary/PFRPG record over adventure path, companion, or variant entries when the evidence is otherwise equal.

## Mapping focus

Prioritize these fields:
- `id`
- `name`
- `summonLevel`
- `alignment`
- `size`
- `creatureType`
- `subtypes`
- `allowedTemplates`
- `initiative`
- `senses`
- `perception`
- `abilities`
- `armorClass`
- `hitPoints`
- `savingThrows`
- `speeds`
- `attacks`
- `space`
- `reach`
- `specialAttacks`
- `specialDefenses`
- `shortAbilities`
- `expandedAbilities`
- `fullStatBlock`

## Verification rule

Before finalizing a transform, check that the output still matches the repository’s expected catalog schema and that the result is still useful for downstream `CreatureTemplate` loading.

When the selected source record is not obvious, document why the official Bestiary/PFRPG entry was chosen so the decision remains auditable.

## References

See `references/mapping-rules.md` for the field mapping guidance and ambiguity handling.
