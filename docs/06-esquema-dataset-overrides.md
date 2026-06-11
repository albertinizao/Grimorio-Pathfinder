# 06 - Esquema de dataset y overrides

## Propósito

Este documento define el contrato técnico de los archivos:

```text
data/generated/spells-es.generated.json
data/overrides/spells-es.overrides.json
```

El objetivo es fijar de forma precisa:

- la estructura del dataset generado;
- la estructura del archivo de overrides;
- los campos obligatorios y opcionales;
- las reglas para obtener el conjuro efectivo;
- las prioridades entre datos generados y datos manuales;
- la preservación de notas personales;
- el comportamiento de los estados de traducción;
- el tratamiento de identificadores y hashes de fuente;
- la reimportación sin pérdida;
- la validación mínima que debe realizar el importador.

Este documento actúa como contrato para:

- el importador de dataset;
- el escritor de overrides;
- la reconstrucción de SQLite;
- la validación de archivos;
- futuras migraciones de esquema.

## Alcance

Este documento define únicamente el contrato de datos de los archivos JSON y sus reglas de combinación.

No define:

- lógica de interfaz de usuario;
- endpoints REST concretos, salvo su relación mínima con lectura/escritura;
- tablas, índices o migraciones concretas de SQLite;
- pipeline detallado de traducción IA;
- exportaciones futuras fuera del contrato mínimo.

SQLite debe tratarse como una proyección local reconstruible desde el dataset generado y los overrides. No debe contener datos irrecuperables que no estén persistidos en archivos o no puedan exportarse antes de una reconstrucción.

## Archivos del contrato

### Dataset generado

Ruta canónica:

```text
data/generated/spells-es.generated.json
```

Reglas:

- Debe ser generado por el pipeline de datos.
- No debe editarse manualmente desde la aplicación.
- Puede regenerarse y sobrescribirse.
- Debe conservar el texto inglés original como referencia.
- Debe contener los datos españoles generados antes de aplicar correcciones manuales.
- Debe contener identificadores estables suficientes para enlazar overrides.

### Overrides

Ruta canónica:

```text
data/overrides/spells-es.overrides.json
```

Reglas:

- Debe contener correcciones manuales y datos personales del usuario.
- Debe tener prioridad sobre el dataset generado.
- No debe sobrescribirse automáticamente durante la regeneración del dataset.
- Debe ser legible, versionable y estable.
- Debe conservar notas personales.
- Debe conservar estados manuales como `REVIEWED`, `MANUALLY_EDITED` y `LOCKED`.

## Versión de esquema

Ambos archivos deben declarar una propiedad superior:

```json
"version": 1
```

Reglas:

- `version` identifica la versión del esquema del archivo, no la versión de Pathfinder ni la versión de la aplicación.
- El importador debe rechazar como error bloqueante un archivo sin `version`.
- El importador debe rechazar como error bloqueante un archivo con una versión mayor que la máxima versión soportada.
- Los cambios incompatibles deben incrementar `version`.
- Los cambios compatibles pueden mantener la misma versión solo si añaden campos opcionales que los importadores antiguos puedan ignorar sin pérdida de datos.
- El escritor de overrides del MVP debe emitir siempre `version: 1`.

## Estados de traducción permitidos

Los únicos valores válidos para `translationStatus` son:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

Cualquier otro valor debe considerarse error de validación.

## Campos editables mediante overrides

El MVP permite overrides solo sobre estos campos:

```text
nameEs
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
descriptionEs
translationStatus
```

Además, `personalNotes` es editable, pero no debe guardarse dentro de `fields`. Tiene reglas propias.

Reglas:

- `nameEn` no debe ser editable mediante overrides en el MVP.
- `descriptionEn` no debe ser editable mediante overrides en el MVP.
- `sourceId` no debe ser editable mediante overrides.
- `sourceHash` no debe ser editable mediante overrides.
- `id` no debe ser editable mediante overrides.
- `slug` no debe ser editable mediante overrides.
- `lists` no debe ser editable mediante overrides en el MVP.
- `sourceBook`, `sourcePage` y `sourceName` no deben ser editables mediante overrides en el MVP.

Si un override intenta modificar un campo no permitido, el importador debe reportarlo como advertencia y no debe aplicarlo. En modo estricto, se permite tratarlo como error bloqueante.

## Estructura del dataset generado

### Estructura superior

El archivo `spells-es.generated.json` debe tener esta forma:

```json
{
  "version": 1,
  "generatedAt": "2026-06-11T00:00:00Z",
  "spells": []
}
```

### Campos superiores

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `version` | integer | sí | Debe ser `1` para este contrato. |
| `generatedAt` | string ISO-8601 | no | Fecha de generación del archivo. Si existe, debe ser parseable. |
| `spells` | array | sí | Lista de conjuros generados. Puede estar vacía solo en datasets de prueba. |

No debe haber correcciones manuales ni notas personales en este archivo.

### Estructura de cada conjuro generado

Cada elemento de `spells` debe representar un conjuro generado.

```json
{
  "id": "neutralize-poison",
  "slug": "neutralize-poison",
  "sourceId": "neutralize-poison",
  "sourceHash": "sha256:7a5f...",
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
  "sourceBook": "Core Rulebook",
  "sourcePage": null,
  "sourceName": null,
  "translationStatus": "AI_TRANSLATED",
  "lists": [
    {
      "listType": "CLASS",
      "listName": "Clérigo",
      "level": 4
    }
  ]
}
```

### Campos de conjuro generado

| Campo | Tipo | Obligatorio | Nulo permitido | Reglas |
|---|---:|---:|---:|---|
| `id` | string | sí | no | Identificador estable del conjuro. No debe depender de SQLite. |
| `slug` | string | sí | no | Referencia humana estable. Puede coincidir con `id`. |
| `sourceId` | string | sí | sí | Identificador del conjuro en la fuente original, si existe. |
| `sourceHash` | string | sí | no | Hash de los datos originales relevantes. No es identificador principal. |
| `nameEs` | string | sí | sí | Nombre español generado. Puede ser `null` solo si `translationStatus` es `NOT_TRANSLATED`. |
| `nameEn` | string | sí | sí | Nombre inglés original si existe en la fuente. |
| `school` | string | sí | sí | Escuela en español. |
| `subschool` | string | sí | sí | Subescuela en español, si existe. |
| `descriptors` | array de string | sí | no | Lista de descriptores en español. Puede estar vacía. |
| `castingTime` | string | sí | sí | Tiempo de lanzamiento en español. |
| `components` | string | sí | sí | Componentes en forma textual. |
| `range` | string | sí | sí | Alcance en español. |
| `target` | string | sí | sí | Objetivo en español, si existe. |
| `effect` | string | sí | sí | Efecto en español, si existe. |
| `area` | string | sí | sí | Área en español, si existe. |
| `duration` | string | sí | sí | Duración en español. |
| `savingThrow` | string | sí | sí | Tirada de salvación en español. |
| `spellResistance` | string | sí | sí | Resistencia a conjuros en español. |
| `descriptionEs` | string | sí | sí | Descripción española generada. Puede ser `null` si no está traducida. |
| `descriptionEn` | string | sí | sí | Descripción inglesa original si existe en la fuente. |
| `sourceBook` | string | sí | sí | Libro o fuente, si existe. |
| `sourcePage` | integer | sí | sí | Página, si existe. Debe ser `>= 0` cuando no sea `null`. |
| `sourceName` | string | sí | sí | Nombre adicional de fuente, sección o procedencia, si existe. |
| `translationStatus` | string enum | sí | no | Debe ser uno de los estados permitidos. |
| `lists` | array | sí | no | Pertenencias a listas de conjuros. Puede estar vacío con advertencia. |

Reglas:

- Los campos anteriores deben estar presentes aunque su valor sea `null`, salvo que se indique lo contrario.
- Un campo textual vacío `""` se permite, pero debe tratarse como valor distinto de `null`.
- El generador debe preferir `null` cuando el dato no exista y `""` solo cuando se quiera representar texto deliberadamente vacío.
- `descriptors` debe ser siempre array; si no hay descriptores, debe ser `[]`.
- `lists` debe ser siempre array.

### Estructura de `lists`

Cada entrada de `lists` debe tener esta forma:

```json
{
  "listType": "CLASS",
  "listName": "Clérigo",
  "level": 4
}
```

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `listType` | string | sí | Tipo de lista. Debe ser un valor conocido o `OTHER`. |
| `listName` | string | sí | Nombre visible de la lista. No debe estar vacío. |
| `level` | integer | sí | Nivel del conjuro dentro de esa lista. Debe ser `>= 0`. |

Valores recomendados de `listType`:

```text
CLASS
DOMAIN
SUBDOMAIN
BLOODLINE
PATRON
MYSTERY
INQUISITION
SCHOOL
RACE
ARCHETYPE
OTHER
```

Reglas:

- El MVP puede centrarse en `CLASS`, pero no debe descartar listas especiales.
- Si el generador no puede clasificar una lista, debe usar `OTHER` antes que perder la entrada.
- `listName` no debe convertirse en un enum cerrado.

## Estructura del archivo de overrides

### Estructura superior

El archivo `spells-es.overrides.json` debe tener esta forma:

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T00:00:00Z",
  "spells": {}
}
```

### Campos superiores

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `version` | integer | sí | Debe ser `1` para este contrato. |
| `updatedAt` | string ISO-8601 | sí | Fecha de última escritura del archivo por la aplicación o herramienta de overrides. |
| `spells` | object | sí | Mapa por `spellId`. Cada clave debe corresponder al `id` estable del conjuro. |

Reglas:

- `spells` debe ser un objeto, no un array.
- Las claves de `spells` deben ser `spellId` estables.
- El archivo puede existir con `spells: {}`.
- La ausencia completa del archivo de overrides se permite en una instalación limpia; en ese caso, el importador debe asumir que no hay overrides.

### Estructura de override por conjuro

Cada entrada de `spells` debe tener esta forma:

```json
{
  "fields": {
    "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
    "translationStatus": "MANUALLY_EDITED"
  },
  "personalNotes": "Muy útil para llevar preparado si esperamos venenos o drow.",
  "updatedAt": "2026-06-11T00:00:00Z"
}
```

### Campos de override por conjuro

| Campo | Tipo | Obligatorio | Reglas |
|---|---:|---:|---|
| `fields` | object | no | Mapa de campos editables sobrescritos. Si falta, no hay overrides de traducción. |
| `personalNotes` | string | no | Notas personales del usuario. Si está presente, su valor sustituye las notas efectivas. |
| `updatedAt` | string ISO-8601 | sí | Fecha de última modificación de este override. |

Reglas:

- Una entrada de override debe contener al menos `fields` no vacío o `personalNotes` presente.
- `fields` no debe contener campos ingleses ni campos de trazabilidad.
- `personalNotes` no debe guardarse dentro de `fields`.
- `personalNotes: ""` significa que el usuario ha vaciado deliberadamente las notas.
- Si `personalNotes` está ausente, no se modifica el valor efectivo de notas. Como el dataset generado no contiene notas, el valor efectivo por defecto será cadena vacía.

## Corrección explícita sobre `locked`

El campo booleano `locked` no forma parte del esquema v1.

Reglas:

- El escritor de overrides no debe emitir `locked`.
- El bloqueo debe expresarse exclusivamente mediante `fields.translationStatus = "LOCKED"`.
- Si un importador encuentra `locked` en un archivo antiguo, debe reportarlo como advertencia.
- En modo de compatibilidad, se permite interpretar `locked: true` como `translationStatus: "LOCKED"`, pero al reescribir el archivo debe eliminarse `locked`.
- En modo estricto, `locked` debe considerarse campo desconocido no permitido.

Esta decisión evita duplicar la misma regla de negocio en dos campos distintos.

## Tipos permitidos dentro de `fields`

| Campo | Tipo permitido | Nulo permitido | Reglas |
|---|---:|---:|---|
| `nameEs` | string | sí | Si está presente, sustituye `generated.nameEs`. |
| `school` | string | sí | Si está presente, sustituye `generated.school`. |
| `subschool` | string | sí | Si está presente, sustituye `generated.subschool`. |
| `descriptors` | array de string | no | Sustituye la lista completa de descriptores. Puede ser `[]`. |
| `castingTime` | string | sí | Sustituye `generated.castingTime`. |
| `components` | string | sí | Sustituye `generated.components`. |
| `range` | string | sí | Sustituye `generated.range`. |
| `target` | string | sí | Sustituye `generated.target`. |
| `effect` | string | sí | Sustituye `generated.effect`. |
| `area` | string | sí | Sustituye `generated.area`. |
| `duration` | string | sí | Sustituye `generated.duration`. |
| `savingThrow` | string | sí | Sustituye `generated.savingThrow`. |
| `spellResistance` | string | sí | Sustituye `generated.spellResistance`. |
| `descriptionEs` | string | sí | Sustituye `generated.descriptionEs`. |
| `translationStatus` | string enum | no | Sustituye `generated.translationStatus`. |

Reglas:

- La presencia de un campo en `fields` significa override manual, incluso si el valor es `null` o `""`.
- La ausencia de un campo en `fields` significa que se usa el valor generado.
- `descriptors` debe sustituir la lista completa; no se definen operaciones parciales de añadir o quitar descriptores en v1.
- No se permiten estructuras de patch parcial, operadores ni expresiones.

## Prioridad exacta de datos

Para cada campo efectivo editable:

```text
override manual presente en fields
        >
dataset generado
        >
valor vacío/nulo
```

Reglas:

- “Presente” significa que la clave existe dentro de `fields`, aunque el valor sea `null` o `""`.
- Si la clave no existe en `fields`, debe usarse el valor del dataset generado.
- Si el dataset generado tampoco tiene valor útil, el valor efectivo será `null`, `[]` o `""` según el tipo del campo.
- El importador no debe inventar contenido para completar campos ausentes.

Para texto inglés:

```text
dataset generado / fuente original
```

Reglas:

- `nameEn` y `descriptionEn` no deben modificarse mediante overrides en el MVP.
- Un override nunca debe eliminar, sustituir ni ocultar el texto inglés original.

Para notas personales:

```text
override.personalNotes presente
        >
cadena vacía
```

Reglas:

- El dataset generado no debe contener `personalNotes`.
- Las notas personales solo proceden de overrides o de un archivo local exportable equivalente si se define en el futuro.

## Algoritmo normativo para construir el spell efectivo

Para cada conjuro generado:

```text
1. Localizar generatedSpell por id.
2. Buscar overrideSpell en overrides.spells[generatedSpell.id].
3. Copiar todos los campos del generatedSpell.
4. Si existe overrideSpell.fields:
   4.1. Validar que cada campo sea editable.
   4.2. Validar el tipo de cada valor.
   4.3. Sustituir en la copia cada campo presente.
5. Calcular personalNotes:
   5.1. Si overrideSpell.personalNotes está presente, usar ese valor.
   5.2. Si no está presente, usar cadena vacía.
6. Calcular translationStatus efectivo.
7. Persistir el resultado efectivo en SQLite.
8. Indexar únicamente campos españoles efectivos y personalNotes.
```

## Reglas para `translationStatus`

### Cálculo del estado efectivo

El estado efectivo se calcula así:

```text
1. Si override.fields.translationStatus existe:
      usar override.fields.translationStatus.
2. Si no existe y override.fields contiene al menos un campo traducido distinto de translationStatus:
      usar MANUALLY_EDITED.
3. Si no existe override de campos traducidos:
      usar generated.translationStatus.
```

Campos traducidos a efectos de esta regla:

```text
nameEs
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
descriptionEs
```

Reglas:

- Editar solo `personalNotes` no debe cambiar el estado de traducción.
- Editar un campo traducido debe marcar el conjuro como `MANUALLY_EDITED`, salvo que el usuario o proceso de escritura establezca explícitamente otro `translationStatus` permitido.
- `REVIEWED` significa que la traducción ha sido revisada, pero no congela los campos frente a cambios futuros del dataset generado.
- `MANUALLY_EDITED` protege solo los campos presentes en `fields`; los campos no presentes siguen viniendo del dataset generado.
- `LOCKED` protege el conjuro frente a sobrescrituras automáticas y tiene reglas adicionales.

### Reglas para `MANUALLY_EDITED`

- Debe persistirse en overrides cuando el usuario edite un campo traducido.
- Debe tener prioridad sobre el estado generado.
- No obliga a copiar todos los campos traducidos al override.
- Permite que campos no editados se actualicen si cambia el dataset generado.

### Reglas para `REVIEWED`

- Debe persistirse en overrides si el usuario marca un conjuro como revisado.
- No debe impedir que campos no sobrescritos tomen valores nuevos del dataset generado.
- No debe ocultar la posibilidad de editar posteriormente el conjuro.
- Si el usuario edita después un campo traducido, el estado debe pasar a `MANUALLY_EDITED`, salvo elección explícita distinta.

### Reglas para `LOCKED`

`LOCKED` representa una protección del conjuro efectivo frente a procesos automáticos.

Reglas:

- Debe expresarse como `fields.translationStatus = "LOCKED"`.
- No debe expresarse mediante `locked: true`.
- Cuando el escritor de overrides marque un conjuro como `LOCKED`, debe materializar en `fields` todos los campos traducidos editables con su valor efectivo actual.
- Un conjuro `LOCKED` correctamente materializado no debe cambiar sus campos españoles aunque se regenere `spells-es.generated.json`.
- Si un override `LOCKED` no contiene todos los campos traducidos editables, el importador debe aplicar los campos presentes, completar los ausentes desde el dataset generado y reportar advertencia de bloqueo incompleto.
- `LOCKED` no debe impedir conservar `nameEn`, `descriptionEn`, `sourceId`, `sourceHash` ni demás datos de trazabilidad del dataset generado.
- Desbloquear un conjuro, si se implementa, debe ser una acción manual explícita que cambie `translationStatus` a otro estado permitido.

## Reglas para `personalNotes`

`personalNotes` representa contenido propio del usuario.

Reglas:

- Debe guardarse en `data/overrides/spells-es.overrides.json`.
- No debe guardarse en `spells-es.generated.json`.
- No debe mezclarse con `descriptionEs`.
- No debe mostrarse ni tratarse como texto oficial del conjuro.
- Debe sobrevivir a reimportaciones y reconstrucciones de SQLite.
- Debe formar parte del índice de búsqueda MVP.
- Editar `personalNotes` no debe modificar `translationStatus` salvo petición explícita.
- Editar `personalNotes` no debe eliminar overrides de campos traducidos.
- Editar un campo traducido no debe eliminar `personalNotes`.
- `personalNotes: ""` debe conservarse como una decisión explícita de dejar las notas vacías.
- La ausencia de `personalNotes` equivale a no tener notas guardadas.

## Reglas para `sourceId`

`sourceId` identifica el conjuro dentro de la fuente original cuando esa fuente proporciona un identificador aprovechable.

Reglas:

- Debe estar presente como campo del dataset generado.
- Puede ser `null` si la fuente original no proporciona identificador claro.
- No debe usarse como clave de overrides.
- No debe sustituir a `id`.
- Si existe, debe conservarse al regenerar el dataset.
- Si cambia `sourceId` pero el conjuro sigue representando el mismo conjuro de Pathfinder, debe intentarse conservar el mismo `id` estable.
- No debe editarse desde overrides en el MVP.

## Reglas para `sourceHash`

`sourceHash` permite detectar cambios en los datos originales relevantes.

Reglas:

- Debe estar presente en cada conjuro generado.
- Debe calcularse desde los datos originales relevantes del conjuro, no desde la traducción manual.
- No debe usarse como identificador principal.
- No debe usarse como clave de overrides.
- Un cambio de `sourceHash` no debe borrar overrides automáticamente.
- Un cambio de `sourceHash` no debe cambiar por sí solo el `id`.
- Si `sourceHash` cambia y `id` se mantiene, los overrides compatibles deben seguir aplicándose.
- El importador puede reportar advertencias de revisión si dispone de información suficiente para detectar que un override podría estar obsoleto, pero no debe eliminarlo automáticamente.
- El escritor de overrides del esquema v1 no está obligado a almacenar `sourceHash`.

## Reglas para identificadores estables

`id` es la clave principal del contrato.

Reglas:

- Debe ser único dentro de `spells-es.generated.json`.
- Debe ser estable entre regeneraciones.
- No debe depender de una clave autoincremental de SQLite.
- No debe cambiar por corregir una traducción española.
- No debe cambiar por cambiar `sourceHash`.
- Debe ser la clave usada en `overrides.spells`.
- Si hay colisión de `id`, el generador o importador debe resolverla explícitamente antes de importar.
- Los duplicados de `id` en el dataset generado deben ser error bloqueante.

## Reglas para reimportación sin pérdida

La reimportación debe poder ejecutarse tantas veces como sea necesario sin perder correcciones manuales.

Reglas:

- El importador debe leer siempre dataset generado y overrides antes de reconstruir SQLite.
- El importador no debe escribir en `spells-es.generated.json`.
- El importador no debe eliminar entradas de `spells-es.overrides.json`.
- Los overrides compatibles deben reaplicarse después de cada regeneración del dataset.
- Las notas personales deben reaplicarse después de cada regeneración del dataset.
- Los estados manuales deben reaplicarse después de cada regeneración del dataset.
- Si SQLite se borra, debe poder reconstruirse desde ambos archivos sin pérdida de datos manuales.
- Si la aplicación permite editar, el caso de uso de edición debe escribir el override antes de considerar persistido el cambio manual.
- La escritura de overrides debe preservar campos no relacionados ya existentes en el mismo conjuro.
- La escritura de overrides debe ser atómica: escribir en un archivo temporal válido y reemplazar el archivo anterior solo tras completar la serialización correctamente.

## Reglas para overrides huérfanos

Un override huérfano es una entrada de `overrides.spells` cuya clave no existe como `id` en el dataset generado actual.

Reglas:

- Un override huérfano no debe romper la importación completa.
- Debe reportarse como advertencia.
- No debe aplicarse a ningún conjuro efectivo.
- No debe borrarse automáticamente.
- Debe conservarse en el archivo de overrides al reescribir otros cambios.
- Si en una regeneración futura vuelve a existir un conjuro con el mismo `id`, el override debe volver a aplicarse.
- La eliminación definitiva de overrides huérfanos solo debe hacerse mediante una acción explícita de limpieza o migración.

## Reglas para datos de lectura y escritura

### Datos de lectura

Durante la consulta normal de la aplicación:

- La aplicación debe leer conjuros efectivos desde SQLite.
- SQLite debe contener el resultado de aplicar overrides sobre el dataset generado.
- El texto inglés debe estar disponible como referencia en detalle.
- La búsqueda MVP debe usar solo campos españoles efectivos y `personalNotes`.
- La búsqueda MVP no debe usar `nameEn`, `descriptionEn` ni texto inglés.

### Datos de escritura

Cuando se edita desde la aplicación:

- Los cambios manuales deben escribirse en `spells-es.overrides.json`.
- No debe escribirse en `spells-es.generated.json`.
- SQLite puede actualizarse para reflejar el cambio inmediatamente, pero no debe ser la única persistencia del cambio.
- Los índices de búsqueda deben actualizarse si cambia un campo buscable.
- La API REST, si existe, debe invocar casos de uso que respeten este contrato; no debe escribir directamente en los archivos desde controladores.

## Compatibilidad y migraciones futuras

Reglas:

- Todo cambio incompatible debe incrementar `version`.
- Una migración de overrides debe conservar todos los campos manuales conocidos.
- Una migración no debe descartar overrides huérfanos salvo instrucción explícita.
- Una migración no debe convertir notas personales en descripción oficial.
- Una migración no debe convertir texto inglés en texto buscable del MVP.
- Si una versión futura añade bloqueo por campo, debe definir cómo convive con el bloqueo por conjuro completo de `translationStatus = LOCKED`.
- Si una versión futura permite editar listas o fuentes, debe definir campos específicos y reglas de prioridad nuevas; no deben reutilizarse de forma ambigua los campos v1.

## Validaciones mínimas del importador

### Validaciones bloqueantes del dataset generado

El importador debe detener la importación si:

- el archivo `spells-es.generated.json` no existe;
- el archivo no es JSON válido;
- falta `version`;
- `version` no es soportada;
- falta `spells`;
- `spells` no es array;
- hay dos conjuros con el mismo `id`;
- un conjuro no tiene `id`;
- un conjuro tiene `id` vacío;
- un conjuro no tiene `sourceHash`;
- un conjuro tiene `translationStatus` fuera de los valores permitidos;
- una entrada de `lists` tiene `level` negativo;
- una entrada de `lists` no tiene `listType`, `listName` o `level`.

### Validaciones no bloqueantes del dataset generado

El importador debe reportar advertencia y continuar si:

- `generatedAt` falta;
- `sourceBook` falta o es `null`;
- `sourcePage` falta o es `null`;
- `sourceId` es `null`;
- `nameEs` es `null` y el estado es `NOT_TRANSLATED`;
- `descriptionEs` es `null` y el estado es `NOT_TRANSLATED`;
- `lists` está vacío;
- un campo opcional está `null`;
- una lista usa `listType = OTHER`.

### Validaciones bloqueantes de overrides

Si el archivo de overrides existe, el importador debe detener su lectura si:

- el archivo no es JSON válido;
- falta `version`;
- `version` no es soportada;
- falta `spells`;
- `spells` no es object;
- una entrada de override no es object;
- `fields` existe pero no es object;
- `personalNotes` existe pero no es string;
- `translationStatus` existe pero no es un estado permitido.

El importador puede continuar sin overrides solo si el archivo no existe. Si existe pero es inválido de forma bloqueante, debe detener la importación para evitar reconstruir SQLite perdiendo correcciones manuales.

### Validaciones no bloqueantes de overrides

El importador debe reportar advertencia y continuar si:

- un override referencia un `spellId` inexistente;
- un override contiene un campo desconocido dentro de `fields`;
- un override intenta modificar `nameEn` o `descriptionEn`;
- un override intenta modificar `sourceId`, `sourceHash`, `id`, `slug`, `lists` o fuente;
- un override contiene el campo legado `locked`;
- un override `LOCKED` no contiene todos los campos traducidos editables;
- `updatedAt` falta o no puede parsearse, siempre que el resto del override sea aplicable.

## Ejemplos válidos

### Dataset generado válido

```json
{
  "version": 1,
  "generatedAt": "2026-06-11T00:00:00Z",
  "spells": [
    {
      "id": "delay-poison",
      "slug": "delay-poison",
      "sourceId": "delay-poison",
      "sourceHash": "sha256:111111",
      "nameEs": "Retrasar veneno",
      "nameEn": "Delay Poison",
      "school": "conjuración",
      "subschool": "curación",
      "descriptors": [],
      "castingTime": "1 acción estándar",
      "components": "V, S, FD",
      "range": "toque",
      "target": "criatura tocada",
      "effect": null,
      "area": null,
      "duration": "1 hora/nivel",
      "savingThrow": "Fortaleza niega (inofensivo)",
      "spellResistance": "sí (inofensivo)",
      "descriptionEs": "El objetivo queda temporalmente protegido contra veneno.",
      "descriptionEn": "The subject becomes temporarily immune to poison.",
      "sourceBook": "Core Rulebook",
      "sourcePage": null,
      "sourceName": null,
      "translationStatus": "AI_TRANSLATED",
      "lists": [
        {
          "listType": "CLASS",
          "listName": "Clérigo",
          "level": 2
        },
        {
          "listType": "CLASS",
          "listName": "Druida",
          "level": 2
        }
      ]
    }
  ]
}
```

### Override válido de descripción y notas

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T10:30:00Z",
  "spells": {
    "delay-poison": {
      "fields": {
        "descriptionEs": "El objetivo queda protegido contra los efectos del veneno durante la duración del conjuro.",
        "translationStatus": "MANUALLY_EDITED"
      },
      "personalNotes": "Preparar si esperamos drow o criaturas venenosas.",
      "updatedAt": "2026-06-11T10:30:00Z"
    }
  }
}
```

Resultado efectivo:

```json
{
  "id": "delay-poison",
  "nameEs": "Retrasar veneno",
  "descriptionEs": "El objetivo queda protegido contra los efectos del veneno durante la duración del conjuro.",
  "descriptionEn": "The subject becomes temporarily immune to poison.",
  "translationStatus": "MANUALLY_EDITED",
  "personalNotes": "Preparar si esperamos drow o criaturas venenosas."
}
```

### Override válido solo de notas

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T10:35:00Z",
  "spells": {
    "delay-poison": {
      "personalNotes": "Bueno para mazmorras con arañas, drow o asesinos.",
      "updatedAt": "2026-06-11T10:35:00Z"
    }
  }
}
```

Reglas aplicadas:

- `personalNotes` se usa en búsqueda.
- `translationStatus` no cambia por editar solo notas.
- Los campos traducidos siguen viniendo del dataset generado.

### Override válido de conjuro bloqueado

Un override `LOCKED` debe materializar todos los campos traducidos editables:

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T11:00:00Z",
  "spells": {
    "delay-poison": {
      "fields": {
        "nameEs": "Retrasar veneno",
        "school": "conjuración",
        "subschool": "curación",
        "descriptors": [],
        "castingTime": "1 acción estándar",
        "components": "V, S, FD",
        "range": "toque",
        "target": "criatura tocada",
        "effect": null,
        "area": null,
        "duration": "1 hora/nivel",
        "savingThrow": "Fortaleza niega (inofensivo)",
        "spellResistance": "sí (inofensivo)",
        "descriptionEs": "El objetivo queda protegido contra los efectos del veneno durante la duración del conjuro.",
        "translationStatus": "LOCKED"
      },
      "personalNotes": "Traducción revisada y cerrada.",
      "updatedAt": "2026-06-11T11:00:00Z"
    }
  }
}
```

Reglas aplicadas:

- Cambios posteriores en el dataset generado no deben alterar esos campos españoles efectivos.
- El texto inglés de referencia seguirá viniendo del dataset generado.
- `sourceHash` nuevo no elimina el override.

### Override huérfano válido con advertencia

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T11:10:00Z",
  "spells": {
    "old-spell-id": {
      "fields": {
        "descriptionEs": "Texto corregido de un conjuro que ya no aparece en el dataset actual.",
        "translationStatus": "MANUALLY_EDITED"
      },
      "personalNotes": "No borrar automáticamente; quizá reaparezca con el mismo id.",
      "updatedAt": "2026-06-11T11:10:00Z"
    }
  }
}
```

Reglas aplicadas:

- Debe reportarse advertencia.
- No debe aplicarse a ningún conjuro efectivo.
- No debe eliminarse automáticamente.

## Ejemplos de errores o invalidaciones

### Error: dataset sin `version`

```json
{
  "spells": []
}
```

Resultado:

```text
Error bloqueante: falta version.
```

### Error: `spells` no es array en dataset generado

```json
{
  "version": 1,
  "spells": {}
}
```

Resultado:

```text
Error bloqueante: spells debe ser array en spells-es.generated.json.
```

### Error: `id` duplicado

```json
{
  "version": 1,
  "spells": [
    { "id": "delay-poison", "sourceHash": "sha256:a", "translationStatus": "AI_TRANSLATED", "lists": [] },
    { "id": "delay-poison", "sourceHash": "sha256:b", "translationStatus": "AI_TRANSLATED", "lists": [] }
  ]
}
```

Resultado:

```text
Error bloqueante: id duplicado delay-poison.
```

### Error: nivel negativo

```json
{
  "listType": "CLASS",
  "listName": "Clérigo",
  "level": -1
}
```

Resultado:

```text
Error bloqueante: level no puede ser negativo.
```

### Error: estado desconocido

```json
{
  "fields": {
    "translationStatus": "FINISHED"
  },
  "updatedAt": "2026-06-11T12:00:00Z"
}
```

Resultado:

```text
Error de validación: translationStatus debe pertenecer al enum permitido.
```

### Advertencia: override intenta editar texto inglés

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T12:05:00Z",
  "spells": {
    "delay-poison": {
      "fields": {
        "descriptionEn": "Modified English text"
      },
      "updatedAt": "2026-06-11T12:05:00Z"
    }
  }
}
```

Resultado:

```text
Advertencia: descriptionEn no es campo editable por overrides en el MVP. El campo no se aplica.
```

### Advertencia o error estricto: `locked` legado

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T12:10:00Z",
  "spells": {
    "delay-poison": {
      "fields": {
        "descriptionEs": "Texto revisado."
      },
      "locked": true,
      "updatedAt": "2026-06-11T12:10:00Z"
    }
  }
}
```

Resultado en modo compatibilidad:

```text
Advertencia: locked es legado. Interpretar como translationStatus LOCKED y reescribir sin locked.
```

Resultado en modo estricto:

```text
Error o advertencia de campo no permitido: locked.
```

### Advertencia: `LOCKED` incompleto

```json
{
  "version": 1,
  "updatedAt": "2026-06-11T12:15:00Z",
  "spells": {
    "delay-poison": {
      "fields": {
        "descriptionEs": "Texto revisado.",
        "translationStatus": "LOCKED"
      },
      "updatedAt": "2026-06-11T12:15:00Z"
    }
  }
}
```

Resultado:

```text
Advertencia: LOCKED incompleto. Faltan campos traducidos materializados.
```

Regla aplicada:

- `descriptionEs` queda protegida por override.
- Los campos ausentes se completan desde el dataset generado.
- El escritor de overrides debe corregir el archivo al siguiente guardado materializando todos los campos traducidos.

### Error: tipo incorrecto en `descriptors`

```json
{
  "fields": {
    "descriptors": "veneno"
  },
  "updatedAt": "2026-06-11T12:20:00Z"
}
```

Resultado:

```text
Error de validación: descriptors debe ser array de string.
```

## Criterios de aceptación

Este contrato se considera cumplido si:

- el dataset generado puede validarse de forma determinista;
- los overrides pueden validarse de forma determinista;
- el conjuro efectivo se calcula siempre con la misma prioridad;
- las correcciones manuales sobreviven a regeneraciones;
- las notas personales sobreviven a reimportaciones;
- los estados `REVIEWED`, `MANUALLY_EDITED` y `LOCKED` tienen comportamiento definido;
- los overrides huérfanos no se pierden;
- el texto inglés original se conserva;
- la búsqueda MVP puede indexar solo español efectivo y notas;
- SQLite puede reconstruirse desde archivos sin pérdida de datos manuales;
- el escritor de overrides no sobrescribe cambios no relacionados.
