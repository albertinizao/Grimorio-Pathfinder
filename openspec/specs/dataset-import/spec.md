# Dataset Import Specification

## Purpose

Define how Grimorio Pathfinder ingests the generated Spanish dataset and overrides to produce the effective local projection. This spec covers import boundaries, minimal validation, warning behavior, rebuild semantics, and offline execution.

## Requirements

### Requirement: Import Inputs and Composition Order

The import flow MUST ingest the generated Spanish dataset first and MUST then apply overrides to produce effective spell data.

#### Scenario: Effective data is composed during import

- GIVEN `data/generated/spells-es.generated.json` and `data/overrides/spells-es.overrides.json` are available locally
- WHEN an import or rebuild is executed
- THEN the system SHALL read the generated dataset as the base input
- AND it SHALL apply overrides after base records are loaded
- AND it MUST produce effective data consistent with the override rules defined in the overrides specification

### Requirement: Minimal Structural Validation

The import flow MUST perform minimal validation sufficient to protect the rebuild process without turning import into a translation review workflow.

#### Scenario: Record passes minimal validation

- GIVEN a spell record is read from the generated dataset
- WHEN the importer validates it
- THEN the record MUST include a stable spell identifier
- AND it SHOULD preserve English reference text when that text exists in the source record
- AND validation SHALL focus on structural importability rather than stylistic quality of Spanish translations

### Requirement: Orphan Override Warning Handling

The import flow MUST preserve orphan overrides as warnings and MUST NOT fail the whole rebuild solely because an override has no matching generated spell.

#### Scenario: Override references a missing spell

- GIVEN an override entry exists for a spell id not present in the generated dataset
- WHEN overrides are applied during import
- THEN the importer SHALL report an orphan override warning
- AND it MUST keep the orphan override in canonical storage
- AND it MUST NOT treat that warning alone as a fatal rebuild error

### Requirement: SQLite as Reconstructible Projection

The import flow MUST treat SQLite as a rebuildable local projection derived from versioned files rather than as an irreplaceable canonical source.

#### Scenario: Local projection is rebuilt

- GIVEN the effective spell data has been composed from generated data and overrides
- WHEN the local database is rebuilt
- THEN SQLite SHALL be populated from that effective data
- AND the rebuild MUST preserve English reference text and effective manual corrections in the projection
- AND the rebuild MUST NOT redefine SQLite as the canonical source of truth

### Requirement: Offline Execution

The import flow MUST run fully offline.

#### Scenario: Import runs without internet access

- GIVEN the machine has no internet connectivity
- WHEN the user imports or rebuilds local spell data
- THEN the process MUST complete using only local files and local storage
- AND it MUST NOT require external APIs, remote databases, or online translation services

## Non-goals

- Defining the spell domain invariants in detail
- Defining manual override authoring semantics beyond applying the override result
- Defining search queries, ranking, or English-text search
- Defining REST endpoints or frontend workflows
- Defining non-MVP synchronization or cloud replication

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\overrides\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-domain\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`

## Assumptions

- The generated dataset is a local, versioned artifact that may be regenerated outside the MVP runtime.
- Overrides remain locally available even when some generated spell ids disappear temporarily.
- Rebuild/import may be re-run multiple times to refresh the SQLite projection.

## Open Questions

- Should minimal validation fail fast on the first malformed record, or continue importing valid records while collecting errors?
- How should orphan override warnings be surfaced to the user in the MVP: logs only, import report, or visible UI notification?
