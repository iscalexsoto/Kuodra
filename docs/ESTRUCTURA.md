# Estructura del proyecto Kuodra

Referencia viva del código. Complementa a [`CLAUDE.md`](../CLAUDE.md) (que fija las reglas de
arquitectura): aquí está **el mapa concreto** de pantallas, navegación, componentes y lógica de
dominio tras recrear el prototipo completo en Compose.

> Estado: **maqueta de alta fidelidad con paridad total frente a los `.dc.html` de `reference/`**.
> El **flujo de autenticación (correo + OTP) ya es real contra PocketBase** vía Ktor, con sesión
> persistida en DataStore y gating de arranque. El resto de datos (movimientos, personas, ajustes,
> historial) siguen siendo seed en memoria (`data/local/KuodraSeedSource`), pendientes de backend.

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
| `AddMovement` | alta de movimiento | `scrAdd` | FAB "Agregar" |
| `MovementDetail(id)` | detalle | `scrMovDetail` | fila de movimiento |
| `AllMovements` | ver todo (búsqueda/filtros) | `scrVerTodo` | "Ver todo" del dashboard |
| `Settings` | ajustes adaptativos | `scr*Settings` | menú → "Ajustes" |
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
  toggle de tema oscuro vive en `Settings` (no en el menú).
- En `AllMovements`: **overlay de búsqueda** (pantalla completa) y **sheet de filtros**.
- En `Settings`: calculadora de monto (presupuesto/fondo) y sheet de **agregar/editar contacto**.
- En `HistoryDetail`: flujo **reenviar corte** (`reshare` → `shared`).

El flujo cambia de **caso de uso** (`Personal` / `Gastos` / `Caja`) variando contenido y
terminología, **no** el styling. Lo controla `Space.useCase` + `terminologyFor()`.

---

## Árbol de paquetes (actual)

```
com.arenacun.kuodra
  KuodraApplication.kt          # startKoin(appModule, networkModule, dataModule, presentationModule)
  MainActivity.kt               # setContent { KuodraRoot() }
  di/                           # AppModule, NetworkModule, DataModule, PresentationModule
  domain/
    model/
      UseCase.kt                # enum + Terminology + terminologyFor()
      Session.kt                # usuario autenticado (userId + email); el token vive en data
      Space.kt, Person.kt, Category.kt, AvatarTone.kt
      Movement.kt              # incluye date: LocalDate + SplitShare + helpers puros
      MovementCategory.kt      # catálogo del selector de categoría (defaults)
      Calc.kt                  # MOTOR PURO de la calculadora (CalcState, CalcKey, evaluate, formatAmount)
      CalendarMonth.kt         # LÓGICA PURA del calendario (rejilla, navegación acotada a hoy)
      DateLabels.kt            # formateo puro de fechas ("Hoy · 20 jun", "Martes · 18 jun")
      SpaceSettings.kt         # BudgetConfig (día por frecuencia)/FundConfig/BudgetFrequency + SpaceSettings
      SettlementRecord.kt      # registro de corte/liquidación + SettlementLine
    usecase/
      MovementQuery.kt         # filter() + groupByDay() puros (búsqueda/filtros/agrupación)
    repository/
      AuthRepository, SpaceRepository, MovementRepository, SummaryRepository,
      PreferencesRepository, SettingsRepository
  data/
    local/
      KuodraSeedSource          # seed in-memory: movimientos, personas, categorías, settings, historial
      KuodraDataStore           # extensión Context.kuodraDataStore (Preferences DataStore único)
      SessionStore              # persiste token + identidad de la sesión PocketBase
    remote/
      PocketBaseClient          # HttpClient Ktor (OkHttp, JSON tolerante) + URLs de la colección users
      AuthApi / KtorAuthApi     # request-otp / auth-with-otp / records (alta) / auth-refresh
      dto/AuthDtos              # DTOs @Serializable de auth
    repository/                 # *RepositoryImpl (AuthRepositoryImpl real; Space/Preferences en DataStore)
  presentation/
    KuodraRoot.kt, navigation/ (Destinations, KuodraNavHost)
    crash/      CrashHandler (uncaught exceptions) + CrashActivity (pantalla de crash)
    app/        AppViewModel  # resuelve StartState (Loading/LoggedOut/NeedsName/Onboarding/Ready) al arrancar
    theme/      Theme(Kuodra accessor), Color(KuodraColors), Type, Shape
    component/
      KuodraButton, KuodraCard, KuodraTextField, KuodraBanner, KuodraListRow, KuodraHeroCard
      KuodraIcons.kt           # Chevron, PlusIcon, KLogoMark (logo oficial), ToneAvatar, CategoryTag, avatar()
      BackCircle
      KuodraCalculator         # numpad (dibuja CalcState, reenvía CalcKey)
      KuodraCalendar           # calendario (dibuja CalendarMonth, mes visible = remember)
      KuodraBottomSheet        # wrapper de ModalBottomSheet con tokens Kuodra
    feature/
      auth/        AuthViewModel + AuthUiState + Welcome/Email/Otp
      onboarding/  NameViewModel, ModeViewModel, CreateSpaceViewModel + Name/Mode/CreateSpace
      dashboard/   DashboardViewModel + DashboardUiState (incl. DashboardOverlay/LeaveStep) + DashboardScreen
      movement/    AddMovement{ViewModel,UiState,Screen}, MovementDetail{ViewModel,Screen}
      allmovements/AllMovements{ViewModel,UiState,Screen}
      settings/    Settings{ViewModel,UiState,Screen}  (adaptativa por caso de uso)
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

### Datos y "hoy"
- El seed (`KuodraSeedSource`) ahora incluye `date: LocalDate` real en cada movimiento, los `SpaceSettings`
  por caso de uso y el historial de cortes.
- El "hoy" es la fecha real del sistema (`LocalDate.now()`), inyectable como parámetro en los ViewModels
  que lo usan para poder fijarlo en tests.

---

## Build & verificación

```bash
./gradlew :app:assembleDebug          # compilar
./gradlew :app:testDebugUnitTest      # tests de host (domain puro + ViewModels/repos con fakes)
./gradlew :app:installDebug           # instalar en dispositivo/emulador
```

Para probar **auth end-to-end**: pon `pocketbase.url=...` en `local.properties` (no versionado) y
ten la instancia PocketBase con `users` (OTP + Create rule pública + SMTP). El test
`AuthRepositoryImplTest` cubre la orquestación (alta-si-falta, `otpId`, persistencia, signOut) sin red.

Pendiente recurrente: recorrido manual en emulador comparando 1:1 contra los `.dc.html` de
`reference/` en tema claro y oscuro (los hex y medidas del handoff son la fuente de verdad).
