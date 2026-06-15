# 09 - Diagramas de arquitectura, flujo y secuencia

## Propósito

Este documento reúne la vista arquitectónica y los diagramas de flujo y secuencia de los casos de uso principales actualmente soportados por **Grimorio Pathfinder**.

## Arquitectura global

```mermaid
flowchart TB
  subgraph Frontend[Frontend Vue]
    UI[App Vue SPA]
    SearchView[Vista de búsqueda]
    DetailView[Vista de detalle]
    EditView[Vista de edición]
  end

  subgraph Web[Web / API REST]
    Controller[SpellApiController]
    Errors[RestExceptionHandler]
    Dtos[SpellApiDtos]
  end

  subgraph App[Application / Servicio]
    Service[SpellCatalogService]
  end

  subgraph Infra[Infrastructure]
    Importer[SpellDatasetImportService]
    JsonRepo[JSON repositories]
    OverrideWriter[SpellOverridesJsonRepository]
    Sqlite[SpellCatalogSqliteRepository]
  end

  subgraph Data[Archivos y proyección]
    Raw[Data raw
Hechizos-1/2/3]
    Generated[spells-es.generated.json]
    Overrides[spells-es.overrides.json]
    LocalDB[data/local/grimorio.sqlite]
  end

  UI --> Controller
  SearchView --> Controller
  DetailView --> Controller
  EditView --> Controller
  Controller --> Service
  Controller --> Errors
  Service --> Importer
  Service --> JsonRepo
  Service --> OverrideWriter
  Service --> Sqlite
  Importer --> Raw
  Importer --> Generated
  Importer --> Overrides
  Sqlite --> LocalDB
```

## Responsabilidades por capa

- **Frontend Vue**: muestra búsqueda, detalle y edición.
- **Web / API**: expone endpoints REST y traduce errores a `ProblemDetail`.
- **Service**: orquesta lectura, búsqueda, detalle y escritura de overrides.
- **Infrastructure**: importa JSON, compone el spell efectivo y reconstruye SQLite.
- **Data**: mantiene la fuente canónica versionada y la proyección local.

## Casos de uso cubiertos

1. Importar y reconstruir dataset
2. Listar listas de conjuros
3. Obtener niveles disponibles de una lista
4. Buscar conjuros
5. Ver detalle de conjuro
6. Editar campos españoles
7. Editar notas personales
8. Cambiar estado de traducción

---

## 1) Importar y reconstruir dataset

### Flujo

```mermaid
flowchart TD
  A[Raw versionado
Hechizos-1/2/3] --> B[Dataset generado
spells-es.generated.json]
  C[Overrides
spells-es.overrides.json] --> D[Importador local]
  B --> D
  D --> E[Spell efectivo]
  E --> F[Rebuild SQLite local]
```

### Secuencia

```mermaid
sequenceDiagram
  participant Raw as Raw files
  participant Gen as Generated JSON
  participant Ov as Overrides JSON
  participant Imp as SpellDatasetImportService
  participant Db as SpellCatalogSqliteRepository

  Raw->>Imp: leer fuentes raw
  Gen->>Imp: leer spells-es.generated.json
  Ov->>Imp: leer spells-es.overrides.json
  Imp->>Imp: validar versión y estructura
  Imp->>Imp: combinar generated + overrides
  Imp->>Db: rebuild(effectiveSpells)
  Db-->>Imp: proyección lista
```

---

## 2) Listar listas de conjuros

### Flujo

```mermaid
flowchart TD
  A[Frontend] --> B[GET /api/spell-lists]
  B --> C[SpellCatalogService.listSpellLists]
  C --> D[SQLite: listSpellLists]
  D --> E[SpellListsResponseDto]
  E --> F[Frontend]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant DB as SpellCatalogSqliteRepository

  UI->>API: GET /api/spell-lists?listType=CLASS
  API->>Service: listSpellLists(listType)
  Service->>DB: listSpellLists(listType)
  DB-->>Service: ListSummary[]
  Service-->>API: SpellListsResponseDto
  API-->>UI: JSON
```

---

## 3) Obtener niveles disponibles de una lista

### Flujo

```mermaid
flowchart TD
  A[Frontend] --> B[GET /api/spell-lists/levels]
  B --> C[SpellCatalogService.getSpellListLevels]
  C --> D[SQLite: getSpellListLevels]
  D --> E{¿Lista existe?}
  E -- sí --> F[SpellListLevelsResponseDto]
  E -- no --> G[422 ProblemDetail]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant DB as SpellCatalogSqliteRepository

  UI->>API: GET /api/spell-lists/levels?listType=CLASS&listName=Clérigo
  API->>Service: getSpellListLevels(listType, listName)
  Service->>DB: getSpellListLevels(listType, listName)
  alt lista encontrada
    DB-->>Service: niveles disponibles
    Service-->>API: SpellListLevelsResponseDto
    API-->>UI: JSON
  else lista inexistente
    DB-->>Service: empty
    Service-->>API: SpellListNotFoundException
    API-->>UI: 422 ProblemDetail
  end
```

---

## 4) Buscar conjuros

### Flujo

```mermaid
flowchart TD
  A[Frontend] --> B[GET /api/spells/search]
  B --> C[SpellCatalogService.searchSpells]
  C --> D[SQLite: candidatos por lista y nivel]
  D --> E[Normalización y match textual]
  E --> F[Ranking y snippet]
  F --> G[SpellSearchResponseDto]
  G --> H[Frontend]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant DB as SpellCatalogSqliteRepository

  UI->>API: GET /api/spells/search?listType=CLASS&listName=Clérigo&maxLevel=3&levelMode=UP_TO&q=veneno
  API->>Service: searchSpells(...)
  Service->>DB: getSpellListLevels(listType, listName)
  Service->>DB: findCandidates(listType, listName, maxLevel, exactLevel)
  DB-->>Service: SearchCandidate[]
  Service->>Service: normalizar q y campos efectivos
  Service->>Service: calcular ranking, snippet y matchSource
  Service-->>API: SpellSearchResponseDto
  API-->>UI: JSON
```

---

## 5) Ver detalle de conjuro

### Flujo

```mermaid
flowchart TD
  A[Frontend] --> B[GET /api/spells/{spellId}]
  B --> C[SpellCatalogService.getSpellDetail]
  C --> D[SQLite: findSpellById]
  D --> E[Detalle efectivo]
  E --> F[SpellDetailResponseDto]
  F --> G[Frontend]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant DB as SpellCatalogSqliteRepository

  UI->>API: GET /api/spells/{spellId}
  API->>Service: getSpellDetail(spellId)
  Service->>DB: findSpellById(spellId)
  DB-->>Service: Spell efectivo
  Service-->>API: SpellDetailResponseDto
  API-->>UI: JSON
```

---

## 6) Editar campos españoles

### Flujo

```mermaid
flowchart TD
  A[Frontend edición] --> B[PATCH /api/spells/{spellId}/fields]
  B --> C[Validación de campos editables]
  C --> D[Leer override actual]
  D --> E[Merge de campos]
  E --> F[Actualizar translationStatus]
  F --> G[Escribir overrides]
  G --> H[Rebuild SQLite]
  H --> I[Detalle actualizado]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant Ov as SpellOverridesJsonRepository
  participant DB as SpellCatalogSqliteRepository

  UI->>API: PATCH /api/spells/{spellId}/fields
  API->>Service: updateSpellFields(spellId, request)
  Service->>Service: validar campos y conflictos
  Service->>Ov: read override file
  Service->>Ov: write updated override entry
  Service->>DB: rebuild(effectiveSpells)
  DB-->>Service: rebuild OK
  Service-->>API: SpellDetailResponseDto actualizado
  API-->>UI: JSON
```

---

## 7) Editar notas personales

### Flujo

```mermaid
flowchart TD
  A[Frontend notas] --> B[PATCH /api/spells/{spellId}/notes]
  B --> C[Validación de personalNotes]
  C --> D[Leer override actual]
  D --> E[Actualizar personalNotes]
  E --> F[Escribir overrides]
  F --> G[Rebuild SQLite]
  G --> H[Detalle actualizado]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant Ov as SpellOverridesJsonRepository
  participant DB as SpellCatalogSqliteRepository

  UI->>API: PATCH /api/spells/{spellId}/notes
  API->>Service: updatePersonalNotes(spellId, request)
  Service->>Service: validar longitud y conflictos
  Service->>Ov: read override file
  Service->>Ov: write updated note entry
  Service->>DB: rebuild(effectiveSpells)
  DB-->>Service: rebuild OK
  Service-->>API: SpellDetailResponseDto actualizado
  API-->>UI: JSON
```

---

## 8) Cambiar estado de traducción

### Flujo

```mermaid
flowchart TD
  A[Frontend estado] --> B[PATCH /api/spells/{spellId}/translation-status]
  B --> C[Validar estado permitido]
  C --> D{¿LOCKED?}
  D -- sí --> E[Materializar campos editables actuales]
  D -- no --> F[Conservar merge de override]
  E --> G[Escribir overrides]
  F --> G
  G --> H[Rebuild SQLite]
  H --> I[Detalle actualizado]
```

### Secuencia

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as SpellApiController
  participant Service as SpellCatalogService
  participant Ov as SpellOverridesJsonRepository
  participant DB as SpellCatalogSqliteRepository

  UI->>API: PATCH /api/spells/{spellId}/translation-status
  API->>Service: updateTranslationStatus(spellId, request)
  Service->>Service: validar estado y conflictos
  alt status = LOCKED
    Service->>Service: materializar campos españoles actuales
  else status != LOCKED
    Service->>Service: merge normal de override
  end
  Service->>Ov: write updated override entry
  Service->>DB: rebuild(effectiveSpells)
  DB-->>Service: rebuild OK
  Service-->>API: SpellDetailResponseDto actualizado
  API-->>UI: JSON
```

---

## 9) Manejo de errores

```mermaid
flowchart TD
  A[Petición REST] --> B{¿JSON / params válidos?}
  B -- no --> C[400 Bad Request]
  B -- sí --> D{¿Recurso existe?}
  D -- no --> E[404 Not Found]
  D -- sí --> F{¿Conflicto expectedUpdatedAt?}
  F -- sí --> G[409 Conflict]
  F -- no --> H{¿Regla de contrato rota?}
  H -- sí --> I[422 Unprocessable Entity]
  H -- no --> J[200 OK]
```

## Resumen

Estos diagramas reflejan el comportamiento actual de la aplicación:

- la fuente canónica vive en archivos versionados;
- SQLite es reconstruible;
- la API local expone lectura y edición;
- el frontend solo consume la API;
- las reglas de negocio viven en el servicio, no en la UI.
