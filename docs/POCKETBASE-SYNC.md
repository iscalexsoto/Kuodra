# PocketBase — colecciones para sincronización (Fase 2)

Para que la sincronización de **gastos personales** funcione end-to-end hay que crear dos
colecciones en PocketBase (además de `users`, que ya existe para el auth). El cliente usa el patrón
offline-first: Room es la fuente de verdad y `SyncManager` hace push de filas `dirty` + pull de
deltas (`updated > cursor`) con last-write-wins.

> Requisitos previos ya cubiertos: `users` con OTP, `pocketbase.url` en `local.properties`,
> permiso `INTERNET` y `network_security_config.xml`.

## Colección `movements` (tipo: Base)

| Campo        | Tipo     | Notas                                                            |
|--------------|----------|-----------------------------------------------------------------|
| `owner`      | relation → `users` (single, required, cascade delete) | Dueño del registro. |
| `amount`     | number   | Monto en **centavos** (entero).                                 |
| `category`   | text     | `categoryId` (referencia lógica a `categories`, no relación).   |
| `title`      | text     |                                                                 |
| `note`       | text     |                                                                 |
| `date`       | text     | Fecha ISO `yyyy-MM-dd` (texto, no el tipo date de PB).          |
| `payer`      | text     | Opcional (Gastos/Caja).                                         |
| `splitNames` | json     | Lista de nombres (Gastos).                                      |
| `deleted`    | bool     | Tombstone: borrado lógico para propagar la baja.               |

`id`, `created`, `updated` son campos de sistema (el cliente envía `id` propio de 15 chars al crear
y usa `updated` como cursor/LWW).

## Colección `categories` (tipo: Base)

| Campo      | Tipo     | Notas                                  |
|------------|----------|----------------------------------------|
| `owner`    | relation → `users` (single, required, cascade delete) | |
| `name`     | text     |                                        |
| `tag`      | text     | Etiqueta corta (2 letras).             |
| `tone`     | text     | Nombre del enum `AvatarTone`.          |
| `archived` | bool     |                                        |
| `deleted`  | bool     | Tombstone.                             |

> La categoría estática "Sin categoría" vive solo en el cliente; **no** se sube ni se crea aquí.

## Colección `budgets` (tipo: Base)

Una fila por usuario; el **id del registro es el `owner`** (el cliente lo crea con ese id).

| Campo            | Tipo     | Notas                                    |
|------------------|----------|------------------------------------------|
| `owner`          | relation → `users` (single, required)    | id del registro = este valor. |
| `enabled`        | bool     |                                          |
| `frequency`      | text     | Nombre del enum (`Weekly`/`Biweekly`/…). |
| `amount`         | number   | Monto límite en **centavos**.            |
| `weekday`        | number   |                                          |
| `firstDay`       | number   |                                          |
| `secondDay`      | number   |                                          |
| `monthlyDay`     | number   |                                          |
| `customInterval` | number   |                                          |
| `deleted`        | bool     |                                          |

## Colección `period_snapshots` (tipo: Base)

| Campo          | Tipo   | Notas                                              |
|----------------|--------|----------------------------------------------------|
| `owner`        | relation → `users` (single, required)              | |
| `title`        | text   |                                                    |
| `periodStart`  | text   | ISO `yyyy-MM-dd`.                                   |
| `periodEnd`    | text   | ISO `yyyy-MM-dd`.                                   |
| `totalSpent`   | number | Centavos.                                          |
| `budgetAmount` | number | Centavos; opcional (vacío si no había presupuesto).|
| `lines`        | json   | `[{categoryName,count,amount(centavos),tone}]`.    |
| `createdAt`    | number | epoch millis.                                      |
| `deleted`      | bool   |                                                    |

## API rules (en TODAS las colecciones de datos)

Aislamiento multi-tenant por usuario. En List, View, Create, Update y Delete:

```
@request.auth.id != "" && owner = @request.auth.id
```

(En Create, además, PocketBase valida que `owner` enviado coincida con el usuario autenticado por la
misma regla.)

## Verificación end-to-end

1. Alta de un gasto/categoría en la app → aparece el registro en PocketBase Admin con el `owner`
   correcto.
2. **Offline:** modo avión, crear/editar/borrar → al reconectar, el `SyncWorker` sube los `dirty`.
3. **Multi-dispositivo:** dos emuladores con la misma cuenta → alta en A; en B aparece tras el pull
   (al reabrir o tras el periódico). Borrado en A se propaga a B vía tombstone.
4. Reinstalar la app e iniciar sesión → se recupera todo (pull completo con cursor vacío).
