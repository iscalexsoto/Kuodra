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

## `users` (Auth) — login con Google (OAuth2)

Segundo método de login sobre la **misma** colección `users` (convive con correo + OTP; un
usuario con el mismo email se unifica en un solo registro). En Android el flujo es
**authorization-code + PKCE con Custom Tabs y redirect por App Link HTTPS** (PocketBase soporta
el flujo de código, **no** el de *ID token* nativo de Google). El cliente hace `auth-methods` →
abre `authURL` en un Custom Tab → Google redirige al App Link
`https://<TU_DOMINIO>/oauth-redirect?code&state` → la app captura el código → `auth-with-oauth2`.

> **Requisito de la app:** además de configurar el servidor, hace falta la clave
> `oauth.redirect.url=https://<TU_DOMINIO>/oauth-redirect` en `local.properties` (no versionada).
> De ahí salen `BuildConfig.OAUTH_REDIRECT_URL` y el `host`/`path` del intent-filter del App Link
> (`manifestPlaceholders` en `app/build.gradle.kts`). Debe coincidir **exactamente** con el
> redirect registrado en Google.

### 1. Google Cloud Console (una vez)

1. **APIs & Services → OAuth consent screen**: User Type **External**; completar App name,
   *user support email* y *developer email*. Añadir scopes básicos (`email`, `profile`,
   `openid`). Publicar (o añadir testers si queda en modo *Testing*).
2. **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Application type: **Web application** (NO "Android"; PocketBase usa cliente web con secret,
     y Google solo admite `redirect_uri` **HTTPS** para ese tipo — por eso el redirect es un App
     Link y no un custom scheme).
   - **Authorized redirect URIs** → añadir **exactamente** `https://<TU_DOMINIO>/oauth-redirect`
     (debe coincidir carácter por carácter con `oauth.redirect.url`). Para probar en local también
     se puede añadir `http://127.0.0.1:8090/api/oauth2-redirect`.
   - Guardar el **Client ID** y **Client Secret** generados.

### 2. PocketBase admin

- Collections → `users` → **Edit collection (engranaje) → Options → OAuth2** → habilitar y
  seleccionar **Google** → pegar **Client ID** y **Client Secret** → Save.
- **Mapping de campos al crear el usuario** (Options → OAuth2 → *Optional users create fields
  mapping*): aplica **solo al crear** la cuenta desde OAuth2 (primer login). Mapear **OAuth2 full
  name → `name`** para que el registro nazca con el nombre real de Google y el usuario se salte la
  pantalla de onboarding "¿cómo te llamas?". El resto (avatar, id, username) puede quedar sin mapear
  (Kuodra no los usa; `username` lo autogenera PocketBase). Si no se mapea, el cliente igual cae al
  `meta.name` de la respuesta como respaldo, pero el mapping deja el nombre persistido en el registro.
- OAuth2 **no** está disponible para la colección `_superusers` (solo colecciones auth normales).
- **Vinculación por email = automática (no hay toggle en el admin).** Al entrar con Google,
  PocketBase busca un registro `users` con el mismo correo y **enlaza** la identidad de Google en
  vez de duplicar (salvo que esa cuenta Google ya esté enlazada a otro registro). Así, quien se dio
  de alta por OTP con `x@gmail.com` y luego entra con Google del mismo correo cae en el mismo
  registro, sin configurar nada.
- **Requisito de versión: PocketBase ≥ v0.22.14** (idealmente la última). Corrige el advisory
  [GHSA-m93w-4fxv-r35v](https://github.com/pocketbase/pocketbase/security/advisories/GHSA-m93w-4fxv-r35v):
  al enlazar OAuth2 a un usuario previo **no verificado** resetea su contraseña. Inofensivo aquí
  porque solo entramos por OTP/OAuth (sin contraseña) y el correo de Google viene verificado.

### 3. App Link (verificación del dominio) — servido por el propio PocketBase

Como el backend ya es público en `https://<TU_DOMINIO>` (p. ej. vía Cloudflare Tunnel → PocketBase),
**no hace falta un sitio web aparte: el propio PocketBase sirve los dos endpoints** con un hook JS.
Crear `pb_hooks/oauth_links.pb.js` junto al binario y reiniciar PocketBase:

```js
/// <reference path="../pb_data/types.d.ts" />

// Digital Asset Links: permite que Android abra la app (App Link) en vez del navegador al volver
// del login con Google. Debe responder 200 + application/json, SIN redirecciones. Incluir la
// huella SHA-256 de CADA firma que uses (debug y, en producción, release / Play App Signing).
routerAdd("GET", "/.well-known/assetlinks.json", (e) => {
    return e.json(200, [{
        relation: ["delegate_permission/common.handle_all_urls"],
        target: {
            namespace: "android_app",
            package_name: "com.arenacun.kuodra",
            sha256_cert_fingerprints: ["<SHA256_DEBUG>", "<SHA256_RELEASE>"],
        },
    }])
})

// Página de aterrizaje del redirect. Si el App Link está verificado y la app instalada, Android
// intercepta esta URL y ni se muestra; es solo el fallback si se abre en navegador.
routerAdd("GET", "/oauth-redirect", (e) => {
    return e.html(200, "<!doctype html><meta charset=utf-8><title>Kuodra</title>" +
        "<p>Ya puedes volver a la app Kuodra.</p>")
})
```

- **Huella SHA-256 de debug:** `./gradlew :app:signingReport` (línea `SHA-256:` del variant `debug`)
  o `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android`.
- **Al sacar build de release:** añade también la huella SHA-256 de **release** al array
  `sha256_cert_fingerprints` (y reinicia PocketBase). Sale del variant `release` de
  `./gradlew :app:signingReport` o, si usas **Play App Signing**, de Play Console → *App integrity →
  App signing key certificate*. Debug y release pueden convivir en el mismo array.
- **Verificar** (no debe haber 301/302 ni 404): `curl -i https://<TU_DOMINIO>/.well-known/assetlinks.json`
  (content-type `application/json`) y `curl -i https://<TU_DOMINIO>/oauth-redirect`. Validador oficial:
  `https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://<TU_DOMINIO>&relation=delegate_permission/common.handle_all_urls`.
- El `intent-filter` con `android:autoVerify="true"` en `.MainActivity` (host/ruta de
  `oauth.redirect.url`) dispara la verificación al instalar. Comprobar en el dispositivo:
  `adb shell pm get-app-links com.arenacun.kuodra` (debe salir `verified` para el host).
- Alternativa: servir `assetlinks.json` como archivo estático en `pb_public/.well-known/assetlinks.json`
  (para `/oauth-redirect` es mejor el hook, para evitar el redirect por barra final).

> **⚠️ ORDEN CORRECTO (importante):** Android verifica el App Link **una sola vez al instalar** y
> **cachea el resultado** (no reintenta solo). Por eso: **1º** deja el hook activo + PocketBase
> reiniciado y confirma con `curl` que `assetlinks.json` responde 200; **2º recién entonces**
> instala/reinstala la app. Si instalas antes de que el endpoint esté en línea, la verificación
> queda cacheada como **fallida** y el login abrirá el navegador (verás la página de `/oauth-redirect`)
> en vez de la app.

#### Troubleshooting: el redirect abre el navegador y no la app

Síntoma: al volver de Google se queda en la página "Ya puedes volver a la app Kuodra" y no entra.
Significa que el App Link **no está verificado** en ese dispositivo. Diagnóstico y arreglo:

```bash
# Estado de verificación (con varios dispositivos, añade -s <serial>)
adb shell pm get-app-links com.arenacun.kuodra
#   api.kuodra.com: verified   → OK
#   api.kuodra.com: 1024       → FALLÓ (verificación cacheada como error; códigos >=1024 = fallo)

# Arreglo 1 (no destructivo): re-lanzar la verificación. Es ASÍNCRONA: espera ~5-10 s y reconsulta,
# no confíes en la primera lectura inmediata.
adb shell pm verify-app-links --re-verify com.arenacun.kuodra

# Arreglo 2 (garantizado): desinstalar y reinstalar con el endpoint YA en línea.
adb uninstall com.arenacun.kuodra && ./gradlew :app:installDebug

# Último recurso (solo ese dispositivo, sin depender de autoVerify): aprobar el dominio a mano.
adb shell pm set-app-links-user-selection --user cur --package com.arenacun.kuodra true api.kuodra.com
```

Antes de tocar el dispositivo, confirma que el servidor está bien (si esto responde correcto, el
problema es el caché del dispositivo, no el backend):

```bash
curl -i https://api.kuodra.com/.well-known/assetlinks.json   # 200 + application/json, sin 3xx/404
curl -s "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://api.kuodra.com&relation=delegate_permission/common.handle_all_urls"
```

Otras causas si `curl` **no** responde bien: `oauth.redirect.url` no estaba en `local.properties` al
compilar (el `intent-filter` queda con el host por defecto `kuodra.app`), o Cloudflare mete un
redirect/challenge sobre `/.well-known/*` (el fetch del verificador debe recibir 200 directo).

### Contrato HTTP (lo que consume `KtorAuthApi`)

```
GET  {POCKETBASE_URL}/api/collections/users/auth-methods
200: { "oauth2": { "enabled": true, "providers": [
        { "name": "google", "state": "…", "codeVerifier": "…",
          "codeChallenge": "…", "codeChallengeMethod": "S256",
          "authURL": "https://accounts.google.com/o/oauth2/auth?...&redirect_uri=" } ] }, ... }

POST {POCKETBASE_URL}/api/collections/users/auth-with-oauth2
Body: { "provider": "google", "code": "<de Google>",
        "codeVerifier": "<el del provider>", "redirectURL": "https://<TU_DOMINIO>/oauth-redirect" }
200:  { "token": "…", "record": { "id", "email", "name", … },
        "meta": { "name", "email", "avatarURL", … } }
```

- La URL a abrir en el Custom Tab = `provider.authURL` + `URLEncoder(redirectURL)` (el `authURL`
  termina en `redirect_uri=` para que el cliente le concatene su redirect).
- El `state` devuelto por Google debe **coincidir** con `provider.state` (anti-CSRF); si no, el
  cliente aborta sin canjear.
- `redirectURL` debe ser **idéntico** en los 3 sitios: Google console, la URL abierta y el body
  de `auth-with-oauth2`.
- `meta.name` prellena el nombre y permite saltarse la pantalla de onboarding "¿cómo te llamas?"
  para usuarios de Google (el cliente lo usa como respaldo si `record.name` viene vacío).

> **Versión de PocketBase:** la forma `oauth2.providers[]` es de PB v0.23+. En versiones
> anteriores el endpoint era `list-auth-methods` con `authProviders[]`; confirmar contra la
> instancia real antes de fijar los DTOs.

> **Añadir más proveedores (Facebook/Apple/…):** el flujo del cliente es genérico. Basta
> habilitar el proveedor aquí (paso 2) + registrar su app en la consola correspondiente + pasar
> su `name` en el cliente. No hace falta tocar el contrato HTTP.

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
| `items`      | json     | Partidas del desglose: `[{id, concept, amount(centavos), payer, inFund}]`; `[]` = sin detalle. **Obligatorio crearlo**: sin esta columna PocketBase ignora el campo del DTO y el pull del sync borra las partidas locales. |
| `scanRawText`| text     | Opcional. Raw OCR del ticket si el movimiento nació de un escaneo (puede ser largo; material para los templates futuros). |
| `scanSource` | text     | Opcional. `Camera`/`Gallery` (nombre del enum `ScanSource`); vacío = captura manual. |
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

## Proxy de análisis de tickets (ruta custom + Mistral)

El escaneo de tickets manda el texto OCR a **Mistral** a través de una **ruta custom** de
PocketBase: la API key vive **solo en el servidor** (env var), nunca en el APK. Si esta ruta no
existe o falla, la app cae automáticamente al parser regex local — la feature degrada, no rompe.

### Contrato HTTP (lo que espera el cliente, `KtorTicketAnalysisApi`)

```
POST {POCKETBASE_URL}/api/kuodra/analyze-ticket
Authorization: <token de usuario PocketBase, crudo>
Content-Type: application/json

Body:    { "text": "<raw OCR normalizado>" }
200 OK:  { "version": 1, "merchant": "OXXO"|null, "total": 187.50|null,
           "date": "2026-07-01"|null, "items": [{"concept": "Coca 600ml", "amount": 19.0}] }
400: body sin "text" · 401: sin auth · 502: Mistral no disponible (el cliente cae a regex)
```

`version` + campos opcionales = extensible: una v2 podrá añadir `template` (paso de templates)
sin romper clientes v1 (`ignoreUnknownKeys`).

### Configuración

1. **Env var** en el entorno del proceso PocketBase: `MISTRAL_API_KEY=<key de api.mistral.ai>`.
2. **Hook JS**: crear `pb_hooks/analyze_ticket.pb.js` junto al binario con el contenido de abajo
   y reiniciar PocketBase.

```js
/// <reference path="../pb_data/types.d.ts" />

const SYSTEM_PROMPT = `Eres un extractor de datos de tickets de compra (principalmente de México, en español).
Recibirás el texto OCR crudo de un ticket, posiblemente con errores de reconocimiento.
Responde ÚNICAMENTE un objeto JSON con exactamente estas claves:
  "merchant": string|null  — nombre corto del comercio (ej. "OXXO", "Walmart"), sin razón social ni RFC.
  "total": number|null     — importe TOTAL pagado, en unidades de la moneda (ej. 187.50). No uses el subtotal.
  "date": string|null      — fecha del ticket en formato yyyy-MM-dd; null si no es legible o es ambigua.
  "items": array           — partidas compradas: [{"concept": string, "amount": number}].
Reglas: no incluyas como items subtotal, IVA, propina, cambio, efectivo, tarjeta ni descuentos;
corrige errores obvios de OCR en los conceptos; si un dato no es confiable usa null o [];
nunca inventes valores; no agregues texto fuera del JSON.`

routerAdd("POST", "/api/kuodra/analyze-ticket", (e) => {
    const body = e.requestInfo().body
    if (!body || typeof body.text !== "string" || body.text.trim() === "") {
        return e.json(400, { error: "text requerido" })
    }
    const key = $os.getenv("MISTRAL_API_KEY")
    if (!key) return e.json(502, { error: "proxy no configurado" })
    try {
        const res = $http.send({
            url: "https://api.mistral.ai/v1/chat/completions",
            method: "POST",
            headers: { "Authorization": "Bearer " + key, "Content-Type": "application/json" },
            timeout: 12, // segundos; menor que los 15s de timeout del cliente
            body: JSON.stringify({
                model: "mistral-small-latest",
                temperature: 0,
                response_format: { type: "json_object" },
                messages: [
                    { role: "system", content: SYSTEM_PROMPT },
                    { role: "user", content: body.text.slice(0, 8000) },
                ],
            }),
        })
        const parsed = JSON.parse(res.json.choices[0].message.content)
        return e.json(200, {
            version: 1,
            merchant: typeof parsed.merchant === "string" ? parsed.merchant : null,
            total: typeof parsed.total === "number" ? parsed.total : null,
            date: typeof parsed.date === "string" ? parsed.date : null,
            items: Array.isArray(parsed.items) ? parsed.items : [],
        })
    } catch (err) {
        return e.json(502, { error: "análisis no disponible" })
    }
}, $apis.requireAuth())
```

Notas:
- `$apis.requireAuth()` exige un token de usuario válido en `Authorization` (401 si falta).
- `mistral-small-latest` basta para extracción estructurada (barato y rápido, ~1-2 s por ticket).
- El texto se trunca a 8000 chars por control de costo (un ticket normal es mucho menor).

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

Escaneo de tickets:
6. Con el hook y `MISTRAL_API_KEY` configurados: escanear un ticket → el formulario se puebla con
   comercio/total/fecha/partidas de Mistral; el movimiento guardado trae `scanRawText` y `scanSource`.
7. Sin la key (o en modo avión): el escaneo sigue funcionando con el parser regex local.

---

## Historial de cambios

| Fecha      | Cambio                                                                 |
|------------|------------------------------------------------------------------------|
| 2026-07-04 | Login con Google (OAuth2) en `users`: cliente Web en Google Cloud + provider Google en PB + App Link `https://<TU_DOMINIO>/oauth-redirect` con `assetlinks.json`. Flujo auth-code + PKCE vía Custom Tabs (`auth-methods` / `auth-with-oauth2`). |
| 2026-07-03 | Campo `items` (json) en `movements`. Faltaba desde que existen las partidas: PocketBase ignoraba el campo del DTO y el pull del sync **borraba las partidas locales** tras cada guardado. |
| 2026-07-02 | Campos `scanRawText` y `scanSource` en `movements` + ruta custom `/api/kuodra/analyze-ticket` (hook JS + `MISTRAL_API_KEY`) para el escaneo de tickets. |
| 2026-07-01 | Alta de `telemetry_events` (observabilidad remota).                    |
| Fase 2     | Colecciones de datos `movements`, `categories`, `budgets`, `period_snapshots`. |
| Base       | `users` (auth + OTP + SMTP) — configuración base del backend.          |
