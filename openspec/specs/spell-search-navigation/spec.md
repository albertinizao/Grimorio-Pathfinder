# Spell Search and Navigation Specification

## Purpose

Define the MVP behavior for spell search and navigation across `listType`, `listName`, `maxLevel`, and optional search text. This spec covers search scope, normalization, ordering, and browse behavior; it does not define final URI paths.

## Requirements

### Requirement: Spell-list scoped navigation

Search and navigation MUST be scoped by spell-list membership using `listType` and `listName`.

#### Scenario: Class spell-list search

- GIVEN the user selects a class spell list
- WHEN the search executes
- THEN the backend SHALL filter spells by the selected `listType` and `listName`
- AND the returned level for each result MUST correspond to that selected spell-list membership

#### Scenario: Future-compatible spell-list model

- GIVEN a spell may belong to non-class spell lists in the future
- WHEN the same search contract is reused
- THEN the search model SHALL remain compatible with other `listType` values
- AND it MUST NOT assume that every spell list is a `CLASS`

### Requirement: Inclusive maximum level filtering

The MVP search MUST filter by an inclusive maximum level.

#### Scenario: Maximum level applied

- GIVEN the user selects `maxLevel = 3`
- WHEN the search runs for one spell list
- THEN only spells with that selected spell-list level from `0` through `3` SHALL be returned
- AND spells above level `3` for that same spell list MUST be excluded

### Requirement: Optional term or phrase input

The MVP search MUST support both text search and navigation without text.

#### Scenario: Browsing without search text

- GIVEN the user selected `listType`, `listName`, and `maxLevel`
- WHEN `q` is absent, empty, or whitespace-only
- THEN the backend SHALL return browse results constrained only by spell-list membership and maximum level
- AND the user MUST still be able to navigate spells without typing a term

#### Scenario: Literal term or phrase search

- GIVEN the user provides `q`
- WHEN the search executes
- THEN the backend SHALL treat `q` as a normalized term or phrase match over searchable MVP fields
- AND it MUST NOT require stemming, synonym expansion, or fuzzy matching for MVP

### Requirement: Spanish-only effective search scope

The MVP search MUST operate only on effective Spanish content and personal notes.

#### Scenario: Searchable fields

- GIVEN a spell has effective Spanish data after overrides are applied
- WHEN text search executes
- THEN the search scope SHALL include Spanish name, Spanish description, school, subschool, descriptors, casting time, components, range, target, effect, area, duration, saving throw, spell resistance, and `personalNotes`
- AND it MUST use the effective post-override values

#### Scenario: English reference excluded from search

- GIVEN a spell preserves English reference text
- WHEN the user searches with `q`
- THEN English name and English description MUST NOT influence matches
- AND a spell MUST NOT be returned only because the term appears in English reference fields

### Requirement: Normalized MVP matching

The MVP search MUST normalize text consistently for common table-use queries.

#### Scenario: Accent-insensitive match

- GIVEN a stored value contains accented Spanish text
- WHEN the user searches with the same word without accents
- THEN the spell SHALL still match
- AND the normalization MUST ignore case differences

#### Scenario: Basic spacing and punctuation normalization

- GIVEN the same phrase may appear with repeated spaces or basic punctuation differences
- WHEN the user searches for that phrase
- THEN the search SHOULD normalize internal whitespace
- AND it SHOULD tolerate basic punctuation variation needed for MVP usability

### Requirement: Deterministic ordering

Browse and search results MUST use predictable ordering for table use.

#### Scenario: Stable result order

- GIVEN multiple spells match the same request
- WHEN the API returns results
- THEN results SHALL be ordered first by selected spell-list level ascending
- AND then by effective Spanish spell name ascending
- AND ties SHOULD use a stable identifier as a final deterministic tie-breaker

### Requirement: Search result usability

The returned results MUST remain usable for compact browsing and drill-down.

#### Scenario: Compact navigation result

- GIVEN a result item is shown in the search list
- WHEN the frontend renders it
- THEN the item SHALL expose the stable spell identifier and selected spell-list level
- AND it SHOULD expose enough summary data to support a compact table-use result row and navigation to detail

### Requirement: Canonical terminology preservation

Spanish-facing labels derived from this behavior SHOULD preserve the project's canonical domain terminology.

#### Scenario: Terminology in UI copy

- GIVEN Spanish labels are rendered for this flow
- WHEN the UI refers to a class-specific spell list
- THEN it SHOULD use `lista de clase lanzadora`
- AND broader references SHOULD use `lista de conjuros`

## Non-goals

- English-language search, translation lookup, or bilingual ranking
- Fuzzy search, stemming, synonym expansion, or typo tolerance
- Favorites, tags, saved searches, or character-specific spell preparation filters
- Relevance-ranking strategies beyond deterministic MVP ordering

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\api-rest\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`

## Assumptions

- Search runs against the local reconstructible SQLite projection after overrides have already been applied to effective content.
- `listName` values come from imported data and are selected through lookup data rather than free-form user invention.
- The MVP dataset size is small enough for deterministic ordering and normalized local search without remote indexing services.

## Open Questions

- Should the final implementation return every matching result in one response for MVP, or should `docs/10-api-rest.md` later define a lightweight pagination shape?
- How should snippets be generated when the only match comes from `personalNotes`: raw note excerpt, highlighted fragment, or a generic match indicator?
