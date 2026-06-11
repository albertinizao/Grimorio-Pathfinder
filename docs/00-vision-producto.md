# 00 - Visión de producto

## Producto

**Grimorio Pathfinder** es una aplicación local y offline para consultar, buscar y editar conjuros de **Pathfinder 1e** en español.

La aplicación está pensada para uso personal en mesa y durante la preparación de partidas o personajes.

## Problema

En Pathfinder 1e hay muchos conjuros repartidos por múltiples listas, niveles y fuentes. Durante una partida o una preparación, no siempre se busca un conjuro por nombre: a menudo se busca una solución para una situación concreta.

Ejemplos:

- encontrar conjuros contra veneno;
- revisar opciones contra enfermedad;
- buscar herramientas contra miedo, invisibilidad, maldiciones o muertos vivientes;
- leer todos los conjuros disponibles para una clase lanzadora hasta cierto nivel;
- decidir qué conjuros merece la pena preparar.

Consultar manualmente todos los conjuros posibles es lento y poco práctico en mesa.

## Objetivo

Crear una biblioteca rápida, navegable y buscable de conjuros de Pathfinder 1e en español.

La aplicación debe permitir:

- consultar conjuros en español;
- buscar por término o frase;
- filtrar por lista de clase lanzadora;
- filtrar por nivel máximo;
- navegar por nivel;
- leer el detalle completo de un conjuro;
- conservar el texto inglés original como referencia;
- editar traducciones campo por campo;
- añadir notas personales buscables;
- marcar el estado de traducción o revisión.

## Usuario principal

El usuario principal es un jugador o director de juego de Pathfinder 1e que quiere consultar conjuros de forma rápida en mesa o durante la preparación.

El uso principal será en tablet Android, con una interfaz cómoda, oscura y rápida.

## Caso de uso principal

El caso principal del MVP es:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

Ejemplo:

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 3
Búsqueda: veneno
```

La aplicación debe devolver todos los conjuros de la lista de clase lanzadora de Clérigo de nivel 0 a 3 que contengan el término buscado en sus campos españoles efectivos o en notas personales.

## Principios de producto

### 1. Biblioteca española, no traductor interactivo

La aplicación principal trabaja con conjuros ya guardados en español.

No debe traducir conjuros al vuelo durante la consulta normal.

La traducción IA pertenece al proceso de generación o mantenimiento del dataset, no al uso habitual en mesa.

### 2. Offline primero

La aplicación debe funcionar al 100% offline.

Durante el uso normal no debe depender de:

- servicios externos;
- APIs remotas;
- servicios IA;
- Combat Manager;
- conexión a internet.

### 3. Dataset versionado

La fuente española canónica debe vivir en archivos versionados.

Estructura conceptual:

```text
data/
  raw/
    spells.csv
  generated/
    spells-es.generated.json
  overrides/
    spells-es.overrides.json
```

La base SQLite local es una proyección reconstruible desde esos archivos y se usa para búsqueda rápida.

Responsabilidad de cada archivo:

- `spells-es.generated.json` contiene el dataset español generado y puede regenerarse.
- `spells-es.overrides.json` contiene correcciones manuales, estados manuales y `personalNotes`.
- SQLite contiene datos efectivos para consulta; no es la fuente canónica irremplazable.

### 4. Correcciones protegidas

Las correcciones manuales del usuario deben persistirse como overrides.

Una corrección manual no debe sobrescribirse automáticamente al regenerar o recargar el dataset.

### 5. Uso rápido en mesa

La interfaz debe priorizar velocidad y claridad.

Es preferible una vista compacta y útil antes que una interfaz visualmente compleja.

## Convenciones funcionales del MVP

### Terminología

Usar siempre:

```text
lista de clase lanzadora
```

para referirse a listas como Clérigo, Druida, Inquisidor o Mago/Hechicero.

Usar:

```text
lista de conjuros
```

para el concepto general que incluye listas de clase lanzadora y futuras listas especiales como dominios, linajes, patronos, misterios o inquisiciones.

### Estado `LOCKED`

`LOCKED` es solo un valor de `translationStatus`.

No existe un flag técnico adicional en el MVP.

Un conjuro con `translationStatus = LOCKED` está protegido frente a procesos automáticos de regeneración, importación o mantenimiento. La edición manual explícita sigue siendo posible si la UI ofrece esa acción.

### Notas personales

`personalNotes` es contenido propio del usuario.

En el MVP se persiste canónicamente dentro de:

```text
data/overrides/spells-es.overrides.json
```

No vive en `spells-es.generated.json` ni en un archivo separado.

SQLite guarda una copia efectiva para mostrarla y buscarla.

## Alcance del MVP

El MVP incluye:

- carga/importación del dataset español;
- aplicación de overrides;
- base local SQLite;
- búsqueda por lista de clase lanzadora, nivel máximo y texto opcional;
- búsqueda en notas personales;
- listado de resultados;
- detalle de conjuro;
- edición campo por campo;
- notas personales;
- estado de traducción/revisión;
- modo oscuro;
- funcionamiento offline.

## Fuera de alcance del MVP

No forman parte del MVP:

- gestión de personajes concretos;
- conjuros conocidos;
- conjuros preparados;
- slots diarios;
- cálculo automático de CD;
- integración con Combat Manager;
- traducción IA desde la interfaz;
- pantalla avanzada de administración de traducciones;
- favoritos;
- etiquetas personales;
- PWA completa;
- multiusuario;
- autenticación;
- Docker;
- PostgreSQL;
- servicios externos;
- búsqueda en inglés.

Estas funciones pueden contemplarse como evolución futura, pero no deben implementarse antes de completar el flujo básico.

## Búsqueda

La búsqueda del MVP debe usar solo contenido español efectivo y notas personales.

Campos buscables:

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

El texto inglés se conserva como referencia, pero no se usa para búsqueda en el MVP.

La búsqueda debe ignorar mayúsculas/minúsculas y acentos.

## API REST

La documentación actual fija el contrato funcional mínimo, pero no cierra todavía una especificación REST definitiva.

El contrato final debe quedar en un documento separado:

```text
docs/10-api-rest.md
```

Hasta que exista, las rutas mencionadas en otros documentos son ejemplos funcionales mínimos, no una especificación OpenAPI cerrada.

## Estilo de traducción

El estilo de traducción debe ser de mesa:

- claro;
- breve;
- preciso;
- consistente;
- cómodo durante partida.

Ejemplos de estilo preferido:

```text
TS: Fortaleza niega
RC: sí
Duración: 1 min./nivel
Alcance: toque
Tiempo de lanzamiento: 1 acción estándar
```

## Criterios de éxito

El producto será útil si permite:

- encontrar rápidamente conjuros relevantes para una situación;
- revisar todos los conjuros disponibles para una clase lanzadora hasta un nivel concreto;
- consultar detalles sin conexión;
- corregir traducciones sin perder cambios;
- añadir notas personales que mejoren búsquedas futuras;
- usar la aplicación cómodamente en mesa desde una tablet.

## Criterio de calidad funcional

Una funcionalidad debe considerarse correcta si:

- funciona offline;
- consulta datos en español;
- conserva el texto inglés original;
- respeta overrides;
- no sobrescribe correcciones manuales;
- responde con rapidez en búsquedas habituales;
- mantiene una experiencia cómoda para mesa.
