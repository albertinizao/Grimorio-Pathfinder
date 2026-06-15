# 09 - Diagramas de flujo y secuencia

## Propósito

Este documento resume el flujo real de la aplicación con diagramas compactos para lectura rápida.

## Flujo general del producto

```mermaid
flowchart TD
  A[Archivos raw versionados
Hechizos-1/2/3] --> B[Dataset generado
spells-es.generated.json]
  C[Overrides manuales
spells-es.overrides.json] --> D[Importador local]
  B --> D
  D --> E[SQLite local
data/local/grimorio.sqlite]
  E --> F[API REST local
/api]
  F --> G[Frontend Vue]
  G --> H[Uso en mesa
o preparación]
```

## Secuencia: importación y reconstrucción

```mermaid
sequenceDiagram
  participant Src as Raw + Generated
  participant Ov as Overrides
  participant Imp as Importador
  participant DB as SQLite

  Src->>Imp: Leer dataset generado
  Ov->>Imp: Leer overrides
  Imp->>Imp: Validar versión y campos
  Imp->>Imp: Combinar generated + overrides
  Imp->>DB: Rebuild proyección local
  DB-->>Imp: Proyección lista
```

## Secuencia: búsqueda principal

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as API REST
  participant Cat as SpellCatalogService
  participant DB as SQLite

  UI->>API: GET /api/spells/search?listType=CLASS&listName=Clérigo&maxLevel=3&q=veneno
  API->>Cat: searchSpells(...)
  Cat->>DB: findCandidates(listType, listName, levelMode)
  DB-->>Cat: Candidatos de lista y nivel
  Cat->>Cat: Normalizar q y campos efectivos
  Cat->>Cat: Aplicar ranking y snippet
  Cat-->>API: SpellSearchResponseDto
  API-->>UI: JSON de resultados
```

## Secuencia: edición de campos o notas

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as API REST
  participant Cat as SpellCatalogService
  participant Ov as Overrides JSON
  participant DB as SQLite

  UI->>API: PATCH /api/spells/{spellId}/fields|notes|translation-status
  API->>Cat: aplicar cambio
  Cat->>Ov: leer-modificar-escribir overrides
  Cat->>DB: rebuild proyección
  DB-->>Cat: actualización aplicada
  Cat-->>API: SpellDetailResponseDto actualizado
  API-->>UI: detalle refrescado
```

## Secuencia: detalle de conjuro

```mermaid
sequenceDiagram
  participant UI as Frontend Vue
  participant API as API REST
  participant Cat as SpellCatalogService
  participant DB as SQLite

  UI->>API: GET /api/spells/{spellId}
  API->>Cat: getSpellDetail(spellId)
  Cat->>DB: findSpellById(spellId)
  DB-->>Cat: Spell efectivo
  Cat-->>API: SpellDetailResponseDto
  API-->>UI: JSON de detalle
```

## Lectura rápida

- La fuente canónica vive en archivos versionados.
- SQLite es una proyección reconstruible.
- La API local no depende de internet.
- El frontend consume la API y no recalcula reglas de negocio.
