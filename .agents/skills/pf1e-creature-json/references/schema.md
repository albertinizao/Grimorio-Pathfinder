# PF1e Creature Catalog JSON Schema

This skill targets the catalog JSON format loaded from `src/main/resources/catalog/*.json`.

## Required top-level fields

```json
{
  "id": "",
  "nombre": "",
  "nivelSummon": 0,
  "alineamiento": "N",
  "tamano": "Small",
  "tipo": "animal",
  "subtipos": [],
  "plantillasPermitidas": [],
  "iniciativa": 0,
  "sentidos": [],
  "percepcion": 0,
  "atributos": {
    "fuerza": 0,
    "destreza": 0,
    "constitucion": 0,
    "inteligencia": 0,
    "sabiduria": 0,
    "carisma": 0
  },
  "ca": {
    "normal": 0,
    "toque": 0,
    "desprevenido": 0,
    "detalle": ""
  },
  "pg": {
    "maximos": 0,
    "formula": ""
  },
  "salvaciones": {
    "fortaleza": 0,
    "reflejos": 0,
    "voluntad": 0
  },
  "velocidades": [
    {
      "tipo": "land",
      "valor": 0
    }
  ],
  "ataques": [
    {
      "id": "",
      "name": "",
      "attackBonus": 0,
      "quantity": 1,
      "attackType": "MELEE",
      "damageComponents": [
        {
          "formula": "",
          "damageType": "PIERCING",
          "multipliesOnCritical": true
        }
      ],
      "critical": {
        "threatRangeStart": 20,
        "multiplier": 2
      },
      "notes": []
    }
  ],
  "espacio": "",
  "alcance": "",
  "ataquesEspeciales": [],
  "defensasEspeciales": [],
  "notasTacticas": [],
  "habilidadesResumidas": [],
  "habilidadesCompletas": [],
  "fullStatBlock": ""
}
```

## Enums and allowed values

### `alignment`

`LG`, `LN`, `LE`, `NG`, `N`, `NE`, `CG`, `CN`, `CE`

### `tamano`

`FINE`, `DIMINUTIVE`, `TINY`, `SMALL`, `MEDIUM`, `LARGE`, `HUGE`, `GARGANTUAN`, `COLOSSAL`

### `velocidades.tipo`

`land`, `fly`, `swim`, `climb`, `burrow`, `other`

### `ataques.attackType`

`MELEE`, `RANGED`

### `damageType`

`PIERCING`, `SLASHING`, `BLUDGEONING`, `FIRE`, `COLD`, `ACID`, `ELECTRICITY`, `SONIC`, `FORCE`, `UNTYPED`, `OTHER`

### `defensasEspeciales.tipo`

`DAMAGE_REDUCTION`, `RESISTANCE`, `IMMUNITY`, `VULNERABILITY`, `OTHER`

## Extraction notes

- `nivelSummon` comes from the creature's summon level, not CR.
- `ca.detalle` should preserve the parenthetical AC breakdown.
- `pg.formula` should preserve the HP dice formula as written.
- `damageComponents` should split mixed damage types into separate entries.
- `notes` should carry rider effects like grab, trip, pull, or other non-damage text.
- `fullStatBlock` should contain the full original source text, with line breaks preserved as a single JSON string.
- When a defense or special ability is described in prose, place a short functional summary in `habilidadesResumidas` and the fuller text in `habilidadesCompletas`.

## Common mistakes to avoid

- Do not use free text where the repository expects enum values.
- Do not merge two different natural attacks into one object.
- Do not remove damage types from attacks.
- Do not translate the original stat block into the JSON fields unless the field is explicitly meant to be localized.
- Do not infer template permissions unless the source or the creature's rules make them explicit.
