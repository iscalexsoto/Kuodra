package com.arenacun.kuodra.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.remote.AuthApi
import com.arenacun.kuodra.data.remote.dto.AuthMethodsResponse
import com.arenacun.kuodra.data.remote.dto.AuthResponse
import com.arenacun.kuodra.data.remote.dto.OAuth2Config
import com.arenacun.kuodra.data.remote.dto.OAuth2ProviderInfo
import com.arenacun.kuodra.data.remote.dto.OAuthMeta
import com.arenacun.kuodra.data.remote.dto.RequestOtpResponse
import com.arenacun.kuodra.data.remote.dto.UserRecordDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun sessionStore(): SessionStore {
        val dataStore = PreferenceDataStoreFactory.create { tmp.newFile("test.preferences_pb") }
        return SessionStore(dataStore)
    }

    /** AuthApi en memoria; permite forzar fallos por endpoint. */
    private class FakeAuthApi(
        var otpId: String = "otp_1",
        var record: UserRecordDto = UserRecordDto("u1", "alex@correo.com"),
        var token: String = "tok_1",
        var createUserThrows: Throwable? = null,
        var authWithOtpThrows: Throwable? = null,
        var updateUserThrows: Throwable? = null,
        // OAuth2
        var oauthProviders: List<OAuth2ProviderInfo> = listOf(
            OAuth2ProviderInfo(
                name = "google", state = "st_1", codeVerifier = "cv_1",
                authURL = "https://accounts.google.com/o/oauth2/auth?redirect_uri=",
            ),
        ),
        var oauthMeta: OAuthMeta? = null,
    ) : AuthApi {
        var createUserCalled = false
        var requestOtpEmail: String? = null
        var updateUserName: String? = null
        var updateUserToken: String? = null
        var authWithOAuth2Args: List<String>? = null

        override suspend fun createUser(email: String): UserRecordDto {
            createUserCalled = true
            createUserThrows?.let { throw it }
            return record
        }

        override suspend fun requestOtp(email: String): RequestOtpResponse {
            requestOtpEmail = email
            return RequestOtpResponse(otpId)
        }

        override suspend fun authWithOtp(otpId: String, code: String): AuthResponse {
            authWithOtpThrows?.let { throw it }
            return AuthResponse(token, record)
        }

        override suspend fun authRefresh(token: String): AuthResponse = AuthResponse(this.token, record)

        override suspend fun updateUser(userId: String, name: String, token: String): UserRecordDto {
            updateUserThrows?.let { throw it }
            updateUserName = name
            updateUserToken = token
            record = record.copy(name = name)
            return record
        }

        override suspend fun listAuthMethods(): AuthMethodsResponse =
            AuthMethodsResponse(OAuth2Config(enabled = true, providers = oauthProviders))

        override suspend fun authWithOAuth2(
            provider: String,
            code: String,
            codeVerifier: String,
            redirectURL: String,
        ): AuthResponse {
            authWithOAuth2Args = listOf(provider, code, codeVerifier, redirectURL)
            return AuthResponse(token, record, meta = oauthMeta)
        }
    }

    @Test
    fun `requestOtp gives alta then verifyOtp persists session`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi()
        val repo = AuthRepositoryImpl(api, store)

        val requested = repo.requestOtp("  alex@correo.com ")
        assertTrue(requested.isSuccess)
        assertTrue(api.createUserCalled)
        assertEquals("alex@correo.com", api.requestOtpEmail)

        val verified = repo.verifyOtp("123456")
        assertTrue(verified.isSuccess)

        val session = store.sessionFlow.first()
        assertNotNull(session)
        assertEquals("u1", session!!.userId)
        assertEquals("alex@correo.com", session.email)
    }

    @Test
    fun `requestOtp ignores duplicate-user error from createUser`() = runTest {
        val api = FakeAuthApi(createUserThrows = RuntimeException("email already exists"))
        val repo = AuthRepositoryImpl(api, sessionStore())

        val result = repo.requestOtp("alex@correo.com")

        assertTrue(result.isSuccess)
        assertEquals("alex@correo.com", api.requestOtpEmail)
    }

    @Test
    fun `verifyOtp without a prior request fails`() = runTest {
        val store = sessionStore()
        val repo = AuthRepositoryImpl(FakeAuthApi(), store)

        val result = repo.verifyOtp("123456")

        assertTrue(result.isFailure)
        assertNull(store.sessionFlow.first())
    }

    @Test
    fun `verifyOtp with wrong code fails and keeps no session`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi(authWithOtpThrows = RuntimeException("invalid otp"))
        val repo = AuthRepositoryImpl(api, store)

        repo.requestOtp("alex@correo.com")
        val result = repo.verifyOtp("000000")

        assertTrue(result.isFailure)
        assertNull(store.sessionFlow.first())
    }

    @Test
    fun `verifyOtp propagates the record name into the session`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi(record = UserRecordDto("u1", "alex@correo.com", "Alex"))
        val repo = AuthRepositoryImpl(api, store)

        repo.requestOtp("alex@correo.com")
        repo.verifyOtp("123456")

        assertEquals("Alex", store.sessionFlow.first()!!.name)
    }

    @Test
    fun `updateName PATCHes and updates the persisted session`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi()
        val repo = AuthRepositoryImpl(api, store)
        repo.requestOtp("alex@correo.com")
        repo.verifyOtp("123456")

        val result = repo.updateName("  Diego  ")

        assertTrue(result.isSuccess)
        assertEquals("Diego", api.updateUserName)
        assertEquals("tok_1", api.updateUserToken)
        assertEquals("Diego", store.sessionFlow.first()!!.name)
    }

    @Test
    fun `updateName without a session fails`() = runTest {
        val repo = AuthRepositoryImpl(FakeAuthApi(), sessionStore())

        val result = repo.updateName("Diego")

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateName keeps the previous name when the server fails`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi(record = UserRecordDto("u1", "alex@correo.com", "Alex"))
        val repo = AuthRepositoryImpl(api, store)
        repo.requestOtp("alex@correo.com")
        repo.verifyOtp("123456")
        api.updateUserThrows = RuntimeException("network down")

        val result = repo.updateName("Diego")

        assertTrue(result.isFailure)
        assertEquals("Alex", store.sessionFlow.first()!!.name)
    }

    @Test
    fun `startGoogleSignIn returns the authURL with the appended redirect`() = runTest {
        val api = FakeAuthApi()
        val repo = AuthRepositoryImpl(api, sessionStore(), oauthRedirectUrl = "https://kuodra.app/oauth-redirect")

        val result = repo.startGoogleSignIn()

        assertTrue(result.isSuccess)
        // authURL base + redirect URL codificado.
        assertTrue(result.getOrThrow().startsWith("https://accounts.google.com/o/oauth2/auth?redirect_uri="))
        assertTrue(result.getOrThrow().endsWith("https%3A%2F%2Fkuodra.app%2Foauth-redirect"))
    }

    @Test
    fun `startGoogleSignIn fails when google provider is not enabled`() = runTest {
        val api = FakeAuthApi(oauthProviders = emptyList())
        val repo = AuthRepositoryImpl(api, sessionStore())

        assertTrue(repo.startGoogleSignIn().isFailure)
    }

    @Test
    fun `completeOAuth2 with matching state persists the session`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi(record = UserRecordDto("g1", "gmail@correo.com", "Alex G"))
        val repo = AuthRepositoryImpl(api, store, oauthRedirectUrl = "https://kuodra.app/oauth-redirect")
        repo.startGoogleSignIn()

        val result = repo.completeOAuth2(code = "auth_code", state = "st_1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("google", "auth_code", "cv_1", "https://kuodra.app/oauth-redirect"), api.authWithOAuth2Args)
        val session = store.sessionFlow.first()
        assertEquals("g1", session!!.userId)
        assertEquals("Alex G", session.name)
    }

    @Test
    fun `completeOAuth2 falls back to meta name when the record has none`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi(
            record = UserRecordDto("g1", "gmail@correo.com", name = ""),
            oauthMeta = OAuthMeta(name = "Alex from Google"),
        )
        val repo = AuthRepositoryImpl(api, store)
        repo.startGoogleSignIn()

        repo.completeOAuth2(code = "auth_code", state = "st_1")

        assertEquals("Alex from Google", store.sessionFlow.first()!!.name)
    }

    @Test
    fun `completeOAuth2 with mismatched state fails and keeps no session`() = runTest {
        val store = sessionStore()
        val api = FakeAuthApi()
        val repo = AuthRepositoryImpl(api, store)
        repo.startGoogleSignIn()

        val result = repo.completeOAuth2(code = "auth_code", state = "forged")

        assertTrue(result.isFailure)
        assertNull(api.authWithOAuth2Args) // ni siquiera se intentó el canje
        assertNull(store.sessionFlow.first())
    }

    @Test
    fun `completeOAuth2 without a prior startGoogleSignIn fails`() = runTest {
        val repo = AuthRepositoryImpl(FakeAuthApi(), sessionStore())

        assertTrue(repo.completeOAuth2(code = "c", state = "s").isFailure)
    }

    @Test
    fun `signOut clears the persisted session`() = runTest {
        val store = sessionStore()
        val repo = AuthRepositoryImpl(FakeAuthApi(), store)
        repo.requestOtp("alex@correo.com")
        repo.verifyOtp("123456")
        assertNotNull(store.sessionFlow.first())

        repo.signOut()

        assertNull(store.sessionFlow.first())
    }
}
