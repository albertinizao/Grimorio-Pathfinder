# Spell Detail Specification

## Purpose

Define the MVP spell detail presentation contract for one spell, including complete effective Spanish content, English reference content, spell-list memberships, personal notes, and translation state visibility.

## Requirements

### Requirement: Stable spell detail retrieval

The system MUST provide spell detail for a single spell using a stable identifier returned by search or other local navigation flows.

#### Scenario: Open detail from results

- GIVEN the user selects a spell from search results
- WHEN the frontend requests the spell detail by stable identifier
- THEN the backend SHALL return exactly one spell detail payload
- AND the payload MUST correspond to the same stable spell identity used across imports and overrides

### Requirement: Complete effective Spanish detail

The detail payload MUST expose the effective Spanish spell content needed for MVP consultation in one place.

#### Scenario: Primary spell detail content

- GIVEN a spell detail response is returned
- WHEN the frontend renders the primary detail view
- THEN it SHALL include at least the effective Spanish name, school, subschool, descriptors, casting time, components, range, target, effect, area, duration, saving throw, spell resistance, Spanish description, source or book, translation status, personal notes, and timestamps when available
- AND the Spanish effective content MUST reflect overrides already applied over generated data

#### Scenario: Optional fields with no value

- GIVEN some optional spell fields are empty for a spell
- WHEN detail is returned
- THEN the contract SHALL represent those fields consistently as omitted or empty according to the final DTO decision
- AND it MUST NOT replace missing Spanish values with English text as primary content

### Requirement: English reference as secondary content

The MVP detail view MUST preserve English reference text while keeping Spanish content primary.

#### Scenario: Secondary English reference

- GIVEN the spell has English reference fields
- WHEN the detail view is shown
- THEN English name and English description SHALL be available as secondary reference content
- AND they SHOULD be presented in a collapsible, foldable, or otherwise visually secondary section
- AND they MUST NOT become searchable MVP content

### Requirement: Spell-list membership visibility

The detail payload MUST expose spell-list membership information needed to understand how the spell is reached from search.

#### Scenario: Membership display

- GIVEN a spell belongs to one or more spell lists
- WHEN detail is retrieved
- THEN the payload SHALL expose spell-list entries with at least `listType`, `listName`, and `level`
- AND the frontend MUST be able to show the selected membership context without losing the broader spell-list memberships

### Requirement: Personal notes visibility

Personal notes MUST be visible in detail as effective user content.

#### Scenario: Show personal notes

- GIVEN a spell has `personalNotes`
- WHEN the detail view is rendered
- THEN the notes SHALL be displayed as part of the spell detail
- AND they MUST reflect the effective override-backed value used by MVP search

### Requirement: Translation state visibility

The spell detail MUST make translation and review status explicit.

#### Scenario: Show translation status

- GIVEN a spell detail response is rendered
- WHEN the user inspects the spell
- THEN the current `translationStatus` SHALL be visible
- AND statuses such as `REVIEW_REQUIRED`, `MANUALLY_EDITED`, and `LOCKED` MUST remain distinguishable

#### Scenario: LOCKED has no extra flag

- GIVEN a spell is protected from automatic overwrite
- WHEN the detail payload represents that state
- THEN it SHALL do so only through `translationStatus = LOCKED`
- AND it MUST NOT introduce a separate technical `locked` flag for MVP

## Non-goals

- Defining field-by-field edit commands or mutation payloads
- Defining how future favorites, tags, or character preparation data appear in detail
- Replacing Spanish primary content with English fallback behavior
- Closing final visual layout details beyond the requirement that English reference remain secondary/collapsible

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\api-rest\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`

## Assumptions

- Search and other navigation flows pass a stable spell identifier into the detail request.
- The English text is preserved in the dataset and remains available locally even though MVP search ignores it.
- Edit workflows will be specified separately; this spec only defines what the read/detail contract must expose.

## Open Questions

- Should timestamps be mandatory in the external detail DTO from MVP day one, or only exposed once edit flows are implemented?
- Should the default UI state for the English reference section be collapsed on tablet layouts, or is that purely a frontend presentation decision?
