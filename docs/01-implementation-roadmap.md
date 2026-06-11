# 02 - Implementation Roadmap

## Purpose

This document defines the recommended implementation order for the Grimorio Pathfinder MVP and makes the blocking relationships between the OpenSpec documents explicit.

The goal is to prevent implementation from starting in layers that depend on unresolved domain, persistence, or contract decisions.

## Principles

- Close the core domain first.
- Close override and rebuild semantics before import or edit work.
- Close the local read contract before UI work.
- Validate the end-to-end MVP only after the lower layers are stable.
- Prefer small, dependency-safe increments over parallel work that would create contradictions.

## Implementation Phases

### Phase 0 - Architecture Baseline

**Primary spec**

- `openspec/specs/project-architecture/spec.md`

**Purpose**

Establish the layer boundaries and dependency rules before any feature work begins.

**Blocks**

- Everything else in the MVP implementation.

**Can run in parallel with**

- Nothing meaningful; this should be treated as the architectural baseline.

---

### Phase 1 - Domain and Override Semantics

**Primary specs**

- `openspec/specs/spell-domain/spec.md`
- `openspec/specs/overrides/spec.md`

**Recommended order**

1. `spell-domain`
2. `overrides`

**Why this order**

The domain must define stable spell identity, list membership, and translation-state vocabulary before override persistence can be specified safely. Overrides then define how manual edits, `personalNotes`, and `translationStatus` are stored and prioritized.

**Blocks**

- `dataset-import`
- `spell-editing`
- `mvp-flow-validation`
- Most of the data-shaping part of `api-rest`

**Can run in parallel with**

- Limited UI exploration only, but no contract or persistence implementation should start yet.

---

### Phase 2 - Local Data Composition and Rebuild

**Primary spec**

- `openspec/specs/dataset-import/spec.md`

**Purpose**

Define how generated Spanish data and overrides become the effective local projection in SQLite.

**Blocks**

- `api-rest`
- `spell-search-navigation`
- `spell-detail`
- `spell-editing` in practice, because edits must persist into the same override model
- `frontend-ui`
- `mvp-flow-validation`

**Can run in parallel with**

- Domain cleanup tasks that do not change the already-approved model.

**Notes**

This phase must come after both `spell-domain` and `overrides`, otherwise the import/rebuild rules would be underspecified.

---

### Phase 3 - Local Read Contract

**Primary specs**

- `openspec/specs/api-rest/spec.md`
- `openspec/specs/spell-search-navigation/spec.md`
- `openspec/specs/spell-detail/spec.md`

**Recommended order**

1. `api-rest`
2. `spell-search-navigation`
3. `spell-detail`

**Why this order**

The API contract establishes the transport boundary first. Search and detail then refine how the frontend reads the local projection.

**Blocks**

- `frontend-ui`
- `mvp-flow-validation`

**Can run in parallel with**

- `spell-editing` only if its persistence rules are already closed by `overrides` and its read-model assumptions are not still changing.

**Notes**

- `api-rest` should remain functional and path-agnostic until `docs/10-api-rest.md` freezes the final REST routes.
- `spell-search-navigation` and `spell-detail` should not introduce English search behavior.

---

### Phase 4 - Manual Editing

**Primary spec**

- `openspec/specs/spell-editing/spec.md`

**Purpose**

Define how manual field edits, `personalNotes`, and translation-state updates are persisted as overrides and survive reimport.

**Blocks**

- `frontend-ui` edit interactions
- `mvp-flow-validation`

**Can run in parallel with**

- Final read-only UI work once the API contract is stable.

---

### Phase 5 - Frontend MVP UI

**Primary spec**

- `openspec/specs/frontend-ui/spec.md`

**Purpose**

Implement the tablet-friendly, dark-mode, offline-first search, results, and detail UI.

**Blocks**

- `mvp-flow-validation`

**Can run in parallel with**

- Small backend polishing tasks that do not change the established contracts.

**Notes**

The UI should only be implemented after the API and read-model expectations are stable. Otherwise the frontend will hard-code assumptions that later have to be undone.

---

### Phase 6 - End-to-End MVP Validation

**Primary spec**

- `openspec/specs/mvp-flow-validation/spec.md`

**Purpose**

Validate the full product flow from generated dataset and overrides through SQLite, search, detail, editing, and reimport.

**Blocks**

- Nothing downstream; this is the closing gate.

**Blocked by**

- `spell-domain`
- `overrides`
- `dataset-import`
- `api-rest`
- `spell-search-navigation`
- `spell-detail`
- `spell-editing`
- `frontend-ui`

---

## Dependency Matrix

| Spec | Depends on | Directly blocks |
| --- | --- | --- |
| `project-architecture` | None | Everything else |
| `spell-domain` | `project-architecture` | `overrides`, `dataset-import`, `api-rest`, `spell-search-navigation`, `spell-detail`, `spell-editing`, `frontend-ui`, `mvp-flow-validation` |
| `overrides` | `spell-domain`, `project-architecture` | `dataset-import`, `spell-editing`, `mvp-flow-validation` |
| `dataset-import` | `spell-domain`, `overrides`, `project-architecture` | `api-rest`, `spell-search-navigation`, `spell-detail`, `spell-editing`, `frontend-ui`, `mvp-flow-validation` |
| `api-rest` | `dataset-import`, `spell-domain`, `project-architecture` | `spell-search-navigation`, `spell-detail`, `frontend-ui`, `mvp-flow-validation` |
| `spell-search-navigation` | `api-rest`, `dataset-import`, `spell-domain`, `project-architecture` | `frontend-ui`, `mvp-flow-validation` |
| `spell-detail` | `api-rest`, `dataset-import`, `spell-domain`, `project-architecture` | `frontend-ui`, `mvp-flow-validation` |
| `spell-editing` | `overrides`, `dataset-import`, `api-rest`, `spell-domain`, `project-architecture` | `frontend-ui` editing flows, `mvp-flow-validation` |
| `frontend-ui` | `api-rest`, `spell-search-navigation`, `spell-detail`, `spell-editing`, `project-architecture` | `mvp-flow-validation` |
| `mvp-flow-validation` | All prior specs | None |

## Parallelization Guidance

The following work can be parallelized safely only after its blockers are closed:

- `spell-search-navigation` and `spell-detail` can be developed in parallel after `api-rest`.
- `spell-editing` can proceed once `overrides` is stable and the read contract is not changing.
- `frontend-ui` can be developed against frozen read contracts, but not before `api-rest`, `spell-search-navigation`, and `spell-detail` are stable.

The following work should not be parallelized early because the risk of rework is too high:

- `spell-domain` with any dependent spec
- `overrides` before `spell-domain`
- `dataset-import` before both `spell-domain` and `overrides`
- `frontend-ui` before the read contract is stable

## Risks

- If override persistence is not closed early, the import and editing flows will diverge.
- If the API is implemented before the read specs are stable, the frontend will likely hard-code unstable assumptions.
- If validation starts before the lower layers are closed, the MVP test suite will become a moving target.

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-domain\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\overrides\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\dataset-import\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\api-rest\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-search-navigation\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-detail\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-editing\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\frontend-ui\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\mvp-flow-validation\spec.md`

## Assumptions

- The OpenSpec files in `openspec/specs/` are the source of truth for implementation sequencing.
- Implementation teams will respect the blocking relationships documented here and avoid starting dependent work too early.
- The final REST route freeze will happen in `docs/10-api-rest.md`, not in this roadmap.

## Open Questions

- Should `spell-editing` be treated as a hard gate before any frontend work, or only before edit-specific UI work?
- Should `spell-search-navigation` and `spell-detail` be implemented sequentially in one slice or as two separate slices after `api-rest`?
- Do we want to track a separate implementation milestone for project scaffolding beyond `project-architecture`?
