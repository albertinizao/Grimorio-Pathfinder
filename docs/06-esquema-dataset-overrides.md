# 06 - Esquema de dataset y overrides

## Propósito

Este documento fija el contrato técnico real que usa la implementación actual para:

- `data/generated/spells-es.generated.json`
- `data/overrides/spells-es.overrides.json`
- `data/local/grimorio.sqlite`

La fuente canónica de verdad para edición manual es el archivo de overrides. SQLite es solo una proyección local reconstruible.

## Visión general del flujo

```text
data/raw/Hechizos-1.json
        +
data/raw/Hechizos-2.json
        +
data/raw/Hechizos-3.json
        ↓
data/generated/spells-es.generated.json
        +
data/overrides/spells-es.overrides.json
        ↓
importador local
        ↓
data/local/grimorio.sqlite
```

## `spells-es.generated.json`

### Forma actual

```json
{
  "version": 1,
  "generatedAt": "2026-06-15T10:15:19Z",
  "sourceName": "Hechizos-1.json + Hechizos-2.json + Hechizos-3.json",
  "spells": []
}
```

### Campos raíz

| Campo | Tipo | Obligatorio | Nota |
|---|---:|---:|---|
| `version` | integer | sí | Versión de esquema. |
| `generatedAt` | string ISO-8601 | sí | Fecha de generación. |
| `sourceName` | string | sí | Procedencia del lote generado. |
| `spells` | array | sí | Lista de conjuros. |

### Forma de cada conjuro generado

La implementación actual persiste estos campos:

- `id`
- `slug`
- `sourceId`
- `sourceHash`
- `nameEs`
- `nameEn`
- `school`
- `subschool`
- `descriptors`
- `castingTime`
- `components`
- `range`
- `target`
- `effect`
- `area`
- `duration`
- `savingThrow`
- `spellResistance`
- `descriptionEs`
- `descriptionEn`
- `sourceBook`
- `sourcePage`
- `sourceName`
- `translationStatus`
- `lists`
- `personalNotes`
- `createdAt`
- `updatedAt`

Notas prácticas:

- `nameEn` y `descriptionEn` se conservan como referencia.
- `personalNotes` puede viajar vacía o auxiliar en el dataset generado, pero las notas del usuario siguen siendo canónicas en overrides.
- `descriptors` se guarda como array.
- `lists` se guarda como array de `SpellListEntry`.

### `SpellListEntry`

```json
{
  "spellId": "neutralize-poison",
  "listType": "CLASS",
  "listName": "Clérigo",
  "level": 4
}
```

`level` debe ser `>= 0`.

## `spells-es.overrides.json`

### Forma actual

```json
{
  "version": 1,
  "updatedAt": "2026-06-15T12:05:34.843548500Z",
  "spells": {}
}
```

### Campos raíz

| Campo | Tipo | Obligatorio | Nota |
|---|---:|---:|---|
| `version` | integer | sí | Versión de esquema. |
| `updatedAt` | string ISO-8601 | sí | Última escritura del archivo. |
| `spells` | object | sí | Mapa por `spellId`. |

### Forma de cada override

```json
{
  "fields": {
    "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
    "translationStatus": "MANUALLY_EDITED"
  },
  "personalNotes": "Muy útil para llevar preparado si esperamos venenos o drow.",
  "updatedAt": "2026-06-15T12:05:25.633880500Z",
  "reason": "Corrección de estilo de mesa"
}
```

### Campos por override

| Campo | Tipo | Obligatorio | Nota |
|---|---:|---:|---|
| `fields` | object/null | no | Cambios sobre campos editables. |
| `personalNotes` | string/null | no | Notas personales canónicas. |
| `updatedAt` | string ISO-8601 | sí | Última edición del override. |
| `reason` | string/null | no | Motivo libre. |

### Campos editables dentro de `fields`

- `nameEs`
- `school`
- `subschool`
- `descriptors`
- `castingTime`
- `components`
- `range`
- `target`
- `effect`
- `area`
- `duration`
- `savingThrow`
- `spellResistance`
- `descriptionEs`
- `translationStatus`

No se editan mediante overrides en el MVP:

- `nameEn`
- `descriptionEn`
- `id`
- `slug`
- `sourceId`
- `sourceHash`
- `sourceBook`
- `sourcePage`
- `sourceName`
- `lists`

## Reglas efectivas de composición

### Prioridad

```text
override.fields presente
  >
dataset generado
  >
valor vacío o nulo
```

Para `personalNotes`:

```text
override.personalNotes presente
  >
string vacía efectiva
```

### `translationStatus`

Valores permitidos:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

Reglas actuales:

- editar un campo traducido pone `MANUALLY_EDITED` salvo que se establezca otro estado explícitamente;
- editar solo `personalNotes` no cambia el estado;
- `LOCKED` se expresa solo con `fields.translationStatus = "LOCKED"`;
- no existe un flag `locked` separado.

### `LOCKED`

Cuando un conjuro queda `LOCKED`, la implementación materializa en el override los campos traducidos actuales para que no se pierdan en una regeneración posterior.

### Importación y warnings

La implementación actual:

- valida versión y estructura mínima;
- aplica overrides sobre el dataset generado;
- reporta overrides huérfanos como advertencia;
- ignora campos de override no permitidos;
- reconstruye SQLite desde los datos efectivos.

## Reglas de implementación relevantes

- `spells-es.generated.json` es regenerable.
- `spells-es.overrides.json` es la fuente manual canónica.
- SQLite no es la fuente de verdad.
- `search_text` en SQLite es un auxiliar interno; no forma parte del contrato JSON.
- Las notas del usuario deben sobrevivir a reimportaciones y rebuilds.
- El texto inglés original debe mantenerse en el dataset generado y en el detalle efectivo.
