package com.arenacun.kuodra.data.telemetry

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.TelemetryDao
import com.arenacun.kuodra.data.local.db.TelemetryEventEntity
import com.arenacun.kuodra.data.remote.TelemetryApi
import com.arenacun.kuodra.data.remote.dto.TelemetryEventDto
import com.arenacun.kuodra.data.remote.dto.TelemetryRecord
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TelemetryUploaderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun sessionStore(): SessionStore {
        val dataStore = PreferenceDataStoreFactory.create { tmp.newFile("t.preferences_pb") }
        return SessionStore(dataStore)
    }

    private fun spool() = CrashSpool(tmp.newFolder("spool"), json)

    private fun record(id: String, createdAt: Long, userId: String = "") =
        TelemetryRecord(id = id, createdAt = createdAt, level = "Error", type = "error", message = id, userId = userId)

    @Test
    fun `with session uploads pending, marks sent and prunes`() = runTest {
        val dao = FakeTelemetryDao()
        dao.enqueue(record("e1", 1).toEntity(json))
        dao.enqueue(record("e2", 2).toEntity(json))
        val api = FakeTelemetryApi()
        val session = sessionStore().apply { save("tok", "u1", "u1@x.com", "U") }
        val uploader = TelemetryUploader(dao, api, session, spool(), json)

        val result = uploader.upload()

        assertTrue(result.isSuccess)
        assertEquals(listOf("e1", "e2"), api.sent.map { it.message })
        assertTrue("la cola queda vacía tras podar", dao.all().isEmpty())
    }

    @Test
    fun `without session keeps the queue and does not upload`() = runTest {
        val dao = FakeTelemetryDao()
        dao.enqueue(record("e1", 1).toEntity(json))
        val api = FakeTelemetryApi()
        val uploader = TelemetryUploader(dao, api, sessionStore(), spool(), json)

        val result = uploader.upload()

        assertTrue(result.isSuccess)
        assertTrue(api.sent.isEmpty())
        assertEquals(1, dao.all().size)
    }

    @Test
    fun `drains crash spool into the queue before uploading`() = runTest {
        val dao = FakeTelemetryDao()
        val spool = spool()
        spool.writeBlocking(record("crash1", 5))
        val api = FakeTelemetryApi()
        val session = sessionStore().apply { save("tok", "u1", "u1@x.com", "U") }
        val uploader = TelemetryUploader(dao, api, session, spool, json)

        uploader.upload()

        assertEquals(listOf("crash1"), api.sent.map { it.message })
    }

    @Test
    fun `anonymous events are attributed to the current user on upload`() = runTest {
        val dao = FakeTelemetryDao()
        dao.enqueue(record("e1", 1, userId = "").toEntity(json))
        val api = FakeTelemetryApi()
        val session = sessionStore().apply { save("tok", "u9", "u9@x.com", "U") }
        val uploader = TelemetryUploader(dao, api, session, spool(), json)

        uploader.upload()

        assertEquals("u9", api.sent.single().owner)
    }

    // --- Fakes ---

    private class FakeTelemetryApi(var throws: Throwable? = null) : TelemetryApi {
        val sent = mutableListOf<TelemetryEventDto>()
        override suspend fun send(dto: TelemetryEventDto, token: String) {
            throws?.let { throw it }
            sent += dto
        }
    }

    private class FakeTelemetryDao : TelemetryDao {
        private val rows = mutableListOf<TelemetryEventEntity>()
        fun all(): List<TelemetryEventEntity> = rows.toList()
        override suspend fun enqueue(event: TelemetryEventEntity) {
            if (rows.none { it.id == event.id }) rows += event
        }
        override suspend fun pending(limit: Int): List<TelemetryEventEntity> =
            rows.filter { !it.sent }.sortedBy { it.createdAt }.take(limit)
        override suspend fun markSent(ids: List<String>) {
            rows.replaceAll { if (it.id in ids) it.copy(sent = true) else it }
        }
        override suspend fun pruneSent() {
            rows.removeAll { it.sent }
        }
        override suspend fun trimTo(keep: Int) {
            val survivors = rows.filter { !it.sent }.sortedByDescending { it.createdAt }.take(keep).map { it.id }.toSet()
            rows.removeAll { !it.sent && it.id !in survivors }
        }
    }
}
