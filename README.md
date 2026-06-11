# Grimorio Pathfinder

Aplicación local y offline para consultar, buscar y editar conjuros de **Pathfinder 1e** en español.

El objetivo es disponer de una biblioteca rápida y cómoda para usar en mesa o durante la preparación de conjuros, especialmente cuando se quiere buscar por situación.

Ejemplo:

```text
Lista de clase lanzadora: Clérigo
Nivel máximo: 3
Búsqueda: veneno
```

La aplicación debe devolver todos los conjuros de la lista de clase lanzadora de Clérigo de nivel 0 a 3 que contengan ese término en sus campos españoles efectivos o en las notas personales.

## Objetivos principales

- Gestionar una biblioteca completa de conjuros en español.
- Navegar por lista de clase lanzadora y nivel.
- Buscar por término, frase o navegación sin texto.
- Consultar conjuros rápidamente en mesa.
- Editar traducciones campo por campo.
- Añadir notas personales buscables.
- Conservar el texto original inglés como referencia.
- Funcionar al 100% offline.

## Decisiones globales

- Backend: Java + Spring Boot.
- Frontend: Vue.
- Arquitectura backend: hexagonal.
- Base de datos MVP: SQLite.
- Aplicación local, personal y 100% offline.
- Sin autenticación en MVP.
- Sin Docker en MVP salvo petición explícita.
- Frontend SPA en MVP; preparar para futura PWA.
- Modo oscuro desde el inicio.
- Optimización para tablet Android.

## Principios de datos

La aplicación no traduce conjuros al vuelo.

El dataset español se genera previamente y se guarda en archivos versionados.

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

Responsabilidades:

- `spells-es.generated.json`: dataset español generado, regenerable y no editable desde la app.
- `spells-es.overrides.json`: única fuente canónica del MVP para correcciones manuales, estados manuales y `personalNotes`.
- SQLite: proyección local reconstruible usada para búsqueda rápida y consulta.

Las correcciones manuales tienen prioridad sobre el dataset generado y no deben sobrescribirse automáticamente.

## Convenciones cerradas del MVP

### `LOCKED`

`LOCKED` es únicamente un valor de `translationStatus`.

No existe en el MVP un flag técnico adicional llamado `locked`.

Semántica:

- indica que el conjuro efectivo está protegido frente a procesos automáticos;
- no debe sobrescribirse al regenerar o reimportar datos;
- no impide una edición manual explícita del usuario;
- se persiste como override mediante `fields.translationStatus = "LOCKED"`.

### `personalNotes`

`personalNotes` vive canónicamente en:

```text
data/overrides/spells-es.overrides.json
```

No forma parte de `spells-es.generated.json` y no se guarda en un archivo separado durante el MVP.

SQLite solo contiene una copia efectiva para consulta y búsqueda.

### Terminología

Forma canónica:

```text
lista de clase lanzadora
```

Usar `lista de conjuros` cuando se hable del concepto general que también puede incluir dominios, linajes, patronos, misterios u otras listas especiales.

No introducir formas alternativas para el mismo concepto en documentación nueva.

## Búsqueda MVP

La búsqueda principal del MVP es:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

Reglas principales:

- busca solo en contenido español efectivo y notas personales;
- no busca en `nameEn`, `descriptionEn` ni otros textos ingleses;
- ignora mayúsculas/minúsculas;
- ignora acentos;
- normaliza espacios y puntuación básica;
- permite navegar sin término de búsqueda;
- ordena por nivel ascendente y nombre español ascendente.

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

## Contrato API

Los documentos actuales describen endpoints de forma funcional y conceptual.

El contrato REST cerrado, con rutas finales, DTOs, códigos HTTP y validaciones de entrada/salida, debe documentarse más adelante en un documento específico:

```text
docs/10-api-rest.md
```

Hasta que ese documento exista, los endpoints incluidos en la documentación son contratos funcionales mínimos, no una especificación OpenAPI definitiva.

## MVP

El MVP debe incluir:

- carga/importación del dataset español;
- aplicación de overrides;
- búsqueda por lista de clase lanzadora, nivel máximo y texto opcional;
- consulta de resultados;
- detalle de conjuro;
- edición campo por campo;
- notas personales;
- estado de traducción/revisión;
- funcionamiento offline.

## Fuera de alcance inicial

No implementar en el MVP salvo petición explícita:

- gestión de personajes;
- conjuros preparados;
- slots;
- cálculo automático de CD;
- integración con Combat Manager;
- traducción IA desde la interfaz;
- favoritos;
- etiquetas;
- autenticación;
- Docker;
- PostgreSQL;
- servicios externos;
- búsqueda en inglés.

## Documentación del proyecto

Documentos base recomendados:

```text
AGENTS.md
README.md
docs/00-vision-producto.md
docs/01-roadmap.md
docs/02-modelo-dominio.md
docs/03-dataset-importacion-overrides.md
docs/04-busqueda-navegacion.md
docs/05-traduccion-edicion.md
```

`AGENTS.md` contiene las reglas globales para agentes IA.

El resto de documentos deben leerse solo cuando la tarea lo requiera, para evitar saturar el contexto.

## Criterio de calidad

Una funcionalidad se considera bien implementada si:

- funciona offline;
- usa el dataset español efectivo;
- conserva el texto inglés;
- no pierde overrides;
- mantiene arquitectura hexagonal;
- tiene tests básicos;
- es cómoda para usar en mesa.
