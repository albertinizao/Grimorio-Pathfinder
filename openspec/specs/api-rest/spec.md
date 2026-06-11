# API REST Specification

## Purpose

Define the MVP local REST contract that the Vue SPA uses to browse spell lists, search spells, and retrieve spell detail from the offline backend. This spec defines functional resource behavior and transport intent only; it does not freeze final URI naming or a final OpenAPI document.

## Requirements

### Requirement: Local read-oriented REST resources

The MVP API MUST expose local REST resources for spell search, spell detail, available spell-list options, and available levels for a selected spell list.

#### Scenario: Frontend loads search inputs

- GIVEN the frontend is rendering the MVP search screen
- WHEN it needs available filtering options
- THEN the API SHALL provide a resource for available spell lists
- AND it SHALL provide a resource for available levels for a selected `listType` and `listName`

#### Scenario: Frontend loads results and detail

- GIVEN the frontend has a valid list selection
- WHEN the user searches or navigates
- THEN the API SHALL provide a spell search resource
- AND it SHALL provide a spell detail resource addressable by a stable spell identifier

### Requirement: Search request semantics

The search resource MUST support the MVP functional inputs `listType`, `listName`, `maxLevel`, and optional `q`.

#### Scenario: Main MVP query

- GIVEN the user selects a spell list and a maximum level
- WHEN the frontend requests search results with `listType`, `listName`, `maxLevel`, and `q`
- THEN the backend SHALL interpret `maxLevel` as inclusive
- AND `q` MAY be absent or empty to support browsing without search text

#### Scenario: Invalid search input

- GIVEN a request omits a required list selection or provides an invalid level value
- WHEN the API validates the request
- THEN it MUST reject the request with a client error response
- AND the response SHOULD identify which input was invalid

### Requirement: Search response transport contract

The search resource MUST return a transport shape suitable for the compact results list without exposing domain internals directly.

#### Scenario: Result summary payload

- GIVEN a search request succeeds
- WHEN the API returns result items
- THEN each item SHALL include at least a stable spell identifier, the effective Spanish spell name, the level for the selected spell list, the translation status, and compact summary fields needed by the MVP results screen
- AND the payload SHOULD include a match fragment or summary snippet derived from Spanish effective content when available

### Requirement: Detail response transport contract

The detail resource MUST return a complete spell detail payload for one spell and SHALL align with `openspec/specs/spell-detail/spec.md`.

#### Scenario: Spell detail retrieval

- GIVEN the frontend requests a spell by stable identifier
- WHEN the spell exists
- THEN the API SHALL return one complete detail payload
- AND the payload MUST preserve English reference text as reference data rather than search input

#### Scenario: Missing spell

- GIVEN the frontend requests a spell identifier that does not exist
- WHEN the API resolves the request
- THEN it MUST return a not-found response
- AND it MUST NOT fabricate fallback content

### Requirement: Lookup resource semantics

The REST contract MUST expose lookup data needed to build the MVP search UI without hardcoding spell-list metadata in the frontend.

#### Scenario: Available spell lists

- GIVEN the frontend needs selectable spell-list values
- WHEN it requests the spell-list lookup resource
- THEN the API SHALL return entries that identify at least `listType` and `listName`
- AND the contract SHOULD remain compatible with class spell lists and future special spell lists

#### Scenario: Available levels by list

- GIVEN the frontend has selected one spell list
- WHEN it requests available levels for that selection
- THEN the API SHALL return the effective level values available for that list
- AND the response MUST reflect imported local data rather than hardcoded rules

### Requirement: Error and boundary behavior

The REST contract MUST remain local, offline, and application-facing.

#### Scenario: Offline usage

- GIVEN the application is running without internet access
- WHEN the frontend calls the local API
- THEN the API MUST remain usable for lookup, search, and detail retrieval
- AND it MUST NOT depend on remote services or authentication for MVP read operations

#### Scenario: DTO boundary protection

- GIVEN a controller returns data to the frontend
- WHEN that data crosses the web boundary
- THEN it SHALL use DTOs or equivalent transport models
- AND it MUST NOT expose persistence entities or domain objects directly as the public contract

## Non-goals

- Freezing final URI paths, query parameter spelling, or a final OpenAPI schema
- Defining mutation endpoints for editing Spanish fields, notes, or translation state
- Defining authentication, authorization, or multi-user behavior
- Public internet API concerns such as rate limiting, external consumers, or API versioning strategy beyond MVP local use

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-search-navigation\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\spell-detail\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`

## Assumptions

- The backend runs locally inside the offline application boundary.
- The Spanish generated dataset and overrides have already been imported into the reconstructible SQLite projection before search and detail requests are served.
- The functional REST contract in this spec intentionally precedes any final route freeze in `docs/10-api-rest.md`.

## Open Questions

- Which exact URI structure should `docs/10-api-rest.md` standardize for the four MVP resources?
- Should client error payloads use a single shared envelope across all local endpoints, or lightweight per-endpoint validation messages?
