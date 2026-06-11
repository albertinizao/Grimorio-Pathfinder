# Overrides Specification

## Purpose

Define how manual spell corrections and user-owned metadata are persisted and take precedence over generated data. This spec covers override ownership, persistence rules, precedence, translation status handling, and `LOCKED` semantics.

## Requirements

### Requirement: Canonical Override Storage

Manual corrections and user-owned spell metadata MUST be persisted canonically in `data/overrides/spells-es.overrides.json`.

#### Scenario: Personal notes are persisted

- GIVEN a user adds or updates `personalNotes` for a spell
- WHEN the change is persisted
- THEN the canonical write target SHALL be `data/overrides/spells-es.overrides.json`
- AND `personalNotes` MUST NOT become canonical in `spells-es.generated.json`
- AND `personalNotes` MUST NOT be moved to a separate MVP file

### Requirement: Override Precedence Over Generated Data

Override-owned values MUST take precedence over generated dataset values when effective spell data is composed.

#### Scenario: Manual correction exists for a Spanish field

- GIVEN the generated dataset provides a Spanish field value
- AND an override provides a manual value for the same field
- WHEN effective spell data is composed
- THEN the override value SHALL win
- AND the generated value MUST remain replaceable only by explicit override changes, not by automatic regeneration alone

### Requirement: Translation Status Persistence

Manual translation state changes MUST be persisted through overrides.

#### Scenario: Translation status is changed manually

- GIVEN a user changes a spell translation state
- WHEN the change is persisted
- THEN the override data SHALL store the updated `translationStatus`
- AND the effective spell state MUST reflect that override after rebuild or reimport

### Requirement: LOCKED Semantics Without Extra Flag

`LOCKED` MUST be expressed only through `translationStatus` and MUST protect effective manual content from automatic overwrite.

#### Scenario: Locked spell is reimported

- GIVEN an override sets `translationStatus = LOCKED`
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

## Non-goals

- Defining the generated dataset schema
- Defining import warning policy except where needed for override preservation
- Defining SQLite table design or search indexing
- Defining English-text search behavior
- Defining UI layout or REST endpoint payloads

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-domain\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`

## Assumptions

- Override entries can be associated reliably with spells through stable spell identifiers.
- English reference text remains sourced from effective spell data, not manually rewritten as part of normal override usage.
- Manual editing may affect Spanish gameplay fields, `personalNotes`, and `translationStatus`.

## Open Questions

- What exact JSON envelope should identify each override entry in the MVP: keyed object, array of entries, or another stable shape?
- When a user intentionally clears a previously overridden field, should the persisted override represent that as explicit empty content or as override removal?
