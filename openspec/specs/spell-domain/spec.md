# Spell Domain Specification

## Purpose

Define the core spell domain model, terminology, and invariants for Grimorio Pathfinder. This spec covers domain semantics only; it does not define persistence, import pipelines, search implementation, or UI behavior.

## Requirements

### Requirement: Stable Spell Identity and Core Fields

A spell MUST have a stable identity and MUST preserve both Spanish gameplay content and English reference content in the domain model.

#### Scenario: Domain spell keeps stable identity and bilingual references

- GIVEN a spell is represented in the domain
- WHEN the spell is loaded, edited, or rebuilt through outer layers
- THEN it SHALL keep the same stable `id`
- AND it MUST retain Spanish gameplay fields and English reference fields as separate values
- AND it MUST NOT require mutation of English reference text when Spanish fields change

### Requirement: Spell List Membership Model

The domain MUST represent membership in a `lista de conjuros` through explicit spell list entries and MUST support `lista de clase lanzadora` plus future special list types.

#### Scenario: Spell belongs to multiple spell lists

- GIVEN a spell appears in more than one spell list
- WHEN the domain represents that membership
- THEN each membership SHALL be modeled as a separate `SpellListEntry`
- AND each entry MUST include `spellId`, `listType`, `listName`, and `level`
- AND the model MAY include non-class list types such as domain, bloodline, patron, mystery, or similar future categories

### Requirement: Translation Status Vocabulary

The domain MUST restrict translation status to the approved vocabulary.

#### Scenario: Valid translation status is assigned

- GIVEN a spell translation status is created or updated
- WHEN the value is validated by the domain
- THEN it MUST be one of `NOT_TRANSLATED`, `AI_TRANSLATED`, `REVIEW_REQUIRED`, `REVIEWED`, `MANUALLY_EDITED`, or `LOCKED`
- AND any other value SHALL be rejected as invalid domain state

### Requirement: LOCKED Status Semantics

`LOCKED` MUST be modeled only as a `translationStatus` value.

#### Scenario: Spell is marked as locked

- GIVEN a spell is protected from automatic overwrite
- WHEN the domain expresses that protection
- THEN it SHALL use `translationStatus = LOCKED`
- AND it MUST NOT introduce a separate `locked` flag or parallel boolean state

### Requirement: English Text Preservation Semantics

The domain MUST preserve English text as reference content and MUST keep it distinct from effective Spanish content.

#### Scenario: Spanish content is manually corrected

- GIVEN a spell has English reference text and Spanish gameplay text
- WHEN a manual correction changes Spanish content
- THEN the English text SHALL remain available as reference
- AND the domain MUST treat English text as distinct from the edited Spanish content

## Non-goals

- Defining JSON file formats or import procedures
- Defining override persistence mechanics
- Defining SQLite tables, indexes, or rebuild workflows
- Defining REST DTOs or frontend presentation
- Defining search behavior, ranking, or English-text search

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`

## Assumptions

- Spell identifiers are stable across generated dataset rebuilds.
- Effective Spanish values may later be derived from generated data plus overrides, but that composition is specified outside this domain spec.
- Canonical terminology remains `lista de clase lanzadora` and `lista de conjuros`.

## Open Questions

- Should `listType` be constrained to a closed enum in the domain now, or remain extensible until more list categories are imported?
- Which spell fields are mandatory at domain-construction time versus optional but displayable as empty values?
