# Overrides Specification

## Purpose

Define how manual spell corrections and user-owned metadata are persisted, reapplied, and protected from automatic overwrite. This spec covers the canonical override file, precedence rules, editable fields, `personalNotes`, and `LOCKED` semantics.

## Requirements

### Requirement: Canonical Override File

Manual corrections and user-owned spell metadata MUST be persisted canonically in `data/overrides/spells-es.overrides.json`.

#### Scenario: Personal notes are persisted canonically

- GIVEN a user adds or updates `personalNotes` for a spell
- WHEN the change is persisted
- THEN the canonical write target SHALL be `data/overrides/spells-es.overrides.json`
- AND `personalNotes` MUST NOT become canonical in `spells-es.generated.json`
- AND `personalNotes` MUST NOT be moved to a separate MVP file

### Requirement: Override File Structure

The override file MUST use a stable versioned envelope with per-spell entries keyed by stable `spellId`.

#### Scenario: The override file is written with the expected top-level shape

- GIVEN the app writes `spells-es.overrides.json`
- WHEN the file is serialized
- THEN the root object SHALL contain `version`, `updatedAt`, and `spells`
- AND `version` MUST be `1` in the MVP
- AND `spells` MUST be an object, not an array
- AND each key in `spells` MUST correspond to a stable spell identifier

### Requirement: Override Entry Shape

Each override entry MUST store manually owned field replacements separately from `personalNotes`.

#### Scenario: A spell override is written

- GIVEN a user edits one or more Spanish fields for a spell
- WHEN the override entry is persisted
- THEN translated field changes SHALL be stored inside `fields`
- AND `personalNotes` SHALL be stored as a direct property of the spell override entry
- AND `updatedAt` SHALL be stored on the spell override entry
- AND the entry MAY omit `fields` when only `personalNotes` changed

### Requirement: Editable Field Boundary

Only approved Spanish fields and `translationStatus` MUST be editable through overrides in the MVP.

#### Scenario: A manual edit targets an allowed field

- GIVEN a user edits a supported field for a spell
- WHEN the override is written
- THEN `fields` MAY contain only these keys:
  - `nameEs`
  - `school`
  - `subschool`
  - `descriptors`
  - `castingTime`
  - `components`
  - `range`
  - `target`
  - `effect`
  - `area`
  - `duration`
  - `savingThrow`
  - `spellResistance`
  - `descriptionEs`
  - `translationStatus`
- AND `fields` MUST NOT contain `nameEn`, `descriptionEn`, identifiers, or source-tracing fields
- AND `personalNotes` MUST NOT be nested under `fields`

### Requirement: Override Precedence Over Generated Data

Override-owned values MUST take precedence over generated dataset values when effective spell data is composed.

#### Scenario: Manual correction exists for a Spanish field

- GIVEN the generated dataset provides a Spanish field value
- AND an override provides a manual value for the same field
- WHEN effective spell data is composed
- THEN the override value SHALL win
- AND the generated value MUST remain replaceable only by explicit override changes, not by automatic regeneration alone

### Requirement: Field Presence Semantics

Presence or absence of a key inside `fields` MUST determine whether the override replaces the generated value.

#### Scenario: A field is intentionally cleared

- GIVEN a user explicitly clears a previously overridden field
- WHEN the override is persisted
- THEN the app SHALL preserve that explicit intent as either an empty string or `null` value in the override entry
- AND absence of the key SHALL mean “use the generated value”

### Requirement: Translation Status Persistence

Manual translation state changes MUST be persisted through overrides.

#### Scenario: Translation status is changed manually

- GIVEN a user changes a spell translation state
- WHEN the change is persisted
- THEN the override data SHALL store the updated `translationStatus`
- AND the effective spell state MUST reflect that override after rebuild or reimport

### Requirement: Allowed Translation Status Values

`translationStatus` in overrides MUST stay within the approved vocabulary.

#### Scenario: A translation status is stored in an override

- GIVEN a spell override writes `fields.translationStatus`
- WHEN the value is validated
- THEN it MUST be one of `NOT_TRANSLATED`, `AI_TRANSLATED`, `REVIEW_REQUIRED`, `REVIEWED`, `MANUALLY_EDITED`, or `LOCKED`
- AND any other value SHALL be rejected as invalid override state

### Requirement: LOCKED Semantics Without Extra Flag

`LOCKED` MUST be expressed only through `translationStatus` and MUST protect effective manual content from automatic overwrite.

#### Scenario: Locked spell is reimported

- GIVEN an override sets `fields.translationStatus = LOCKED`
- WHEN generated data is regenerated or reimported
- THEN automatic processes MUST NOT overwrite override-owned effective content for that spell
- AND the system MUST NOT introduce or require a separate `locked` flag

### Requirement: No Automatic Overwrite of Manual Edits

Automatic processes MUST NOT overwrite override-owned manual edits, including manual Spanish corrections, `personalNotes`, and manually persisted translation state.

#### Scenario: Generated dataset changes after manual editing

- GIVEN a spell already has persisted manual override content
- WHEN a newer generated dataset is imported
- THEN the importer SHALL preserve the manual override content
- AND it MUST keep English reference text available from the effective spell data
- AND it MAY update only non-override-owned generated fields

### Requirement: Override Scope Stays Focused

Override persistence SHOULD remain limited to manually owned spell deltas and user metadata instead of duplicating the full generated dataset.

#### Scenario: Manual change is stored as an override

- GIVEN only a subset of a spell's effective fields were manually changed
- WHEN override data is written
- THEN the persisted override SHOULD contain only the manually owned changes needed to reconstruct effective data
- AND it SHOULD avoid duplicating unchanged generated content unless required for integrity

### Requirement: Unknown Override Data Is Preserved as a Warning

The importer MUST preserve unknown override payloads in storage and report them without blocking the entire import.

#### Scenario: An override contains a non-canonical field

- GIVEN an override entry contains a field that is not supported by the MVP
- WHEN the importer reads the override file
- THEN it SHALL report the field as a warning
- AND it MUST NOT silently apply the unsupported field to the effective spell
- AND it MUST NOT delete the unsupported payload from canonical storage during read-only import

### Requirement: Orphan Overrides Are Retained

Overrides MUST survive even when the generated dataset no longer contains the referenced spell.

#### Scenario: An override references a missing spell

- GIVEN an override exists for a `spellId` not present in the generated dataset
- WHEN overrides are applied
- THEN the importer SHALL report an orphan override warning
- AND it MUST keep the orphan override in canonical storage
- AND it MUST NOT fail the whole rebuild solely because the override is orphaned

## Non-goals

- Defining the generated dataset schema in full detail
- Defining SQLite table design or search indexing
- Defining REST DTOs or UI layouts
- Defining English-text search behavior
- Defining AI translation pipeline behavior

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\03-dataset-importacion-overrides.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\05-traduccion-edicion.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\06-esquema-dataset-overrides.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-domain\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\dataset-import\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`

## Assumptions

- Override entries can be associated reliably with spells through stable spell identifiers.
- English reference text remains sourced from effective spell data, not manually rewritten as part of normal override usage.
- Manual editing may affect Spanish gameplay fields, `personalNotes`, and `translationStatus`.
