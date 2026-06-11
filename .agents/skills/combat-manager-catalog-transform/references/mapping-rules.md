# Mapping rules for Combat Manager -> project catalog

## Goal

Normalize raw Combat Manager monster payloads into the static catalog JSON already consumed by the project’s repository loader.

## Expected destination shape

Use the current catalog convention with fields such as:
- `id`
- `nombre`
- `nivelSummon`
- `alineamiento`
- `tamano`
- `tipo`
- `subtipos`
- `plantillasPermitidas`
- `iniciativa`
- `sentidos`
- `percepcion`
- `atributos`
- `ca`
- `pg`
- `salvaciones`
- `velocidades`
- `ataques`
- `espacio`
- `alcance`
- `ataquesEspeciales`
- `defensasEspeciales`
- `notasTacticas`
- `habilidadesResumidas`
- `habilidadesCompletas`
- `fullStatBlock`

## Mapping guidance

- Prefer direct field mapping when the source already contains the value.
- If the source uses a different name for the same concept, map conservatively and keep the original meaning.
- If several source fields could map to one target field, choose the one that best matches the current app behavior.
- If the source exposes multiple damage parts, keep them separate in the transformed structure.
- If a field is ambiguous, prefer preserving the source text in `fullStatBlock` or the closest descriptive field rather than guessing a game rule.
- Do not invent extra rules, derived bonuses, or template permissions unless the source payload makes them explicit.
- If multiple Combat Manager records share the same creature name, prefer the official Bestiary/PFRPG version when the evidence is otherwise equal. Treat adventure path, companion, or variant records as fallback candidates.

## Practical warning

The repository loader is strict about the current catalog shape. A transform is only correct if the resulting JSON can still be parsed by the existing catalog repository without changing the application contract.
