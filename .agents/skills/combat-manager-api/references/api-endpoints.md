# Combat Manager API endpoints

## Base URL

`http://localhost:12457`

## Discovery

### POST `/api/monster/list`

Request body:
```json
{
  "Name": "Air Elemental"
}
```

Response shape:
```json
{
  "Items": [
    {
      "Name": "Small Air Elemental",
      "ID": 154,
      "IsCustom": false
    }
  ]
}
```

## Full monster payload

### GET `/api/monster/getregular/{id}`

Use the `ID` from the list response.

Note: the local Combat Manager instance used during extraction responded to `GET` for this endpoint; treat that as the observed behavior for this repository unless the running API explicitly differs.

## Handling rules

- Treat the source as unstable: fields may be omitted, renamed, or nested differently.
- Prefer conservative extraction over guessing.
- Record exact source field names when they differ from prior payloads.
- Preserve the original payload when a field is ambiguous.
- Use the list endpoint only to resolve IDs; use `getregular` for the actual extraction.
