package com.arenacun.kuodra.di

import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.KuodraDatabase
import com.arenacun.kuodra.data.local.kuodraDataStore
import com.arenacun.kuodra.data.repository.AuthRepositoryImpl
import com.arenacun.kuodra.data.repository.BudgetRepository
import com.arenacun.kuodra.data.repository.CategoryRepositoryImpl
import com.arenacun.kuodra.data.repository.MovementRepositoryImpl
import com.arenacun.kuodra.data.repository.PersonRepositoryImpl
import com.arenacun.kuodra.data.repository.PreferencesRepositoryImpl
import com.arenacun.kuodra.data.repository.SettingsRepositoryImpl
import com.arenacun.kuodra.data.repository.SettlementRepositoryImpl
import com.arenacun.kuodra.data.repository.SnapshotRepositoryImpl
import com.arenacun.kuodra.data.repository.SpaceRepositoryImpl
import com.arenacun.kuodra.data.repository.SummaryRepositoryImpl
import com.arenacun.kuodra.data.scan.MistralTicketParser
import com.arenacun.kuodra.data.scan.MlKitOcrEngine
import com.arenacun.kuodra.data.sync.SyncCursorStore
import com.arenacun.kuodra.data.sync.SyncManager
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.data.sync.WorkManagerSyncTrigger
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.scan.OcrEngine
import com.arenacun.kuodra.domain.scan.RegexTicketParser
import com.arenacun.kuodra.domain.usecase.ScanTicketUseCase
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

/** Implementaciones de los contratos de dominio (capa data). */
val dataModule = module {
    // Persistencia local
    single { androidContext().kuodraDataStore }
    single { SessionStore(get()) }

    // Room (fuente de verdad offline)
    single {
        // pre-release: sin migraciones. Si Room se recrea, limpiamos los cursores para que el
        // siguiente sync vuelva a traer TODO desde PocketBase (si no, el delta se saltaría los
        // registros viejos y los datos locales quedarían vacíos pese a existir en el servidor).
        val cursors = get<SyncCursorStore>()
        Room.databaseBuilder(androidContext(), KuodraDatabase::class.java, "kuodra.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    // Scope con SupervisorJob + handler propio: si el borrado de cursores falla, se
                    // registra aquí en vez de escalar al handler global de corrutinas (que en tests
                    // acabaría achacándole la excepción a un `runTest` cualquiera). No se usa el
                    // puerto Telemetry porque encola en esta misma base: sería una dependencia
                    // circular con la instancia que se está construyendo.
                    val handler = CoroutineExceptionHandler { _, e ->
                        Log.e("Kuodra", "no se pudieron limpiar los cursores de sync", e)
                    }
                    CoroutineScope(SupervisorJob() + Dispatchers.IO + handler).launch { cursors.clear() }
                }
            })
            .build()
    }
    single { get<KuodraDatabase>().movementDao() }
    single { get<KuodraDatabase>().categoryDao() }
    single { get<KuodraDatabase>().budgetDao() }
    single { get<KuodraDatabase>().periodSnapshotDao() }
    single { get<KuodraDatabase>().telemetryDao() }
    single { get<KuodraDatabase>().spaceDao() }
    single { get<KuodraDatabase>().personDao() }
    single { get<KuodraDatabase>().settlementDao() }

    // Sincronización (push/pull + agendado con WorkManager)
    single { SyncCursorStore(get()) }
    single { WorkManagerSyncTrigger(androidContext()) } bind SyncTrigger::class
    // Orden POSICIONAL: 7 APIs (movement, category, budget, snapshot, space, person, settlement),
    // luego 7 DAOs en el mismo orden, luego sessionStore y cursors.
    single {
        SyncManager(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(),
        )
    }

    // Escaneo de tickets: OCR (MLKit) + cadena de parsers en orden de prioridad
    // (Mistral remoto → regex local). Futuro: insertar el TemplateTicketParser al frente.
    single { MlKitOcrEngine(androidContext()) } bind OcrEngine::class
    single { MistralTicketParser(get(), get(), get()) }
    single { RegexTicketParser() }
    single {
        ScanTicketUseCase(
            get(),
            listOf(get<MistralTicketParser>(), get<RegexTicketParser>()),
            get(),
        )
    }

    single { AuthRepositoryImpl(get(), get(), get(), get()) } bind AuthRepository::class
    single { SpaceRepositoryImpl(get(), get(), get(), get()) } bind SpaceRepository::class
    single { PersonRepositoryImpl(get(), get(), get()) } bind PersonRepository::class
    single { SettlementRepositoryImpl(get(), get(), get(), get()) } bind SettlementRepository::class
    single { CategoryRepositoryImpl(get(), get(), get()) } bind CategoryRepository::class
    single { MovementRepositoryImpl(get(), get(), get()) } bind MovementRepository::class
    single { SummaryRepositoryImpl(get()) } bind SummaryRepository::class
    single { BudgetRepository(get(), get(), get()) }
    single { SnapshotRepositoryImpl(get(), get(), get()) } bind SnapshotRepository::class
    single { SettingsRepositoryImpl(get(), get()) } bind SettingsRepository::class
    single { PreferencesRepositoryImpl(get()) } bind PreferencesRepository::class
}
