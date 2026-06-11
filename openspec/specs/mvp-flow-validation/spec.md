# MVP Flow Validation Specification

## Purpose

Define end-to-end MVP acceptance for Grimorio Pathfinder across import, projection rebuild, search, detail, editing, and reimport. This spec validates the product flow; it does not redefine architecture internals, UI layout details, or storage schemas.

## Requirements

### Requirement: Effective Local Data Can Be Built from Generated Data and Overrides

The MVP flow MUST start from the generated Spanish dataset plus overrides and produce an effective local projection for use by the app.

#### Scenario: Build the local projection from canonical file sources

- GIVEN `data/generated/spells-es.generated.json` and `data/overrides/spells-es.overrides.json` are available locally
- WHEN the application imports or rebuilds local data
- THEN the SQLite projection SHALL be derived from those file sources
- AND override values SHALL win over regenerated Spanish values for the same spell

### Requirement: Search Flow Matches the Main MVP Use Case

The MVP MUST support the primary product flow of `lista de clase lanzadora + maximum level + optional term` using only effective Spanish content and `personalNotes`.

#### Scenario: Search by class spell list, level ceiling, and Spanish term

- GIVEN the effective local projection has been built
- WHEN the user searches for `Clérigo` with maximum level `3` and the term `veneno`
- THEN the system SHALL return spells in that `lista de clase lanzadora` whose effective Spanish content or `personalNotes` match the term
- AND it MUST NOT use English reference text as a search source in the MVP

#### Scenario: Browse without a term

- GIVEN the effective local projection has been built
- WHEN the user filters by `lista de clase lanzadora` and maximum level without a search term
- THEN the system SHALL allow browsing the matching effective spell set

### Requirement: Result-to-Detail Continuity

The MVP MUST let the user move from results to a coherent spell detail view.

#### Scenario: Open a spell detail from results

- GIVEN the user has a visible result set
- WHEN the user opens one spell
- THEN the detail view SHALL show the same effective Spanish spell data represented in results
- AND it SHALL also expose English reference text, `personalNotes`, and `translationStatus`

### Requirement: Edit-to-Reimport Durability

Manual spell changes MUST remain effective after projection rebuilds and generated dataset reimports.

#### Scenario: Reimport after manual edits

- GIVEN the user has manually changed a Spanish field, `personalNotes`, or `translationStatus`
- WHEN the local projection is rebuilt from the generated dataset plus overrides
- THEN the manual values SHALL still appear as the effective values for that spell
- AND `personalNotes` SHALL remain canonically sourced from overrides
- AND English reference text SHALL remain preserved as reference

#### Scenario: Reimport respects locked status without a separate flag

- GIVEN a spell override stores `fields.translationStatus = "LOCKED"`
- WHEN the generated dataset is reimported
- THEN the effective spell SHALL remain `LOCKED`
- AND the flow MUST NOT depend on any separate `locked` flag to preserve that behavior

### Requirement: Fully Offline MVP Acceptance

The validated MVP flow MUST remain usable without internet connectivity.

#### Scenario: Run the core product flow offline

- GIVEN the device has no internet connection
- WHEN the user imports local data, searches, opens detail, edits a spell, and rebuilds the local projection
- THEN the complete MVP workflow SHALL remain functional offline
- AND it MUST NOT require remote APIs, hosted services, or online translation features

## Non-goals

- Define controller, repository, or adapter implementation details
- Define exact API routes, DTOs, or persistence schemas
- Define UI visual design details beyond flow-level acceptance
- Define future features outside the MVP such as favorites, character management, or English search

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`

## Assumptions

- The generated dataset already contains the English reference text and baseline Spanish content needed for the MVP.
- The local projection rebuild process can be triggered repeatedly without requiring network access.
- The search, detail, and edit flows are exposed through the local application stack described by the architecture spec.

## Open Questions

- What level of automated end-to-end test coverage is required to prove this flow: API-only, UI-only, or both?
- Should reimport validation also assert behavior for orphan overrides in the MVP acceptance suite, or is that covered elsewhere?
