# 07 - Contrato API REST

## Propósito

Este documento define el contrato REST local del backend de **Grimorio Pathfinder** para consulta, detalle, búsqueda y edición básica de conjuros.

El objetivo es que backend y frontend puedan implementar el mismo contrato sin interpretar de forma distinta:

- qué endpoints existen en el MVP;
- qué parámetros aceptan;
- qué DTOs intercambian;
- qué campos son editables;
- qué errores deben devolver;
- cómo se exponen texto español, texto inglés, notas personales y estado de traducción;
- cómo se relaciona la API con búsqueda, overrides y arquitectura hexagonal.

## Alcance

Este documento cubre únicamente la API REST local del MVP.

Incluye:

- búsqueda/listado de conjuros;
- detalle de conjuro;
- consulta de listas de conjuros disponibles;
- consulta de niveles disponibles para una lista de conjuros;
- actualización de campos españoles editables;
- actualización de notas personales;
- actualización de estado de traducción.

No incluye:

- autenticación;
- usuarios;
- autorización;
- servicios externos;
- endpoints de IA;
- traducción al vuelo;
- gestión de personajes;
- conjuros preparados;
- slots;
- favoritos;
- etiquetas;
- sincronización;
- administración avanzada de datasets;
- endpoints de importación manual desde la UI.

## Estado de las rutas

Las rutas de este documento son una **propuesta cerrada para el MVP**.

Se consideran estables para implementar backend y frontend, pero pueden evolucionar si en el futuro se genera una especificación OpenAPI formal.

Si otros documentos anteriores mencionan `docs/10-api-rest.md` como ubicación futura del contrato REST, esta especificación debe entenderse como el documento que cubre ese contrato dentro de la numeración actual:

```text
docs/07-contrato-api-rest.md
```

## Principios del contrato REST

### 1. API local y offline

La API se expone solo para consumo local por el frontend Vue.

Reglas:

- no requiere conexión a internet;
- no llama a servicios externos;
- no llama a servicios IA;
- no depende de Combat Manager;
- no incluye autenticación en el MVP;
- no debe exponer secretos ni claves externas porque no existen en el MVP.

### 2. El español es el contenido principal

La API muestra y edita principalmente campos españoles.

Reglas:

- los resultados de búsqueda usan contenido español efectivo y notas personales;
- el detalle incluye texto inglés solo como referencia secundaria;
- el texto inglés no se usa para búsqueda en el MVP;
- el texto inglés no es editable desde la API del MVP.

### 3. Los DTOs REST no son entidades de dominio

La API puede usar nombres de campos coherentes con el dataset (`nameEs`, `descriptionEs`, etc.), pero eso no significa que el controlador exponga directamente entidades de dominio.

Reglas:

- los controladores reciben requests y devuelven DTOs;
- los casos de uso trabajan con comandos/resultados de aplicación;
- los mappers transforman entre aplicación y DTO REST;
- el dominio no debe depender de Spring, HTTP, controladores ni JSON.

### 4. Los controladores no contienen lógica de negocio

Un controlador REST debe limitarse a:

1. recibir la petición HTTP;
2. validar estructura básica de entrada;
3. mapear DTO de request a comando de aplicación;
4. invocar un caso de uso;
5. mapear resultado de aplicación a DTO de response;
6. traducir errores conocidos a códigos HTTP.

No debe:

- buscar directamente en SQLite;
- escribir directamente en `spells-es.overrides.json`;
- decidir reglas de prioridad entre generated y overrides;
- decidir cuándo un conjuro pasa a `MANUALLY_EDITED`;
- decidir cómo se materializa un `LOCKED`;
- reconstruir índices;
- contener reglas de normalización de búsqueda.

### 5. La API trabaja con el conjuro efectivo

Toda lectura devuelve datos efectivos:

```text
spell efectivo = dataset generado + overrides aplicados
```

Por tanto:

- si un campo tiene override, se devuelve el valor del override;
- si no tiene override, se devuelve el valor generado;
- `personalNotes` se devuelve desde overrides o como cadena vacía;
- `nameEn` y `descriptionEn` se devuelven desde el dataset generado/fuente original.

### 6. PATCH significa edición parcial

La API usa `PATCH` para cambios parciales.

Reglas:

- editar campos españoles no obliga a reenviar todo el conjuro;
- editar notas no modifica campos españoles;
- editar estado no modifica notas;
- editar estado no modifica texto inglés;
- el backend debe preservar overrides existentes no relacionados con la petición.

### 7. Sin autenticación en MVP

No hay login ni usuario.

Reglas:

- no se documentan respuestas `401` ni `403` como parte esperada del MVP;
- no se documentan cabeceras `Authorization`;
- no se documentan roles ni permisos.

## Base URL

Base propuesta:

```text
/api
```

Ejemplo:

```text
GET /api/spells/search
```

No se introduce `/api/v1` en el MVP para evitar versionado prematuro. Si en el futuro se versiona la API, deberá documentarse una migración explícita.

## Formato general

### Content-Type

Requests con cuerpo:

```http
Content-Type: application/json
```

Responses JSON:

```http
Content-Type: application/json
```

### Fechas

Las fechas se devuelven como `string` ISO-8601 con zona horaria cuando existan.

Ejemplo:

```json
"2026-06-11T10:30:00Z"
```

### Campos nulos

Reglas:

- si un campo existe pero no aplica, puede devolverse como `null`;
- las listas vacías se devuelven como `[]`;
- `personalNotes` se devuelve como `""` cuando no hay notas;
- los DTOs de detalle deben mantener claves estables aunque algunos valores sean `null`.

### Identificadores

`spellId` es el identificador estable del conjuro.

Reglas:

- no depende de SQLite;
- no debe cambiar al reimportar;
- se usa en rutas de detalle y edición;
- debe codificarse correctamente en URL;
- no debe contener `/`.

## Resumen de endpoints

| Caso de uso | Método | Ruta propuesta | Estado |
|---|---:|---|---|
| Búsqueda/listado de conjuros | `GET` | `/api/spells/search` | MVP |
| Detalle de conjuro | `GET` | `/api/spells/{spellId}` | MVP |
| Listas de conjuros disponibles | `GET` | `/api/spell-lists` | MVP |
| Niveles disponibles por lista | `GET` | `/api/spell-lists/levels` | MVP |
| Actualizar campos españoles | `PATCH` | `/api/spells/{spellId}/fields` | MVP |
| Actualizar notas personales | `PATCH` | `/api/spells/{spellId}/notes` | MVP |
| Actualizar estado de traducción | `PATCH` | `/api/spells/{spellId}/translation-status` | MVP |

## Estados de traducción

Valores permitidos en responses y requests de estado:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

Reglas:

- cualquier otro valor recibido en request debe devolver `422`;
- `LOCKED` es solo un valor de `translationStatus`;
- no existe campo booleano `locked` en el contrato REST del MVP;
- el frontend no debe inferir otro bloqueo técnico distinto de `translationStatus = "LOCKED"`.

## Campos editables y no editables

### Campos editables mediante `/fields`

Estos campos españoles pueden editarse:

| Campo | Tipo JSON | Nulo permitido | Regla |
|---|---:|---:|---|
| `nameEs` | string | sí | Nombre español efectivo. |
| `school` | string | sí | Escuela en español. |
| `subschool` | string | sí | Subescuela en español. |
| `descriptors` | array[string] | no | Sustituye la lista completa. Puede ser `[]`. |
| `castingTime` | string | sí | Tiempo de lanzamiento en español. |
| `components` | string | sí | Componentes en español o abreviados. |
| `range` | string | sí | Alcance en español. |
| `target` | string | sí | Objetivo en español. |
| `effect` | string | sí | Efecto en español. |
| `area` | string | sí | Área en español. |
| `duration` | string | sí | Duración en español. |
| `savingThrow` | string | sí | Tirada de salvación en español. |
| `spellResistance` | string | sí | Resistencia a conjuros en español. |
| `descriptionEs` | string | sí | Descripción española. |

### Campos editables mediante endpoints específicos

| Campo | Endpoint | Regla |
|---|---|---|
| `personalNotes` | `/api/spells/{spellId}/notes` | No modifica `translationStatus`. |
| `translationStatus` | `/api/spells/{spellId}/translation-status` | No modifica notas ni texto inglés. |

### Campos no editables en el MVP

No pueden modificarse mediante la API REST del MVP:

- `id`;
- `spellId`;
- `slug`;
- `sourceId`;
- `sourceHash`;
- `nameEn`;
- `descriptionEn`;
- `sourceBook`;
- `sourcePage`;
- `sourceName`;
- `lists`;
- `createdAt`;
- `updatedAt` generado por el backend;
- `reviewedAt` generado por el backend.

Si un request intenta modificar uno de estos campos, debe devolverse `422`.

## DTOs comunes

## `TranslationStatusDto`

Tipo: `string enum`.

Valores:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

## `SpellListEntryDto`

```json
{
  "listType": "CLASS",
  "listName": "Clérigo",
  "level": 4
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `listType` | string | sí | Tipo de lista de conjuros. |
| `listName` | string | sí | Nombre visible de la lista. |
| `level` | integer | sí | Nivel dentro de esa lista. Siempre `>= 0`. |

## `SourceReferenceDto`

```json
{
  "sourceBook": "Core Rulebook",
  "sourcePage": null,
  "sourceName": "spells.csv"
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `sourceBook` | string/null | sí | Libro o fuente si existe. |
| `sourcePage` | integer/null | sí | Página si existe. |
| `sourceName` | string/null | sí | Procedencia adicional si existe. |

`sourceHash` y `sourceId` no se exponen en el MVP salvo que se cree una pantalla técnica de diagnóstico, que queda fuera de alcance.

## `PageDto`

```json
{
  "page": 0,
  "size": 20,
  "totalItems": 2,
  "totalPages": 1,
  "hasNext": false
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `page` | integer | sí | Página solicitada, empezando en `0`. |
| `size` | integer | sí | Tamaño de página efectivo. |
| `totalItems` | integer | sí | Total de resultados antes de paginar. |
| `totalPages` | integer | sí | Total de páginas. |
| `hasNext` | boolean | sí | Indica si hay página siguiente. |

## `ApiErrorResponseDto`

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "La petición contiene valores no válidos.",
    "details": [
      {
        "field": "maxLevel",
        "message": "maxLevel debe ser mayor o igual que 0."
      }
    ]
  }
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `error.code` | string | sí | Código estable para frontend y logs. |
| `error.message` | string | sí | Mensaje legible. |
| `error.details` | array | sí | Puede ser `[]`. |
| `error.details[].field` | string/null | sí | Campo afectado, si aplica. |
| `error.details[].message` | string | sí | Detalle legible. |

Códigos de error recomendados:

```text
BAD_REQUEST
SPELL_NOT_FOUND
VALIDATION_ERROR
CONFLICT
UNPROCESSABLE_ENTITY
```

## Endpoint: búsqueda/listado de conjuros

### Ruta

```http
GET /api/spells/search
```

### Propósito

Devuelve una página de resultados compactos para:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

También cubre navegación sin término de búsqueda usando `q` vacío o ausente.

### Query parameters

| Parámetro | Tipo | Obligatorio | Valor por defecto | Reglas |
|---|---:|---:|---:|---|
| `listType` | string | sí | — | En el flujo principal MVP será `CLASS`. |
| `listName` | string | sí | — | Nombre exacto de lista devuelto por `/api/spell-lists`. |
| `maxLevel` | integer | sí | — | Incluye niveles `<= maxLevel`. Debe ser `>= 0`. |
| `q` | string | no | `""` | Término o frase. Puede estar vacío. Máximo 200 caracteres. |
| `page` | integer | no | `0` | Debe ser `>= 0`. |
| `size` | integer | no | `50` | Debe estar entre `1` y `200`. |

No se define parámetro de ordenación en el MVP. La ordenación es fija.

### Ordenación

Orden obligatorio:

1. `level` ascendente;
2. `nameEs` ascendente normalizado, ignorando mayúsculas y acentos.

### Búsqueda textual

Reglas:

- buscar solo en campos españoles efectivos y `personalNotes`;
- no buscar en `nameEn`;
- no buscar en `descriptionEn`;
- ignorar mayúsculas/minúsculas;
- ignorar acentos;
- normalizar espacios repetidos y puntuación básica;
- si `q` está vacío, devolver todos los conjuros de la lista con nivel `<= maxLevel`;
- si `q` no tiene comillas, exigir todos los términos normalizados;
- si `q` tiene comillas, buscar frase normalizada exacta;
- permitir prefijo de token para términos de 3 o más caracteres;
- no hacer coincidencia arbitraria en mitad de palabra.

### Campos buscables

- `nameEs`;
- `descriptionEs`;
- `school`;
- `subschool`;
- `descriptors`;
- `castingTime`;
- `components`;
- `range`;
- `target`;
- `effect`;
- `area`;
- `duration`;
- `savingThrow`;
- `spellResistance`;
- `personalNotes`.

### Response `200`

DTO: `SpellSearchResponseDto`.

```json
{
  "filters": {
    "listType": "CLASS",
    "listName": "Clérigo",
    "maxLevel": 3,
    "q": "veneno"
  },
  "page": {
    "page": 0,
    "size": 20,
    "totalItems": 2,
    "totalPages": 1,
    "hasNext": false
  },
  "sort": "LEVEL_ASC_NAME_ES_ASC",
  "results": [
    {
      "spellId": "delay-poison",
      "slug": "delay-poison",
      "nameEs": "Retrasar veneno",
      "selectedList": {
        "listType": "CLASS",
        "listName": "Clérigo",
        "level": 2
      },
      "school": "conjuración",
      "descriptors": [],
      "castingTime": "1 acción estándar",
      "range": "toque",
      "savingThrow": "Fortaleza niega (inofensivo)",
      "spellResistance": "sí (inofensivo)",
      "translationStatus": "AI_TRANSLATED",
      "snippet": "El objetivo queda temporalmente protegido contra veneno...",
      "matchSource": "descriptionEs",
      "hasPersonalNotes": false
    }
  ]
}
```

### `SpellSearchResultDto`

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `spellId` | string | sí | Identificador estable. |
| `slug` | string | sí | Referencia humana estable. |
| `nameEs` | string/null | sí | Nombre español efectivo. |
| `selectedList` | object | sí | Lista por la que aparece en la búsqueda. |
| `school` | string/null | sí | Escuela efectiva. |
| `descriptors` | array[string] | sí | Descriptores efectivos. |
| `castingTime` | string/null | sí | Tiempo de lanzamiento. |
| `range` | string/null | sí | Alcance. |
| `savingThrow` | string/null | sí | TS. |
| `spellResistance` | string/null | sí | RC. |
| `translationStatus` | string enum | sí | Estado efectivo. |
| `snippet` | string/null | sí | Fragmento español o resumen. Nunca inglés. |
| `matchSource` | string/null | sí | Campo principal de coincidencia, si existe. |
| `hasPersonalNotes` | boolean | sí | Indica si el conjuro tiene notas. |

`matchSource` puede tomar estos valores:

```text
nameEs
descriptionEs
school
subschool
descriptors
castingTime
components
range
target
effect
area
duration
savingThrow
spellResistance
personalNotes
null
```

### Ejemplo de navegación sin texto

Request:

```http
GET /api/spells/search?listType=CLASS&listName=Inquisidor&maxLevel=2&page=0&size=50
```

Response resumida:

```json
{
  "filters": {
    "listType": "CLASS",
    "listName": "Inquisidor",
    "maxLevel": 2,
    "q": ""
  },
  "page": {
    "page": 0,
    "size": 50,
    "totalItems": 37,
    "totalPages": 1,
    "hasNext": false
  },
  "sort": "LEVEL_ASC_NAME_ES_ASC",
  "results": []
}
```

## Endpoint: detalle de conjuro

### Ruta

```http
GET /api/spells/{spellId}
```

### Propósito

Devuelve el detalle completo efectivo de un conjuro.

Debe incluir:

- campos españoles completos;
- texto inglés de referencia;
- listas de conjuros;
- fuente;
- notas personales;
- estado de traducción;
- metadatos mínimos para edición.

### Path parameters

| Parámetro | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `spellId` | string | sí | Identificador estable. Debe existir. |

### Response `200`

DTO: `SpellDetailResponseDto`.

```json
{
  "spellId": "neutralize-poison",
  "slug": "neutralize-poison",
  "nameEs": "Neutralizar veneno",
  "nameEn": "Neutralize Poison",
  "school": "conjuración",
  "subschool": "curación",
  "descriptors": [],
  "castingTime": "1 acción estándar",
  "components": "V, S, FD",
  "range": "toque",
  "target": "criatura u objeto tocado",
  "effect": null,
  "area": null,
  "duration": "instantáneo",
  "savingThrow": "Voluntad niega (inofensivo, objeto)",
  "spellResistance": "sí (inofensivo, objeto)",
  "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
  "descriptionEn": "You detoxify any sort of venom in the creature or object touched.",
  "personalNotes": "Muy útil para llevar preparado si esperamos venenos o drow.",
  "translationStatus": "MANUALLY_EDITED",
  "lists": [
    {
      "listType": "CLASS",
      "listName": "Clérigo",
      "level": 4
    },
    {
      "listType": "CLASS",
      "listName": "Druida",
      "level": 3
    }
  ],
  "source": {
    "sourceBook": "Core Rulebook",
    "sourcePage": null,
    "sourceName": "spells.csv"
  },
  "editableFields": [
    "nameEs",
    "school",
    "subschool",
    "descriptors",
    "castingTime",
    "components",
    "range",
    "target",
    "effect",
    "area",
    "duration",
    "savingThrow",
    "spellResistance",
    "descriptionEs",
    "personalNotes",
    "translationStatus"
  ],
  "updatedAt": "2026-06-11T10:30:00Z",
  "reviewedAt": null
}
```

### Reglas del detalle

- `descriptionEs` es la descripción principal para UI.
- `descriptionEn` es referencia secundaria y debe poder mostrarse plegada.
- `personalNotes` no forma parte de `descriptionEs`.
- `personalNotes` debe distinguirse visualmente como contenido del usuario.
- `lists` puede contener varias listas de conjuros.
- La API no devuelve un campo `locked`.
- Si `translationStatus = "LOCKED"`, el frontend debe mostrarlo como estado de traducción protegido.

## Endpoint: listas de conjuros disponibles

### Ruta

```http
GET /api/spell-lists
```

### Propósito

Devuelve las listas de conjuros disponibles en el dataset efectivo.

El frontend debe usar este endpoint para poblar selectores, en lugar de codificar listas a mano.

### Query parameters

| Parámetro | Tipo | Obligatorio | Valor por defecto | Reglas |
|---|---:|---:|---:|---|
| `listType` | string | no | `null` | Si se informa, filtra por tipo de lista. En MVP normalmente `CLASS`. |

### Response `200`

DTO: `SpellListsResponseDto`.

```json
{
  "items": [
    {
      "listType": "CLASS",
      "listName": "Clérigo",
      "minLevel": 0,
      "maxLevel": 9,
      "levels": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9],
      "spellCount": 312
    },
    {
      "listType": "CLASS",
      "listName": "Inquisidor",
      "minLevel": 0,
      "maxLevel": 6,
      "levels": [0, 1, 2, 3, 4, 5, 6],
      "spellCount": 148
    }
  ]
}
```

### `SpellListSummaryDto`

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `listType` | string | sí | Tipo de lista. |
| `listName` | string | sí | Nombre visible. |
| `minLevel` | integer | sí | Mínimo nivel disponible. |
| `maxLevel` | integer | sí | Máximo nivel disponible. |
| `levels` | array[integer] | sí | Niveles existentes para esa lista. |
| `spellCount` | integer | sí | Número de conjuros de la lista. |

### Ordenación

Orden recomendado:

1. `listType` ascendente;
2. `listName` ascendente normalizado.

## Endpoint: niveles disponibles por lista

### Ruta

```http
GET /api/spell-lists/levels
```

### Propósito

Devuelve los niveles disponibles para una lista de conjuros concreta.

Se usa para que el frontend pueda poblar el selector de nivel máximo tras elegir lista.

### Query parameters

| Parámetro | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `listType` | string | sí | Tipo de lista. En MVP normalmente `CLASS`. |
| `listName` | string | sí | Nombre exacto de lista. |

### Response `200`

DTO: `SpellListLevelsResponseDto`.

```json
{
  "listType": "CLASS",
  "listName": "Clérigo",
  "minLevel": 0,
  "maxLevel": 9,
  "levels": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
}
```

### Reglas

- `levels` debe contener solo niveles existentes en el dataset efectivo.
- `levels` debe ordenarse ascendente.
- `maxLevel` en búsqueda debe validarse contra esta lista cuando sea posible.
- Si la lista no existe, devolver `422`, no una lista vacía, porque indica un estado inválido del frontend o una URL manual incorrecta.

## Endpoint: actualizar campos españoles

### Ruta

```http
PATCH /api/spells/{spellId}/fields
```

### Propósito

Actualiza uno o varios campos españoles editables de un conjuro.

La petición genera o actualiza overrides en `fields` y actualiza la proyección SQLite y el índice de búsqueda.

### Path parameters

| Parámetro | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `spellId` | string | sí | Identificador estable. Debe existir. |

### Request

DTO: `UpdateSpellFieldsRequestDto`.

```json
{
  "fields": {
    "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
    "savingThrow": "Voluntad niega (inofensivo, objeto)",
    "spellResistance": "sí (inofensivo, objeto)"
  },
  "expectedUpdatedAt": "2026-06-11T10:30:00Z",
  "reason": "Corrección de estilo de mesa."
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `fields` | object | sí | Debe contener al menos un campo editable. |
| `expectedUpdatedAt` | string/null | no | Control opcional de conflicto. |
| `reason` | string/null | no | Motivo libre de la corrección. Máximo 500 caracteres. |

### Reglas de edición

- Solo se aceptan campos españoles editables.
- `translationStatus` no se acepta en este endpoint; debe usarse `/translation-status`.
- `personalNotes` no se acepta en este endpoint; debe usarse `/notes`.
- Si se edita un campo español y el conjuro no está `LOCKED`, el backend debe marcar el estado efectivo como `MANUALLY_EDITED`.
- Si se edita un conjuro `LOCKED`, el estado debe seguir siendo `LOCKED` salvo cambio explícito posterior mediante `/translation-status`.
- El texto inglés debe conservarse intacto.
- Las notas personales existentes no deben eliminarse.
- La edición debe actualizar índices de búsqueda si cambia un campo buscable.
- La edición debe persistirse en `data/overrides/spells-es.overrides.json` antes de considerarse completada.

### Response `200`

Devuelve el detalle efectivo actualizado.

DTO: `SpellDetailResponseDto`.

```json
{
  "spellId": "neutralize-poison",
  "slug": "neutralize-poison",
  "nameEs": "Neutralizar veneno",
  "nameEn": "Neutralize Poison",
  "school": "conjuración",
  "subschool": "curación",
  "descriptors": [],
  "castingTime": "1 acción estándar",
  "components": "V, S, FD",
  "range": "toque",
  "target": "criatura u objeto tocado",
  "effect": null,
  "area": null,
  "duration": "instantáneo",
  "savingThrow": "Voluntad niega (inofensivo, objeto)",
  "spellResistance": "sí (inofensivo, objeto)",
  "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
  "descriptionEn": "You detoxify any sort of venom in the creature or object touched.",
  "personalNotes": "Muy útil para llevar preparado si esperamos venenos o drow.",
  "translationStatus": "MANUALLY_EDITED",
  "lists": [],
  "source": {
    "sourceBook": "Core Rulebook",
    "sourcePage": null,
    "sourceName": "spells.csv"
  },
  "editableFields": [],
  "updatedAt": "2026-06-11T11:00:00Z",
  "reviewedAt": null
}
```

En implementación real, `lists` y `editableFields` deben devolverse completos como en el endpoint de detalle.

## Endpoint: actualizar notas personales

### Ruta

```http
PATCH /api/spells/{spellId}/notes
```

### Propósito

Actualiza las notas personales de un conjuro.

Las notas personales son contenido propio del usuario, se guardan canónicamente en overrides y forman parte de la búsqueda.

### Request

DTO: `UpdatePersonalNotesRequestDto`.

```json
{
  "personalNotes": "Preparar si esperamos drow o criaturas venenosas.",
  "expectedUpdatedAt": "2026-06-11T10:30:00Z"
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `personalNotes` | string | sí | Puede ser `""` para vaciar notas. Máximo 20000 caracteres. |
| `expectedUpdatedAt` | string/null | no | Control opcional de conflicto. |

### Reglas

- No modifica `descriptionEs`.
- No modifica `descriptionEn`.
- No modifica `translationStatus`.
- No elimina overrides de campos españoles.
- Actualiza índice de búsqueda.
- Persiste `personalNotes` como propiedad directa del override, no dentro de `fields`.

### Response `200`

Devuelve el detalle efectivo actualizado.

DTO: `SpellDetailResponseDto`.

## Endpoint: actualizar estado de traducción

### Ruta

```http
PATCH /api/spells/{spellId}/translation-status
```

### Propósito

Actualiza el estado de traducción/revisión de un conjuro.

### Request

DTO: `UpdateTranslationStatusRequestDto`.

```json
{
  "translationStatus": "REVIEWED",
  "expectedUpdatedAt": "2026-06-11T10:30:00Z",
  "reason": "Revisado para uso en mesa."
}
```

Campos:

| Campo | Tipo | Obligatorio | Regla |
|---|---:|---:|---|
| `translationStatus` | string enum | sí | Debe ser un estado permitido. |
| `expectedUpdatedAt` | string/null | no | Control opcional de conflicto. |
| `reason` | string/null | no | Motivo libre. Máximo 500 caracteres. |

### Reglas

- El estado se persiste en overrides como `fields.translationStatus`.
- No se crea ni se devuelve un campo `locked`.
- Cambiar a `REVIEWED` no congela los campos frente a cambios futuros del dataset generado.
- Cambiar a `MANUALLY_EDITED` no obliga a copiar todos los campos españoles a overrides.
- Cambiar a `LOCKED` debe materializar todos los campos españoles editables con su valor efectivo actual dentro de overrides.
- Cambiar desde `LOCKED` a otro estado es una acción manual explícita de desbloqueo de estado.
- Cambiar estado no debe borrar notas personales.
- Cambiar estado no debe modificar texto inglés.
- Cambiar estado no debe modificar listas de conjuros.

### Ejemplo: bloquear conjuro

Request:

```json
{
  "translationStatus": "LOCKED",
  "reason": "Traducción revisada y cerrada."
}
```

Resultado funcional esperado:

- `translationStatus` efectivo pasa a `LOCKED`;
- el override guarda `fields.translationStatus = "LOCKED"`;
- el escritor de overrides materializa los campos españoles editables;
- futuras regeneraciones no alteran esos campos españoles efectivos;
- `nameEn` y `descriptionEn` siguen viniendo del dataset generado.

### Response `200`

Devuelve el detalle efectivo actualizado.

DTO: `SpellDetailResponseDto`.

## Validaciones de entrada

## Validaciones generales

| Elemento | Regla | Error |
|---|---|---:|
| JSON mal formado | El cuerpo no puede parsearse como JSON. | `400` |
| `Content-Type` incorrecto en requests con cuerpo | Debe ser `application/json`. | `400` |
| Campo obligatorio ausente | Falta un campo requerido del DTO. | `400` |
| Tipo JSON incorrecto | Ejemplo: `maxLevel` como texto. | `400` |
| Campo no permitido | Ejemplo: editar `descriptionEn`. | `422` |
| Valor fuera de contrato | Ejemplo: `translationStatus = "FINISHED"`. | `422` |
| Recurso inexistente | Ejemplo: `spellId` no existe. | `404` |
| Conflicto de actualización | `expectedUpdatedAt` no coincide. | `409` |

## Validaciones de búsqueda

| Campo | Regla | Error |
|---|---|---:|
| `listType` | Obligatorio, no vacío. | `400` |
| `listName` | Obligatorio, no vacío. | `400` |
| `maxLevel` | Obligatorio, entero `>= 0`. | `400` |
| `q` | Máximo 200 caracteres tras trim. | `422` |
| `page` | Entero `>= 0`. | `400` |
| `size` | Entero entre `1` y `200`. | `400` |
| lista inexistente | `listType + listName` no existe en dataset efectivo. | `422` |
| nivel no disponible | `maxLevel` no es compatible con la lista seleccionada. | `422` |

## Validaciones de campos españoles

| Campo | Regla | Error |
|---|---|---:|
| `fields` | Debe existir y no estar vacío. | `400` |
| `nameEs` | string/null, máximo 300 caracteres. | `422` |
| campos mecánicos string | string/null, máximo 1000 caracteres. | `422` |
| `descriptionEs` | string/null, máximo 100000 caracteres. | `422` |
| `descriptors` | array de string, máximo 50 elementos. | `422` |
| `descriptors[]` | string no nulo, máximo 100 caracteres. | `422` |
| `translationStatus` en `/fields` | No permitido en este endpoint. | `422` |
| `personalNotes` en `/fields` | No permitido en este endpoint. | `422` |
| campos ingleses | No editables. | `422` |
| identificadores/fuente/listas | No editables. | `422` |

## Validaciones de notas

| Campo | Regla | Error |
|---|---|---:|
| `personalNotes` | Obligatorio. | `400` |
| `personalNotes` | Debe ser string. | `400` |
| `personalNotes` | Máximo 20000 caracteres. | `422` |
| `personalNotes` | `""` es válido y significa vaciar notas. | — |

## Validaciones de estado

| Campo | Regla | Error |
|---|---|---:|
| `translationStatus` | Obligatorio. | `400` |
| `translationStatus` | Debe pertenecer al enum permitido. | `422` |
| `translationStatus` | No puede ser `null`. | `422` |

## Errores HTTP esperados

## `400 Bad Request`

Usar cuando la petición no puede interpretarse correctamente.

Ejemplos:

- JSON mal formado;
- falta un query parameter obligatorio;
- `maxLevel` no es entero;
- `page` es negativo;
- `fields` no es un objeto;
- falta `personalNotes` en el endpoint de notas.

Ejemplo:

```json
{
  "error": {
    "code": "BAD_REQUEST",
    "message": "La petición no tiene una estructura válida.",
    "details": [
      {
        "field": "maxLevel",
        "message": "maxLevel es obligatorio y debe ser un entero."
      }
    ]
  }
}
```

## `404 Not Found`

Usar cuando se solicita un conjuro inexistente por `spellId`.

Ejemplos:

- `GET /api/spells/unknown-spell`;
- `PATCH /api/spells/unknown-spell/fields`;
- `PATCH /api/spells/unknown-spell/notes`;
- `PATCH /api/spells/unknown-spell/translation-status`.

Ejemplo:

```json
{
  "error": {
    "code": "SPELL_NOT_FOUND",
    "message": "No existe ningún conjuro con el identificador indicado.",
    "details": [
      {
        "field": "spellId",
        "message": "unknown-spell"
      }
    ]
  }
}
```

## `409 Conflict`

Usar cuando la petición es válida, pero no puede aplicarse por conflicto con el estado actual.

En el MVP aplica principalmente a control opcional de concurrencia local mediante `expectedUpdatedAt`.

Regla:

- si `expectedUpdatedAt` se informa y no coincide con el `updatedAt` efectivo actual del conjuro, devolver `409`.

También puede usarse si el archivo de overrides cambió entre lectura y escritura y el backend detecta que no puede fusionar la modificación de forma segura.

Ejemplo:

```json
{
  "error": {
    "code": "CONFLICT",
    "message": "El conjuro ha cambiado desde que fue leído.",
    "details": [
      {
        "field": "expectedUpdatedAt",
        "message": "Recarga el detalle antes de guardar cambios."
      }
    ]
  }
}
```

## `422 Unprocessable Entity`

Usar cuando la petición es JSON válido, pero viola reglas del contrato o del dominio.

Ejemplos:

- editar `descriptionEn`;
- enviar `locked: true`;
- usar `translationStatus = "FINISHED"`;
- enviar `descriptors` como string en vez de array;
- buscar una lista de conjuros inexistente;
- usar un `maxLevel` que no existe para la lista indicada.

Ejemplo:

```json
{
  "error": {
    "code": "UNPROCESSABLE_ENTITY",
    "message": "La petición viola reglas funcionales del contrato.",
    "details": [
      {
        "field": "fields.descriptionEn",
        "message": "descriptionEn no es editable en el MVP."
      }
    ]
  }
}
```

## Relación con búsqueda

La API de búsqueda debe usar la infraestructura de búsqueda sobre SQLite o índice equivalente local.

Reglas:

- no leer `spells-es.generated.json` en cada búsqueda;
- no leer `spells-es.overrides.json` en cada búsqueda;
- buscar contra datos efectivos ya importados;
- incluir notas personales en el índice;
- excluir texto inglés del índice;
- actualizar índice tras editar campos buscables o notas personales.

## Relación con overrides

Los endpoints de edición deben persistir cambios manuales en:

```text
data/overrides/spells-es.overrides.json
```

Reglas:

- no escribir cambios manuales en `spells-es.generated.json`;
- no perder overrides existentes;
- no borrar notas al editar campos;
- no borrar campos al editar notas;
- no borrar texto inglés;
- no borrar overrides huérfanos al reescribir el archivo;
- escribir overrides de forma atómica cuando sea posible;
- actualizar SQLite y búsqueda después de persistir el cambio canónico.

## Relación con texto inglés

El inglés se expone solo como referencia secundaria.

Reglas:

- `nameEn` y `descriptionEn` aparecen en detalle;
- `nameEn` y `descriptionEn` no aparecen obligatoriamente en resultados de búsqueda;
- `nameEn` y `descriptionEn` no son editables;
- `nameEn` y `descriptionEn` no participan en la búsqueda MVP;
- las respuestas de error no deben sugerir editar inglés desde la UI del MVP.

## Relación con arquitectura hexagonal

## Casos de uso esperados

La capa web debe invocar casos de uso equivalentes a:

- `SearchSpellsUseCase`;
- `GetSpellDetailUseCase`;
- `ListSpellListsUseCase`;
- `ListSpellLevelsUseCase`;
- `UpdateSpellFieldsUseCase`;
- `UpdatePersonalNotesUseCase`;
- `UpdateTranslationStatusUseCase`.

Los nombres exactos pueden variar, pero las responsabilidades deben mantenerse separadas.

## Puertos esperados

La capa de aplicación debería depender de puertos equivalentes a:

- `SpellRepository`;
- `SpellSearchRepository`;
- `OverridesWriter`;
- `ClockProvider`.

Los controladores REST no deben depender directamente de adaptadores concretos de SQLite ni de escritura de archivos.

## Responsabilidades por capa

| Capa | Responsabilidad |
|---|---|
| Web | HTTP, DTOs, validación superficial, mapeo, códigos de error. |
| Application | Casos de uso, flujo de operación, transacciones funcionales. |
| Domain | Reglas de edición, estados, invariantes. |
| Ports | Contratos hacia persistencia, búsqueda y escritura de overrides. |
| Infrastructure | SQLite, índices, JSON, filesystem, migraciones. |

## Relación con frontend

El frontend Vue debe consumir esta API sin conocer la estructura física de archivos.

Reglas:

- usar `/api/spell-lists` para poblar listas;
- usar `/api/spell-lists/levels` para poblar niveles disponibles;
- usar `/api/spells/search` para resultados;
- usar `/api/spells/{spellId}` para detalle;
- usar endpoints `PATCH` para edición;
- no escribir directamente overrides;
- no reconstruir el spell efectivo en frontend;
- no aplicar reglas de sobrescritura;
- no decidir cuándo un conjuro pasa a `MANUALLY_EDITED`;
- no buscar en inglés;
- mostrar el texto inglés como sección secundaria o plegable;
- distinguir notas personales de descripción oficial/traducida;
- conservar el contexto de búsqueda al volver desde detalle.

## Ejemplo de flujo completo frontend-backend

```text
1. Frontend carga listas:
   GET /api/spell-lists?listType=CLASS

2. Usuario elige Clérigo.

3. Frontend carga niveles:
   GET /api/spell-lists/levels?listType=CLASS&listName=Clérigo

4. Usuario elige nivel máximo 3 y busca veneno.

5. Frontend consulta resultados:
   GET /api/spells/search?listType=CLASS&listName=Clérigo&maxLevel=3&q=veneno

6. Usuario abre un resultado.

7. Frontend carga detalle:
   GET /api/spells/delay-poison

8. Usuario edita notas.

9. Frontend guarda notas:
   PATCH /api/spells/delay-poison/notes

10. Backend escribe override, actualiza SQLite e índice.

11. Frontend refresca detalle o usa la respuesta actualizada.
```

## Criterios de aceptación

El contrato REST se considera correcto si:

- permite buscar por `listType`, `listName`, `maxLevel` y `q`;
- permite navegar sin término de búsqueda;
- devuelve resultados compactos ordenados por nivel y nombre español;
- devuelve detalle completo con español, inglés secundario, listas, fuente, notas y estado;
- permite editar campos españoles sin tocar inglés;
- permite editar notas sin tocar traducción ni estado;
- permite cambiar estado de traducción sin crear `locked`;
- persiste cambios manuales mediante overrides;
- no acopla dominio a controladores;
- no introduce autenticación;
- no introduce servicios externos;
- mantiene la búsqueda MVP solo en español efectivo y notas;
- da errores `400`, `404`, `409` y `422` de forma predecible;
- permite al frontend implementar el flujo de búsqueda, detalle y edición sin reglas ocultas.
