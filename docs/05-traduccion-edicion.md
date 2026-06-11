# 05 - Traducción y edición

## Propósito

Este documento define cómo **Grimorio Pathfinder** debe tratar las traducciones de conjuros y su edición manual.

La aplicación principal no es un traductor interactivo. Su función es gestionar, consultar, buscar y corregir una biblioteca de conjuros ya disponible en español.

Este documento debe leerse junto a:

```text
docs/02-modelo-dominio.md
docs/03-dataset-importacion-overrides.md
docs/04-busqueda-navegacion.md
```

## Decisión principal

La aplicación trabaja con conjuros ya guardados en español.

No debe traducir conjuros al vuelo durante el uso normal.

La traducción IA pertenece al proceso de generación o mantenimiento del dataset, no al flujo habitual de consulta en mesa.

Flujo conceptual:

```text
fuente inglesa
    ↓
pipeline de generación/traducción
    ↓
data/generated/spells-es.generated.json
    ↓
overrides manuales
    ↓
SQLite local
    ↓
consulta, búsqueda y edición en la app
```

## Terminología canónica

- `lista de clase lanzadora`: lista de conjuros de una clase como Clérigo, Druida, Inquisidor o Mago/Hechicero.
- `lista de conjuros`: concepto general que incluye listas de clase lanzadora y listas especiales.

## Decisiones cerradas del MVP

### `LOCKED`

`LOCKED` es únicamente un valor de `translationStatus`.

No existe en el MVP un flag técnico adicional llamado `locked`.

Semántica:

- protege el conjuro frente a sobrescrituras automáticas;
- se persiste como `fields.translationStatus = "LOCKED"` en overrides;
- no impide una edición manual explícita;
- no representa bloqueo por campo.

El bloqueo por campo queda fuera del MVP.

### `personalNotes`

`personalNotes` vive canónicamente en:

```text
data/overrides/spells-es.overrides.json
```

No vive en `spells-es.generated.json` ni en un archivo separado durante el MVP.

SQLite solo guarda una copia efectiva para consulta y búsqueda.

## Objetivos

El sistema debe permitir:

- conservar el texto inglés original como referencia;
- mostrar y buscar el conjuro en español;
- editar traducciones campo por campo;
- guardar correcciones manuales como overrides;
- proteger correcciones frente a regeneraciones futuras;
- marcar estados de traducción y revisión;
- mantener un estilo de traducción breve y útil en mesa.

## Fuera de alcance del MVP

No implementar en el MVP salvo petición explícita:

- traducción IA desde la interfaz;
- pantalla administrativa de traducciones;
- memoria de traducción completa;
- revisión por lotes desde la app;
- reintentos automáticos de traducción;
- comparación visual avanzada de versiones;
- historial completo de versiones;
- búsqueda en inglés;
- sincronización con servicios externos;
- bloqueo por campo.

Estas funciones pueden existir más adelante en herramientas de generación o administración, pero no forman parte del uso normal de la aplicación.

## Principios de traducción

### 1. Traducción de mesa

El estilo debe ser claro, breve y práctico durante partida.

Prioridades:

1. precisión mecánica;
2. claridad;
3. brevedad;
4. consistencia;
5. lectura cómoda en tablet.

Ejemplos de estilo preferido:

```text
TS: Fortaleza niega
RC: sí
Duración: 1 min./nivel
Alcance: toque
Tiempo de lanzamiento: 1 acción estándar
```

Evitar traducciones largas o literarias si una forma breve es más útil en mesa.

### 2. El español es la vista principal

La aplicación muestra y busca principalmente en español.

El inglés se conserva como referencia secundaria.

### 3. No inventar reglas

La traducción no debe añadir, eliminar ni reinterpretar reglas.

No se deben inventar:

- números;
- dados;
- duraciones;
- objetivos;
- áreas;
- acciones;
- componentes;
- tiradas de salvación;
- resistencia a conjuros;
- condiciones;
- límites;
- excepciones.

### 4. Las correcciones manuales mandan

Si el usuario corrige un campo, esa corrección tiene prioridad sobre cualquier traducción generada.

Una corrección manual no debe sobrescribirse automáticamente.

### 5. El texto inglés no se edita en el MVP

El flujo normal de edición permite corregir campos españoles y notas personales.

El texto inglés original se conserva como referencia y no debe editarse desde la UI del MVP.

## Campos traducidos

Los campos principales en español son:

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

Además, la app permite editar:

- `personalNotes`
- `translationStatus`

## Campos ingleses de referencia

Campos de referencia:

- `nameEn`
- `descriptionEn`
- otros campos originales si el dataset los conserva.

Reglas:

- Deben conservarse si existen.
- Deben estar disponibles en el detalle.
- No participan en la búsqueda del MVP.
- No deben ser sobrescritos por overrides manuales.
- No deben eliminarse al editar la traducción.

## Estados de traducción

Estados permitidos:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

## Significado de estados

### `NOT_TRANSLATED`

No existe traducción española útil.

Reglas:

- El conjuro puede mostrarse igualmente.
- Debe marcarse claramente.
- Puede requerir revisión o generación posterior.

### `AI_TRANSLATED`

Existe traducción generada automáticamente.

Reglas:

- Puede mostrarse en mesa.
- No implica que haya sido revisada.
- Puede pasar a `REVIEWED`, `REVIEW_REQUIRED` o `MANUALLY_EDITED`.

### `REVIEW_REQUIRED`

La traducción requiere revisión manual.

Reglas:

- No debe ocultar el conjuro.
- Debe marcarse de forma visible pero no intrusiva.
- Puede proceder de validaciones automáticas o de una marca manual.

### `REVIEWED`

La traducción ha sido revisada y aceptada.

Reglas:

- Indica confianza razonable.
- Puede editarse más adelante si se detecta un problema.

### `MANUALLY_EDITED`

El usuario ha corregido uno o varios campos.

Reglas:

- Debe aplicarse al editar campos traducidos, salvo que el conjuro esté `LOCKED` o el usuario seleccione otro estado explícitamente.
- Tiene prioridad sobre contenido generado.
- Debe persistirse como override.

### `LOCKED`

El conjuro está protegido frente a sobrescrituras automáticas.

Reglas:

- No debe sobrescribirse por procesos automáticos.
- Puede usarse para traducciones revisadas y cerradas.
- Si en el futuro existe pipeline de traducción, debe respetar este estado.
- No existe un campo `locked` adicional en el MVP.
- No bloquea una edición manual explícita del usuario.

## Transiciones habituales

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

No es obligatorio implementar una máquina de estados estricta en el MVP, pero sí deben respetarse estas reglas funcionales.

## Edición campo por campo

La edición debe ser campo por campo, no solo sobre un bloque completo de texto.

Campos editables en MVP:

- nombre español;
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
- descripción española;
- notas personales;
- estado de traducción/revisión.

## Reglas de edición

Cuando el usuario edita un campo traducido:

```text
1. Actualizar el valor efectivo en SQLite.
2. Crear o actualizar el override correspondiente dentro de fields.
3. Marcar el conjuro como MANUALLY_EDITED, salvo que el conjuro esté LOCKED o el usuario elija explícitamente otro estado.
4. Actualizar índices si el campo es buscable.
5. Conservar el texto inglés original.
```

Cuando el usuario edita notas personales:

```text
1. Actualizar notas en SQLite.
2. Persistir notas como personalNotes en spells-es.overrides.json.
3. Actualizar índices de búsqueda.
4. No modificar descripción española.
5. No modificar estado de traducción salvo que se indique explícitamente.
```

Cuando el usuario edita un conjuro `LOCKED`:

```text
1. La edición manual explícita está permitida.
2. El estado debe seguir siendo LOCKED salvo que el usuario cambie el estado.
3. La protección frente a procesos automáticos debe mantenerse.
```

## Overrides

Las ediciones manuales deben persistirse como overrides.

Archivo conceptual:

```text
data/overrides/spells-es.overrides.json
```

Ejemplo:

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
      "personalNotes": "Muy útil si esperamos drow o enemigos con venenos.",
      "updatedAt": "2026-06-11T00:00:00Z"
    }
  }
}
```

Ejemplo de bloqueo:

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

## Reglas de overrides

- No escribir correcciones manuales en `spells-es.generated.json`.
- No eliminar overrides existentes al editar otro campo.
- No eliminar notas al editar traducciones.
- No eliminar traducciones al editar notas.
- No sustituir texto inglés mediante overrides en el MVP.
- Reimportar el dataset debe reaplicar overrides.
- Un override huérfano debe conservarse y reportarse como advertencia.
- No crear ni usar `locked` como flag; usar `fields.translationStatus = "LOCKED"`.
- `personalNotes` debe guardarse como propiedad directa de la entrada de override, no dentro de `fields`.

## Visualización en la app

La pantalla de detalle debe priorizar el texto español.

Estructura recomendada:

1. cabecera;
2. campos mecánicos en español;
3. descripción española;
4. notas personales;
5. estado de traducción;
6. texto inglés original plegable o secundario;
7. acciones de edición.

## Texto inglés en detalle

El texto inglés debe estar disponible como referencia.

Reglas:

- Debe ser secundario o plegable.
- No debe competir visualmente con el texto español.
- No debe participar en búsqueda.
- Debe ayudar a revisar o corregir traducciones.

## Notas personales

Las notas personales son contenido del usuario.

Reglas:

- No son texto oficial del conjuro.
- No son traducción.
- Deben distinguirse visualmente de `descriptionEs`.
- Deben ser editables.
- Deben buscarse.
- Deben sobrevivir a reimportaciones.
- Deben persistirse en `spells-es.overrides.json`.
- No deben alterar `translationStatus` salvo acción explícita.

Ejemplos:

```text
Preparar si esperamos drow.
Útil contra venenos y enfermedades.
Situacional; revisar antes de preparar.
Muy bueno en mazmorras largas.
```

## Estilo terminológico

Usar terminología breve y consistente.

Tabla inicial recomendada:

| Concepto | Forma preferida |
|---|---|
| Saving Throw | TS |
| Spell Resistance | RC |
| Fortitude | Fortaleza |
| Reflex | Reflejos |
| Will | Voluntad |
| Caster level | nivel de lanzador |
| Standard action | acción estándar |
| Swift action | acción rápida |
| Immediate action | acción inmediata |
| Full-round action | acción de asalto completo |
| Range | alcance |
| Target | objetivo |
| Area | área |
| Effect | efecto |
| Duration | duración |
| Components | componentes |

## Ejemplos de formato

### Tiradas de salvación

Formas preferidas:

```text
TS: Fortaleza niega
TS: Reflejos mitad
TS: Voluntad parcial
TS: ninguno
```

### Resistencia a conjuros

Formas preferidas:

```text
RC: sí
RC: no
RC: sí (inofensivo)
```

### Duración

Formas preferidas:

```text
instantáneo
1 asalto/nivel
1 min./nivel
10 min./nivel
1 hora/nivel
```

### Alcance

Formas preferidas:

```text
personal
toque
corto
medio
largo
```

## Validaciones de traducción

Aunque la validación automática completa pertenece al pipeline futuro, el diseño debe permitir detectar problemas.

La traducción debe conservar:

- dados: `1d6`, `2d8`, etc.;
- números relevantes;
- bonificadores y penalizadores;
- CD;
- niveles;
- duraciones;
- acciones;
- componentes;
- TS;
- RC;
- referencias a otros conjuros.

## Validaciones mínimas futuras

Ejemplos:

```text
Original contiene 1d6 -> traducción debe contener 1d6
Original contiene Fortitude -> traducción debería conservar Fortaleza/TS
Original contiene Spell Resistance -> traducción debería reflejar RC
Original contiene 1 round/level -> traducción debería reflejar 1 asalto/nivel
```

Si una validación falla, el estado puede pasar a:

```text
REVIEW_REQUIRED
```

## Dataset generado

El dataset generado puede contener traducciones IA.

Archivo:

```text
data/generated/spells-es.generated.json
```

Reglas:

- Puede regenerarse.
- No contiene correcciones manuales finales.
- No contiene `personalNotes`.
- Debe conservar texto inglés.
- Debe traer estados iniciales de traducción.
- Debe poder combinarse con overrides.

## Pipeline de traducción futuro

No forma parte del MVP, pero el diseño debe permitir un pipeline futuro.

Responsabilidades posibles:

- leer CSV o fuentes inglesas;
- normalizar campos;
- traducir por lotes;
- aplicar glosario;
- reutilizar memoria de traducción;
- validar números, dados y reglas;
- generar `spells-es.generated.json`;
- marcar conjuros como `AI_TRANSLATED` o `REVIEW_REQUIRED`.

Regla clave:

- el pipeline puede regenerar `spells-es.generated.json`;
- el pipeline no debe sobrescribir `spells-es.overrides.json`;
- si consume overrides, debe respetar `translationStatus = LOCKED`.

## Memoria de traducción futura

La memoria de traducción puede existir como herramienta auxiliar del pipeline.

Reglas:

- No reconstruye conjuros dinámicamente durante la consulta.
- No forma parte de la búsqueda normal.
- No sustituye los campos españoles guardados.
- Sirve para generar traducciones consistentes.
- Puede guardar segmentos o frases aprobadas.

La app debe mostrar el conjuro español completo ya guardado.

## Regla importante sobre reconstrucción

No hacer esto en uso normal:

```text
abrir conjuro
    ↓
buscar frases traducidas
    ↓
reconstruir descripción al vuelo
```

Hacer esto:

```text
abrir conjuro
    ↓
leer conjuro efectivo desde SQLite
    ↓
mostrar campos españoles ya guardados
```

La memoria de traducción pertenece a generación/mantenimiento del dataset, no a la consulta en mesa.

## Edición y búsqueda

Si se edita un campo buscable, la búsqueda debe reflejarlo.

Campos editables que afectan a búsqueda:

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

Reglas:

- Tras editar, actualizar SQLite.
- Tras editar, actualizar índice.
- La siguiente búsqueda debe usar el valor efectivo.
- No buscar en el valor inglés aunque exista.

## Conflictos entre generated y overrides

Si un campo existe en ambos lugares:

```text
override > generated
```

Si un override existe para un conjuro eliminado del dataset generado:

- no borrarlo automáticamente;
- reportarlo como advertencia;
- no romper la importación completa.

Si un conjuro cambia de `sourceHash`:

- no borrar overrides automáticamente;
- aplicar overrides si el `spellId` sigue siendo válido;
- marcar para revisión si procede en una fase futura.

## Bloqueo de traducciones

`LOCKED` se usa para proteger una traducción.

Reglas:

- Se aplica al conjuro completo en el MVP.
- No existe bloqueo por campo en el MVP.
- Un proceso automático no debe sobrescribir un conjuro bloqueado.
- El usuario puede cambiar manualmente el contenido si la UI lo permite.
- El usuario puede cambiar manualmente el estado si la UI lo permite.
- El bloqueo se persiste como `translationStatus`, no como flag separado.

## Relación con API REST

La API debe permitir:

- obtener detalle con campos españoles;
- obtener texto inglés de referencia;
- actualizar campos españoles;
- actualizar notas personales;
- cambiar estado de traducción/revisión.

Endpoints conceptuales:

```text
GET /api/spells/{spellId}
PATCH /api/spells/{spellId}
PATCH /api/spells/{spellId}/notes
PATCH /api/spells/{spellId}/translation-status
```

Las rutas finales pueden cambiar, pero deben cubrir estos casos de uso.

El contrato REST definitivo debe documentarse en:

```text
docs/10-api-rest.md
```

Hasta que exista ese documento, estos endpoints son contrato funcional mínimo, no especificación OpenAPI final.

## Relación con frontend

El frontend debe permitir:

- ver campos españoles;
- ver inglés como referencia secundaria;
- editar campos españoles;
- editar notas;
- cambiar estado;
- distinguir texto traducido de notas personales;
- indicar estado de revisión.

El frontend no debe:

- traducir;
- llamar a IA;
- escribir directamente en `spells-es.generated.json`;
- buscar en inglés;
- decidir reglas de sobrescritura;
- crear flags técnicos de bloqueo fuera de `translationStatus`.

## Casos funcionales

### Caso 1 - Corregir descripción

El usuario abre un conjuro y cambia `descriptionEs`.

Resultado esperado:

- se actualiza SQLite;
- se crea/actualiza override en `fields.descriptionEs`;
- el conjuro pasa a `MANUALLY_EDITED`, salvo que estuviera `LOCKED` o se indique otro estado explícitamente;
- el texto inglés permanece intacto;
- la búsqueda usa la nueva descripción.

### Caso 2 - Añadir nota

El usuario añade:

```text
Preparar si esperamos drow.
```

Resultado esperado:

- la nota se guarda en `personalNotes` dentro de overrides;
- la nota se busca;
- la descripción española no cambia;
- el estado de traducción no cambia salvo petición explícita.

### Caso 3 - Reimportar dataset

Se regenera `spells-es.generated.json`.

Resultado esperado:

- los overrides se aplican;
- las correcciones manuales siguen presentes;
- las notas siguen presentes;
- los conjuros `LOCKED` no se sobrescriben automáticamente.

### Caso 4 - Ver texto inglés

El usuario abre un conjuro y despliega el texto inglés.

Resultado esperado:

- puede consultar el original;
- no se mezcla con la descripción española;
- no afecta a búsqueda;
- no se edita desde el flujo normal.

### Caso 5 - Marcar revisado

El usuario marca un conjuro como `REVIEWED`.

Resultado esperado:

- se actualiza el estado;
- se persiste en `fields.translationStatus` si procede;
- el resultado de búsqueda muestra el estado actualizado.

### Caso 6 - Bloquear conjuro

El usuario marca un conjuro como `LOCKED`.

Resultado esperado:

- se actualiza `translationStatus`;
- se persiste en `fields.translationStatus`;
- no se crea `locked`;
- los procesos automáticos no deben sobrescribirlo.

## Tests prioritarios

Cubrir al menos:

1. Editar `descriptionEs` crea override.
2. Editar `nameEs` crea override.
3. Editar notas no modifica descripción.
4. Editar descripción no elimina notas.
5. Reimportar conserva campos manuales.
6. Reimportar conserva notas.
7. Texto inglés se conserva tras edición.
8. Campo editado entra en búsqueda.
9. Nota editada entra en búsqueda.
10. `LOCKED` no se sobrescribe automáticamente.
11. `MANUALLY_EDITED` tiene prioridad sobre `AI_TRANSLATED`.
12. El MVP no busca en inglés.
13. `LOCKED` se persiste como `translationStatus`, no como flag.
14. Editar un conjuro `LOCKED` manualmente no pierde el estado salvo cambio explícito.

## Criterios de aceptación

La gestión de traducción y edición es correcta si:

- la app no traduce al vuelo;
- el español es la vista principal;
- el inglés se conserva como referencia;
- los campos españoles son editables;
- las notas personales son editables y buscables;
- las ediciones se guardan como overrides;
- las correcciones sobreviven a reimportaciones;
- los estados de traducción son visibles;
- `LOCKED` no tiene doble semántica;
- no se sobrescriben correcciones manuales;
- el flujo funciona completamente offline.
