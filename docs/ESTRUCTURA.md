# Estructura del proyecto Kuodra

Referencia viva del código. Complementa a [`CLAUDE.md`](../CLAUDE.md) (que fija las reglas de
arquitectura): aquí está **el mapa concreto** de pantallas, navegación, componentes y lógica de
dominio tras recrear el prototipo completo en Compose.

> Estado: **Personal y Gastos funcionan end-to-end** sobre el mismo patrón (**Room fuente de verdad
> offline + sync con PocketBase**, LWW + tombstones). Personal: auth (correo + OTP + Google), movimientos,
> categorías, presupuesto e historial de cortes, escaneo de tickets y telemetría. Gastos: espacios
> multi-instancia archivables, contactos con teléfono, gastos con pagadores múltiples y división por id
> (equitativo/montos/porcentajes) en pantalla dedicada, balances y liquidación reales (corte →
> `settlements`, WhatsApp para cobrar) y gasto Personal derivado. **La caja chica y el seed en memoria
> (`KuodraSeedSource`) se eliminaron.** Sesión + espacio activo en DataStore, con gating de arranque.

---

## Pantallas y navegación

Rutas type-safe en [`navigation/Destinations.kt`](../app/src/main/java/com/arenacun/kuodra/presentation/navigation/Destinations.kt),
cableadas en [`navigation/KuodraNavHost.kt`](../app/src/main/java/com/arenacun/kuodra/presentation/navigation/KuodraNavHost.kt).

| Destino | Pantalla | Prototipo | Entrada |
|---|---|---|---|
| `AuthGraph` → `Welcome`/`Email`/`Otp` | auth (correo + OTP y Google OAuth2, PocketBase) | `Kuodra Auth` | inicio (si no hay sesión) |
| `Name` | onboarding: captura el nombre del usuario | — | tras OTP (o al arrancar sin nombre) |
| `Mode` / `CreateSpace(useCase)` | onboarding | `isMode`/`isCreate` | tras `Name` |
| `Dashboard` | dashboard | `scrDashboard` | raíz de app |
| `AddGraph` → `AddMovement(editId?)`/`ScanTicket(source)` | alta **y edición** de movimiento (grafo anidado; comparte `ScanDraftViewModel`) | `scrAdd` | FAB "Agregar" → sheet de 3 opciones; Detalle → "Editar" (con `editId`) |
| `MovementDetail(id)` | detalle (reactivo: refleja ediciones al volver) | `scrMovDetail` | fila de movimiento |
| `AllMovements` | ver todo (búsqueda/filtros) | `scrVerTodo` | "Ver todo" del dashboard |
| `Settings` | ajustes adaptativos | `scr*Settings` | menú → "Ajustes" |
| `Categories` | gestión de categorías (buscador) | — | Ajustes → "Categorías" |
| `Settle` | liquidación: corte global + **liquidar por persona** (pago parcial/total con `KuodraNumberPad`) + WhatsApp | `scrSettle` | tarjeta "Liquidar saldos" |
| `AddGraph` → `SplitConfig` | pagadores + división de un gasto compartido (comparte el `AddMovementViewModel`); con >2 miembros la lista se **colapsa** a los seleccionados + botón que abre una hoja de selección; pagador único = total no editable; montos/% vía `KuodraNumberPad` | — | FieldRow "Dividir gasto" en `AddMovement` |
| `AddGraph` → `DetailConfig` | **detalle (partidas)** en pantalla propia (comparte el `AddMovementViewModel`): solo la lista scrollea; "Ajuste" + "Listo" fijos abajo; number pad de la cantidad | — | FieldRow/"Añadir detalle" en `AddMovement` |
| `History` / `HistoryDetail(id)` | historial de cortes y **pagos**; el detalle **reenvía** las deudas por WhatsApp o comparte el resumen | `scrHistory` | Ajustes → "Historial" / menú |

**Overlays sin destino propio** (estado en el `UiState` del ViewModel, no en `remember`):
- En `AddMovement`: **calculadora** (`Dialog`), **calendario** (`Dialog`), sheet de **categoría**
  (`ModalBottomSheet`), y **diálogo** de gasto Personal derivado. Detalle, pagadores y división van a
  pantallas propias (`DetailConfig` / `SplitConfig`).
- En `SplitConfig` (estado en el `AddMovementUiState` compartido): **number pad** de monto/porcentaje
  (`Dialog`, ruteado por `SplitPadTarget`) y **hojas de selección** de pagadores/participantes
  (`KuodraBottomSheet`, enum `SplitSheet`).
- En `Dashboard` (estado `DashboardOverlay`, hoja activa en el enum `DashboardSheet`):
  **selector de espacios** "Tus espacios" (Personal + espacios de Gastos + archivados, al tocar el
  título; "Crear espacio" navega a `CreateSpace`), y **menú "Opciones"** del espacio (botón ···, filas
  según caso de uso: compartir resumen, ajustes, cerrar periodo, historial, salir), más sus sheets
  disparados (`Share`/`Shared`, `PCloseConfirm`/`PClosed`) y el **flujo salir/archivar grupo** (overlay
  inline de 3 pasos `LeaveStep`; Confirmar llama `SpaceRepository.archive`). El FAB "Agregar" abre el
  sheet **`AddOptions`**: escanear ticket / tomar de la galería / capturar manual.
- En `AllMovements`: **overlay de búsqueda** (pantalla completa) y **sheet de filtros**.
- En `Settings`: calculadora de monto (presupuesto) y sheet de **agregar/editar contacto** (Nombre + Teléfono).
- En `HistoryDetail`: flujo **reenviar corte** (`reshare` → `shared`).

El flujo cambia de **caso de uso** (`Personal` / `Gastos`) variando contenido y terminología, **no**
el styling. Lo controla `Space.useCase` + `terminologyFor()`.

---

## Árbol de paquetes (actual)

```
com.arenacun.kuodra
  KuodraApplication.kt          # startKoin(networkModule, dataModule, telemetryModule, presentationModule)
                                #   + conecta CrashHandler↔Telemetry y hace flush() de telemetría al arrancar
  MainActivity.kt               # setContent { KuodraRoot() }
  di/                           # NetworkModule, DataModule, TelemetryModule, PresentationModule
  domain/
    model/
      UseCase.kt                # enum + Terminology + terminologyFor()
      Session.kt                # usuario autenticado (userId + email); el token vive en data
      Space.kt, Person.kt, Category.kt, AvatarTone.kt
      SpacePerson.kt           # contacto de un espacio (id, name, phone) + PersonRef.ME ("Tú")
      SharedExpense.kt         # PayerShare, SplitMode (None/Equal/Amount/Percent), SplitShare
      Settlement.kt            # corte de Gastos (lines por persona + transfers) + Transfer
      Movement.kt              # date + spaceId + payers/splitMode/splits + settlementId + items + returnStatus/... + helpers
      ReturnStatus.kt          # enum devolución (None/Pending/Returned), solo Personal
      MovementCategory.kt      # catálogo del selector de categoría (defaults)
      Calc.kt                  # MOTOR PURO de la calculadora (CalcState, CalcKey, evaluate, formatAmount)
      CalendarMonth.kt         # LÓGICA PURA del calendario (rejilla, navegación acotada a hoy)
      DateLabels.kt            # formateo puro de fechas ("Hoy · 20 jun", "Martes · 18 jun")
      SpaceSettings.kt         # BudgetConfig (día por frecuencia + returnPercent global)/BudgetFrequency + SpaceSettings
      SettlementRecord.kt      # registro de corte/liquidación (display) + SettlementLine
    scan/
      TicketScan.kt            # ScanSource, TicketParseSource, ParsedTicket(+Item), TicketScan
      OcrEngine.kt             # puerto de OCR (impl MLKit en data); Uri como String
      TicketParser.kt          # eslabón de la cadena de parseo (null ⇒ pasa al siguiente)
      OcrNormalizer.kt         # PUNTO ÚNICO de normalización del raw OCR (función pura)
      RegexTicketParser.kt     # fallback local puro (total/fecha/comercio/items por heurísticas)
    usecase/
      MovementQuery.kt         # filter() + groupByDay() puros (búsqueda/filtros/agrupación)
      ReturnCalc.kt            # PURO: base devolvible (por partidas), reembolso vivo/congelado, total por cobrar
      SplitCalc, SharedBalances, SettleSuggestions, CloseSettlement, WhatsAppMessage  # PUROS de Gastos
      ScanTicketUseCase.kt     # orquestador: OCR → normalize → cadena [Mistral, Regex]
    repository/
      AuthRepository, SpaceRepository, PersonRepository, MovementRepository, CategoryRepository,
      SummaryRepository, PreferencesRepository, SettingsRepository, SettlementRepository, SnapshotRepository
    telemetry/
      Telemetry.kt             # PUERTO NEUTRAL de observabilidad (breadcrumb/log/capture/setUser/flush)
                               #   + LogLevel + NoOpTelemetry. Impl detrás; swap a Sentry = otra impl
  data/
    local/
      KuodraDataStore           # extensión Context.kuodraDataStore (Preferences DataStore único)
      SessionStore              # persiste token + identidad de la sesión PocketBase
      db/                       # Room (fuente de verdad offline): KuodraDatabase (v10) + entities + DAOs + Converters
                                #   MovementEntity/Dao, CategoryEntity/Dao, BudgetEntity/Dao, PeriodSnapshotEntity/Dao,
                                #   SpaceEntity/Dao, PersonEntity/Dao, SettlementEntity/Dao, TelemetryEventEntity/Dao
    remote/
      PocketBaseClient          # HttpClient Ktor (OkHttp, JSON tolerante) + URLs de colecciones
      AuthApi / KtorAuthApi     # request-otp / auth-with-otp / records (alta) / auth-refresh
      MovementApi, CategoryApi, BudgetApi, PeriodSnapshotApi, SpaceApi, PersonApi, SettlementApi  # list(since)/create/update por colección
      TelemetryApi / KtorTelemetryApi  # POST a la colección telemetry_events (create rule autenticada)
      TicketAnalysisApi / KtorTicketAnalysisApi  # POST /api/kuodra/analyze-ticket (proxy Mistral, timeout 15s)
      dto/AuthDtos, dto/SyncDtos, dto/TelemetryDtos, dto/ScanDtos  # DTOs @Serializable por área
    sync/
      SyncManager               # push filas dirty (create/update) + pull deltas (updated>cursor), LWW + tombstones
      SyncCursorStore           # cursor por colección en DataStore
      SyncTrigger / WorkManagerSyncTrigger + SyncWorker  # agendado con WorkManager (red requerida)
    mapper/                     # DTO/Entity ↔ dominio: MovementMapper, CategoryMapper, BudgetMapper,
                                #   SnapshotMapper, SpaceMapper, PersonMapper, SettlementMapper, ScanMapper
    telemetry/                  # impl PocketBase del puerto Telemetry (ver "Observabilidad" abajo)
      PocketBaseTelemetry       # ring buffer de breadcrumbs + arma eventos → cola Room → TelemetryTrigger
      TelemetryUploader         # motor de entrega puro: drena spool, sube por lotes si hay sesión
      TelemetryUploadWorker / WorkManagerTelemetryTrigger  # WorkManager (patrón SyncWorker)
      CrashSpool                # spool en disco para crashes fatales (síncrono, a prueba de muerte)
      DeviceContextProvider, BreadcrumbBuffer, TelemetryMapper
    scan/
      MlKitOcrEngine            # impl del puerto OcrEngine (MLKit text-recognition BUNDLED, offline)
      MistralTicketParser       # eslabón remoto: proxy PocketBase→Mistral; cualquier fallo ⇒ null (cae a regex)
    repository/                 # *RepositoryImpl: Personal y Gastos reales (Room + sync) — Movement/Category/Budget/
                                #   Snapshot/Settings/Space/Person/Settlement; Auth real; espacio activo + Preferences en DataStore
  presentation/
    KuodraRoot.kt, navigation/ (Destinations, KuodraNavHost)
    crash/      CrashHandler (uncaught exceptions) + CrashActivity (pantalla de crash)
    app/        AppViewModel  # resuelve StartState (Loading/LoggedOut/NeedsName/Onboarding/Ready) al arrancar
    theme/      Theme(Kuodra accessor), Color(KuodraColors), Type, Shape
    component/
      KuodraButton, KuodraCard, KuodraTextField, KuodraBanner, KuodraListRow, KuodraHeroCard
      KuodraIcons.kt           # KIcon (pinta drawables ic_*), Chevron/PlusIcon (sobre ic_chevron_right/ic_add),
                               #   SearchGlyph, KLogoMark (logo oficial), ToneAvatar, CategoryTag, avatar()
      BackCircle
      KuodraCalculator         # numpad (dibuja CalcState, reenvía CalcKey)
      KuodraNumberPad          # teclado numérico ligero (solo dígitos/punto/borrar, sin operadores; reusa Calc)
      KuodraCalendar           # calendario (dibuja CalendarMonth, mes visible = remember)
      KuodraBottomSheet        # wrapper de ModalBottomSheet con tokens Kuodra
      AddOptionsSheetContent   # sheet "Agregar": escanear (ic_camera) / galería (ic_image_up) / manual (ic_notebook)
    feature/
      auth/        AuthViewModel + AuthUiState + Welcome/Email/Otp + OAuthRedirectBus (deeplink OAuth2)
      onboarding/  NameViewModel, ModeViewModel, CreateSpaceViewModel + Name/Mode/CreateSpace
      dashboard/   DashboardViewModel + DashboardUiState (incl. DashboardOverlay/LeaveStep/GastosHero) + DashboardScreen
      movement/    AddMovement{ViewModel,UiState,Screen}, SplitConfigScreen (pagadores+división), MovementDetail{ViewModel,Screen}
      scan/        ScanDraftViewModel (holder del TicketScan, scope AddGraph),
                   ScanTicket{UiState (ScanPhase), ViewModel, Screen} (CameraX + Photo Picker + animación)
      allmovements/AllMovements{ViewModel,UiState,Screen}
      settings/    Settings{ViewModel,UiState,Screen}  (adaptativa por caso de uso)
      categories/  Categories{ViewModel,UiState,Screen}  (catálogo + buscador, todos los casos)
      settle/      Settle{ViewModel,UiState,Screen}  (balances reales + WhatsApp)
      history/     History{ViewModel,Screen}, HistoryDetail{ViewModel,Screen}
```

---

## Convenciones aprendidas (aplícalas al extender)

### Lógica testeable → `domain`, sin Android
Toda la lógica no trivial vive como **funciones/objetos puros** en `domain` (usan `java.time` y
`kotlin.*`, nunca `android.*` ni Compose), con test en `app/src/test`:
- `Calc` (evaluación de expresión + formateo de monto) → `CalcTest`
- `CalendarMonth` (rejilla, deshabilitar futuro, navegación) → `CalendarMonthTest`
- `MovementQuery` (búsqueda/filtros/agrupación) → `MovementQueryTest`
- `DateLabels` (etiquetas de fecha)

La UI (`KuodraCalculator`, `KuodraCalendar`) es **stateless**: dibuja el modelo puro y reenvía
intenciones. Patrón a repetir para cualquier lógica nueva (p. ej. liquidación real).

### Estado de pantalla y overlays
- `UiState` inmutable en `StateFlow`; derivados con `combine`/`flatMapLatest` + `stateIn(...WhileSubscribed(5_000))`.
- **Los overlays (diálogos/sheets/pasos) son estado del ViewModel**, no `remember`. Excepción:
  estado puramente de UI y transitorio (texto de un campo efímero, el **mes visible** del calendario).
- Eventos one-shot (guardar→volver, registrar→volver) con `Channel(...).receiveAsFlow()` + `LaunchedEffect`.
- Modales centrados: `androidx.compose.ui.window.Dialog`. Hojas inferiores: `KuodraBottomSheet`
  (Material3 con `containerColor = Kuodra.colors.surface`, porque el tema usa `KuodraColors`, no `ColorScheme`).
- Confirmaciones/flujos cortos inline: patrón `Box` + scrim con tokens (ver `MovementDetail.confirmDelete`
  y el flujo salir/archivar del dashboard).

### Logo de marca (fuente única)
La "K" de Kuodra vive **una sola vez** como vector oficial en `res/drawable/ic_kuodra_logo.xml`
(monocromo). En pantalla se usa **siempre** vía `KLogoMark(boxSize, cornerRadius, background, foreground)`
(`KuodraIcons.kt`): un `Box` redondeado con `background` que contiene el vector con
`Icon(painterResource(ic_kuodra_logo), tint = foreground)`, así el logo hereda los tokens del tema
(claro/oscuro). No se redibuja a mano. Usos: Splash (`KuodraRoot`), Welcome (`WelcomeScreen`) y header
del Dashboard (versión chica, `boxSize = 38.dp`). El ícono de launcher es independiente (adaptive icon
en `mipmap-anydpi*` + `ic_launcher_foreground/_monochrome` + `@color/ic_launcher_background`); si cambia
el logo, edita esos drawables, nunca las pantallas.

### Pantalla adaptativa por caso de uso
`SettingsScreen` es **una sola pantalla** que ramifica con `when (useCase)` (igual que `DashboardScreen`),
no tres pantallas. El contrato `SettingsRepository` es mínimo (`settings()` observable + `update()` del
`SpaceSettings` completo); la lógica de edición vive en el ViewModel.

### Navegación y DI
- Destino `@Serializable` en `Destinations.kt`; la decisión de navegar va en **callbacks de pantalla**
  dentro de `KuodraNavHost`, nunca en ViewModels.
- ViewModels sin args de ruta: `viewModelOf(::VM)`. Con arg de ruta: `viewModel { (id) -> VM(id, get()) }`.
- Con un parámetro **no inyectable con default** (p. ej. `today: LocalDate = LocalDate.now()` en
  `AllMovementsViewModel`): usar factory explícito `viewModel { VM(get(), get()) }`, **no** `viewModelOf`.
- Contrato↔impl nuevo: `single { Impl(get()) } bind Contrato::class` en `DataModule`.

### Autenticación y sesión (PocketBase)
- Flujo OTP de dos pasos: `AuthRepository.requestOtp(email)` da de **alta-si-no-existe** (PocketBase
  solo envía el código a registros existentes) y guarda el `otpId`; `verifyOtp(code)` lo canjea por
  token+registro y persiste la sesión en `SessionStore` (DataStore). El token **no sale de `data`**.
- **Nombre del usuario:** se captura en la pantalla `Name` del onboarding y `AuthRepository.updateName`
  lo persiste en el registro `users` de PocketBase (PATCH) + cachea en `SessionStore`. Vive en
  `Session.name` (identidad del usuario, distinta del nombre del espacio que maneja `CreateSpace`/`Settings`).
- La URL de PocketBase llega por `BuildConfig.POCKETBASE_URL`, leída de `local.properties`
  (`pocketbase.url=...`, no versionada; default `http://10.0.2.2:8090` para el emulador). HTTP en claro
  permitido vía `res/xml/network_security_config.xml` (endurecer a HTTPS en producción).
- **Gating de arranque:** `AppViewModel.restoreSession()` valida el token (`auth-refresh`; 4xx ⇒ logout,
  error de red ⇒ conserva sesión) y `KuodraRoot` elige destino inicial: sin sesión→`AuthGraph`,
  con sesión sin nombre→`Name`, con sesión sin onboarding→`Mode`, con sesión configurada→`Dashboard`
  (mientras tanto, `Splash`).
- El `UseCase` elegido en onboarding y el tema oscuro se **persisten en DataStore** (sobreviven reinicios).
- **Login con Google (OAuth2):** flujo authorization-code + PKCE (PocketBase no soporta el ID-token
  nativo de Android, solo el de código). `AuthRepository.startGoogleSignIn()` pide el proveedor a
  `auth-methods`, guarda `state`+`codeVerifier` y devuelve la `authURL`; `WelcomeScreen` la abre en un
  **Custom Tab** (`androidx.browser`). Google redirige al **App Link** `https://<dominio>/oauth-redirect`,
  que `MainActivity` (`launchMode=singleTop`, intent-filter `autoVerify`) captura y publica en el
  `OAuthRedirectBus` (singleton Koin, `SharedFlow` replay=1 para el arranque en frío). `WelcomeScreen`
  lo consume y llama `AuthViewModel.completeOAuth2(code, state)` → `AuthRepository.completeOAuth2`, que
  **valida el `state`** (anti-CSRF), canjea vía `auth-with-oauth2` y persiste la sesión (nombre desde
  `meta` si el registro no lo trae, para saltarse la pantalla `Name`). El redirect sale de
  `BuildConfig.OAUTH_REDIRECT_URL` (`local.properties`), que también alimenta el host/path del
  intent-filter (`manifestPlaceholders`). Es **genérico**: añadir Facebook/Apple es solo config
  (ver [`docs/POCKETBASE.md`](POCKETBASE.md)).

### Observabilidad (telemetría remota, casera sobre PocketBase)
- Todo pasa por el **puerto neutral** `domain/telemetry/Telemetry` (breadcrumbs, `log`, `capture`,
  `captureFatalBlocking`, `setUser`, `flush`). Pantallas/repos **solo** dependen de esa interfaz;
  el default es `NoOpTelemetry`. **Migrar a Sentry** = crear `SentryTelemetry : Telemetry` y cambiar
  el binding en `TelemetryModule` — cero cambios en call sites.
- Impl `PocketBaseTelemetry`: mantiene un ring buffer de breadcrumbs y adjunta contexto de dispositivo;
  cada evento va a la **cola durable en Room** (`telemetry_events`) y se sube con `TelemetryUploader`
  (patrón `SyncManager`) vía `TelemetryUploadWorker` (WorkManager, con red).
- **Offline + pre-login:** los eventos se encolan siempre y se suben **solo cuando hay sesión** (la
  colección tiene create rule autenticada); los pre-login se atribuyen al usuario al hacer login
  (`flush()` en `verifyOtp`). **Crashes fatales:** se persisten síncronos en `CrashSpool` (disco) desde
  `CrashHandler` y se suben en el siguiente arranque.
- La telemetría **nunca debe tumbar la app**: toda operación de la impl es best-effort (no lanza).
- **Requiere una colección `telemetry_events` en PocketBase.** El esquema y las reglas están en
  [`POCKETBASE.md`](POCKETBASE.md) (fuente única de la config del servidor).

### Escaneo de tickets (pipeline en cadena)
- **Flujo:** FAB "Agregar" → sheet `AddOptions` → `ScanTicket(source)` (CameraX con permiso runtime,
  o Photo Picker sin permisos en minSdk 33) → `ScanTicketUseCase` → formulario `AddMovement`
  pre-poblado (comercio→concepto, total→monto, fecha acotada a hoy, partidas→items). El usuario
  **siempre** cae al formulario editable, aunque el parseo sea parcial.
- **Pipeline:** `OcrEngine` (MLKit bundled, offline) → `OcrNormalizer.normalize()` (**punto único**
  de manipulación del raw; toda corrección de OCR va ahí) → cadena `List<TicketParser>` en orden de
  prioridad inyectada por DI: hoy `[MistralTicketParser, RegexTicketParser]`. Un parser devuelve
  `null` para ceder al siguiente y **nunca lanza**. El futuro `TemplateTicketParser` (tickets de
  comercios conocidos sin red) se inserta **al frente de la lista en `DataModule`**, sin tocar contratos.
- **Mistral vía proxy PocketBase** (`/api/kuodra/analyze-ticket`, hook JS + `MISTRAL_API_KEY` en el
  servidor; ver [`POCKETBASE.md`](POCKETBASE.md)): la key nunca viaja en el APK. Sin red/sesión/hook
  ⇒ regex local, transparente para el usuario.
- **Resultado → formulario:** `ScanDraftViewModel` compartido en el grafo `AddGraph` (patrón
  AuthGraph); `consume()` entrega el `TicketScan` **una sola vez** y `AddMovementViewModel.applyScan`
  lo aplica. El flujo manual navega directo a `AddMovement` con draft vacío. En **modo edición**
  (`AddMovement(editId)`) el draft se ignora: el formulario se pre-puebla desde el movimiento
  existente, guardar llama `MovementRepository.update` conservando id/nota/metadata de escaneo, y
  si el `editId` no existe degrada a alta nueva.
- **Persistencia:** el movimiento escaneado guarda `scanRawText` (raw OCR **sin normalizar**,
  material para los templates futuros) y `scanSource` (`Camera`/`Gallery`; null = manual) en Room y
  PocketBase.
- La respuesta del proxy es **versionada** (`version` + campos opcionales): extensible a
  `template` sin romper clientes.

### Datos y "hoy"
- **Todo = Room + sync (fuente de verdad offline).** Los `*RepositoryImpl` leen/escriben Room
  (`data/local/db`) filtrando por `owner` (y por `space` en Gastos); las escrituras se marcan `dirty` y
  disparan el `SyncTrigger`. `SyncManager` hace por colección **push** de `dirty` + **pull** de deltas
  (`updated > cursor`) con **last-write-wins** y tombstones (`deleted`); no pisa filas con cambios locales
  pendientes ni las que ya están en la versión remota (evita que un pull borre datos que el servidor
  ignoró). Cada colección se sincroniza aislada. Esquema y reglas: [`POCKETBASE.md`](POCKETBASE.md).
- **El seed en memoria se eliminó**: Gastos ya es real (Room + sync), igual que Personal.
- **Lógica de negocio pura reutilizable** en `domain/usecase`: `BudgetPeriod`, `ClosePeriod` y
  `ReturnCalc` (Personal); y de Gastos: `SplitCalc` (resuelve/valida la división a centavos),
  `SharedBalances` (neto por persona; resta los **pagos** vivos), `SettleSuggestions` (transferencias,
  greedy determinístico), `RecordPayment` (arma un pago individual como `Settlement` kind=Payment),
  `CloseSettlement` (congela el corte + ids de movimientos y pagos a consumir), `ShareSummary` y
  `WhatsAppMessage`. Un corte y un pago individual comparten la colección `settlements`
  (`SettlementKind` = `Corte`/`Payment`; `settledBy` marca los pagos consumidos por un corte).
- El "hoy" es la fecha real del sistema (`LocalDate.now()`), inyectable como parámetro en los ViewModels
  que lo usan para poder fijarlo en tests.

---

## Build & verificación

```bash
./gradlew :app:assembleDebug          # compilar
./gradlew :app:testDebugUnitTest      # tests de host (domain puro + ViewModels/repos con fakes)
./gradlew :app:installDebug           # instalar en dispositivo/emulador
```

Para probar **end-to-end** (auth + datos): pon `pocketbase.url=...` en `local.properties` (no versionado)
y ten la instancia PocketBase con `users` (OTP + Create rule pública + SMTP) y las colecciones de datos
(`movements`, `categories`, `budgets`, `period_snapshots`, `telemetry_events`) con sus reglas —todo en
[`POCKETBASE.md`](POCKETBASE.md). El checklist de verificación (alta online, offline→reconexión,
multi-dispositivo, reinstalar→pull completo) vive en ese archivo. Tests de host: `AuthRepositoryImplTest`
(orquestación de auth sin red), `SyncManager`/`MovementRepositoryImpl` y ViewModels con repos *fake*.

Pendiente recurrente: recorrido manual en emulador comparando 1:1 contra los `.dc.html` de
`reference/` en tema claro y oscuro (los hex y medidas del handoff son la fuente de verdad).
