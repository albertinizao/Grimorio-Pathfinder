# Project Architecture Specification

## Purpose

Define the project structure, layer boundaries, and dependency rules for Grimorio Pathfinder. This spec covers architecture only; it does not define business logic, algorithms, or UI behavior.

## Requirements

### Requirement: Hexagonal Backend Structure

The backend MUST be organized into domain, application, ports, and infrastructure layers.

#### Scenario: Valid layer separation

- GIVEN a backend component is being added
- WHEN the component belongs to business rules
- THEN it SHALL live in domain
- AND it MUST NOT depend on Spring, SQLite, controllers, or filesystem APIs

#### Scenario: Application orchestration

- GIVEN a use case needs implementation
- WHEN the use case coordinates domain objects and ports
- THEN it SHALL live in application
- AND it MUST depend only on domain and ports

### Requirement: Adapter and Delivery Boundaries

The backend MUST expose technical adapters for web, persistence, and file I/O without leaking those concerns into domain or application.

#### Scenario: REST delivery

- GIVEN an HTTP request reaches the backend
- WHEN the request is handled
- THEN controller code SHALL remain in web adapters
- AND it MUST delegate to application services without business rules

#### Scenario: Persistence adapter

- GIVEN data must be stored or read
- WHEN SQLite or JSON is involved
- THEN the implementation SHALL live in infrastructure
- AND it MUST satisfy ports defined by the inner layers

### Requirement: Frontend Isolation

The frontend MUST be a separate Vue SPA that consumes backend APIs only.

#### Scenario: No direct backend internals

- GIVEN the frontend needs data
- WHEN it renders lists, detail, or filters
- THEN it MUST call the local API
- AND it MUST NOT read SQLite or data files directly

### Requirement: Reconstructible Local Data Model

The project MUST treat versioned JSON files as the canonical data source and SQLite as a reconstructible projection.

#### Scenario: Dataset rebuild

- GIVEN generated data and overrides exist
- WHEN the local projection is rebuilt
- THEN the system SHALL derive SQLite from those files
- AND it MUST preserve the English reference text and manual overrides

### Requirement: Offline Local-First Dependency Model

The MVP MUST run without external services, authentication, Docker, or remote database dependencies.

#### Scenario: Offline startup

- GIVEN the machine has no internet access
- WHEN the app starts
- THEN it MUST remain usable for local search and consultation
- AND it MUST NOT require any remote API or hosted service

### Requirement: Project-Level Technology Boundaries

The architecture MUST use Java + Spring Boot for the backend, Vue for the frontend, and SQLite for the local projection.

#### Scenario: Technology choice consistency

- GIVEN new modules are introduced
- WHEN implementation decisions are made
- THEN they SHALL align with the approved stack
- AND they SHOULD preserve future PWA readiness without changing the MVP stack
