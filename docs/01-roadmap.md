# 01 - Roadmap

## Propósito

Este documento define el orden recomendado de construcción de **Grimorio Pathfinder**.

El objetivo es evitar que el desarrollo generado por IA implemente demasiadas cosas a la vez o mezcle funcionalidades futuras con el MVP.

Cada fase debe producir algo ejecutable, comprobable y coherente con la visión del producto.

## Reglas generales

- Avanzar en fases pequeñas.
- No implementar funcionalidades futuras antes de completar el MVP.
- Mantener la aplicación 100% offline.
- No introducir servicios externos.
- No añadir Docker en el MVP.
- No sustituir SQLite salvo petición explícita.
- No traducir conjuros al vuelo.
- No buscar en inglés en el MVP.
- No sobrescribir correcciones manuales.
- Mantener arquitectura hexagonal en backend.
- Añadir tests básicos en cada fase.

## Convenciones obligatorias para todas las fases

### Terminología

Usar `lista de clase lanzadora` para listas como Clérigo, Druida, Inquisidor o Mago/Hechicero.

Usar `lista de conjuros` para el concepto general que puede incluir listas especiales futuras como dominios, linajes, patronos, misterios o inquisiciones.

### Estado `LOCKED`

`LOCKED` es únicamente un valor de `translationStatus`.

No existe en el MVP un flag técnico adicional llamado `locked`.

### Notas personales

`personalNotes` se persiste canónicamente en:

```text
data/overrides/spells-es.overrides.json
```

No vive en `spells-es.generated.json` ni en un archivo separado durante el MVP.

SQLite contiene una copia efectiva reconstruible para consulta y búsqueda.

### API REST

Las fases de API de este roadmap fijan contratos funcionales mínimos.

La especificación REST cerrada debe documentarse posteriormente en:

```text
docs/10-api-rest.md
```

Hasta entonces, las rutas citadas son ejemplos funcionales obligatorios, no una especificación OpenAPI final.

## Fase 0 - Estructura inicial del proyecto

### Objetivo

Crear la estructura mínima del repositorio y dejar preparado el proyecto para desarrollo incremental.

### Incluye

- Crear estructura base de carpetas.
- Crear backend Spring Boot.
- Crear frontend Vue.
- Configurar SQLite para entorno local.
- Añadir configuración básica de tests.
- Añadir documentación inicial.

### Estructura orientativa

```text
backend/
frontend/
data/
  raw/
  generated/
  overrides/
docs/
AGENTS.md
README.md
```

### Criterios de aceptación

- El backend arranca.
- El frontend arranca.
- Existe una base SQLite local o configuración preparada para crearla.
- Hay tests mínimos ejecutables.
- No hay dependencias externas obligatorias.

## Fase 1 - Modelo de dominio

### Objetivo

Definir el modelo mínimo necesario para representar conjuros, listas de conjuros y estados de traducción.

### Incluye

- Entidad/concepto `Spell`.
- Entidad/concepto `SpellListEntry`.
- Estados de traducción.
- Notas personales como parte del conjuro efectivo.
- Fuente/libro.
- Texto español editable.
- Texto inglés conservado como referencia.
- Identificadores estables.

### Criterios de aceptación

- El dominio no depende de Spring, JPA, SQLite ni controladores.
- Un conjuro puede pertenecer a varias listas de conjuros.
- El modelo permite listas especiales futuras mediante `listType`, `listName` y `level`.
- El texto inglés no se pierde.
- Las notas personales forman parte del modelo efectivo.
- `LOCKED` se modela solo como `translationStatus`.

Documento de referencia: `docs/02-modelo-dominio.md`.

## Fase 2 - Dataset español e importación

### Objetivo

Permitir que la aplicación cargue una fuente española versionada y la convierta en datos consultables en SQLite.

### Incluye

- Lectura de `data/generated/spells-es.generated.json`.
- Lectura de `data/overrides/spells-es.overrides.json`.
- Validación mínima de ambos archivos.
- Aplicación de overrides sobre el dataset generado.
- Conservación de overrides huérfanos como advertencias.
- Persistencia/importación en SQLite.
- Reconstrucción de la base local desde archivos.
- Tests de importación.

### Criterios de aceptación

- La base SQLite puede reconstruirse desde el dataset español.
- Los overrides tienen prioridad sobre el dataset generado.
- Una corrección manual no se sobrescribe automáticamente.
- Las notas personales se leen únicamente desde overrides.
- El importador conserva el texto inglés original.
- El proceso funciona offline.
- Los overrides huérfanos se conservan y se reportan, pero no detienen la importación.

Documento de referencia: `docs/03-dataset-importacion-overrides.md`.

## Fase 3 - API REST de consulta

### Objetivo

Exponer los datos importados mediante una API REST local para que el frontend pueda consultarlos.

### Incluye

- Endpoint conceptual de listado/búsqueda de conjuros.
- Endpoint conceptual de detalle de conjuro.
- Endpoint conceptual de listas de conjuros disponibles.
- Endpoint conceptual de niveles disponibles por lista de conjuros.
- DTOs de consulta.
- Mappers entre dominio y API.

### Parámetros funcionales mínimos

La API debe cubrir como mínimo:

```text
listType
listName
maxLevel
q
```

Para el flujo principal del MVP, `listType` será normalmente `CLASS`.

### Criterios de aceptación

- Los controladores no contienen lógica de negocio.
- El frontend puede obtener listas, resultados y detalle.
- El detalle incluye texto español, texto inglés de referencia y notas personales.
- La API funciona sin conexión externa.
- Los nombres y estructuras finales quedan pendientes de `docs/10-api-rest.md`.

## Fase 4 - Búsqueda avanzada MVP

### Objetivo

Implementar la búsqueda principal del MVP:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

### Incluye

- Filtro por lista de clase lanzadora.
- Filtro por nivel máximo.
- Búsqueda por término o frase.
- Navegación sin término de búsqueda.
- Búsqueda solo sobre contenido español efectivo y notas personales.
- Normalización de mayúsculas/minúsculas.
- Normalización de acentos.
- Normalización de espacios y puntuación básica.
- Índices suficientes en SQLite para unos pocos miles de conjuros.
- Tests de búsqueda.

### Campos buscables

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

### Criterios de aceptación

- Buscar `Clérigo + nivel máximo 3 + veneno` devuelve conjuros de la lista de clase lanzadora de Clérigo de nivel 0 a 3 relacionados con ese término.
- La búsqueda no usa texto inglés.
- Las notas personales influyen en los resultados.
- Buscar `proteccion` puede encontrar `protección`.
- La búsqueda responde con rapidez en el dataset completo.
- Hay tests para búsquedas con y sin notas personales.

Documento de referencia: `docs/04-busqueda-navegacion.md`.

## Fase 5 - Frontend de búsqueda y detalle

### Objetivo

Crear la interfaz mínima útil para mesa.

### Incluye

- Pantalla de búsqueda.
- Selector de lista de clase lanzadora.
- Selector de nivel máximo.
- Campo de término/frase.
- Listado de resultados.
- Pantalla de detalle.
- Modo oscuro.
- Diseño cómodo para tablet Android.

### Resultado compacto

Cada resultado debe mostrar, como mínimo:

- nombre español;
- nivel en la lista de conjuros seleccionada;
- escuela;
- descriptores;
- tiempo de lanzamiento;
- alcance;
- TS;
- RC;
- estado de traducción;
- fragmento o resumen.

### Detalle de conjuro

Debe mostrar:

- campos españoles;
- texto inglés original plegable o secundario;
- notas personales;
- estado de traducción/revisión;
- acciones de edición.

### Criterios de aceptación

- La búsqueda puede usarse cómodamente desde tablet.
- El detalle es legible en mesa.
- El modo oscuro está disponible desde el inicio.
- No hay dependencias online.

## Fase 6 - Edición campo por campo

### Objetivo

Permitir corregir traducciones y notas desde la aplicación sin perder cambios al regenerar el dataset.

### Incluye

- Edición de campos españoles.
- Edición de notas personales.
- Cambio de estado de traducción/revisión.
- Persistencia de cambios como overrides.
- Protección contra sobrescritura automática.
- Tests de edición y overrides.

### Campos editables

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

### Criterios de aceptación

- Editar un campo traducido genera o actualiza un override en `fields`.
- Editar notas personales actualiza `personalNotes` en `spells-es.overrides.json`.
- Reimportar el dataset conserva la corrección manual.
- El texto inglés original sigue disponible.
- El estado `MANUALLY_EDITED` se aplica cuando corresponde.
- `LOCKED`, como `translationStatus`, impide sobrescrituras automáticas.
- No se crea ni usa un flag `locked` separado.

Documento de referencia: `docs/05-traduccion-edicion.md`.

## Fase 7 - Validación del flujo MVP

### Objetivo

Comprobar que el flujo básico completo funciona de principio a fin.

### Flujo esperado

```text
dataset español generado
        ↓
overrides manuales
        ↓
importación a SQLite
        ↓
búsqueda por lista de clase lanzadora + nivel máximo + término
        ↓
detalle de conjuro
        ↓
edición campo por campo
        ↓
nuevo override
        ↓
reimportación sin pérdida de cambios
```

### Criterios de aceptación

- El usuario puede buscar conjuros útiles en mesa.
- El usuario puede corregir traducciones.
- Las correcciones sobreviven a reconstrucciones de la base local.
- Las notas personales sobreviven a reconstrucciones de la base local.
- El sistema funciona offline.
- Hay tests para el flujo principal.

## Fase 8 - Mejoras posteriores al MVP

No implementar antes de completar el MVP salvo petición explícita.

### Favoritos y etiquetas

- Marcar conjuros favoritos.
- Añadir etiquetas personales.
- Permitir filtrar por etiquetas.

### Gestión de personajes

- Crear personajes locales.
- Marcar conjuros conocidos.
- Marcar conjuros preparados.
- Filtrar por nivel real del personaje.

### PWA

- Hacer la app instalable.
- Mejorar caché local.
- Optimizar uso desde tablet.

### Administración de traducciones

- Pipeline visual de traducción.
- Revisión de lotes.
- Reintentos de traducción.
- Memoria de traducción.
- Validación automática de dados, números, CD, duraciones y términos mecánicos.

### Integraciones auxiliares

- Importación desde Combat Manager como fuente secundaria.
- Exportación a Markdown legible.
- Herramientas de diagnóstico del dataset.

## Orden recomendado para agentes IA

Cuando se pida implementar una fase, el agente debe leer:

```text
AGENTS.md
README.md
docs/00-vision-producto.md
docs/01-roadmap.md
```

Y además solo los documentos específicos de la fase.

No debe leer toda la documentación si no es necesario.

## Definición de MVP completo

El MVP se considera completo cuando existe una aplicación local que permite:

- cargar un dataset español;
- aplicar overrides;
- buscar por lista de clase lanzadora, nivel máximo y término opcional;
- buscar en notas personales;
- consultar detalle;
- editar campos españoles;
- persistir correcciones manuales;
- conservar texto inglés de referencia;
- funcionar offline;
- usarse cómodamente en tablet con modo oscuro.
