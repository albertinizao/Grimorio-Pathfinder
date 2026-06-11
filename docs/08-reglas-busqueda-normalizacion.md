# 08 - Reglas de búsqueda y normalización

## Propósito

Este documento define el contrato funcional y técnico de la búsqueda del MVP de **Grimorio Pathfinder**.

Su objetivo es cerrar de forma precisa:

- qué datos entran en el índice de búsqueda;
- qué datos quedan fuera;
- cómo se normaliza el texto;
- cómo se interpretan términos y frases;
- cómo se calculan coincidencias;
- cómo se ordenan resultados;
- qué fragmento se devuelve;
- cómo afectan las notas personales;
- cómo debe actualizarse la búsqueda tras editar overrides.

Este documento actúa como contrato para:

- motor de búsqueda;
- índice SQLite o FTS;
- frontend de resultados;
- pruebas automatizadas de búsqueda.

## Alcance

Este documento define reglas de búsqueda, normalización, ordenación y fragmentos de resultados.

No define:

- diseño visual de la interfaz, salvo campos mínimos visibles en resultados;
- endpoints REST definitivos;
- esquema físico concreto de tablas SQLite;
- migraciones SQL concretas;
- búsqueda en inglés;
- búsqueda semántica;
- favoritos, etiquetas o perfiles de personaje.

La búsqueda del MVP debe funcionar completamente offline sobre SQLite, usando el conjuro efectivo resultante de combinar:

```text
data/generated/spells-es.generated.json
        +
data/overrides/spells-es.overrides.json
        ↓
spell efectivo indexado en SQLite
```

## 1. Objetivo funcional de la búsqueda

La búsqueda debe permitir encontrar conjuros útiles de forma rápida durante una partida o preparación.

El caso principal del MVP es:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

Ejemplo:

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 3
Query: veneno
```

Resultado esperado:

```text
Conjuros de la lista de clase lanzadora de Clérigo de nivel 0, 1, 2 o 3
que contengan el término normalizado "veneno" en campos españoles efectivos
o en notas personales.
```

Decisión de producto:

- se prioriza la utilidad en mesa frente a una precisión lingüística estricta;
- la búsqueda debe tolerar falta de tildes, diferencias de mayúsculas, puntuación irregular y espacios repetidos;
- no debe inventar sinónimos ni traducir términos;
- no debe buscar en inglés durante el MVP.

## 2. Parámetros de búsqueda

La búsqueda principal recibe estos parámetros conceptuales:

| Parámetro | Obligatorio | Descripción |
|---|---:|---|
| `listType` | sí | Tipo de lista de conjuros. En el flujo principal será `CLASS`. |
| `listName` | sí | Nombre de la lista de clase lanzadora o lista de conjuros seleccionada. |
| `maxLevel` | sí | Nivel máximo incluido dentro de esa lista. |
| `q` / `query` | no | Término, conjunto de términos o frase entre comillas. |

### 2.1. Lista/clase

La búsqueda debe filtrar por `SpellListEntry.listType` y `SpellListEntry.listName`.

Para el MVP, el uso principal es:

```text
listType = CLASS
```

Ejemplos de `listName`:

```text
Clérigo
Druida
Inquisidor
Mago/Hechicero
Bardo
Paladín
Explorador
```

Reglas:

- `listName` no debe convertirse en un enum cerrado.
- El filtrado de lista debe realizarse sobre la pertenencia efectiva del conjuro a listas de conjuros.
- Un conjuro con varias listas puede aparecer o no según la lista seleccionada y el nivel que tenga dentro de esa lista.
- El resultado debe mostrar el nivel correspondiente a la lista seleccionada, no el nivel más bajo global del conjuro.

### 2.2. Nivel máximo

`maxLevel` filtra conjuros cuyo nivel en la lista seleccionada sea menor o igual al valor indicado.

Reglas:

- `maxLevel` debe ser un entero `>= 0`.
- `0` es válido.
- El filtro se aplica sobre `SpellListEntry.level`.
- No representa nivel de personaje.
- No representa nivel de lanzador.
- No representa nivel universal del conjuro fuera de la lista seleccionada.

Ejemplo:

```text
Clérigo + maxLevel = 3
```

Incluye:

```text
Clérigo 0
Clérigo 1
Clérigo 2
Clérigo 3
```

Excluye:

```text
Clérigo 4+
```

### 2.3. Término/frase

`q` puede contener:

- una palabra;
- varias palabras;
- una frase exacta entre comillas;
- texto vacío, `null` o solo espacios.

Ejemplos:

```text
veneno
daño fuego
proteccion
"daño de fuego"
TIRADA   DE   SALVACIÓN
```

Reglas:

- La búsqueda textual se aplica después de filtrar por lista y nivel.
- La consulta se normaliza con las mismas reglas que el índice.
- Si `q` está vacío, no se aplica filtro textual.
- El MVP no define operadores avanzados como `escuela:`, `descriptor:` o `fuente:`.

## 3. Campos incluidos en el índice

El índice de búsqueda debe construirse únicamente con campos españoles efectivos y notas personales.

Campos incluidos:

| Campo | Peso funcional | Uso |
|---|---:|---|
| `nameEs` | alto | Encontrar por nombre español. |
| `descriptionEs` | alto | Encontrar por texto principal del conjuro. |
| `school` | medio | Encontrar por escuela. |
| `subschool` | medio | Encontrar por subescuela. |
| `descriptors` | medio | Encontrar por descriptores. |
| `castingTime` | bajo | Encontrar por tiempo de lanzamiento. |
| `components` | bajo | Encontrar por componentes. |
| `range` | bajo | Encontrar por alcance. |
| `target` | bajo | Encontrar por objetivo. |
| `effect` | bajo | Encontrar por efecto. |
| `area` | bajo | Encontrar por área. |
| `duration` | bajo | Encontrar por duración. |
| `savingThrow` | bajo | Encontrar por tirada de salvación. |
| `spellResistance` | bajo | Encontrar por resistencia a conjuros. |
| `personalNotes` | medio | Encontrar por notas del usuario. |

Reglas:

- Todos los campos deben ser los valores efectivos tras aplicar overrides.
- `descriptors` debe indexarse como texto concatenado de sus valores efectivos.
- Los campos `null` no aportan texto al índice.
- Los campos vacíos `""` no aportan texto al índice.
- El índice debe permitir identificar el campo donde se produjo la coincidencia principal.

## 4. Campos excluidos del índice

No deben entrar en el índice textual del MVP:

- `nameEn`;
- `descriptionEn`;
- cualquier texto inglés original;
- `id`;
- `slug`;
- `sourceId`;
- `sourceHash`;
- `sourceBook`;
- `sourcePage`;
- `sourceName`;
- `translationStatus`;
- `createdAt`;
- `updatedAt`;
- `reviewedAt`;
- logs de importación;
- advertencias de importación;
- nombres de archivo;
- metadatos técnicos de SQLite.

Reglas:

- El texto inglés puede mostrarse en detalle, pero no debe afectar a resultados del MVP.
- La fuente/libro puede mostrarse en detalle o usarse como filtro futuro, pero no forma parte de la búsqueda textual MVP.
- `translationStatus` puede mostrarse en resultados, pero no debe hacer aparecer un conjuro por coincidencia textual.

## 5. Normalización recomendada

La normalización debe ser idéntica para:

- texto indexado;
- consulta introducida por el usuario;
- claves auxiliares de ordenación alfabética.

La normalización no modifica los campos originales. Solo genera texto auxiliar para búsqueda y ordenación.

### 5.1. Algoritmo normativo de normalización

Dado un texto de entrada, aplicar en este orden:

```text
1. Si el valor es null, convertir a cadena vacía.
2. Convertir a minúsculas.
3. Normalizar Unicode a forma NFD o NFKD.
4. Eliminar marcas diacríticas combinantes.
5. Convertir ñ/Ñ normalizada a n.
6. Convertir ç/Ç a c.
7. Sustituir puntuación y símbolos separadores por espacios.
8. Conservar letras y números.
9. Conservar secuencias alfanuméricas relevantes como 1d6, 2d8 o 10.
10. Colapsar espacios repetidos a un único espacio.
11. Recortar espacios iniciales y finales.
```

Resultado:

```text
"TIRADA   DE   SALVACIÓN" -> "tirada de salvacion"
"protección"              -> "proteccion"
"daño de fuego"           -> "dano de fuego"
"1 min./nivel"            -> "1 min nivel"
"Mago/Hechicero"          -> "mago hechicero"
"TS: Fortaleza niega"     -> "ts fortaleza niega"
```

### 5.2. Acentos y diacríticos

La búsqueda debe ignorar acentos y diacríticos.

Ejemplos:

| Query | Debe poder encontrar |
|---|---|
| `proteccion` | `protección` |
| `salvacion` | `salvación` |
| `conjuracion` | `conjuración` |
| `dano` | `daño` |

Decisión explícita:

- `ñ` se normaliza como `n`.
- Esta decisión puede producir alguna coincidencia menos precisa, pero mejora la utilidad en mesa cuando se escribe rápido o sin teclado español.

### 5.3. Mayúsculas/minúsculas

La búsqueda debe ignorar diferencias de mayúsculas y minúsculas.

Ejemplos equivalentes:

```text
veneno
VENENO
Veneno
VeNeNo
```

### 5.4. Puntuación

La puntuación básica debe tratarse como separador.

Caracteres que deben actuar como espacio:

```text
. , ; : / \ | - _ ( ) [ ] { } ¿ ? ¡ ! " ' « » “ ” ‘ ’
```

Reglas:

- No hacer que la puntuación impida encontrar una frase.
- `min./nivel` debe poder encontrarse con `min nivel`.
- `Mago/Hechicero` debe normalizarse como dos tokens.
- `TS: Fortaleza` debe normalizarse como `ts fortaleza`.

### 5.5. Espacios repetidos

Cualquier secuencia de espacios, saltos de línea o tabulaciones debe convertirse en un único espacio.

Ejemplo:

```text
"tirada   de\n salvación" -> "tirada de salvacion"
```

## 6. Búsqueda por frase

Una frase exacta se indica cuando la consulta empieza y termina con comillas dobles después de recortar espacios.

Ejemplo:

```text
"daño de fuego"
```

Reglas:

- Se elimina la comilla exterior.
- Se normaliza el contenido de la frase.
- Se busca la secuencia exacta de tokens normalizados.
- Las palabras deben aparecer juntas y en el mismo orden.
- Se ignoran mayúsculas, acentos, puntuación básica y espacios repetidos.
- La frase puede coincidir dentro de cualquiera de los campos buscables.
- La frase no puede cruzar de un campo a otro.

Ejemplos:

| Query | Texto efectivo | Resultado |
|---|---|---|
| `"daño de fuego"` | `inflige daño de fuego al objetivo` | coincide |
| `"daño de fuego"` | `daño. De fuego` | coincide tras normalización |
| `"daño de fuego"` | `daño por fuego` | no coincide |
| `"fuego daño"` | `daño de fuego` | no coincide |

### 6.1. Comillas incompletas

Si la consulta contiene una sola comilla exterior o comillas no balanceadas, el MVP debe tratar la consulta como búsqueda normal sin comillas, eliminando las comillas como puntuación.

Ejemplo:

```text
"daño de fuego
```

Se interpreta como:

```text
daño de fuego
```

Motivo:

- en mesa es preferible devolver resultados útiles antes que fallar por sintaxis menor.

## 7. Búsqueda parcial

La búsqueda sin comillas se interpreta como búsqueda por términos.

Ejemplo:

```text
daño fuego
```

Reglas:

1. Normalizar la consulta.
2. Dividir la consulta en tokens por espacio.
3. Eliminar tokens vacíos.
4. Exigir que todos los tokens de la consulta aparezcan.
5. Cada token puede coincidir en cualquier campo buscable.
6. Los tokens no tienen que aparecer en el mismo campo.
7. Los tokens no tienen que estar juntos ni en orden.
8. Para tokens de 1 o 2 caracteres, exigir coincidencia exacta de token.
9. Para tokens de 3 o más caracteres, permitir coincidencia exacta o por prefijo de token.
10. No permitir coincidencia arbitraria en mitad de palabra.

Ejemplos:

| Query | Texto efectivo | Resultado | Motivo |
|---|---|---:|---|
| `veneno` | `protege contra veneno` | sí | token exacto |
| `venen` | `protege contra veneno` | sí | prefijo de token >= 3 caracteres |
| `eno` | `protege contra veneno` | no | coincidencia interna no permitida |
| `daño fuego` | `inflige daño de fuego` | sí | aparecen ambos términos |
| `daño fuego` | `inflige daño ácido` | no | falta `fuego` |
| `ts` | `TS: Fortaleza niega` | sí | token corto exacto |
| `t` | `toque` | no | token corto no usa prefijo |

Decisión:

- Se usa semántica `AND` entre términos para reducir ruido.
- Se permite prefijo para facilitar búsquedas rápidas como `venen`, `protecc` o `invis`.
- No se permite búsqueda por subcadena interna para evitar resultados excesivamente ruidosos.

## 8. Búsqueda vacía o navegación sin query

Si `q` es `null`, cadena vacía o solo espacios, la búsqueda textual se considera vacía.

Reglas:

- Deben aplicarse igualmente los filtros `listType`, `listName` y `maxLevel`.
- Deben devolverse todos los conjuros de la lista seleccionada cuyo nivel sea `<= maxLevel`.
- No se calcula `matchSource` textual.
- El `snippet` debe ser un resumen de `descriptionEs` si existe.
- Si `descriptionEs` está vacío, el `snippet` debe generarse desde el primer campo mecánico útil o quedar vacío.
- La ordenación sigue siendo la misma: nivel ascendente y nombre español normalizado ascendente.

Ejemplo:

```text
listType = CLASS
listName = Inquisidor
maxLevel = 2
q = ""
```

Resultado:

```text
Todos los conjuros de Inquisidor de nivel 0, 1 y 2.
```

## 9. Reglas de ordenación de resultados

La ordenación obligatoria del MVP es determinista y no depende de relevancia.

Orden:

```text
1. level ascendente dentro de la lista seleccionada
2. nameEs ascendente normalizado
3. spellId ascendente como desempate técnico estable
```

Reglas:

- El nivel usado para ordenar es el de la lista seleccionada.
- El nombre se ordena usando la forma normalizada de `nameEs`.
- Si `nameEs` es `null` o vacío, ordenar usando cadena vacía y desempatar por `spellId`.
- El orden no debe cambiar por editar notas personales, salvo que también cambie el nombre o nivel efectivo.
- El orden no debe cambiar por puntuación, mayúsculas o tildes.

Ejemplo de orden equivalente:

```text
Ácido
Acido
ácido
```

Estos nombres tienen la misma clave normalizada:

```text
acido
```

En ese caso, debe usarse `spellId` como desempate estable.

### 9.1. Relevancia

El MVP no ordena por relevancia.

La relevancia puede usarse internamente para elegir `snippet` o `matchSource`, pero no debe alterar el orden principal.

Motivo:

- para mesa es más útil y predecible revisar primero por nivel;
- la ordenación por relevancia puede ocultar conjuros de bajo nivel entre resultados más largos o con más texto.

## 10. Reglas para fragmentos o snippets

Cada resultado debe incluir un fragmento compacto.

Campo conceptual:

```text
snippet
```

Campo conceptual adicional:

```text
matchSource
```

Valores recomendados de `matchSource`:

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
summary
none
```

### 10.1. Longitud del snippet

Reglas:

- Longitud recomendada: entre 120 y 180 caracteres.
- Longitud máxima funcional: 220 caracteres.
- No cortar en mitad de palabra si puede evitarse.
- Añadir `...` si el texto se recorta.
- No alterar el texto original salvo recorte.
- No devolver texto inglés.

### 10.2. Prioridad de selección del snippet con query

Cuando hay `q`, elegir el primer campo coincidente según esta prioridad:

```text
1. nameEs
2. descriptionEs
3. school
4. subschool
5. descriptors
6. castingTime
7. components
8. range
9. target
10. effect
11. area
12. duration
13. savingThrow
14. spellResistance
15. personalNotes
```

Reglas:

- La prioridad favorece texto oficial/traducido sobre notas personales.
- Aunque una nota personal coincida, si también coincide la descripción, el fragmento principal debe venir de `descriptionEs`.
- Si solo coinciden notas personales, el fragmento debe venir de `personalNotes` y `matchSource = personalNotes`.
- En campos cortos, el snippet puede ser el campo completo.
- En arrays como `descriptors`, el snippet puede ser la lista unida por comas.

### 10.3. Snippet de descripción

Si la coincidencia está en `descriptionEs`:

- localizar la primera aparición normalizada de la palabra o frase;
- devolver una ventana del texto original alrededor de esa aparición;
- intentar incluir unas palabras antes y después;
- mantener tildes y puntuación originales del texto efectivo.

Ejemplo:

```json
{
  "snippet": "...el objetivo queda protegido contra los efectos del veneno durante la duración del conjuro...",
  "matchSource": "descriptionEs"
}
```

### 10.4. Snippet por nombre

Si la coincidencia está en `nameEs`:

- `snippet` puede ser el nombre completo;
- `matchSource = nameEs`.

Ejemplo:

```json
{
  "snippet": "Retrasar veneno",
  "matchSource": "nameEs"
}
```

### 10.5. Snippet sin query

Si no hay query:

1. usar el inicio de `descriptionEs` si existe;
2. si no existe, usar una composición breve de campos mecánicos disponibles;
3. si no hay texto útil, devolver cadena vacía y `matchSource = none`.

Ejemplo de composición mecánica aceptable:

```text
Escuela: conjuración; Alcance: toque; Duración: 1 min./nivel.
```

Esta composición es solo un resumen visible del resultado, no debe indexarse como campo nuevo independiente.

## 11. Reglas para búsquedas sobre notas personales

Las notas personales forman parte del índice de búsqueda.

Campo:

```text
personalNotes
```

Reglas:

- Proceden únicamente de `data/overrides/spells-es.overrides.json`.
- Se aplican al conjuro efectivo durante importación o edición.
- Se indexan con la misma normalización que el resto de campos.
- Pueden hacer aparecer un conjuro aunque el término no exista en campos oficiales/traducidos.
- No deben mezclarse con `descriptionEs`.
- No deben mostrarse como texto oficial del conjuro.
- Editar notas no debe modificar `translationStatus` salvo acción explícita.
- Si `personalNotes` es `""`, no aporta tokens al índice.

Ejemplo:

```text
personalNotes = "Preparar si esperamos drow o arañas venenosas."
query = "drow"
```

Resultado esperado:

```text
El conjuro aparece aunque drow no esté en descriptionEs.
matchSource = personalNotes
```

### 11.1. Prioridad de notas frente a texto oficial

Si un término aparece tanto en notas como en descripción:

- el conjuro aparece una sola vez;
- `matchSource` debe preferir `descriptionEs` frente a `personalNotes`;
- el snippet debe preferir el texto oficial/traducido.

Motivo:

- las notas enriquecen la búsqueda, pero no deben sustituir la lectura principal del conjuro.

## 12. Reglas para resultados tras editar overrides

La búsqueda siempre debe usar datos efectivos.

Si se edita un campo buscable:

```text
1. Persistir el cambio en overrides si procede.
2. Actualizar el conjuro efectivo en SQLite.
3. Actualizar el índice de búsqueda.
4. La siguiente búsqueda debe reflejar el nuevo valor.
```

Campos editables que afectan al índice:

- `nameEs`;
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
- `descriptionEs`;
- `personalNotes`.

Campos editables que no afectan a coincidencias textuales:

- `translationStatus`.

Reglas:

- Cambiar `translationStatus` puede cambiar el valor mostrado en resultados, pero no debe cambiar si aparece o no por texto.
- Cambiar `nameEs` puede cambiar coincidencias, snippet y orden alfabético dentro de nivel.
- Cambiar `descriptionEs` puede cambiar coincidencias y snippet.
- Cambiar `personalNotes` puede hacer aparecer o desaparecer un conjuro por búsquedas en notas.
- Reimportar SQLite desde dataset + overrides debe producir el mismo índice lógico que las ediciones incrementales.

### 12.1. Conjuros bloqueados

Si un conjuro tiene `translationStatus = LOCKED`:

- sus campos españoles efectivos materializados en override deben indexarse;
- no debe indexarse el valor generado si hay override para ese campo;
- si el dataset generado cambia, la búsqueda no debe cambiar para campos bloqueados materializados;
- si el usuario edita manualmente un conjuro bloqueado, la búsqueda debe actualizarse con el nuevo valor efectivo.

## 13. Casos de prueba funcionales

### Caso 1 - Buscar por descripción española

Datos:

```text
listType = CLASS
listName = Clérigo
level = 2
descriptionEs = "El objetivo queda protegido contra el veneno."
query = veneno
```

Resultado esperado:

```text
El conjuro aparece.
matchSource = descriptionEs
```

### Caso 2 - No buscar en inglés

Datos:

```text
descriptionEn = "The subject becomes immune to poison."
descriptionEs = "El objetivo queda protegido."
query = poison
```

Resultado esperado:

```text
El conjuro no aparece solo por descriptionEn.
```

### Caso 3 - Buscar por notas personales

Datos:

```text
personalNotes = "Preparar si esperamos drow."
descriptionEs no contiene drow
query = drow
```

Resultado esperado:

```text
El conjuro aparece.
matchSource = personalNotes
```

### Caso 4 - Nivel máximo

Datos:

```text
listName = Clérigo
level = 4
query = veneno
maxLevel = 3
```

Resultado esperado:

```text
El conjuro no aparece.
```

### Caso 5 - Varias listas con distinto nivel

Datos:

```text
Conjuro A:
- CLASS / Clérigo / 4
- CLASS / Druida / 3
```

Búsqueda:

```text
Druida + maxLevel 3
```

Resultado esperado:

```text
Aparece con level = 3.
```

Búsqueda:

```text
Clérigo + maxLevel 3
```

Resultado esperado:

```text
No aparece.
```

### Caso 6 - Acentos

Datos:

```text
nameEs = "Protección contra el mal"
query = proteccion
```

Resultado esperado:

```text
El conjuro aparece.
```

### Caso 7 - Ñ normalizada

Datos:

```text
descriptionEs = "Inflige daño de fuego."
query = dano fuego
```

Resultado esperado:

```text
El conjuro aparece.
```

### Caso 8 - Mayúsculas y espacios

Datos:

```text
savingThrow = "TS: Voluntad niega"
query = "  tS     voluntad   "
```

Resultado esperado:

```text
El conjuro aparece.
```

### Caso 9 - Puntuación

Datos:

```text
duration = "1 min./nivel"
query = min nivel
```

Resultado esperado:

```text
El conjuro aparece.
```

### Caso 10 - Frase exacta

Datos:

```text
descriptionEs = "Inflige daño de fuego al objetivo."
query = "daño de fuego"
```

Resultado esperado:

```text
El conjuro aparece.
```

### Caso 11 - Frase no exacta

Datos:

```text
descriptionEs = "Inflige daño por fuego al objetivo."
query = "daño de fuego"
```

Resultado esperado:

```text
El conjuro no aparece por frase exacta.
```

### Caso 12 - Prefijo permitido

Datos:

```text
descriptionEs = "Protege contra veneno."
query = venen
```

Resultado esperado:

```text
El conjuro aparece.
```

### Caso 13 - Subcadena interna no permitida

Datos:

```text
descriptionEs = "Protege contra veneno."
query = eno
```

Resultado esperado:

```text
El conjuro no aparece solo por coincidencia interna.
```

### Caso 14 - Todos los términos obligatorios

Datos:

```text
descriptionEs = "Inflige daño ácido."
query = daño fuego
```

Resultado esperado:

```text
El conjuro no aparece porque falta fuego.
```

### Caso 15 - Términos en campos distintos

Datos:

```text
descriptionEs = "Inflige daño al objetivo."
descriptors = ["fuego"]
query = daño fuego
```

Resultado esperado:

```text
El conjuro aparece porque ambos términos existen en campos buscables.
```

### Caso 16 - Navegación sin query

Datos:

```text
listName = Inquisidor
maxLevel = 2
query = ""
```

Resultado esperado:

```text
Aparecen todos los conjuros de Inquisidor nivel 0 a 2.
```

### Caso 17 - Edición de descripción

Estado inicial:

```text
descriptionEs no contiene veneno
query = veneno
```

Acción:

```text
Editar descriptionEs para incluir "veneno".
```

Resultado esperado:

```text
La siguiente búsqueda por veneno encuentra el conjuro.
```

### Caso 18 - Edición de notas

Estado inicial:

```text
personalNotes = ""
query = drow
```

Acción:

```text
Editar personalNotes = "Preparar contra drow."
```

Resultado esperado:

```text
La siguiente búsqueda por drow encuentra el conjuro.
```

### Caso 19 - Ordenación por nivel y nombre

Datos:

```text
Conjuro B: level 2, nameEs = "Bendición"
Conjuro A: level 1, nameEs = "Auxilio"
Conjuro C: level 1, nameEs = "Ángel guardián"
```

Resultado esperado:

```text
1. Ángel guardián
2. Auxilio
3. Bendición
```

### Caso 20 - Snippet desde notas

Datos:

```text
personalNotes = "Muy útil si esperamos drow o asesinos."
query = drow
```

Resultado esperado:

```json
{
  "matchSource": "personalNotes",
  "snippet": "Muy útil si esperamos drow o asesinos."
}
```

## 14. Criterios de aceptación

La búsqueda del MVP se considera correcta si cumple todos estos criterios:

- filtra por `listType`;
- filtra por `listName`;
- filtra por `maxLevel` usando el nivel de la lista seleccionada;
- permite búsqueda vacía como navegación;
- busca solo en campos españoles efectivos y notas personales;
- no busca en `nameEn`, `descriptionEn` ni otros textos ingleses;
- usa datos efectivos tras aplicar overrides;
- normaliza mayúsculas y minúsculas;
- normaliza acentos y diacríticos;
- normaliza `ñ` como `n`;
- normaliza puntuación básica como separación de tokens;
- normaliza espacios repetidos;
- permite frase exacta con comillas dobles;
- permite búsqueda por todos los términos sin comillas;
- permite prefijo de token para términos de 3 o más caracteres;
- no permite coincidencia arbitraria en mitad de palabra;
- ordena por nivel ascendente;
- ordena por nombre español normalizado ascendente dentro de cada nivel;
- usa `spellId` como desempate estable;
- devuelve fragmento en español efectivo;
- indica `matchSource` cuando hay coincidencia textual;
- permite que notas personales hagan aparecer resultados;
- no mezcla notas personales con descripción oficial;
- actualiza resultados tras editar campos buscables;
- actualiza resultados tras editar notas personales;
- reconstruir SQLite desde dataset + overrides produce resultados equivalentes;
- funciona completamente offline;
- responde con rapidez sobre un dataset de pocos miles de conjuros.

## Relación mínima con API REST

La API REST local debe poder consumir estas reglas mediante parámetros equivalentes a:

```text
listType
listName
maxLevel
q
```

La API no debe redefinir reglas de búsqueda propias.

Reglas:

- los controladores REST no contienen lógica de búsqueda;
- la lógica debe residir en casos de uso y puertos de búsqueda;
- la respuesta de listado debe incluir los campos necesarios para mostrar resultado compacto;
- la especificación de rutas, DTOs exactos, códigos HTTP y validaciones REST queda en el documento de contrato API correspondiente.

## Relación con SQLite o FTS

SQLite puede implementar estas reglas mediante:

- tabla FTS;
- columnas normalizadas auxiliares;
- índices normales;
- combinación de FTS y filtros relacionales.

Contrato obligatorio:

- no leer JSON en cada búsqueda;
- no usar servicios externos;
- no depender de PostgreSQL, Elasticsearch ni Docker;
- no indexar inglés;
- reconstruir el índice durante importación;
- actualizar el índice tras ediciones de campos buscables;
- producir resultados equivalentes a las reglas funcionales de este documento.

## Relación con pruebas automatizadas

Las pruebas automatizadas deben cubrir:

- normalización aislada;
- búsqueda por término;
- búsqueda por frase;
- búsqueda por prefijo;
- rechazo de subcadena interna;
- filtro por lista;
- filtro por nivel máximo;
- notas personales;
- no búsqueda en inglés;
- ordenación;
- snippet y `matchSource`;
- actualización tras edición;
- reconstrucción desde dataset + overrides.

Las pruebas no deben depender de un dataset completo real. Deben poder ejecutarse con fixtures mínimos y deterministas.
