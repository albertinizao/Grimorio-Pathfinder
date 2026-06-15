# 08 - Reglas de búsqueda y normalización

## Propósito

Este documento fija el comportamiento real de la búsqueda del MVP de **Grimorio Pathfinder**.

La búsqueda actual se ejecuta en dos pasos:

1. SQLite filtra candidatos por `listType`, `listName` y nivel.
2. La capa de servicio normaliza el texto y aplica el match textual sobre los candidatos.

No hay búsqueda en inglés en el MVP.

## Entrada funcional

La búsqueda principal usa:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

Parámetros efectivos:

- `listType`
- `listName`
- `maxLevel`
- `levelMode`
- `q`
- `page`
- `size`

## Alcance textual

La implementación busca solo en estos campos efectivos:

- `nameEs`
- `descriptionEs`
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
- `personalNotes`

No se usan `nameEn`, `descriptionEn`, `sourceBook`, `sourcePage`, `sourceName` ni metadatos técnicos para hacer coincidir términos.

## Normalización

La normalización actual hace lo siguiente:

1. si el valor es `null`, usa cadena vacía;
2. convierte a minúsculas;
3. descompone Unicode (`NFD`);
4. elimina marcas diacríticas;
5. reemplaza caracteres no alfanuméricos por espacios;
6. colapsa espacios repetidos;
7. recorta bordes.

Ejemplos:

| Entrada | Normalizado |
|---|---|
| `protección` | `proteccion` |
| `TS: Fortaleza niega` | `ts fortaleza niega` |
| `Mago/Hechicero` | `mago hechicero` |
| `1 min./nivel` | `1 min nivel` |

## Búsqueda vacía

Si `q` está vacío o contiene solo espacios, la búsqueda devuelve todos los candidatos filtrados por lista y nivel.

## Búsqueda por frase

La implementación actual trata cualquier consulta que contenga comillas dobles como búsqueda de frase:

- elimina todas las comillas;
- normaliza el texto resultante;
- busca la secuencia completa dentro de cualquiera de los campos buscables.

Las comillas no tienen que estar perfectamente balanceadas para que la consulta funcione.

## Búsqueda por términos

Si no hay comillas, la consulta se interpreta como términos separados por espacios.

Reglas actuales:

- lógica `AND` entre términos;
- cada término debe aparecer en algún campo buscable;
- términos de longitud 1 o 2 exigen coincidencia exacta de token;
- términos de 3 o más caracteres permiten prefijo de token.

Ejemplos:

| Query | Resultado esperado |
|---|---|
| `veneno` | coincide con `veneno` |
| `venen` | coincide con `veneno` |
| `eno` | no coincide |
| `daño fuego` | ambos términos deben aparecer |

## Orden de resultados

El orden determinista actual es:

1. rank de coincidencia;
2. nivel de la lista seleccionada ascendente;
3. `nameEs` normalizado ascendente;
4. `spellId` ascendente.

Rank de coincidencia:

- `nameEs` tiene la prioridad más alta;
- otros campos están en prioridad intermedia;
- `descriptionEs` queda al final.

## Fragmentos y coincidencia principal

La implementación devuelve:

- `matchSource`: campo donde se detectó la coincidencia principal;
- `snippet`: fragmento corto del campo coincidente;
- `hasPersonalNotes`: indicador de que el conjuro tiene notas guardadas.

El `snippet` se recorta a un máximo razonable para la UI y se genera desde el campo que produjo la mejor coincidencia.

## Nivel y lista

La búsqueda filtra por `SpellListEntry`:

- `listType` y `listName` deben coincidir exactamente con la lista seleccionada;
- `maxLevel` se valida contra los niveles reales de esa lista;
- `levelMode = UP_TO` usa `<= maxLevel`;
- `levelMode = EXACT` usa `== maxLevel`.

## Reglas de implementación relevantes

- La API no busca en inglés.
- La búsqueda se puede hacer sin texto.
- La búsqueda respeta notas personales.
- El frontend no debe recalcular la normalización por su cuenta.
- La proyección SQLite almacena un `search_text` auxiliar, pero la coincidencia actual se resuelve en la capa de servicio sobre los candidatos.

## Casos borde conocidos

- cualquier comilla en `q` activa modo de frase;
- `size = 0` se comporta como búsqueda no paginada;
- `q` vacío no rompe la navegación;
- un nivel no disponible para la lista devuelve `422` desde la API.
