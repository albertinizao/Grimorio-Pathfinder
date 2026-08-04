# 07 - Contrato API REST

## Propósito

Este documento fija el contrato REST real expuesto por la implementación actual de **Grimorio Pathfinder**.

Base URL:

```text
/api
```

La API es local, sin autenticación, pensada para consumo del frontend Vue y para uso offline sobre la proyección MariaDB reconstruida desde los archivos versionados.

## Resumen del contrato actual

Endpoints implementados:

- `GET /api/spell-lists`
- `GET /api/spell-lists/levels`
- `GET /api/spells/search`
- `GET /api/spells/{spellId}`
- `PATCH /api/spells/{spellId}/fields`
- `PATCH /api/spells/{spellId}/notes`
- `PATCH /api/spells/{spellId}/translation-status`

La API devuelve DTOs Java serializados por Jackson y errores en formato `ProblemDetail` de Spring (`type`, `title`, `status`, `detail`, `instance`).

## `GET /api/spell-lists`

Lista las listas de conjuros disponibles en la proyección local.

Query parameters:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `listType` | no | Filtra por tipo de lista. Si falta o está vacío, devuelve todos los tipos. |

Respuesta `200`:

```json
{
  "items": [
    {
      "listType": "CLASS",
      "listName": "Clérigo",
      "minLevel": 0,
      "maxLevel": 9,
      "levels": [0,1,2,3,4,5,6,7,8,9],
      "spellCount": 312
    }
  ]
}
```

DTO de salida: `SpellListsResponseDto` con `items: SpellListSummaryDto[]`.

Orden actual: `listType` ascendente, `listName` ascendente normalizado.

## `GET /api/spell-lists/levels`

Devuelve los niveles disponibles para una lista concreta.

Query parameters obligatorios:

- `listType`
- `listName`

Respuesta `200`:

```json
{
  "listType": "CLASS",
  "listName": "Clérigo",
  "minLevel": 0,
  "maxLevel": 9,
  "levels": [0,1,2,3,4,5,6,7,8,9]
}
```

Si la lista no existe, la implementación actual responde `422 Unprocessable Entity` con `ProblemDetail`.

## `GET /api/spells/search`

Busca conjuros dentro de una lista concreta y hasta un nivel máximo.

Query parameters:

| Parámetro | Obligatorio | Valores | Descripción |
|---|---:|---|---|
| `listType` | sí | texto | Tipo de lista. En el flujo principal: `CLASS`. |
| `listName` | sí | texto | Nombre exacto de la lista ya normalizada por el importador. |
| `maxLevel` | sí | entero `>= 0` | Nivel máximo inclusivo. |
| `levelMode` | no | `UP_TO` / `EXACT` | Por defecto `UP_TO`. |
| `q` | no | texto | Término, varios términos o frase. Por defecto `""`. |
| `page` | no | entero `>= 0` | Paginación opcional basada en cero. |
| `size` | no | entero `1..200` | Tamaño de página. Si falta o es `<= 0`, no se pagina. |

### Semántica de búsqueda

- `UP_TO`: incluye entradas con nivel `<= maxLevel`.
- `EXACT`: incluye solo entradas con nivel `== maxLevel`.
- `q` vacío devuelve navegación sin filtro textual.
- Si `q` contiene cualquier comilla doble, la implementación actual lo trata como búsqueda de frase tras eliminar todas las comillas.
- Si `q` no contiene comillas, se interpreta como búsqueda por términos con lógica `AND`.
- Los términos de 3 o más caracteres permiten prefijo; los de 1 o 2 caracteres exigen coincidencia exacta.
- La búsqueda solo usa campos españoles efectivos y `personalNotes`.
- No busca en `nameEn` ni `descriptionEn`.

### Respuesta `200`

DTO raíz: `SpellSearchResponseDto`.

```json
{
  "filters": {
    "listType": "CLASS",
    "listName": "Clérigo",
    "maxLevel": 3,
    "levelMode": "UP_TO",
    "q": "veneno"
  },
  "page": {
    "page": 0,
    "size": 20,
    "totalItems": 3,
    "totalPages": 1,
    "hasNext": false
  },
  "sort": "LEVEL_ASC_NAME_ES_ASC",
  "results": []
}
```

Cada resultado usa `SpellSearchResultDto` con:

- `spellId`
- `slug`
- `nameEs`
- `selectedList`
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
- `snippet`
- `matchSource`
- `hasPersonalNotes`

### Orden actual de resultados

La implementación ordena por:

1. rank de coincidencia: `nameEs` primero, otros campos después, `descriptionEs` al final;
2. nivel de la lista seleccionada ascendente;
3. `nameEs` normalizado ascendente;
4. `spellId` ascendente.

## `GET /api/spells/{spellId}`

Devuelve el detalle efectivo de un conjuro.

Respuesta `200`: `SpellDetailResponseDto`.

Campos principales del detalle:

- `spellId`
- `slug`
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
- `personalNotes`
- `translationStatus`
- `lists`
- `source`
- `editableFields`
- `updatedAt`
- `reviewedAt`

La API no devuelve un campo `locked` separado.

## `PATCH /api/spells/{spellId}/fields`

Actualiza uno o varios campos españoles editables.

Request DTO: `UpdateSpellFieldsRequestDto`.

```json
{
  "fields": {
    "descriptionEs": "Neutralizas cualquier tipo de veneno.",
    "savingThrow": "Voluntad niega"
  },
  "expectedUpdatedAt": "2026-06-11T10:30:00Z",
  "reason": "Corrección de mesa"
}
```

Reglas:

- solo acepta los campos editables permitidos;
- no acepta `personalNotes`;
- no acepta `translationStatus`;
- conserva `nameEn` y `descriptionEn`;
- si el conjuro no está `LOCKED`, el estado pasa a `MANUALLY_EDITED`;
- si está `LOCKED`, el estado se mantiene `LOCKED`.

## `PATCH /api/spells/{spellId}/notes`

Actualiza `personalNotes`.

Request DTO: `UpdatePersonalNotesRequestDto`.

```json
{
  "personalNotes": "Preparar si esperamos drow.",
  "expectedUpdatedAt": "2026-06-11T10:30:00Z"
}
```

Reglas:

- `personalNotes` es obligatorio en el cuerpo;
- puede ser `""`;
- no modifica campos españoles;
- no modifica texto inglés;
- no modifica `translationStatus`.

## `PATCH /api/spells/{spellId}/translation-status`

Actualiza el estado de traducción.

Request DTO: `UpdateTranslationStatusRequestDto`.

```json
{
  "translationStatus": "REVIEWED",
  "expectedUpdatedAt": "2026-06-11T10:30:00Z",
  "reason": "Revisado para mesa"
}
```

Reglas:

- acepta solo los estados permitidos;
- el bloqueo se expresa solo con `translationStatus = "LOCKED"`;
- no crea un booleano `locked`;
- no modifica notas ni texto inglés;
- al pasar a `LOCKED`, la implementación materializa los campos españoles editables actuales en el override.

## Errores HTTP

La implementación actual usa `ProblemDetail` y estos códigos:

- `400 Bad Request`: JSON inválido, parámetro obligatorio ausente o tipo incorrecto.
- `404 Not Found`: `spellId` inexistente.
- `409 Conflict`: `expectedUpdatedAt` no coincide.
- `422 Unprocessable Entity`: valor fuera de contrato, lista inexistente, estado no permitido o campo no editable.

## Relación con el frontend

El frontend actual consume estos recursos vía proxy local de Vite:

- `GET /api/spell-lists`
- `GET /api/spell-lists/levels`
- `GET /api/spells/search`
- `GET /api/spells/{spellId}`
- `PATCH /api/spells/{spellId}/fields`
- `PATCH /api/spells/{spellId}/notes`
- `PATCH /api/spells/{spellId}/translation-status`

No debe reconstruir el conjuro efectivo por su cuenta ni escribir overrides directamente.

