# 02 - Modelo de dominio

## Propósito

Este documento define el modelo funcional de dominio de **Grimorio Pathfinder**.

No es un diseño de base de datos ni una especificación de clases finales. Su objetivo es fijar los conceptos, reglas e invariantes que deben respetar backend, importador, búsqueda, API y frontend.

El dominio debe mantenerse independiente de Spring, SQLite, JPA, controladores REST y detalles de infraestructura.

## Principios del dominio

- El conjuro se gestiona principalmente en español.
- El texto inglés original se conserva como referencia.
- La búsqueda del MVP usa solo contenido español efectivo y notas personales.
- Un conjuro puede pertenecer a varias listas de conjuros.
- Las listas de conjuros no deben limitarse a clases rígidas.
- Las correcciones manuales tienen prioridad sobre contenido generado.
- La base SQLite es una proyección local reconstruible, no la fuente canónica irremplazable.
- El dominio debe permitir reconstruirse desde dataset generado + overrides.

## Terminología canónica

Usar siempre:

```text
lista de clase lanzadora
```

para listas como Clérigo, Druida, Inquisidor, Bardo o Mago/Hechicero.

Usar:

```text
lista de conjuros
```

para el concepto general que también incluye dominios, subdominios, linajes, patronos, misterios, inquisiciones, escuelas, razas, arquetipos u otras agrupaciones.

## Entidades principales

## Spell

Representa un conjuro de Pathfinder 1e.

Un `Spell` contiene la información mecánica y textual necesaria para consultar, buscar y editar un conjuro.

En el dominio, `Spell` representa el conjuro efectivo: dataset generado más overrides aplicados.

### Identidad

Campos conceptuales:

- `id`
- `slug`
- `sourceId`
- `sourceHash`

### Reglas

- `id` debe ser estable.
- `id` no debe depender del identificador interno de SQLite.
- `slug` puede usarse para rutas o referencias humanas.
- `sourceId` identifica el conjuro dentro de la fuente original si existe.
- `sourceHash` permite detectar cambios en el texto o datos originales.
- Un cambio de `sourceHash` no debe invalidar ni borrar overrides automáticamente.

### Campos de nombre

- `nameEs`
- `nameEn`

Reglas:

- `nameEs` es el nombre usado por defecto en la aplicación.
- `nameEn` se conserva como referencia.
- La búsqueda MVP usa `nameEs`, no `nameEn`.
- `nameEs` puede editarse campo por campo.
- Editar `nameEs` debe generar o actualizar un override.
- El flujo normal del MVP no edita `nameEn`.

### Campos mecánicos

Campos conceptuales:

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

Reglas:

- Los campos mecánicos se guardan en español cuando se muestran o buscan.
- Si un campo no existe en un conjuro, debe poder quedar vacío o nulo según corresponda.
- No inventar datos que no estén en el dataset.
- No fusionar campos distintos en una descripción larga si pueden mantenerse estructurados.
- Los campos mecánicos deben ser editables.
- La edición de campos mecánicos debe persistir como override.

### Campos descriptivos

- `descriptionEs`
- `descriptionEn`

Reglas:

- `descriptionEs` es la descripción principal para consulta.
- `descriptionEn` se conserva como referencia.
- La búsqueda MVP usa `descriptionEs`, no `descriptionEn`.
- `descriptionEs` puede editarse.
- Editar `descriptionEs` debe generar o actualizar un override.
- El flujo normal del MVP no edita `descriptionEn`.

### Fuente

Campos conceptuales:

- `sourceBook`
- `sourcePage`
- `sourceName`

Reglas:

- Conservar la fuente/libro siempre que exista en el dataset.
- La fuente puede mostrarse en detalle y usarse en filtros futuros.
- No bloquear el importador si la página o fuente exacta falta.

### Estado de traducción

Campo:

- `translationStatus`

Valores permitidos:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

Reglas:

- `NOT_TRANSLATED`: no existe traducción española útil.
- `AI_TRANSLATED`: traducción generada automáticamente.
- `REVIEW_REQUIRED`: requiere revisión manual.
- `REVIEWED`: revisado y aceptado.
- `MANUALLY_EDITED`: el usuario ha corregido uno o varios campos.
- `LOCKED`: el conjuro efectivo está protegido frente a sobrescrituras automáticas.

Los estados no revisados se muestran igualmente, pero deben marcarse en la UI.

### Decisión sobre `LOCKED`

`LOCKED` es solo un estado de `translationStatus`.

No existe en el MVP:

- un booleano `locked` separado;
- un flag técnico adicional equivalente;
- un sistema de bloqueo por campo.

Semántica de `LOCKED`:

- impide que procesos automáticos sustituyan el conjuro efectivo;
- debe respetarse al regenerar o reimportar datos;
- debe persistirse como override en `fields.translationStatus`;
- no impide una edición manual explícita del usuario.

El bloqueo por campo puede considerarse en el futuro, pero queda fuera del MVP.

### Notas personales

Campo efectivo:

- `personalNotes`

Reglas:

- Las notas personales son texto libre del usuario.
- Las notas personales forman parte de la búsqueda.
- Las notas personales no proceden del dataset generado.
- Las notas personales se persisten canónicamente en `data/overrides/spells-es.overrides.json`.
- SQLite solo contiene una copia efectiva reconstruible para consulta y búsqueda.
- No existe archivo separado de notas en el MVP.
- Las notas personales deben conservarse al reconstruir la base SQLite.

### Fechas

Campos conceptuales:

- `createdAt`
- `updatedAt`
- `reviewedAt`

Reglas:

- `createdAt` indica cuándo se creó/importó el registro efectivo en la proyección local.
- `updatedAt` cambia al editar datos españoles, notas o estado.
- `reviewedAt` se informa cuando se marca como revisado.
- Las fechas son metadatos; no deben sustituir la identidad estable del conjuro.

## SpellListEntry

Representa que un conjuro pertenece a una lista de conjuros concreta en un nivel determinado.

Ejemplos:

```text
CLASS / Clérigo / 2
CLASS / Inquisidor / 3
DOMAIN / Curación / 4
BLOODLINE / Dracónico / 5
PATRON / Invierno / 2
```

### Campos

- `spellId`
- `listType`
- `listName`
- `level`

### Reglas

- Un conjuro puede tener muchas entradas de lista.
- La misma lista de conjuros puede contener muchos conjuros.
- `level` debe ser un entero mayor o igual que 0.
- El MVP debe centrarse en listas de clase lanzadora.
- El modelo debe permitir listas especiales futuras.
- No convertir `listName` en un enum cerrado si el dataset puede traer listas no previstas.

### Tipos de lista recomendados

Valores conceptuales:

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

- `CLASS` se usa para listas de clase lanzadora.
- `OTHER` permite importar datos que todavía no tengan categoría clara.
- Si hay duda durante importación, conservar la entrada y marcarla para revisión antes que descartarla.

## SpellDescriptor

Representa un descriptor de conjuro.

Ejemplos:

```text
miedo
mal
bien
legal
caótico
fuego
frío
veneno
enajenador
```

### Reglas

- Los descriptores se guardan en español para consulta y búsqueda.
- Deben poder filtrarse en fases futuras.
- Deben poder buscarse como texto en el MVP.
- No duplicar descriptores equivalentes por diferencias de mayúsculas o acentos.
- Si el dataset contiene descriptor inglés, conservarlo en datos de referencia o trazabilidad si el importador lo permite.

## SpellComponent

Representa los componentes de lanzamiento.

Ejemplos:

```text
V
S
M
F
FD
```

O sus versiones traducidas/expandidas:

```text
verbal
somático
material
foco
foco divino
```

### Reglas

- En el MVP puede conservarse como texto español normalizado.
- No es obligatorio modelar cada componente como entidad separada desde el inicio.
- Si el dataset trae componentes complejos, no perder el texto original.
- Debe poder mostrarse y buscarse.

## PersonalNotes

Representa las notas del usuario sobre un conjuro.

Aunque pueda implementarse como campo de `Spell`, conceptualmente debe tratarse como contenido propio del usuario.

### Reglas

- Se busca junto al texto español.
- No debe mezclarse con la descripción oficial/traducida.
- Debe sobrevivir a reimportaciones.
- Debe poder editarse desde la pantalla de detalle.
- No debe sobrescribirse por regeneración del dataset.
- Su fuente canónica del MVP es `spells-es.overrides.json`.

## TranslationOverride

Representa una corrección manual sobre el dataset generado.

### Campos conceptuales

- `spellId`
- `fields`
- `personalNotes`
- `updatedAt`
- `reason`

### Reglas

- El override afecta a campos concretos.
- El override tiene prioridad sobre el dataset generado.
- Reimportar el dataset debe reaplicar overrides.
- Un override no debe eliminar el texto inglés original.
- Los overrides permiten reconstruir el estado corregido de la aplicación.
- El estado `LOCKED` se expresa mediante `fields.translationStatus = "LOCKED"`.
- No existe `locked: true/false` en el contrato del MVP.

Ejemplo conceptual:

```json
{
  "spellId": "neutralize-poison",
  "fields": {
    "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
    "translationStatus": "MANUALLY_EDITED"
  },
  "personalNotes": "Muy útil para llevar preparado si esperamos venenos o drow."
}
```

## TranslationStatus

Estados permitidos:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

### Transiciones habituales

```text
NOT_TRANSLATED -> AI_TRANSLATED
AI_TRANSLATED -> REVIEW_REQUIRED
AI_TRANSLATED -> REVIEWED
REVIEW_REQUIRED -> REVIEWED
AI_TRANSLATED -> MANUALLY_EDITED
REVIEW_REQUIRED -> MANUALLY_EDITED
REVIEWED -> MANUALLY_EDITED
MANUALLY_EDITED -> LOCKED
REVIEWED -> LOCKED
LOCKED -> MANUALLY_EDITED, si el usuario desbloquea o cambia estado explícitamente
```

### Reglas

- `LOCKED` debe impedir sobrescrituras automáticas.
- `MANUALLY_EDITED` debe aplicarse al editar campos españoles, salvo que el estado efectivo actual sea `LOCKED` o el usuario seleccione otro estado explícitamente.
- `REVIEWED` indica aceptación manual.
- `REVIEW_REQUIRED` no impide mostrar el conjuro.
- No ocultar conjuros por estado de traducción en el MVP.

## Value Objects recomendados

Los siguientes conceptos pueden implementarse como value objects si aporta claridad.

## StableSpellId

Identificador estable del conjuro.

Reglas:

- No depende de SQLite.
- No cambia al reimportar.
- Debe ser apto para enlazar overrides.
- Debe derivarse de forma predecible desde la fuente o dataset.

## SpellLevel

Nivel de conjuro dentro de una lista de conjuros.

Reglas:

- Entero mayor o igual que 0.
- No representa nivel de personaje.
- No representa nivel de lanzador.
- Se interpreta dentro de una lista concreta.

## SearchableText

Texto español preparado para búsqueda.

Reglas:

- Puede combinar campos españoles y notas personales.
- No debe incluir texto inglés en el MVP.
- Debe normalizar mayúsculas, acentos, puntuación básica y espacios repetidos para mejorar búsqueda.
- No debe reemplazar los campos originales editables.

## SourceReference

Referencia a la fuente del conjuro.

Campos posibles:

- `book`
- `page`
- `section`
- `externalSource`

Reglas:

- Puede estar incompleto.
- Debe conservarse si existe.
- No debe bloquear la importación si falta algún dato.

## Reglas de edición

La aplicación debe permitir edición campo por campo.

Campos editables en MVP:

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
- `personalNotes`
- `translationStatus`

Reglas:

- Editar un campo español crea o actualiza un override en `fields`.
- Editar notas personales crea o actualiza `personalNotes` en overrides.
- Editar un campo debe actualizar `updatedAt`.
- Editar un campo traducido debe marcar el conjuro como `MANUALLY_EDITED`, salvo que el conjuro esté `LOCKED` o el usuario seleccione otro estado explícitamente.
- Si el conjuro está `LOCKED`, una edición manual explícita puede cambiar el contenido, pero no debe perder el estado `LOCKED` salvo que el usuario cambie el estado.
- El texto inglés no debe editarse desde el flujo normal del MVP.

## Reglas de búsqueda relacionadas con dominio

La búsqueda principal del MVP usa:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

El dominio debe permitir responder esta pregunta:

> Dame los conjuros de una lista de conjuros concreta cuyo nivel sea menor o igual que N y cuyo contenido español efectivo o notas contengan el texto buscado.

Reglas:

- El filtro de nivel máximo se aplica sobre `SpellListEntry.level`.
- El filtro de lista se aplica sobre `SpellListEntry.listType` y `SpellListEntry.listName`.
- En el MVP, el flujo principal usa `listType = CLASS`.
- El texto buscado se aplica solo sobre campos españoles y notas.
- No usar `descriptionEn` ni `nameEn` para búsqueda MVP.
- Las notas personales pueden hacer que un conjuro aparezca aunque el término no esté en la descripción oficial.

## Invariantes

Estas reglas deben cumplirse siempre:

- Todo `Spell` debe tener `id` estable.
- Todo `Spell` debe conservar `nameEn` o referencia original si existe.
- Todo `Spell` debe conservar `descriptionEn` o referencia original si existe.
- Todo `SpellListEntry` debe apuntar a un `Spell` existente en el modelo efectivo importado.
- `SpellListEntry.level` no puede ser negativo.
- Un override nunca debe eliminar el texto inglés original.
- Un override manual tiene prioridad sobre contenido generado.
- `LOCKED` no debe sobrescribirse automáticamente.
- La búsqueda MVP no debe incluir texto inglés.
- Las notas personales deben sobrevivir a reconstrucciones de SQLite.
- El dominio no debe depender de infraestructura.

## Relación con SQLite

SQLite es una proyección local para consulta rápida.

El modelo de dominio no debe asumir:

- nombres de tablas;
- claves autoincrementales;
- detalles de índices;
- detalles de FTS;
- detalles de migraciones.

Estos detalles pertenecen a infraestructura.

La fuente canónica sigue siendo:

```text
data/generated/spells-es.generated.json
data/overrides/spells-es.overrides.json
```

## Relación con API REST

Los DTOs REST no son el dominio.

Reglas:

- No exponer entidades de dominio directamente si eso acopla la API al modelo interno.
- Usar DTOs de listado y detalle.
- El detalle debe incluir texto inglés como referencia.
- Los resultados de búsqueda deben ser compactos.
- La lógica de filtrado y búsqueda no debe vivir en controladores.
- La documentación actual define contrato funcional mínimo.
- La especificación REST cerrada debe quedar en `docs/10-api-rest.md`.

## Relación con frontend

El frontend puede mostrar vistas distintas del mismo dominio:

- resultado compacto;
- detalle completo;
- formulario de edición;
- panel de notas;
- indicador de estado de traducción.

Reglas:

- El frontend no decide reglas de sobrescritura.
- El frontend no reconstruye overrides por su cuenta.
- El frontend no traduce.
- El frontend no busca en inglés en el MVP.

## Fuera del modelo MVP

No modelar todavía salvo necesidad explícita:

- personajes;
- conjuros conocidos;
- conjuros preparados;
- slots diarios;
- CD calculada;
- favoritos;
- etiquetas personales;
- usuarios;
- sincronización;
- historial completo de versiones;
- memoria de traducción completa;
- bloqueo por campo.

El diseño debe permitir añadir esas funciones más adelante sin obligar a rehacer `Spell` y `SpellListEntry`.

## Criterios de aceptación del modelo

El modelo de dominio es válido si permite:

- representar todos los conjuros del dataset español;
- conservar el texto inglés;
- representar pertenencia a múltiples listas de conjuros;
- soportar listas especiales futuras;
- buscar por lista de clase lanzadora, nivel máximo y texto español;
- incluir notas personales en búsqueda;
- editar campos españoles;
- persistir correcciones como overrides;
- evitar sobrescritura de correcciones manuales;
- funcionar sin servicios externos;
- mantenerse independiente de infraestructura.
