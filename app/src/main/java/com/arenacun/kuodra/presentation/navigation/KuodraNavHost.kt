package com.arenacun.kuodra.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.arenacun.kuodra.presentation.app.toDestination
import com.arenacun.kuodra.presentation.feature.auth.AuthViewModel
import com.arenacun.kuodra.presentation.feature.auth.EmailScreen
import com.arenacun.kuodra.presentation.feature.auth.OtpScreen
import com.arenacun.kuodra.presentation.feature.allmovements.AllMovementsScreen
import com.arenacun.kuodra.presentation.feature.auth.WelcomeScreen
import com.arenacun.kuodra.presentation.feature.categories.CategoriesScreen
import com.arenacun.kuodra.presentation.feature.dashboard.DashboardScreen
import com.arenacun.kuodra.presentation.feature.history.HistoryDetailScreen
import com.arenacun.kuodra.presentation.feature.history.HistoryScreen
import com.arenacun.kuodra.presentation.feature.movement.AddMovementScreen
import com.arenacun.kuodra.presentation.feature.movement.AddMovementViewModel
import com.arenacun.kuodra.presentation.feature.movement.MovementDetailScreen
import com.arenacun.kuodra.presentation.feature.movement.SplitConfigScreen
import com.arenacun.kuodra.presentation.feature.onboarding.CreateSpaceScreen
import com.arenacun.kuodra.presentation.feature.onboarding.ModeScreen
import com.arenacun.kuodra.presentation.feature.onboarding.NameScreen
import com.arenacun.kuodra.presentation.feature.scan.ScanDraftViewModel
import com.arenacun.kuodra.presentation.feature.scan.ScanTicketScreen
import com.arenacun.kuodra.presentation.feature.settings.SettingsScreen
import com.arenacun.kuodra.presentation.feature.settle.SettleScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun KuodraNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: Destination = Destination.AuthGraph,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Réplica de la transición scrIn del prototipo: fade + slide-up 8px.
        enterTransition = { fadeIn(tween(280)) + slideInVertically(tween(280)) { 8 } },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(280)) + slideInVertically(tween(280)) { 8 } },
        popExitTransition = { fadeOut(tween(120)) },
    ) {
        // ===== Auth (grafo anidado: comparte AuthViewModel) =====
        navigation<Destination.AuthGraph>(startDestination = Destination.Welcome) {
            composable<Destination.Welcome> { entry ->
                WelcomeScreen(
                    onContinue = { navController.navigate(Destination.Email) },
                    // Google resuelve el login desde aquí (sin pasar por Otp): mismo enrutado
                    // con el estado ya resuelto (nombre/onboarding/dashboard).
                    onVerified = { state ->
                        navController.navigate(state.toDestination()) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    viewModel = entry.sharedAuthViewModel(navController),
                )
            }
            composable<Destination.Email> { entry ->
                EmailScreen(
                    onBack = { navController.popBackStack() },
                    onCodeSent = { navController.navigate(Destination.Otp) },
                    viewModel = entry.sharedAuthViewModel(navController),
                )
            }
            composable<Destination.Otp> { entry ->
                OtpScreen(
                    viewModel = entry.sharedAuthViewModel(navController),
                    onBack = { navController.popBackStack() },
                    onChangeEmail = { navController.popBackStack() },
                    // Rutea con el estado ya resuelto: si el backend tenía el nombre, se salta
                    // la pantalla de nombre (y va a Mode/Dashboard según corresponda).
                    onVerified = { state ->
                        navController.navigate(state.toDestination()) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
        }

        // ===== Onboarding =====
        composable<Destination.Name> {
            NameScreen(
                onContinue = {
                    navController.navigate(Destination.Mode) {
                        popUpTo(Destination.Name) { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.Mode> {
            ModeScreen(
                onContinueToCreate = { useCase -> navController.navigate(Destination.CreateSpace(useCase)) },
                onContinueToApp = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(Destination.Mode) { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.CreateSpace> { entry ->
            val args = entry.toRoute<Destination.CreateSpace>()
            CreateSpaceScreen(
                useCase = args.useCase,
                onBack = { navController.popBackStack() },
                // Funciona desde el onboarding (Mode) y desde el dashboard: limpia toda la pila
                // y deja el Dashboard como única pantalla, reflejando el espacio recién creado.
                onCreated = {
                    navController.navigate(Destination.Dashboard) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }

        // ===== App =====
        composable<Destination.Dashboard> {
            DashboardScreen(
                onAddManual = { navController.navigate(Destination.AddMovement()) },
                onScanTicket = { source -> navController.navigate(Destination.ScanTicket(source)) },
                onOpenMovement = { id -> navController.navigate(Destination.MovementDetail(id)) },
                onSeeAllMovements = { navController.navigate(Destination.AllMovements) },
                onOpenSettings = { navController.navigate(Destination.Settings) },
                onSettle = { navController.navigate(Destination.Settle) },
                onOpenHistory = { navController.navigate(Destination.History) },
                onCreateSpace = { useCase -> navController.navigate(Destination.CreateSpace(useCase)) },
            )
        }
        composable<Destination.AllMovements> {
            AllMovementsScreen(
                onBack = { navController.popBackStack() },
                onOpenMovement = { id -> navController.navigate(Destination.MovementDetail(id)) },
            )
        }
        composable<Destination.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCategories = { navController.navigate(Destination.Categories) },
                onSignedOut = {
                    navController.navigate(Destination.AuthGraph) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable<Destination.Categories> {
            CategoriesScreen(onBack = { navController.popBackStack() })
        }
        composable<Destination.History> {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate(Destination.HistoryDetail(id)) },
            )
        }
        composable<Destination.HistoryDetail> { entry ->
            val args = entry.toRoute<Destination.HistoryDetail>()
            HistoryDetailScreen(recordId = args.id, onBack = { navController.popBackStack() })
        }
        composable<Destination.Settle> {
            SettleScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
        composable<Destination.MovementDetail> { entry ->
            val args = entry.toRoute<Destination.MovementDetail>()
            MovementDetailScreen(
                movementId = args.id,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Destination.AddMovement(editId = id)) },
            )
        }
        // ===== Alta de movimiento (grafo anidado: comparte ScanDraftViewModel) =====
        // "Capturar manualmente" navega directo a AddMovement (el grafo se crea solo y el draft
        // queda vacío); el escaneo pasa por ScanTicket, que deja el TicketScan en el draft.
        navigation<Destination.AddGraph>(startDestination = Destination.AddMovement()) {
            composable<Destination.ScanTicket> { entry ->
                val args = entry.toRoute<Destination.ScanTicket>()
                val draftViewModel = entry.sharedScanDraftViewModel(navController)
                ScanTicketScreen(
                    viewModel = koinViewModel { parametersOf(args.source) },
                    onBack = { navController.popBackStack() },
                    // La pantalla de escaneo no queda en el back stack: volver desde el
                    // formulario regresa al dashboard, no a la cámara.
                    onScanned = { scan ->
                        draftViewModel.set(scan)
                        navController.navigate(Destination.AddMovement()) {
                            popUpTo<Destination.ScanTicket> { inclusive = true }
                        }
                    },
                    onFallbackToManual = {
                        navController.navigate(Destination.AddMovement()) {
                            popUpTo<Destination.ScanTicket> { inclusive = true }
                        }
                    },
                )
            }
            composable<Destination.AddMovement> { entry ->
                val args = entry.toRoute<Destination.AddMovement>()
                AddMovementScreen(
                    draftViewModel = entry.sharedScanDraftViewModel(navController),
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onOpenSplit = { navController.navigate(Destination.SplitConfig) },
                    viewModel = entry.sharedAddMovementViewModel(navController, args.editId),
                )
            }
            composable<Destination.SplitConfig> { entry ->
                SplitConfigScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = entry.sharedAddMovementViewModel(navController, null),
                )
            }
        }
    }
}

/** AuthViewModel compartido entre las pantallas del grafo de auth (scope = AuthGraph). */
@Composable
private fun NavBackStackEntry.sharedAuthViewModel(navController: NavHostController): AuthViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry<Destination.AuthGraph>() }
    return koinViewModel(viewModelStoreOwner = parentEntry)
}

/** ScanDraftViewModel compartido entre Scan y AddMovement (scope = AddGraph). */
@Composable
private fun NavBackStackEntry.sharedScanDraftViewModel(navController: NavHostController): ScanDraftViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry<Destination.AddGraph>() }
    return koinViewModel(viewModelStoreOwner = parentEntry)
}

/**
 * AddMovementViewModel compartido entre AddMovement y SplitConfig (scope = AddGraph). El [editId]
 * solo se usa al crearlo (lo fija la primera pantalla del grafo, AddMovement); SplitConfig recibe
 * la misma instancia ya creada, así que pasa null.
 */
@Composable
private fun NavBackStackEntry.sharedAddMovementViewModel(
    navController: NavHostController,
    editId: String?,
): AddMovementViewModel {
    val parentEntry = remember(this) { navController.getBackStackEntry<Destination.AddGraph>() }
    return koinViewModel(viewModelStoreOwner = parentEntry) { parametersOf(editId) }
}
