# AGENTS.md

## Proyecto

**Grimorio Pathfinder** es una aplicación local y offline para consultar, buscar y editar conjuros de **Pathfinder 1e** en español.

Uso principal: encontrar rápidamente conjuros útiles durante una partida o durante la preparación, por ejemplo: lista de clase lanzadora de Clérigo + nivel máximo 3 + término "veneno".

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

## Terminología obligatoria

Usar `lista de clase lanzadora` para listas como Clérigo, Druida, Inquisidor o Mago/Hechicero.

Usar `lista de conjuros` para el concepto general que también puede incluir dominios, linajes, patronos, misterios o inquisiciones.

No introducir formas alternativas para el mismo concepto.

## Datos y traducción

- La app trabaja con conjuros ya guardados en español.
- No traducir conjuros al vuelo durante el uso normal.
- La traducción IA pertenece al pipeline de generación del dataset, no a la consulta en mesa.
- El texto inglés original debe conservarse como referencia.
- La búsqueda del MVP se realiza solo sobre contenido español efectivo y notas personales.
- La fuente canónica española vive en archivos versionados.
- La base SQLite es una proyección local reconstruible para búsqueda y uso rápido.

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

Reglas:

- `spells-es.generated.json` contiene el dataset español generado.
- `spells-es.generated.json` puede regenerarse.
- `spells-es.generated.json` no debe editarse desde la app.
- `spells-es.overrides.json` contiene correcciones manuales, estados manuales y `personalNotes`.
- Las correcciones manuales no deben sobrescribirse automáticamente.
- Una traducción manual tiene prioridad sobre una generada.
- La app debe poder aplicar overrides sobre el dataset generado.
- SQLite no es fuente canónica irremplazable.

## Decisiones cerradas

### `LOCKED`

`LOCKED` es solo un valor de `translationStatus`.

No existe en el MVP un flag técnico adicional `locked`.

El bloqueo se persiste como:

```json
{
  "fields": {
    "translationStatus": "LOCKED"
  }
}
```

### `personalNotes`

`personalNotes` vive canónicamente en:

```text
data/overrides/spells-es.overrides.json
```

No vive en `spells-es.generated.json` ni en un archivo separado durante el MVP.

## Funcionalidad MVP

La app debe permitir:

- importar/cargar el dataset español;
- aplicar overrides;
- buscar conjuros;
- filtrar por lista de clase lanzadora;
- filtrar por nivel máximo;
- buscar una frase o término;
- navegar sin término de búsqueda;
- ver detalle de conjuro;
- editar campos españoles;
- editar notas personales;
- marcar estado de traducción/revisión.

Búsqueda principal MVP:

```text
lista de clase lanzadora + nivel máximo + término/frase opcional
```

Ejemplo:

```text
Clérigo + nivel máximo 3 + veneno
```

Debe buscar en:

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

No buscar en inglés en el MVP.

La búsqueda debe ignorar mayúsculas/minúsculas y acentos.

## Modelo conceptual

### Spell

Debe conservar al menos:

- id estable;
- nombre español;
- nombre inglés;
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
- descripción inglesa;
- fuente/libro;
- estado de traducción;
- notas personales;
- fechas de creación/modificación.

### SpellListEntry

Representa la pertenencia de un conjuro a una lista de conjuros.

Debe permitir listas de clase lanzadora y futuras listas especiales.

Campos conceptuales:

- spellId;
- listType;
- listName;
- level.

Ejemplos:

```text
CLASS / Clérigo / 2
CLASS / Inquisidor / 3
DOMAIN / Curación / 4
BLOODLINE / Dracónico / 5
PATRON / Invierno / 2
```

En el MVP, la UI puede centrarse en listas de clase lanzadora, pero el modelo debe soportar listas especiales.

## Estados de traducción

Usar estados simples:

```text
NOT_TRANSLATED
AI_TRANSLATED
REVIEW_REQUIRED
REVIEWED
MANUALLY_EDITED
LOCKED
```

Reglas:

- Mostrar conjuros no revisados, pero marcarlos.
- `MANUALLY_EDITED` implica prioridad sobre contenido generado.
- `LOCKED` no debe sobrescribirse por procesos automáticos.
- `LOCKED` no debe duplicarse con un flag técnico adicional.

## Estilo de traducción

Usar traducción de mesa:

- clara;
- breve;
- precisa;
- consistente;
- cómoda durante partida.

Preferir formas como:

```text
TS: Fortaleza niega
RC: sí
Duración: 1 min./nivel
Alcance: toque
Tiempo de lanzamiento: 1 acción estándar
```

Evitar traducciones literarias largas si una forma breve es más útil en mesa.

## Arquitectura backend

Mantener arquitectura hexagonal.

### Domain

- entidades;
- value objects;
- reglas de negocio;
- estados;
- validaciones.

No depende de Spring, JPA, SQLite ni controladores.

### Application

Casos de uso, por ejemplo:

- `ImportSpanishDatasetUseCase`;
- `ApplyOverridesUseCase`;
- `SearchSpellsUseCase`;
- `GetSpellDetailUseCase`;
- `UpdateSpellFieldUseCase`;
- `UpdatePersonalNotesUseCase`;
- `MarkSpellAsReviewedUseCase`.

### Ports

Interfaces, por ejemplo:

- `SpellRepository`;
- `SpellSearchRepository`;
- `SpanishDatasetReader`;
- `OverridesReader`;
- `OverridesWriter`;
- `ClockProvider`.

### Infrastructure

Adaptadores técnicos:

- SQLite;
- lector JSON;
- escritor de overrides;
- repositorios concretos;
- migraciones.

### Web

- controladores REST;
- DTOs;
- mappers;
- validaciones de entrada.

Los controladores no deben contener lógica de negocio.

## API REST

La documentación actual define contratos funcionales mínimos.

El contrato REST cerrado debe documentarse en:

```text
docs/10-api-rest.md
```

Hasta que ese documento exista, no tratar las rutas conceptuales como una especificación OpenAPI final.

## Frontend MVP

Pantallas mínimas:

1. Búsqueda:
   - lista de clase lanzadora;
   - nivel máximo;
   - término/frase.

2. Resultados:
   - nombre;
   - nivel;
   - escuela;
   - descriptores;
   - tiempo;
   - alcance;
   - TS;
   - RC;
   - estado;
   - fragmento.

3. Detalle:
   - campos españoles;
   - texto inglés plegable/secundario;
   - notas;
   - estado;
   - edición campo por campo.

## Fuera de alcance MVP

No implementar salvo petición explícita:

- personajes concretos;
- conjuros conocidos/preparados;
- slots;
- cálculo automático de CD;
- integración con Combat Manager;
- traducción IA desde UI;
- pantalla admin de traducciones;
- favoritos;
- etiquetas personales;
- PWA completa;
- multiusuario;
- autenticación;
- Docker;
- PostgreSQL;
- servicios externos;
- búsqueda en inglés;
- bloqueo por campo.

## Reglas para agentes IA

- No leer toda la documentación para cada tarea.
- Leer siempre `AGENTS.md`, `README.md`, `docs/00-vision-producto.md` y `docs/01-roadmap.md` si existen.
- Leer documentos específicos solo si la tarea lo requiere.
- Hacer cambios pequeños y verificables.
- No cambiar stack, arquitectura o base de datos sin petición explícita.
- No introducir dependencias externas innecesarias.
- No eliminar texto inglés original.
- No sobrescribir overrides.
- No mezclar lógica de negocio en controladores.
- No crear un flag `locked`; usar `translationStatus = LOCKED`.
- No guardar `personalNotes` fuera de overrides en el MVP.

## Tests prioritarios

Cubrir como mínimo:

1. importación del dataset español;
2. aplicación de overrides;
3. búsqueda por lista de clase lanzadora + nivel máximo + término;
4. búsqueda en notas personales;
5. edición campo por campo;
6. preservación del texto inglés;
7. no sobrescritura de traducciones manuales;
8. `LOCKED` como estado sin flag adicional;
9. persistencia canónica de `personalNotes` en overrides.

## Criterio de calidad

Una funcionalidad está bien implementada si:

- funciona offline;
- respeta el dataset español;
- no pierde correcciones manuales;
- mantiene separación hexagonal;
- tiene tests básicos;
- es cómoda para usar en mesa.
