# 02 - Implementation Roadmap

## Propósito

Este documento resume el orden de dependencias que sigue el MVP de Grimorio Pathfinder. Hoy sirve sobre todo como mapa de lectura: las piezas principales ya están implementadas y la prioridad es mantener la coherencia entre documentación y código.

## Orden real de construcción

### 0. Architecture baseline

Referencia: `openspec/specs/project-architecture/spec.md`

Objetivo: fijar capas y límites antes de mezclar lógica.

### 1. Domain + overrides

Referencias:

- `openspec/specs/spell-domain/spec.md`
- `openspec/specs/overrides/spec.md`

Objetivo: identidad estable, listas de conjuros, `translationStatus`, `personalNotes` y reglas de override.

### 2. Dataset + rebuild

Referencia: `openspec/specs/dataset-import/spec.md`

Objetivo: combinar `data/generated/spells-es.generated.json` + `data/overrides/spells-es.overrides.json` y reconstruir SQLite.

### 3. Read contract

Referencias:

- `openspec/specs/api-rest/spec.md`
- `openspec/specs/spell-search-navigation/spec.md`
- `openspec/specs/spell-detail/spec.md`

Objetivo: exponer consulta, búsqueda y detalle sobre la proyección local.

### 4. Editing contract

Referencia: `openspec/specs/spell-editing/spec.md`

Objetivo: persistir edición de campos españoles, notas personales y estado de traducción en overrides.

### 5. Frontend UI

Referencia: `openspec/specs/frontend-ui/spec.md`

Objetivo: UI Vue local, oscura y apta para tablet.

### 6. MVP validation

Referencia: `openspec/specs/mvp-flow-validation/spec.md`

Objetivo: validar el flujo completo desde archivos hasta edición y reimportación.

## Estado práctico del repositorio

La implementación actual ya cubre:

- backend Spring Boot con arquitectura hexagonal básica;
- dataset generado y overrides versionados;
- importación a SQLite;
- API REST local;
- frontend Vue con búsqueda, detalle y edición;
- tests que cubren flujo MVP y fronteras de arquitectura.

## Dependencias clave

- `spell-domain` bloquea `overrides`.
- `overrides` bloquea `dataset-import` y `spell-editing`.
- `dataset-import` bloquea `api-rest` y la capa de lectura.
- `api-rest` bloquea `frontend-ui`.
- `spell-search-navigation` y `spell-detail` dependen de la API de lectura.
- `spell-editing` depende de overrides + importación + API.
- `frontend-ui` depende del contrato de lectura y de edición.
- `mvp-flow-validation` depende de todo lo anterior.

## Contrato de rutas finales

La congelación final del contrato REST está documentada en:

```text
docs/07-contrato-api-rest.md
```
