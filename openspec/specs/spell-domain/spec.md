# Spell Domain Specification

## Purpose

Define the core spell domain model, terminology, and invariants for Grimorio Pathfinder.
This spec covers domain semantics only; it does not define persistence, import pipelines,
search implementation, or UI behavior.

## Requirements

### Requirement: Stable Spell Identity and Bilingual References

A spell MUST have a stable identity and MUST preserve both Spanish gameplay content and
English reference content as distinct domain values.

#### Scenario: Domain spell keeps stable identity and bilingual references

- GIVEN a spell is represented in the domain
- WHEN the spell is loaded, edited, or rebuilt through outer layers
- THEN it SHALL keep the same stable `id`
- AND it MUST retain Spanish gameplay fields and English reference fields as separate values
- AND it MUST NOT require mutation of English reference text when Spanish fields change

### Requirement: Minimal Domain Construction Contract

The domain MUST allow spell construction from a minimal stable core while keeping optional
fields representable as empty or null values.

#### Scenario: A sparse spell record is loaded

- GIVEN a spell record is created from imported or reconstructed data
- WHEN the domain validates the record
- THEN `id`, `nameEs`, and `translationStatus` MUST be present
- AND the remaining spell fields MAY be absent, null, or empty according to their meaning
- AND the domain MUST NOT invent missing content

### Requirement: Spell Fields Stay Structurally Separated

The domain MUST keep the main spell fields structurally separated instead of collapsing them
into a single unstructured blob.

#### Scenario: A spell is edited field by field

- GIVEN a spell exposes Spanish gameplay data
- WHEN the domain models the spell
- THEN it SHALL preserve separate fields for `nameEs`, `school`, `subschool`,
  `descriptors`, `castingTime`, `components`, `range`, `target`, `effect`, `area`,
  `duration`, `savingThrow`, `spellResistance`, and `descriptionEs`
- AND it SHALL preserve `nameEn` and `descriptionEn` as English reference values when present
- AND it MAY preserve source metadata such as `slug`, `sourceId`, `sourceHash`,
  `sourceBook`, `sourcePage`, `sourceName`, `createdAt`, `updatedAt`, and `reviewedAt`

### Requirement: Spell List Membership Model

The domain MUST represent membership in a `lista de conjuros` through explicit spell list
entries and MUST support `lista de clase lanzadora` plus future special list types.

#### Scenario: Spell belongs to multiple spell lists

- GIVEN a spell appears in more than one spell list
- WHEN the domain represents that membership
- THEN each membership SHALL be modeled as a separate `SpellListEntry`
- AND each entry MUST include `spellId`, `listType`, `listName`, and `level`
- AND `level` MUST be an integer greater than or equal to 0
- AND the model SHOULD recognize `CLASS`, `DOMAIN`, `SUBDOMAIN`, `BLOODLINE`, `PATRON`,
  `MYSTERY`, `INQUISITION`, `SCHOOL`, `RACE`, `ARCHETYPE`, and `OTHER` as recommended
  conceptual values
- AND the model MUST remain extensible so future list types are not rejected solely because
  they were not known at the time this spec was written

### Requirement: Translation Status Vocabulary

The domain MUST restrict translation status to the approved vocabulary.

#### Scenario: Valid translation status is assigned

- GIVEN a spell translation status is created or updated
- WHEN the value is validated by the domain
- THEN it MUST be one of `NOT_TRANSLATED`, `AI_TRANSLATED`, `REVIEW_REQUIRED`,
  `REVIEWED`, `MANUALLY_EDITED`, or `LOCKED`
- AND any other value SHALL be rejected as invalid domain state

### Requirement: Translation Status Semantics

Each translation status MUST have a consistent meaning in the domain model.

#### Scenario: A spell changes translation state

- GIVEN a spell translation status is updated
- WHEN the domain interprets the new value
- THEN `NOT_TRANSLATED` SHALL mean there is no useful Spanish translation yet
- AND `AI_TRANSLATED` SHALL mean the Spanish text was generated automatically
- AND `REVIEW_REQUIRED` SHALL mean the text still needs manual review
- AND `REVIEWED` SHALL mean the text was manually accepted
- AND `MANUALLY_EDITED` SHALL mean a user corrected one or more fields
- AND `LOCKED` SHALL mean the effective spell content is protected from automatic overwrite

### Requirement: LOCKED Status Semantics

`LOCKED` MUST be modeled only as a `translationStatus` value.

#### Scenario: Spell is marked as locked

- GIVEN a spell is protected from automatic overwrite
- WHEN the domain expresses that protection
- THEN it SHALL use `translationStatus = LOCKED`
- AND it MUST NOT introduce a separate `locked` flag or parallel boolean state
- AND it MUST NOT rely on a per-field locking model in the MVP

### Requirement: English Text Preservation Semantics

The domain MUST preserve English text as reference content and MUST keep it distinct from
effective Spanish content.

#### Scenario: Spanish content is manually corrected

- GIVEN a spell has English reference text and Spanish gameplay text
- WHEN a manual correction changes Spanish content
- THEN the English text SHALL remain available as reference
- AND the domain MUST treat English text as distinct from the edited Spanish content
- AND English text MUST NOT be used as the authoritative source for MVP search

### Requirement: Personal Notes Belong to the Effective Spell

The domain MUST model `personalNotes` as part of the effective spell view while keeping it
separate from the official Spanish description.

#### Scenario: A user stores notes for a spell

- GIVEN a user adds notes to a spell
- WHEN the domain represents the spell
- THEN it SHALL expose `personalNotes` as distinct user-owned content
- AND it MUST keep `personalNotes` separate from `descriptionEs`
- AND it MUST allow `personalNotes` to participate in future search and display flows

### Requirement: Source References Stay Available When Present

The domain MUST preserve source metadata when the imported dataset provides it.

#### Scenario: A spell carries source information

- GIVEN imported data contains book or source metadata
- WHEN the spell is represented in the domain
- THEN it SHALL keep the available source reference values
- AND it MUST NOT fail construction because `sourcePage` or another optional source field is missing

## Non-goals

- Defining JSON file formats or import procedures
- Defining override persistence mechanics
- Defining SQLite tables, indexes, or rebuild workflows
- Defining REST DTOs or frontend presentation
- Defining search behavior, ranking, or English-text search
- Defining translation workflows or AI generation pipelines

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\02-modelo-dominio.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\overrides\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\dataset-import\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`

## Assumptions

- Spell identifiers are stable across generated dataset rebuilds.
- Effective Spanish values may later be derived from generated data plus overrides, but that
  composition is specified outside this domain spec.
- Canonical terminology remains `lista de clase lanzadora` and `lista de conjuros`.
- `listType` must remain extensible rather than locked to a closed enum in the MVP.
