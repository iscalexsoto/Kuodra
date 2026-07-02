# PocketBase — configuración del servidor (fuente única)

Este archivo describe **todo lo que tiene que existir en la instancia de PocketBase** para que la app
funcione: colecciones, campos, reglas de API y settings. Sirve para levantar una instancia desde cero
y para revisar qué cambió en el servidor.

> **REGLA:** todo cambio en el cliente que requiera una actualización en PocketBase (colección, campo,
> regla de API, índice, settings de auth/SMTP…) **debe** actualizar este archivo en el mismo commit.
> Ver la regla en [`CLAUDE.md`](../CLAUDE.md) §Backend.

**Cómo aplicar los cambios:** en el panel de admin de PocketBase (Collections → New/Edit), o como
migración versionada en `pb_migrations/` de tu servidor. La URL de la instancia va en `local.properties`
(`pocketbase.url=…`, no versionada). Hace falta el permiso `INTERNET` y `res/xml/network_security_config.xml`
en la app.

---

## `users` (tipo: Auth) — login por correo + OTP

- **OTP habilitado** (Options → Auth methods → One-time password).
- **SMTP configurado** (Settings → Mail settings) para que PocketBase envíe el código.
- **Create rule pública** (vacía / abierta): el alta-si-no-existe del cliente crea el registro antes de
  pedir el OTP. Endurecer más adelante si se cierra el auto-registro.
- Campos usados por la app: `email`, `name` (texto). El resto son los de una colección auth estándar.

Flujo cliente: `request-otp` → `auth-with-otp` → `auth-refresh` (ver `KtorAuthApi`).

---

## Colecciones de datos (sincronización offline-first)

Para que la sincronización de **gastos personales** funcione end-to-end. El cliente usa Room como
fuente de verdad y `SyncManager` hace push de filas `dirty` + pull de deltas (`updated > cursor`) con
last-write-wins. En todas, `id`/`created`/`updated` son de sistema (el cliente envía su propio `id` de
15 chars al crear y usa `updated` como cursor/LWW).

### `movements` (tipo: Base)

| Campo        | Tipo     | Notas                											|
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

### `categories` (tipo: Base)

| Campo      | Tipo     | Notas                                  |
|------------|----------|----------------------------------------|
| `owner`    | relation → `users` (single, required, cascade delete) | |
| `name`     | text     |                                        |
| `tag`      | text     | Etiqueta corta (2 letras).             |
| `tone`     | text     | Nombre del enum `AvatarTone`.          |
| `archived` | bool     |                                        |
| `deleted`  | bool     | Tombstone.                             |

> La categoría estática "Sin categoría" vive solo en el cliente; **no** se sube ni se crea aquí.

### `budgets` (tipo: Base)

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

### `period_snapshots` (tipo: Base)

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

### API rules (en TODAS las colecciones de datos de arriba)

Aislamiento multi-tenant por usuario. En List, View, Create, Update y Delete:

```
@request.auth.id != "" && owner = @request.auth.id
```

(En Create, además, PocketBase valida que el `owner` enviado coincida con el usuario autenticado por la
misma regla.)

---

## `telemetry_events` (tipo: Base) — observabilidad remota

Recibe logs, errores y crashes (ver `docs/ESTRUCTURA.md` §Observabilidad). Los nombres de campo deben
coincidir **exactamente** con `data/remote/dto/TelemetryDtos.kt` (`TelemetryEventDto`).

| Campo            | Tipo en PB            | Notas                                   |
|------------------|-----------------------|-----------------------------------------|
| `level`          | Plain text            | `Debug`/`Info`/`Warning`/`Error`/`Fatal`|
| `type`           | Plain text            | `log` / `error` / `crash`               |
| `message`        | Plain text            |                                         |
| `stacktrace`     | Plain text            | puede ser largo                         |
| `breadcrumbs`    | JSON                  | array de migas                          |
| `context`        | JSON                  | dispositivo/OS/versión                  |
| `tags`           | JSON                  | extras del evento                       |
| `release`        | Plain text            | `versionName (versionCode)`             |
| `session_id`     | Plain text            | UUID por proceso                        |
| `fingerprint`    | Plain text            | agrupación de eventos similares         |
| `client_created` | Number                | epoch ms del cliente                    |
| `owner`          | Relation → `users`    | single, **opcional** (no required)      |

**Reglas de API:**

- **Create:** `@request.auth.id != ""` (solo autenticados).
- **List / View / Update / Delete:** vacías con el candado cerrado (solo superusuario desde el admin).
  El cliente nunca lee esta colección.

Notas:
- PocketBase ignora campos del body que no estén en el esquema, pero si marcas alguno como *required* y
  el cliente no lo manda, el `POST` falla. Por eso `owner` va **opcional** (en la práctica siempre se
  sube con sesión, así que nunca llega vacío).
- La app **encola** los eventos localmente y los sube solo cuando hay sesión; sin esta colección, la
  telemetría se acumula en el dispositivo pero no llega al servidor.

---

## Verificación end-to-end

Datos (sync):
1. Alta de un gasto/categoría en la app → aparece el registro en PocketBase Admin con el `owner` correcto.
2. **Offline:** modo avión, crear/editar/borrar → al reconectar, el `SyncWorker` sube los `dirty`.
3. **Multi-dispositivo:** dos emuladores con la misma cuenta → alta en A; en B aparece tras el pull (al
   reabrir o tras el periódico). Borrado en A se propaga a B vía tombstone.
4. Reinstalar la app e iniciar sesión → se recupera todo (pull completo con cursor vacío).

Telemetría:
5. Genera un `log(Warning)`, una excepción capturada y un crash → aparecen en `telemetry_events` con
   breadcrumbs y contexto (el crash, tras reabrir la app).

---

## Historial de cambios

| Fecha      | Cambio                                                                 |
|------------|------------------------------------------------------------------------|
| 2026-07-01 | Alta de `telemetry_events` (observabilidad remota).                    |
| Fase 2     | Colecciones de datos `movements`, `categories`, `budgets`, `period_snapshots`. |
| Base       | `users` (auth + OTP + SMTP) — configuración base del backend.          |
