# 04 - Búsqueda y navegación

## Propósito

Este documento define cómo debe funcionar la búsqueda y la navegación de **Grimorio Pathfinder**.

La búsqueda es el núcleo del MVP: la aplicación debe permitir encontrar rápidamente conjuros útiles por situación, filtrando por lista de clase lanzadora, nivel máximo y término o frase opcional.

Este documento no define detalles concretos de UI ni implementación SQL final. Define el comportamiento esperado que deben respetar backend, API y frontend.

## Principio principal

La búsqueda del MVP se realiza solo sobre contenido español efectivo y notas personales.

No se busca en inglés durante el MVP.

El texto inglés se conserva como referencia en el detalle del conjuro, pero no participa en el índice ni en los resultados de búsqueda.

## Terminología canónica

- `lista de clase lanzadora`: lista de conjuros asociada a una clase, por ejemplo Clérigo, Druida, Inquisidor o Mago/Hechicero.
- `lista de conjuros`: concepto general que puede incluir listas de clase lanzadora y listas especiales como dominio, linaje, patrono, misterio o inquisición.

En el MVP, la UI principal se centra en listas de clase lanzadora.

## Caso de uso principal

El caso principal es:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

Ejemplo:

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 3
Texto: veneno
```

Resultado esperado:

```text
Todos los conjuros de la lista de clase lanzadora de Clérigo de nivel 0, 1, 2 o 3
que contengan "veneno" en sus campos españoles efectivos o notas personales.
```

## Objetivos de búsqueda

La búsqueda debe permitir:

- encontrar conjuros por necesidad de mesa;
- revisar opciones hasta un nivel máximo;
- explorar una lista de conjuros por clase lanzadora;
- encontrar conjuros enriquecidos con notas personales;
- obtener resultados compactos y legibles;
- abrir rápidamente el detalle completo.

## Fuera de alcance del MVP

No implementar en el MVP salvo petición explícita:

- búsqueda en inglés;
- operadores avanzados tipo `escuela:`, `descriptor:`, `fuente:`;
- favoritos;
- etiquetas;
- rankings personalizados;
- búsqueda semántica con IA;
- conexión a servicios externos;
- búsqueda dependiente de Combat Manager;
- perfiles de personaje;
- conjuros preparados;
- filtros por dotes, CD o nivel de lanzador real.

## Parámetros mínimos de búsqueda

La búsqueda principal debe aceptar:

### Tipo de lista

Campo conceptual:

```text
listType
```

Para el flujo principal del MVP:

```text
CLASS
```

Reglas:

- `CLASS` identifica listas de clase lanzadora.
- El modelo puede importar otros tipos de lista de conjuros.
- La UI del MVP puede ocultar o dejar en segundo plano listas especiales.

### Nombre de lista

Campo conceptual:

```text
listName
```

Ejemplos:

```text
Clérigo
Inquisidor
Druida
Mago/Hechicero
Paladín
Explorador
Bardo
```

Reglas:

- En el MVP, la interfaz se centra en listas de clase lanzadora.
- El modelo debe soportar listas especiales futuras.
- La búsqueda debe usar `SpellListEntry`.
- Un conjuro puede aparecer en varias listas de conjuros con distintos niveles.
- El resultado debe mostrar el nivel correspondiente a la lista seleccionada.

### Nivel máximo

Campo conceptual:

```text
maxLevel
```

Reglas:

- Debe incluir conjuros con nivel `<= maxLevel`.
- El nivel se interpreta dentro de la lista de conjuros seleccionada.
- `0` es un nivel válido.
- No representa nivel de personaje.
- No representa nivel de lanzador.
- No representa nivel máximo de conjuro universal, sino nivel máximo dentro de la lista filtrada.

Ejemplo:

```text
Clérigo + nivel máximo 3
```

Incluye:

```text
Clérigo 0
Clérigo 1
Clérigo 2
Clérigo 3
```

No incluye:

```text
Clérigo 4+
```

### Texto buscado

Campo conceptual:

```text
query
```

Alias habitual en API conceptual:

```text
q
```

Reglas:

- Puede ser una palabra.
- Puede ser una frase.
- Puede estar vacío si el usuario solo quiere navegar por lista de conjuros y nivel.
- Debe buscarse sobre contenido español efectivo y notas personales.
- No debe buscarse en texto inglés en el MVP.

Ejemplos:

```text
veneno
enfermedad
miedo
invisible
maldición
muerto viviente
daño de fuego
tirada de salvación
"daño de fuego"
```

## Campos buscables

La búsqueda debe considerar los siguientes campos efectivos, después de aplicar overrides:

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

## Campos excluidos de búsqueda en MVP

No deben incluirse en el índice de búsqueda del MVP:

- `nameEn`;
- `descriptionEn`;
- texto inglés original;
- identificadores internos;
- logs de importación;
- metadatos técnicos;
- `sourceHash`;
- `sourceBook`;
- `sourcePage`;
- `sourceName`;
- fechas de creación/modificación.

La fuente/libro puede mostrarse en detalle y quizá filtrarse en el futuro, pero no es campo principal de búsqueda del MVP.

## Normalización de búsqueda

La búsqueda MVP debe ser predecible y tolerante a escritura común en mesa.

### Normalizaciones obligatorias

El índice y la consulta deben normalizar:

- mayúsculas/minúsculas;
- acentos y diacríticos;
- espacios repetidos;
- puntuación básica.

Ejemplo:

```text
proteccion
```

Debe poder encontrar:

```text
protección
```

Ejemplo:

```text
TIRADA   DE   SALVACIÓN
```

Debe tratarse como equivalente funcional a:

```text
tirada de salvacion
```

### Reglas

- La normalización afecta al índice y a la consulta, no a los campos originales.
- Los campos originales deben conservar tildes, formato y texto editado.
- No aplicar traducción automática ni expansión semántica en el MVP.
- No añadir sinónimos automáticos en el MVP.

## Coincidencia parcial y frase exacta

### Consulta sin comillas

Si el usuario introduce texto sin comillas:

```text
daño fuego
```

Regla MVP:

- dividir la consulta normalizada en términos;
- exigir que todos los términos aparezcan en alguno de los campos buscables;
- permitir coincidencia exacta de token;
- permitir coincidencia por prefijo de token para términos de 3 o más caracteres;
- no hacer coincidencia arbitraria en mitad de palabra.

Ejemplos:

- `venen` puede encontrar `veneno`.
- `proteccion` puede encontrar `protección`.
- `eno` no debe considerarse suficiente para encontrar `veneno` por estar en mitad de palabra.

### Consulta con comillas

Si el usuario introduce texto entre comillas:

```text
"daño de fuego"
```

Regla MVP:

- buscar la frase normalizada exacta en los campos buscables;
- exigir que las palabras aparezcan juntas y en ese orden;
- ignorar diferencias de mayúsculas/minúsculas, acentos, espacios repetidos y puntuación básica.

Si la infraestructura elegida no soporta frase exacta de forma fiable, debe documentarse como limitación técnica antes de cerrar el MVP. Funcionalmente, la intención del MVP es soportarla.

## Búsqueda en notas personales

Las notas personales forman parte del índice de búsqueda.

Ejemplo:

```text
personalNotes = "Muy útil contra drow o enemigos venenosos."
query = "drow"
```

El conjuro debe aparecer aunque la palabra `drow` no esté en la descripción oficial traducida.

### Reglas

- Las notas no deben mezclarse con la descripción oficial.
- En resultados, debe poder indicarse si la coincidencia procede de notas.
- Las notas deben sobrevivir a reimportaciones.
- Si se editan notas, la búsqueda debe actualizarse.
- La fuente canónica de notas personales es `spells-es.overrides.json`.

## Resultado de búsqueda

Cada resultado debe ser compacto y útil en mesa.

Campos mínimos:

- identificador estable del conjuro;
- nombre español;
- `listType` seleccionado;
- `listName` seleccionado;
- nivel del conjuro en la lista seleccionada;
- escuela;
- descriptores;
- tiempo de lanzamiento;
- alcance;
- TS;
- RC;
- estado de traducción;
- fragmento coincidente o resumen;
- campo origen principal de la coincidencia, si existe.

### Ejemplo conceptual

```json
{
  "spellId": "neutralize-poison",
  "nameEs": "Neutralizar veneno",
  "listType": "CLASS",
  "listName": "Clérigo",
  "level": 4,
  "school": "conjuración",
  "descriptors": [],
  "castingTime": "1 acción estándar",
  "range": "toque",
  "savingThrow": "Voluntad niega (inofensivo, objeto)",
  "spellResistance": "sí (inofensivo, objeto)",
  "translationStatus": "REVIEWED",
  "snippet": "Neutralizas cualquier tipo de veneno presente...",
  "matchSource": "descriptionEs"
}
```

## Ordenación de resultados

Orden obligatorio para el MVP:

1. nivel ascendente;
2. nombre español ascendente normalizado.

Ejemplo:

```text
nivel 0
nivel 1
nivel 2
nivel 3
```

Dentro de cada nivel, ordenar alfabéticamente ignorando mayúsculas y acentos.

### Futuro

Más adelante podrían añadirse ordenaciones por:

- relevancia;
- escuela;
- tiempo de lanzamiento;
- favoritos;
- etiquetas;
- revisados primero;
- fuente/libro.

No implementar antes de completar la búsqueda básica.

## Coincidencias y fragmentos

Cuando haya término de búsqueda, los resultados deben intentar mostrar un fragmento útil.

Prioridad de fragmento:

1. coincidencia en nombre;
2. coincidencia en descripción;
3. coincidencia en campos mecánicos;
4. coincidencia en notas personales.

Si no hay término de búsqueda, mostrar un resumen o primeras líneas de `descriptionEs`.

### Reglas

- No mostrar fragmentos del texto inglés en el MVP.
- Si la coincidencia procede de notas, indicarlo de forma discreta mediante `matchSource = personalNotes` o equivalente.
- No modificar el texto original para crear el fragmento salvo recorte visual.
- Si se resalta el término, hacerlo en frontend sin alterar los datos.
- El fragmento debe salir del valor efectivo tras aplicar overrides.

## Navegación sin término de búsqueda

La aplicación debe permitir navegar sin introducir término.

Ejemplo:

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 2
Texto: vacío
```

Resultado:

```text
Todos los conjuros de la lista de clase lanzadora de Clérigo de nivel 0 a 2.
```

Esto es importante para preparar conjuros y leer opciones por nivel.

## Navegación principal

La navegación principal del MVP debe poder seguir este flujo:

```text
Inicio
  ↓
Seleccionar lista de clase lanzadora
  ↓
Seleccionar nivel máximo
  ↓
Opcional: escribir término/frase
  ↓
Ver resultados
  ↓
Abrir detalle
  ↓
Opcional: editar notas o traducción
```

## Navegación por lista y nivel

Además de la búsqueda textual, debe ser posible revisar conjuros por lista de conjuros y nivel.

Flujo:

```text
Lista de clase lanzadora
  ↓
Nivel
  ↓
Conjuros de ese nivel
```

Ejemplo:

```text
Inquisidor
  ↓
Nivel 2
  ↓
Todos los conjuros de inquisidor nivel 2
```

Esta navegación puede implementarse como caso particular de búsqueda:

```text
listType = CLASS
listName = Inquisidor
maxLevel = 2
query = vacío
```

Y un filtro visual que permita mostrar solo un nivel concreto puede añadirse más adelante.

## Pantalla de búsqueda

La pantalla de búsqueda debe estar optimizada para tablet.

Controles mínimos:

- selector de lista de clase lanzadora;
- selector de nivel máximo;
- campo de texto;
- botón de buscar;
- opción de limpiar búsqueda.

### Reglas UX

- Los controles deben ser grandes y fáciles de pulsar.
- La búsqueda debe recordar los últimos valores usados durante la sesión.
- El modo oscuro debe estar disponible desde el inicio.
- La pantalla debe evitar ruido visual.
- El usuario debe poder repetir búsquedas rápidamente.

## Pantalla de resultados

La pantalla de resultados debe priorizar lectura rápida.

Cada resultado debe mostrar información suficiente para decidir si abrirlo.

Campos recomendados:

```text
Nombre
Nivel
Escuela / descriptores
Tiempo
Alcance
TS / RC
Estado
Fragmento
```

### Reglas

- No mostrar bloques enormes de descripción en la lista.
- No ocultar conjuros no revisados.
- Marcar estado de traducción de forma discreta.
- Permitir abrir detalle con una pulsación clara.
- Mantener el contexto de búsqueda al volver desde el detalle.

## Pantalla de detalle

El detalle debe mostrar el conjuro completo y permitir edición.

Secciones recomendadas:

1. cabecera;
2. datos mecánicos;
3. descripción española;
4. notas personales;
5. texto inglés original plegable o secundario;
6. estado de traducción/revisión;
7. acciones de edición.

### Reglas

- El texto español es la vista principal.
- El texto inglés no debe competir visualmente con el español.
- Las notas personales deben distinguirse claramente del texto oficial/traducido.
- Editar desde detalle debe actualizar búsqueda e índices.
- El detalle debe ser legible en modo oscuro.

## Estados de traducción en resultados

Los estados permitidos son:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

### Reglas

- Mostrar todos los conjuros aunque no estén revisados.
- Marcar `REVIEW_REQUIRED` de forma visible pero no intrusiva.
- Marcar `MANUALLY_EDITED` para indicar corrección del usuario.
- Marcar `LOCKED` para indicar que está protegido frente a sobrescrituras automáticas.
- `LOCKED` no implica un flag técnico separado.
- No ordenar por estado en el MVP salvo que se pida.

## Listas especiales

El modelo permite listas especiales como dominios, linajes, patronos o misterios.

En el MVP:

- la UI puede centrarse en listas de clase lanzadora;
- las listas especiales pueden importarse;
- no hace falta una pantalla específica para cada tipo especial;
- no se deben descartar listas especiales si aparecen en el dataset.

### Futuro

Más adelante podrá añadirse navegación por:

- dominios;
- subdominios;
- linajes;
- patronos;
- misterios;
- inquisiciones;
- escuelas;
- arquetipos;
- razas.

## API funcional mínima para búsqueda

La API debe permitir consultar con parámetros equivalentes a:

```text
GET /api/spells/search?listType=CLASS&listName=Clérigo&maxLevel=3&q=veneno
```

Este endpoint es conceptual. La ruta final puede ajustarse en implementación, pero debe cubrir los parámetros principales.

La especificación REST cerrada debe documentarse en:

```text
docs/10-api-rest.md
```

### Respuesta conceptual

```json
{
  "query": "veneno",
  "listType": "CLASS",
  "listName": "Clérigo",
  "maxLevel": 3,
  "total": 2,
  "results": [
    {
      "spellId": "delay-poison",
      "nameEs": "Retrasar veneno",
      "level": 2,
      "school": "conjuración",
      "descriptors": [],
      "castingTime": "1 acción estándar",
      "range": "toque",
      "savingThrow": "Fortaleza niega (inofensivo)",
      "spellResistance": "sí (inofensivo)",
      "translationStatus": "AI_TRANSLATED",
      "snippet": "El objetivo queda temporalmente protegido contra veneno...",
      "matchSource": "descriptionEs"
    }
  ]
}
```

## API funcional mínima para detalle

La API debe permitir obtener el detalle completo por identificador estable.

Ejemplo conceptual:

```text
GET /api/spells/{spellId}
```

Debe devolver:

- campos españoles completos;
- texto inglés de referencia;
- listas de conjuros del conjuro;
- fuente/libro;
- notas personales;
- estado de traducción;
- datos necesarios para edición.

## API funcional mínima para listas

La API debe permitir obtener listas de conjuros disponibles.

Ejemplo conceptual:

```text
GET /api/spell-lists
```

Respuesta conceptual:

```json
[
  {
    "listType": "CLASS",
    "listName": "Clérigo",
    "minLevel": 0,
    "maxLevel": 9
  },
  {
    "listType": "CLASS",
    "listName": "Inquisidor",
    "minLevel": 0,
    "maxLevel": 6
  }
]
```

## Rendimiento esperado

El dataset esperado ronda unos pocos miles de conjuros.

La búsqueda debe sentirse inmediata en uso normal.

Objetivo práctico:

```text
Buscar en el dataset completo no debe bloquear perceptiblemente la UI.
```

### Reglas

- Indexar campos de búsqueda.
- No leer JSON en cada búsqueda.
- Buscar contra SQLite, no contra archivos.
- Evitar reconstruir índices salvo importación o edición de campos buscables.
- La UI debe mostrar estado de carga si la búsqueda tarda.

## Relación con SQLite

SQLite debe almacenar lo necesario para búsqueda rápida.

Puede usarse:

- índices normales;
- tabla de texto buscable;
- FTS si resulta conveniente.

### Reglas

- No depender de PostgreSQL.
- No depender de Elasticsearch/OpenSearch.
- No requerir Docker.
- No buscar leyendo archivos JSON en tiempo real.
- La decisión técnica concreta pertenece a infraestructura.

## Relación con overrides

La búsqueda siempre usa el conjuro efectivo.

```text
dataset generado + overrides = datos buscables efectivos
```

Si un override modifica un campo buscable:

- debe actualizarse SQLite;
- debe actualizarse el índice;
- la siguiente búsqueda debe reflejar el cambio.

Si un override modifica notas personales:

- debe actualizarse el índice;
- la búsqueda debe poder encontrar el conjuro por esas notas.

## Casos de prueba funcionales

### Caso 1 - Buscar por descripción

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 3
Query: veneno
```

Debe devolver conjuros de la lista de clase lanzadora de Clérigo nivel 0 a 3 cuyo texto español contenga `veneno`.

### Caso 2 - Buscar por notas

```text
Notas de un conjuro: "Preparar si esperamos drow."
Query: drow
```

Debe devolver ese conjuro aunque `drow` no aparezca en la descripción.

### Caso 3 - No buscar en inglés

```text
descriptionEn contiene: poison
descriptionEs no contiene: veneno
Query: poison
```

No debe devolver el conjuro solo por coincidencia en inglés.

### Caso 4 - Nivel máximo

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 2
Query: veneno
```

No debe devolver conjuros de Clérigo nivel 3 o superior.

### Caso 5 - Varias listas

Un conjuro aparece como:

```text
Clérigo 4
Druida 3
```

Si se busca:

```text
Druida + nivel máximo 3
```

puede aparecer.

Si se busca:

```text
Clérigo + nivel máximo 3
```

no debe aparecer.

### Caso 6 - Navegación sin query

```text
Lista de clase lanzadora: Inquisidor
Nivel máximo: 2
Query: vacío
```

Debe devolver todos los conjuros de Inquisidor de nivel 0 a 2.

### Caso 7 - Override buscable

Si se edita `descriptionEs` para incluir `veneno`, una búsqueda posterior por `veneno` debe encontrar el conjuro.

### Caso 8 - Acentos

Buscar:

```text
proteccion
```

debe poder encontrar:

```text
protección
```

### Caso 9 - Frase exacta

Buscar:

```text
"daño de fuego"
```

debe encontrar coincidencias donde la frase normalizada aparezca junta y en ese orden.

### Caso 10 - Prefijo de token

Buscar:

```text
venen
```

puede encontrar:

```text
veneno
```

pero buscar:

```text
eno
```

no debe encontrarlo solo por coincidencia interna.

## Criterios de aceptación

La búsqueda y navegación del MVP son correctas si:

- permiten filtrar por lista de clase lanzadora;
- permiten filtrar por nivel máximo;
- permiten buscar por término o frase;
- permiten buscar frase exacta con comillas;
- ignoran mayúsculas/minúsculas y acentos;
- buscan solo en contenido español efectivo y notas;
- no usan texto inglés;
- usan datos efectivos tras aplicar overrides;
- responden rápido sobre el dataset completo;
- permiten navegar sin término de búsqueda;
- muestran resultados compactos;
- permiten abrir detalle;
- conservan el contexto de búsqueda al volver;
- funcionan completamente offline.
