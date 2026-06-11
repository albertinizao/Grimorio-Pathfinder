# Spell Editing Specification

## Purpose

Define MVP rules for manual spell editing. This spec covers field-by-field Spanish content edits, `personalNotes`, `translationStatus`, persistence as overrides, and rebuild safety. It does not define UI layout, search behavior, or dataset generation.

## Requirements

### Requirement: Field-by-Field Spanish Content Editing

The system MUST support manual updates to effective Spanish spell content one field at a time without mutating unrelated fields or the generated dataset.

#### Scenario: Edit a single Spanish field

- GIVEN a spell exists with effective content derived from generated data and optional overrides
- WHEN the user changes one editable Spanish field
- THEN the system SHALL persist only that field as an override for that spell
- AND it MUST leave unrelated effective fields unchanged unless they were part of the same save operation
- AND it MUST NOT write the manual change back into the generated dataset file

#### Scenario: Preserve English reference text during Spanish editing

- GIVEN a spell contains English reference fields
- WHEN the user edits Spanish content
- THEN the English reference text SHALL remain available as reference content
- AND the edit operation MUST NOT delete or replace English reference text as part of MVP editing

### Requirement: Canonical Override Persistence

Manual edits MUST be persisted in the overrides source so they survive projection rebuilds.

#### Scenario: Persist a Spanish field override

- GIVEN a user saves a manual Spanish field edit
- WHEN the edit is committed
- THEN the system SHALL store the changed value in the spell override entry
- AND the generated dataset SHALL remain unchanged

#### Scenario: Persist personal notes canonically in overrides

- GIVEN a user creates or updates `personalNotes`
- WHEN the note is saved
- THEN the system SHALL persist `personalNotes` in `data/overrides/spells-es.overrides.json`
- AND it MUST NOT treat `personalNotes` as canonical data in the generated dataset or in a separate MVP notes file

### Requirement: Translation Status Editing Model

The system MUST persist translation state only through `translationStatus`. `LOCKED` SHALL be a valid `translationStatus` value and MUST NOT require a separate locked flag.

#### Scenario: Mark a spell as locked

- GIVEN a user sets the translation status to `LOCKED`
- WHEN the status change is saved
- THEN the system SHALL persist `fields.translationStatus = "LOCKED"` in the spell override entry
- AND it MUST NOT create or require any additional `locked` boolean or technical flag

#### Scenario: Manual edit updates status for non-locked content

- GIVEN a spell does not currently have `translationStatus = LOCKED`
- WHEN the user saves a Spanish content change without explicitly selecting a different translation status
- THEN the effective `translationStatus` SHOULD become `MANUALLY_EDITED`
- AND an explicit user-selected valid status SHALL take precedence over that default behavior

#### Scenario: Manual edit keeps explicit locked status unless changed

- GIVEN a spell already has `translationStatus = LOCKED`
- WHEN the user performs an explicit manual content edit
- THEN the system SHALL allow the manual edit
- AND it SHALL preserve `LOCKED` unless the same operation explicitly changes `translationStatus`

### Requirement: Reimport and Rebuild Safety

Manual overrides MUST survive generated dataset reloads and SQLite rebuilds without data loss.

#### Scenario: Reimport generated data after manual editing

- GIVEN a spell has override values for Spanish fields, `personalNotes`, or `translationStatus`
- WHEN the generated dataset is reimported and the effective local projection is rebuilt
- THEN override values SHALL take precedence over regenerated Spanish values for that spell
- AND `personalNotes` SHALL remain intact
- AND English reference text SHALL remain available after rebuild

## Non-goals

- Define screen layout, navigation, or visual editing patterns
- Define API route names or payload schemas
- Define dataset generation or AI translation workflows
- Define search matching, ranking, or English search behavior

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`

## Assumptions

- Each spell has a stable identifier that allows matching generated data and overrides across rebuilds.
- The override model can represent per-spell field overrides, `personalNotes`, and `translationStatus` in the same spell entry.
- English reference fields remain sourced from the imported dataset in the MVP.

## Open Questions

- When a user clears a previously overridden Spanish field, should the system persist an explicit empty override or remove that override key?
- Does the MVP need field-level metadata such as last-edited timestamps in overrides, or is spell-level change tracking sufficient?
