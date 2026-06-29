package com.arenacun.kuodra.di

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
import com.arenacun.kuodra.data.repository.PreferencesRepositoryImpl
import com.arenacun.kuodra.data.repository.SettingsRepositoryImpl
import com.arenacun.kuodra.data.repository.SnapshotRepositoryImpl
import com.arenacun.kuodra.data.repository.SpaceRepositoryImpl
import com.arenacun.kuodra.data.repository.SummaryRepositoryImpl
import com.arenacun.kuodra.data.sync.SyncCursorStore
import com.arenacun.kuodra.data.sync.SyncManager
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.data.sync.WorkManagerSyncTrigger
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                    CoroutineScope(Dispatchers.IO).launch { cursors.clear() }
                }
            })
            .build()
    }
    single { get<KuodraDatabase>().movementDao() }
    single { get<KuodraDatabase>().categoryDao() }
    single { get<KuodraDatabase>().budgetDao() }
    single { get<KuodraDatabase>().periodSnapshotDao() }

    // Sincronización (push/pull + agendado con WorkManager)
    single { SyncCursorStore(get()) }
    single { WorkManagerSyncTrigger(androidContext()) } bind SyncTrigger::class
    single { SyncManager(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    single { AuthRepositoryImpl(get(), get(), get()) } bind AuthRepository::class
    single { SpaceRepositoryImpl(get()) } bind SpaceRepository::class
    single { CategoryRepositoryImpl(get(), get(), get()) } bind CategoryRepository::class
    single { MovementRepositoryImpl(get(), get(), get(), get()) } bind MovementRepository::class
    single { SummaryRepositoryImpl(get(), get()) } bind SummaryRepository::class
    single { BudgetRepository(get(), get(), get()) }
    single { SnapshotRepositoryImpl(get(), get(), get()) } bind SnapshotRepository::class
    single { SettingsRepositoryImpl(get(), get(), get()) } bind SettingsRepository::class
    single { PreferencesRepositoryImpl(get()) } bind PreferencesRepository::class
}
