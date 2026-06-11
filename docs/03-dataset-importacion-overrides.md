# 03 - Dataset, importación y overrides

## Propósito

Este documento define cómo **Grimorio Pathfinder** gestiona su fuente de datos española, cómo la importa a SQLite y cómo conserva las correcciones manuales del usuario.

El objetivo es que la aplicación pueda reconstruir su base local desde archivos versionados sin perder traducciones corregidas, notas personales ni estados de revisión.

## Decisión principal

La fuente canónica española vive en archivos versionados.

SQLite no es la fuente única de verdad. SQLite es una proyección local reconstruible usada para consulta, búsqueda y rendimiento.

Flujo conceptual:

```text
data/generated/spells-es.generated.json
        +
data/overrides/spells-es.overrides.json
        ↓
importador local
        ↓
SQLite
        ↓
búsqueda y consulta en la app
```

## Terminología canónica

- `lista de clase lanzadora`: lista de conjuros de una clase como Clérigo, Druida, Inquisidor o Mago/Hechicero.
- `lista de conjuros`: concepto general que incluye listas de clase lanzadora y listas especiales como dominios, linajes, patronos, misterios o inquisiciones.

## Decisiones cerradas del MVP

### `LOCKED`

`LOCKED` es únicamente un valor de `translationStatus`.

No existe un flag técnico adicional llamado `locked` en el MVP.

Por tanto, el archivo de overrides no debe incluir:

```json
{
  "locked": true
}
```

El bloqueo se expresa así:

```json
{
  "fields": {
    "translationStatus": "LOCKED"
  }
}
```

### `personalNotes`

`personalNotes` se persiste canónicamente en:

```text
data/overrides/spells-es.overrides.json
```

No vive en:

- `spells-es.generated.json`;
- un archivo separado de notas;
- SQLite como fuente canónica.

SQLite contiene una copia efectiva reconstruible para consulta y búsqueda.

## Estructura de carpetas

Estructura esperada:

```text
data/
  raw/
    spells.csv
  generated/
    spells-es.generated.json
  overrides/
    spells-es.overrides.json
  exports/
```

### `data/raw/`

Contiene fuentes originales.

Ejemplo:

```text
data/raw/spells.csv
```

Reglas:

- Puede contener datos en inglés.
- No se usa directamente en la consulta normal de la aplicación.
- Sirve como entrada para procesos de generación del dataset.
- Debe conservarse para trazabilidad cuando sea posible.

### `data/generated/`

Contiene el dataset español generado.

Ejemplo:

```text
data/generated/spells-es.generated.json
```

Reglas:

- Es generado por el pipeline de datos.
- Puede regenerarse.
- No debe editarse manualmente desde la aplicación.
- Puede sobrescribirse al regenerar traducciones.
- No contiene las correcciones manuales finales si existen overrides.
- No contiene `personalNotes`.

### `data/overrides/`

Contiene correcciones manuales y datos propios del usuario.

Ejemplo:

```text
data/overrides/spells-es.overrides.json
```

Reglas:

- Tiene prioridad sobre `spells-es.generated.json`.
- No debe sobrescribirse automáticamente.
- Debe conservar correcciones campo por campo.
- Debe conservar notas personales.
- Debe conservar estados manuales de revisión.
- Debe ser legible y versionable.
- Es la fuente canónica del MVP para `personalNotes`.

### `data/exports/`

Carpeta opcional para exportaciones futuras.

Ejemplos posibles:

```text
data/exports/spells-es.final.json
data/exports/spells-es.md
```

No forma parte obligatoria del MVP.

## Contrato de `spells-es.generated.json`

Archivo principal:

```text
data/generated/spells-es.generated.json
```

Representa la biblioteca española generada previamente.

### Reglas generales

- Debe contener todos los conjuros disponibles en el dataset generado.
- Debe conservar el texto inglés original como referencia.
- Debe usar identificadores estables.
- Debe incluir pertenencias a listas de conjuros.
- Debe incluir fuente/libro cuando exista.
- Debe incluir estado de traducción inicial.
- Debe estar estructurado para importarse de forma determinista.
- Puede regenerarse y sobrescribirse por el pipeline.
- La aplicación no debe escribir manualmente en este archivo.

### Campos raíz obligatorios

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `schemaVersion` | integer | sí | Versión del contrato JSON. Para este documento: `1`. |
| `generatedAt` | string ISO-8601 | sí | Fecha de generación del dataset. |
| `sourceName` | string/null | sí | Nombre de la fuente o pipeline si se conoce. Puede ser `null`. |
| `spells` | array | sí | Lista de conjuros generados. |

### Campos obligatorios por conjuro

Todos estos campos deben existir en cada elemento de `spells`. Si el dato no existe en la fuente, usar `null` o `[]` según corresponda; no omitir claves canónicas.

| Campo | Tipo | Valor vacío permitido | Reglas |
|---|---:|---:|---|
| `id` | string | no | Identificador estable. Bloqueante si falta o se duplica. |
| `slug` | string | sí | Puede coincidir con `id`. |
| `sourceId` | string/null | sí | Identificador de la fuente original si existe. |
| `sourceHash` | string/null | sí | Hash de los datos fuente relevantes. |
| `nameEs` | string/null | sí | `null` solo si `translationStatus = NOT_TRANSLATED`. |
| `nameEn` | string/null | sí | Debe conservarse si existe en fuente. Advertencia si falta. |
| `school` | string/null | sí | Español efectivo generado. |
| `subschool` | string/null | sí | `null` si no aplica. |
| `descriptors` | array[string] | sí | `[]` si no hay descriptores. |
| `castingTime` | string/null | sí | Texto español generado. |
| `components` | string/null | sí | Texto español generado o abreviado. |
| `range` | string/null | sí | Texto español generado. |
| `target` | string/null | sí | `null` si no aplica. |
| `effect` | string/null | sí | `null` si no aplica. |
| `area` | string/null | sí | `null` si no aplica. |
| `duration` | string/null | sí | Texto español generado. |
| `savingThrow` | string/null | sí | `null` si no aplica. |
| `spellResistance` | string/null | sí | `null` si no aplica. |
| `descriptionEs` | string/null | sí | `null` solo si no hay traducción útil. |
| `descriptionEn` | string/null | sí | Debe conservarse si existe en fuente. Advertencia si falta. |
| `sourceBook` | string/null | sí | `null` si no se conoce. |
| `sourcePage` | integer/null | sí | `null` si no se conoce. |
| `sourceName` | string/null | sí | Nombre de fuente específica si existe. |
| `translationStatus` | string | no | Debe pertenecer al enum permitido. |
| `lists` | array | sí | Entradas `SpellListEntry`; puede estar vacío solo con advertencia. |

### Campos obligatorios por entrada de lista

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `listType` | string | sí | Ejemplo: `CLASS`, `DOMAIN`, `BLOODLINE`. |
| `listName` | string | sí | Nombre mostrado de la lista de conjuros. |
| `level` | integer | sí | Entero `>= 0`. |

### Estados permitidos

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

En `spells-es.generated.json`, lo normal es usar `NOT_TRANSLATED`, `AI_TRANSLATED` o `REVIEW_REQUIRED`.

`REVIEWED`, `MANUALLY_EDITED` y `LOCKED` pueden aparecer si el pipeline consume información manual, pero no deben sustituir ni borrar overrides.

### Ejemplo válido

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-06-11T00:00:00Z",
  "sourceName": "pipeline-local",
  "spells": [
    {
      "id": "neutralize-poison",
      "slug": "neutralize-poison",
      "sourceId": "neutralize-poison",
      "sourceHash": "abc123",
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
      "descriptionEs": "Neutralizas cualquier tipo de veneno.",
      "descriptionEn": "You detoxify any sort of venom.",
      "sourceBook": "Core Rulebook",
      "sourcePage": null,
      "sourceName": "spells.csv",
      "translationStatus": "AI_TRANSLATED",
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
      ]
    }
  ]
}
```

## Contrato de `spells-es.overrides.json`

Archivo principal:

```text
data/overrides/spells-es.overrides.json
```

Los overrides representan cambios manuales del usuario sobre el dataset generado.

### Reglas generales

- Un override se aplica a un conjuro identificado por `spellId`.
- Un override modifica campos concretos.
- Los overrides tienen prioridad sobre el dataset generado.
- Los overrides deben poder reaplicarse tras regenerar el dataset.
- Los overrides no deben eliminar ni sustituir el texto inglés original.
- Los overrides deben conservar notas personales.
- Los overrides deben conservar estados manuales como `MANUALLY_EDITED`, `REVIEWED` o `LOCKED`.
- La app nunca debe sobrescribir el archivo completo perdiendo entradas existentes.
- La app debe hacer escrituras tipo read-modify-write, preservando campos no tocados.

### Campos raíz obligatorios

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `schemaVersion` | integer | sí | Versión del contrato JSON. Para este documento: `1`. |
| `updatedAt` | string ISO-8601/null | sí | Última actualización conocida del archivo. |
| `spells` | object | sí | Mapa por `spellId`. Puede estar vacío. |

### Campos por entrada de override

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `fields` | object | no | Mapa de campos editados. Si falta, equivale a `{}`. |
| `personalNotes` | string/null | no | Notas personales canónicas del usuario. |
| `updatedAt` | string ISO-8601/null | no | Última edición de esa entrada. |
| `reason` | string/null | no | Motivo libre de la corrección. |

No se permite `locked` como campo del MVP. El bloqueo se guarda en `fields.translationStatus`.

### Campos permitidos dentro de `fields`

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

`personalNotes` no debe ir dentro de `fields`; debe ir como propiedad directa de la entrada de conjuro.

No se permiten overrides de `nameEn`, `descriptionEn`, `sourceHash`, `sourceBook` o identificadores en el flujo normal del MVP.

### Ejemplo válido

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-06-11T00:00:00Z",
  "spells": {
    "neutralize-poison": {
      "fields": {
        "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
        "translationStatus": "MANUALLY_EDITED"
      },
      "personalNotes": "Muy útil para llevar preparado si esperamos venenos o drow.",
      "updatedAt": "2026-06-11T00:00:00Z",
      "reason": "Corrección de estilo de mesa."
    }
  }
}
```

### Ejemplo de conjuro bloqueado

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-06-11T00:00:00Z",
  "spells": {
    "neutralize-poison": {
      "fields": {
        "translationStatus": "LOCKED"
      },
      "personalNotes": "Traducción revisada y cerrada para mesa.",
      "updatedAt": "2026-06-11T00:00:00Z"
    }
  }
}
```

## Resultado efectivo

El conjuro efectivo que usa la aplicación se calcula aplicando overrides sobre el dataset generado.

```text
spell efectivo = spell generado + overrides del spellId
```

Ejemplo:

```text
generated.descriptionEs = "Desintoxicas cualquier tipo de ponzoña..."
override.descriptionEs  = "Neutralizas cualquier tipo de veneno..."
resultado.descriptionEs = "Neutralizas cualquier tipo de veneno..."
```

## Orden de prioridad

Para campos españoles editables:

```text
override manual
        >
dataset generado
        >
valor vacío/nulo
```

Para `personalNotes`:

```text
data/overrides/spells-es.overrides.json
        >
valor vacío/nulo
```

Para texto inglés:

```text
dataset generado / fuente original
```

Los overrides no deben sustituir `nameEn` ni `descriptionEn` en el MVP.

## Aplicación de overrides

### Algoritmo funcional

```text
1. Cargar conjuro generado por id.
2. Buscar override con el mismo spellId.
3. Si existe fields, aplicar cada campo permitido sobre el conjuro generado.
4. Si existe personalNotes, asignarlo al conjuro efectivo.
5. Si fields.translationStatus existe, asignarlo como estado efectivo.
6. Conservar siempre nameEn, descriptionEn y trazabilidad original desde generated.
7. Registrar advertencias sin borrar entradas.
```

### Campos desconocidos en overrides

Si un override contiene un campo no reconocido:

- no debe aplicarse al conjuro efectivo;
- debe conservarse en el archivo;
- debe reportarse como advertencia;
- no debe romper la importación completa.

### Overrides huérfanos

Un override es huérfano si referencia un `spellId` que no existe en `spells-es.generated.json`.

Reglas:

- no borrarlo automáticamente;
- no aplicarlo a ningún conjuro efectivo;
- reportarlo como advertencia;
- conservarlo en `spells-es.overrides.json` para posible recuperación futura.

## Importación a SQLite

La importación debe construir la base local a partir de:

```text
data/generated/spells-es.generated.json
data/overrides/spells-es.overrides.json
```

### Flujo

```text
1. Leer dataset generado.
2. Validar estructura mínima.
3. Leer overrides si existen.
4. Validar estructura mínima de overrides.
5. Aplicar overrides permitidos.
6. Construir modelo efectivo.
7. Persistir en SQLite.
8. Reconstruir índices de búsqueda.
9. Informar de errores o advertencias.
```

### Reglas

- La importación debe ser repetible.
- Reimportar no debe perder overrides.
- Reimportar no debe perder notas personales.
- Reimportar no debe perder estados manuales.
- Si el dataset generado cambia, los overrides compatibles deben seguir aplicándose.
- Si un override referencia un `spellId` inexistente, debe conservarse y reportarse como advertencia, no borrarse automáticamente.

## Reconstrucción de SQLite

SQLite puede borrarse y reconstruirse.

Esto debe ser seguro siempre que existan:

```text
data/generated/spells-es.generated.json
data/overrides/spells-es.overrides.json
```

### Reglas

- La base SQLite no debe contener información irrecuperable que no exista en archivos.
- Si la app permite editar, debe escribir overrides antes de depender de una reconstrucción.
- Los datos locales de usuario del MVP deben ir a overrides.
- No usar SQLite como única fuente de verdad para `personalNotes`.

## Identificadores estables

Los identificadores son críticos para que los overrides sobrevivan.

### Reglas

- Cada conjuro debe tener un `id` estable.
- El `id` no debe depender de SQLite.
- El `id` no debe cambiar por modificar la traducción española.
- El `id` debe ser apto para enlazar overrides.
- Si el nombre inglés cambia ligeramente, debe intentarse conservar el mismo `id` si representa el mismo conjuro.
- Si hay colisiones, deben resolverse de forma explícita durante generación/importación.

### Recomendación

Usar un identificador derivado de la fuente original cuando exista.

Si no existe, usar un slug estable basado en el nombre inglés y, si es necesario, fuente/libro.

## Hash de fuente

`sourceHash` permite detectar cambios en el contenido original.

### Reglas

- Debe calcularse desde los datos originales relevantes.
- Sirve para saber si una traducción puede haber quedado obsoleta.
- No debe usarse como identificador principal.
- Un cambio de `sourceHash` no debe borrar overrides automáticamente.
- Si un conjuro tiene overrides y cambia `sourceHash`, puede marcarse para revisión en una fase futura, pero no debe perder sus correcciones.

## Estados de traducción en importación

Estados permitidos:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

### Reglas

- El dataset generado puede traer estados iniciales.
- Un override puede cambiar el estado mediante `fields.translationStatus`.
- Si un campo español se edita manualmente, el estado efectivo debe ser `MANUALLY_EDITED`, salvo que el conjuro esté `LOCKED` o el usuario elija explícitamente otro estado.
- `LOCKED` debe impedir sobrescrituras automáticas en procesos futuros.
- Los conjuros no revisados se importan igualmente.

## Notas personales

Las notas personales son contenido del usuario.

### Reglas

- Deben persistirse en overrides.
- Deben aplicarse durante importación.
- Deben guardarse en SQLite para búsqueda.
- Deben incluirse en el texto buscable.
- No deben mezclarse con `descriptionEs`.
- No deben aparecer como si fueran texto del conjuro.
- No deben modificar `translationStatus` salvo petición explícita.

## Búsqueda e índices

SQLite se usa para búsqueda rápida.

### Reglas

- La búsqueda MVP se realiza solo sobre contenido español efectivo y notas personales.
- El texto inglés no debe formar parte del índice de búsqueda del MVP.
- Se deben indexar los campos necesarios para responder a:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

### Campos incluidos en búsqueda

- nombre español;
- descripción española;
- escuela;
- subescuela;
- descriptores;
- tiempo de lanzamiento;
- componentes;
- alcance;
- objetivo;
- efecto;
- área;
- duración;
- tirada de salvación;
- resistencia a conjuros;
- notas personales.

### Normalización obligatoria para MVP

La infraestructura de búsqueda debe normalizar:

- mayúsculas/minúsculas;
- acentos;
- espacios repetidos;
- puntuación básica.

La normalización no debe modificar los campos originales editables.

## Validaciones mínimas de dataset

Al importar, validar al menos:

- el JSON generado existe;
- el JSON generado puede parsearse;
- `schemaVersion` es compatible;
- cada conjuro tiene `id`;
- no hay duplicados de `id`;
- cada conjuro tiene `nameEs` o está marcado como `NOT_TRANSLATED`;
- cada conjuro conserva `nameEn` si existe en la fuente;
- cada conjuro conserva `descriptionEn` si existe en la fuente;
- cada `SpellListEntry` tiene `listType`, `listName` y `level`;
- ningún `level` es negativo;
- `translationStatus` pertenece a los valores permitidos;
- los overrides referencian campos conocidos o se reportan como advertencia;
- no aparece `locked` como flag técnico en overrides.

## Manejo de errores

### Error bloqueante

Debe detener la importación si:

- el JSON generado no existe;
- el JSON generado no puede parsearse;
- falta una estructura básica imprescindible;
- `schemaVersion` no es compatible;
- hay duplicados irresolubles de `id`;
- un `SpellListEntry.level` es negativo.

### Advertencia

Debe continuar importando si:

- falta una fuente/libro;
- falta una página;
- un campo opcional está vacío;
- un override referencia un conjuro que ya no existe;
- un override referencia un campo desconocido;
- un override contiene `locked` como campo heredado o inválido;
- un conjuro está marcado como `REVIEW_REQUIRED`;
- una entrada de lista de conjuros tiene un `listType` desconocido pero puede conservarse como `OTHER`.

Las advertencias deben ser visibles en logs o resultado de importación.

## Escritura de overrides desde la app

Cuando el usuario edita desde la aplicación:

```text
1. Validar la petición en el caso de uso.
2. Actualizar el modelo efectivo.
3. Persistir cambio en SQLite.
4. Leer overrides actuales.
5. Crear o actualizar únicamente la entrada y campo afectados.
6. Escribir el archivo completo preservando overrides existentes.
7. Actualizar índices si el campo es buscable.
```

### Reglas

- No escribir cambios manuales en `spells-es.generated.json`.
- No perder overrides existentes de otros campos.
- No eliminar notas personales al editar traducción.
- No eliminar traducción al editar notas.
- Mantener formato estable y legible del archivo de overrides.
- No escribir `locked`; usar `fields.translationStatus = "LOCKED"`.

## Campos editables que generan overrides

Dentro de `fields`:

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

Fuera de `fields`, como propiedad directa:

- `personalNotes`

El texto inglés no se edita desde el flujo normal del MVP.

## Exportación futura

No forma parte obligatoria del MVP, pero el diseño debe permitir exportar:

```text
data/exports/spells-es.final.json
data/exports/spells-es.md
```

### `spells-es.final.json`

Dataset efectivo ya combinado.

Uso posible:

- copia de seguridad;
- revisión;
- transporte a otra instalación.

### `spells-es.md`

Exportación legible para humanos.

Uso posible:

- lectura fuera de la app;
- revisión manual;
- documentación personal.

## Relación con pipeline de traducción

El pipeline de traducción no forma parte del uso normal de la app.

Puede existir como herramienta posterior para generar:

```text
data/generated/spells-es.generated.json
```

### Reglas

- El pipeline puede sobrescribir el dataset generado.
- El pipeline no debe sobrescribir overrides.
- El pipeline debe respetar `translationStatus = LOCKED` si consume overrides.
- La app no debe llamar a IA durante la consulta normal.

## Relación con Combat Manager

Combat Manager no es dependencia del MVP.

Puede usarse en el futuro como fuente auxiliar, pero:

- la app debe funcionar sin Combat Manager;
- la importación principal debe venir de archivos locales;
- ninguna búsqueda debe depender de su API;
- ningún dato corregido manualmente debe depender de Combat Manager.

## Tests prioritarios

Cubrir al menos:

1. Importar dataset generado válido.
2. Aplicar override de campo simple.
3. Aplicar override de notas personales.
4. Reimportar sin perder overrides.
5. Ignorar texto inglés en búsqueda.
6. Incluir notas personales en búsqueda.
7. Detectar `id` duplicados.
8. Reportar override huérfano como advertencia.
9. No sobrescribir `LOCKED`.
10. Reconstruir SQLite desde archivos.
11. Rechazar o advertir `locked` como flag no canónico.
12. Preservar campos desconocidos de overrides sin aplicarlos.

## Criterios de aceptación

Este módulo se considera correcto si:

- la app puede cargar el dataset español;
- SQLite se reconstruye desde archivos;
- los overrides se aplican correctamente;
- las correcciones manuales sobreviven a reimportaciones;
- las notas personales sobreviven a reimportaciones;
- la búsqueda usa los datos efectivos;
- el texto inglés se conserva como referencia;
- no hay dependencia de internet ni de servicios externos;
- no se escribe manualmente sobre el dataset generado;
- no hay ambigüedad entre `LOCKED` como estado y un flag técnico inexistente.
