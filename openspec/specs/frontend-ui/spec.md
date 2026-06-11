# Frontend UI Specification

## Purpose

Define MVP UI behavior for Grimorio Pathfinder. This spec covers tablet-friendly search, compact results, readable spell detail, dark mode, and offline/local-first expectations. It defines UI behavior only and does not define persistence rules or backend architecture.

## Requirements

### Requirement: Tablet-Friendly Search Workspace

The frontend MUST provide a search workspace that is comfortable on Android tablet-sized screens and uses canonical product terminology.

#### Scenario: Main search controls are directly accessible on tablet

- GIVEN the user opens the main search screen on a tablet-oriented viewport
- WHEN the initial UI is rendered
- THEN controls for `lista de clase lanzadora`, maximum level, and optional search text SHALL be directly accessible without desktop-only interaction patterns
- AND primary actions SHOULD use touch-friendly sizing and spacing

#### Scenario: Canonical terminology is used in user-facing labels

- GIVEN the UI renders spell list filtering labels
- WHEN the filter refers to class-based spell lists
- THEN it SHALL use the term `lista de clase lanzadora`
- AND when it refers to the broader concept it SHOULD use `lista de conjuros`

### Requirement: Dark Mode from the Start

The MVP UI MUST support dark mode from initial delivery and apply it consistently across the primary workflow.

#### Scenario: Dark mode is available on first use

- GIVEN the user launches the MVP for the first time
- WHEN search, results, or detail views are shown
- THEN dark mode SHALL be available immediately
- AND content contrast SHOULD remain readable for table use in low-light conditions

### Requirement: Compact Search Results for Table Use

The results view MUST help the user compare spells quickly without opening every detail screen.

#### Scenario: Result cards or rows expose essential summary data

- GIVEN the user runs a search or browses without a text term
- WHEN results are displayed
- THEN each result SHALL show the Spanish spell name, level in the selected list context, school, descriptors, casting time, range, saving throw, spell resistance, translation status, and a short snippet
- AND the layout SHOULD remain compact enough for multi-result scanning on a tablet

#### Scenario: Browsing works without a text query

- GIVEN the user selects a `lista de clase lanzadora` and maximum level without entering search text
- WHEN the results view is requested
- THEN the UI SHALL support browsing the matching spell set without requiring a text query

### Requirement: Readable Detail with Secondary English Reference

The detail view MUST prioritize effective Spanish content while preserving English text as reference.

#### Scenario: Detail view keeps Spanish content primary

- GIVEN the user opens a spell detail view
- WHEN the spell is rendered
- THEN effective Spanish fields SHALL be shown as the primary reading surface
- AND English reference text SHALL be presented as collapsible or otherwise secondary content
- AND `personalNotes`, `translationStatus`, and field-by-field edit entry points SHALL be accessible from the detail flow

### Requirement: Local-First UI Expectations

The frontend MUST behave as an offline/local-first client for the MVP.

#### Scenario: Core workflow remains usable without internet

- GIVEN the device has no internet connection
- WHEN the local application stack and local data are available
- THEN search, results, detail, and local edit flows SHALL remain usable
- AND the UI MUST NOT require cloud sign-in, remote search, or hosted synchronization to complete the MVP workflow

## Non-goals

- Define override file structure or projection rebuild behavior
- Define API route contracts or DTO shapes
- Define search normalization algorithms or ranking rules
- Define post-MVP features such as favorites, tags, or full PWA behavior

## Dependencies / References

- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\openspec\specs\project-architecture\spec.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\AGENTS.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\README.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\00-vision-producto.md`
- `C:\Users\Alberto\workspace\Grimorio-Pathfinder\docs\01-roadmap.md`

## Assumptions

- A local backend API is available to the SPA for search, detail, and edit operations.
- Android tablets are the primary MVP device target, even though the SPA may also run on desktop.
- Translation status values and editable spell fields are defined by other specs and domain artifacts.

## Open Questions

- Should the tablet MVP use a single-pane flow only, or switch to a split-view layout on wider tablet screens?
- Should field-by-field editing happen inline inside the detail view, or through a dedicated edit mode per field group?
- Is dark mode the only MVP theme, or does the product require a user-visible theme toggle from day one?
