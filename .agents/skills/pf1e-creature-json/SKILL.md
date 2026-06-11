---
name: pf1e-creature-json
description: Convert Pathfinder 1e creature stat blocks into this repository's catalog JSON schema. Use when the user pastes a PF1e creature, asks to normalize a stat block into JSON, or needs a creature entry for `src/main/resources/catalog/*.json` in this app.
---

# PF1e Creature JSON

## Overview

Convert a Pathfinder 1e creature stat block into the exact JSON shape used by this application.
Produce repository-ready JSON only. Do not explain the conversion unless the user explicitly asks.

## Workflow

1. Read the full stat block first.
2. Extract only facts that appear in the source or are safely deducible from PF1e notation.
3. Map every value to the repository schema in `references/schema.md`.
4. Normalize enums and list values exactly as the app expects.
5. Return a single valid JSON object.

## Rules

- Use the repository schema exactly; do not invent new keys.
- Keep names, attack data, defenses, speeds, and abilities separate.
- Preserve the original full stat block in `fullStatBlock`.
- Use empty arrays for missing list fields and `null` only where the schema allows optional object values.
- Never hallucinate template permissions, special defenses, attacks, or abilities.
- Keep damage split by type in `damageComponents`.
- If an attack has multiple natural attacks, represent each attack entry with its `quantity`.
- If a value is ambiguous, prefer the safest literal reading from the text over an inference.

## Normalization

- `id`: lowercase, stable, hyphenated slug with no accents.
- `alignment`: use PF1e codes such as `LG`, `NG`, `N`, `CE`.
- `tamano`: use `Tiny`, `Small`, `Medium`, `Large`, etc.
- `tipo`: use the creature type in lowercase.
- `velocidades.tipo`: use `land`, `fly`, `swim`, `climb`, `burrow`, or `other`.
- `ataques.attackType`: use `MELEE` or `RANGED`.
- `damageType`: use repository enum values such as `PIERCING`, `SLASHING`, `BLUDGEONING`, `FIRE`, `COLD`, `ACID`, `ELECTRICITY`, `SONIC`, `FORCE`, `UNTYPED`, or `OTHER`.
- `defensasEspeciales.tipo`: use repository enum values only: `DAMAGE_REDUCTION`, `RESISTANCE`, `IMMUNITY`, `VULNERABILITY`, or `OTHER`.

## Output Contract

- Output JSON only.
- Do not wrap the result in markdown.
- Do not add commentary, assumptions, or alternative versions.
- Keep the JSON parseable by the application as-is.

## When uncertain

- Prefer omission over invention.
- If a field cannot be extracted reliably, leave the safest empty representation.
- If the source includes a parenthetical bonus or note, preserve it in the closest matching field rather than rewriting it.

## Reference

- See `references/schema.md` for the exact field structure and a compact normalization reference.
