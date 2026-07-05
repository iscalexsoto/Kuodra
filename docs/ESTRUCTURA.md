# Estructura del proyecto Kuodra

Referencia viva del código. Complementa a [`CLAUDE.md`](../CLAUDE.md) (que fija las reglas de
arquitectura): aquí está **el mapa concreto** de pantallas, navegación, componentes y lógica de
dominio tras recrear el prototipo completo en Compose.

> Estado: **maqueta de alta fidelidad con paridad total frente a los `.dc.html` de `reference/`**,
> ya con el **flujo Personal funcional end-to-end**: autenticación (correo + OTP), movimientos,
> categorías, presupuesto e historial de cortes sobre **Room (fuente de verdad offline) + sync con
> PocketBase**, más escaneo de tickets y telemetría remota. Sesión persistida en DataStore y gating de
> arranque. Los casos de uso **Gastos y Caja** siguen siendo seed en memoria
> (`data/local/KuodraSeedSource`), pendientes de backend.

---

## Pantallas y navegación

Rutas type-safe en [`navigation/Destinations.kt`](../app/src/main/java/com/arenacun/kuodra/presentation/navigation/Destinations.kt),
cableadas en [`navigation/KuodraNavHost.kt`](../app/src/main/java/com/arenacun/kuodra/presentation/navigation/KuodraNavHost.kt).

| Destino | Pantalla | Prototipo | Entrada |
|---|---|---|---|
| `AuthGraph` → `Welcome`/`Email`/`Otp` | auth (correo + OTP real, PocketBase) | `Kuodra Auth` | inicio (si no hay sesión) |
| `Name` | onboarding: captura el nombre del usuario | — | tras OTP (o al arrancar sin nombre) |
| `Mode` / `CreateSpace(useCase)` | onboarding | `isMode`/`isCreate` | tras `Name` |
| `Dashboard` | dashboard | `scrDashboard` | raíz de app |
| `AddGraph` → `AddMovement(editId?)`/`ScanTicket(source)` | alta **y edición** de movimiento (grafo anidado; comparte `ScanDraftViewModel`) | `scrAdd` | FAB "Agregar" → sheet de 3 opciones; Detalle → "Editar" (con `editId`) |
| `MovementDetail(id)` | detalle (reactivo: refleja ediciones al volver) | `scrMovDetail` | fila de movimiento |
| `AllMovements` | ver todo (búsqueda/filtros) | `scrVerTodo` | "Ver todo" del dashboard |
| `Settings` | ajustes adaptativos | `scr*Settings` | menú → "Ajustes" |
| `Categories` | gestión de categorías (buscador) | — | Ajustes → "Categorías" |
| `Settle` | liquidación / corte | `scrSettle` | tarjeta "Liquidar/Corte" |
| `Replenish` | reponer fondo (Caja) | `scrRepon` | banner "Reponer" / menú |
| `History` / `HistoryDetail(id)` | historial de cortes | `scrHistory` | Ajustes → "Historial" / menú |

**Overlays sin destino propio** (estado en el `UiState` del ViewModel, no en `remember`):
- En `AddMovement`: **calculadora** (`Dialog`), **calendario** (`Dialog`), sheets de **categoría**,
  **pagador** y **dividir** (`ModalBottomSheet`).
- En `Dashboard` (estado `DashboardOverlay`, hoja activa en el enum `DashboardSheet`):
  **selector de espacios** "Tus espacios" (al tocar el título), **crear espacio** y **menú "Opciones"**
  del espacio (botón ···, filas según caso de uso: compartir resumen/corte, ajustes, cerrar periodo,
  historial, reponer, salir), más sus sheets disparados (`Share`/`Shared`, `PCloseConfirm`/`PClosed`) y
  el **flujo salir/archivar grupo** (overlay inline de 3 pasos `LeaveStep`, patrón `confirmDelete`). El
  toggle de tema oscuro vive en `Settings` (no en el menú). El FAB "Agregar" abre el sheet
  **`AddOptions`** (`AddOptionsSheetContent`): escanear ticket / tomar de la galería / capturar manual.
- En `AllMovements`: **overlay de búsqueda** (pantalla completa) y **sheet de filtros**.
- En `Settings`: calculadora de monto (presupuesto/fondo) y sheet de **agregar/editar contacto**.
- En `HistoryDetail`: flujo **reenviar corte** (`reshare` → `shared`).

El flujo cambia de **caso de uso** (`Personal` / `Gastos` / `Caja`) variando contenido y
terminología, **no** el styling. Lo controla `Space.useCase` + `terminologyFor()`.

---

## Árbol de paquetes (actual)

```
com.arenacun.kuodra
  KuodraApplication.kt          # startKoin(appModule, networkModule, dataModule, telemetryModule, presentationModule)
                                #   + conecta CrashHandler↔Telemetry y hace flush() de telemetría al arrancar
  MainActivity.kt               # setContent { KuodraRoot() }
  di/                           # AppModule, NetworkModule, DataModule, TelemetryModule, PresentationModule
  domain/
    model/
      UseCase.kt                # enum + Terminology + terminologyFor()
      Session.kt                # usuario autenticado (userId + email); el token vive en data
      Space.kt, Person.kt, Category.kt, AvatarTone.kt
      Movement.kt              # incluye date: LocalDate + items (MovementItem) + adjustmentOf() + helpers puros
      MovementCategory.kt      # catálogo del selector de categoría (defaults)
      Calc.kt                  # MOTOR PURO de la calculadora (CalcState, CalcKey, evaluate, formatAmount)
      CalendarMonth.kt         # LÓGICA PURA del calendario (rejilla, navegación acotada a hoy)
      DateLabels.kt            # formateo puro de fechas ("Hoy · 20 jun", "Martes · 18 jun")
      SpaceSettings.kt         # BudgetConfig (día por frecuencia)/FundConfig/BudgetFrequency + SpaceSettings
      SettlementRecord.kt      # registro de corte/liquidación + SettlementLine
    scan/
      TicketScan.kt            # ScanSource, TicketParseSource, ParsedTicket(+Item), TicketScan
      OcrEngine.kt             # puerto de OCR (impl MLKit en data); Uri como String
      TicketParser.kt          # eslabón de la cadena de parseo (null ⇒ pasa al siguiente)
      OcrNormalizer.kt         # PUNTO ÚNICO de normalización del raw OCR (función pura)
      RegexTicketParser.kt     # fallback local puro (total/fecha/comercio/items por heurísticas)
    usecase/
      MovementQuery.kt         # filter() + groupByDay() puros (búsqueda/filtros/agrupación)
      ScanTicketUseCase.kt     # orquestador: OCR → normalize → cadena [Mistral, Regex]
    repository/
      AuthRepository, SpaceRepository, MovementRepository, CategoryRepository,
      SummaryRepository, PreferencesRepository, SettingsRepository, SnapshotRepository
    telemetry/
      Telemetry.kt             # PUERTO NEUTRAL de observabilidad (breadcrumb/log/capture/setUser/flush)
                               #   + LogLevel + NoOpTelemetry. Impl detrás; swap a Sentry = otra impl
  data/
    local/
      KuodraDataStore           # extensión Context.kuodraDataStore (Preferences DataStore único)
      SessionStore              # persiste token + identidad de la sesión PocketBase
      KuodraSeedSource          # seed in-memory — SOLO Gastos/Caja (aún sin backend); Personal ya no lo usa
      db/                       # Room (fuente de verdad offline): KuodraDatabase (v7) + entities + DAOs + Converters
                                #   MovementEntity/Dao, CategoryEntity/Dao, BudgetEntity/Dao,
                                #   PeriodSnapshotEntity/Dao, TelemetryEventEntity/Dao
    remote/
      PocketBaseClient          # HttpClient Ktor (OkHttp, JSON tolerante) + URLs de colecciones
      AuthApi / KtorAuthApi     # request-otp / auth-with-otp / records (alta) / auth-refresh
      MovementApi, CategoryApi, BudgetApi, PeriodSnapshotApi  # list(since)/create/update por colección (interfaz + impl Ktor)
      TelemetryApi / KtorTelemetryApi  # POST a la colección telemetry_events (create rule autenticada)
      TicketAnalysisApi / KtorTicketAnalysisApi  # POST /api/kuodra/analyze-ticket (proxy Mistral, timeout 15s)
      dto/AuthDtos, dto/SyncDtos, dto/TelemetryDtos, dto/ScanDtos  # DTOs @Serializable por área
    sync/
      SyncManager               # push filas dirty (create/update) + pull deltas (updated>cursor), LWW + tombstones
      SyncCursorStore           # cursor por colección en DataStore
      SyncTrigger / WorkManagerSyncTrigger + SyncWorker  # agendado con WorkManager (red requerida)
    mapper/                     # DTO/Entity ↔ dominio: MovementMapper, CategoryMapper, BudgetMapper,
                                #   SnapshotMapper, ScanMapper
    telemetry/                  # impl PocketBase del puerto Telemetry (ver "Observabilidad" abajo)
      PocketBaseTelemetry       # ring buffer de breadcrumbs + arma eventos → cola Room → TelemetryTrigger
      TelemetryUploader         # motor de entrega puro: drena spool, sube por lotes si hay sesión
      TelemetryUploadWorker / WorkManagerTelemetryTrigger  # WorkManager (patrón SyncWorker)
      CrashSpool                # spool en disco para crashes fatales (síncrono, a prueba de muerte)
      DeviceContextProvider, BreadcrumbBuffer, TelemetryMapper
    scan/
      MlKitOcrEngine            # impl del puerto OcrEngine (MLKit text-recognition BUNDLED, offline)
      MistralTicketParser       # eslabón remoto: proxy PocketBase→Mistral; cualquier fallo ⇒ null (cae a regex)
    repository/                 # *RepositoryImpl: Personal real (Room + sync) — Movement/Category/Budget/
                                #   Snapshot/Settings; Auth real; Space/Preferences en DataStore; seed solo Gastos/Caja
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
      auth/        AuthViewModel + AuthUiState + Welcome/Email/Otp
      onboarding/  NameViewModel, ModeViewModel, CreateSpaceViewModel + Name/Mode/CreateSpace
      dashboard/   DashboardViewModel + DashboardUiState (incl. DashboardOverlay/LeaveStep) + DashboardScreen
      movement/    AddMovement{ViewModel,UiState,Screen}, MovementDetail{ViewModel,Screen}
      scan/        ScanDraftViewModel (holder del TicketScan, scope AddGraph),
                   ScanTicket{UiState (ScanPhase), ViewModel, Screen} (CameraX + Photo Picker + animación)
      allmovements/AllMovements{ViewModel,UiState,Screen}
      settings/    Settings{ViewModel,UiState,Screen}  (adaptativa por caso de uso)
      categories/  Categories{ViewModel,UiState,Screen}  (catálogo + buscador, todos los casos)
      settle/      Settle{ViewModel,UiState,Screen}
      replenish/   Replenish{ViewModel,UiState,Screen}
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
- Google OAuth: **diferido** (botón "Próximamente" deshabilitado en `WelcomeScreen`).

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
- **Personal = Room + sync (fuente de verdad offline).** Los `*RepositoryImpl` leen/escriben Room
  (`data/local/db`) filtrando por `owner`; las escrituras se marcan `dirty` y disparan el `SyncTrigger`.
  `SyncManager` hace por colección **push** de `dirty` + **pull** de deltas (`updated > cursor`) con
  **last-write-wins** y tombstones (`deleted`); no pisa filas con cambios locales pendientes ni las que
  ya están en la versión remota (evita que un pull borre datos que el servidor ignoró). Cada colección
  se sincroniza aislada. Esquema y reglas del servidor: [`POCKETBASE.md`](POCKETBASE.md).
- **El seed (`KuodraSeedSource`) solo respalda Gastos y Caja** (movimientos, personas, ajustes,
  historial) mientras no tengan backend. Personal ya no depende del seed.
- **Lógica de negocio pura reutilizable** en `domain/usecase`: `BudgetPeriod` (ventana del periodo de
  presupuesto) y `ClosePeriod` (arma el `PeriodSnapshot` del corte Personal). Patrón a repetir para la
  liquidación de Gastos/Caja.
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
