package com.arenacun.kuodra.presentation.feature.auth

import com.arenacun.kuodra.MainDispatcherRule
import com.arenacun.kuodra.domain.model.Session
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.presentation.app.StartState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Fake que simula el canje del OTP/OAuth2: al verificar, publica la sesión del backend. */
    private class FakeAuthRepository(
        private val verifiedSession: Session,
        private val startGoogleResult: Result<String> = Result.success("https://auth.url/redirect_uri="),
        private val completeResult: Result<Unit> = Result.success(Unit),
    ) : AuthRepository {
        private val _session = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = _session
        override fun isValidEmail(email: String): Boolean = true
        override suspend fun requestOtp(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyOtp(code: String): Result<Unit> {
            _session.value = verifiedSession
            return Result.success(Unit)
        }
        override suspend fun startGoogleSignIn(): Result<String> = startGoogleResult
        override suspend fun completeOAuth2(code: String, state: String): Result<Unit> {
            if (completeResult.isSuccess) _session.value = verifiedSession
            return completeResult
        }
        override suspend fun restoreSession(): Session? = _session.value
        override suspend fun updateName(name: String): Result<Unit> = Result.success(Unit)
        override suspend fun signOut() { _session.value = null }
    }

    private class FakeSpaceRepository(private val configured: Boolean) : SpaceRepository {
        override val activeSpace: StateFlow<Space> = MutableStateFlow(Space.PERSONAL)
        override val spaces: kotlinx.coroutines.flow.Flow<List<Space>> = MutableStateFlow(emptyList())
        override fun selectPersonal() = Unit
        override fun selectSpace(id: String) = Unit
        override suspend fun createSpace(name: String): Space = Space.PERSONAL
        override suspend fun rename(id: String, name: String) = Unit
        override suspend fun setReminder(id: String, enabled: Boolean) = Unit
        override suspend fun archive(id: String) = Unit
        override suspend fun unarchive(id: String) = Unit
        override suspend fun setSplitRule(id: String, rule: com.arenacun.kuodra.domain.model.SplitRule) = Unit
        override suspend fun isConfigured(): Boolean = configured
    }

    private fun enterOtp(viewModel: AuthViewModel) {
        listOf("1", "2", "3", "4", "5", "6").forEach(viewModel::onOtpDigit)
    }

    @Test
    fun `verifying OTP with no name routes to NeedsName`() = runTest {
        val viewModel = AuthViewModel(
            authRepository = FakeAuthRepository(Session("u1", "a@b.com", name = "")),
            spaceRepository = FakeSpaceRepository(configured = false),
        )
        var emitted: StartState? = null
        val job = launch { viewModel.otpVerified.collect { emitted = it } }

        enterOtp(viewModel)
        advanceUntilIdle()

        assertEquals(StartState.NeedsName, emitted)
        job.cancel()
    }

    @Test
    fun `verifying OTP with recovered name but unconfigured space routes to Onboarding`() = runTest {
        val viewModel = AuthViewModel(
            authRepository = FakeAuthRepository(Session("u1", "a@b.com", name = "Alex")),
            spaceRepository = FakeSpaceRepository(configured = false),
        )
        var emitted: StartState? = null
        val job = launch { viewModel.otpVerified.collect { emitted = it } }

        enterOtp(viewModel)
        advanceUntilIdle()

        assertEquals(StartState.Onboarding, emitted)
        job.cancel()
    }

    @Test
    fun `verifying OTP with name and configured space routes to Ready`() = runTest {
        val viewModel = AuthViewModel(
            authRepository = FakeAuthRepository(Session("u1", "a@b.com", name = "Alex")),
            spaceRepository = FakeSpaceRepository(configured = true),
        )
        var emitted: StartState? = null
        val job = launch { viewModel.otpVerified.collect { emitted = it } }

        enterOtp(viewModel)
        advanceUntilIdle()

        assertEquals(StartState.Ready, emitted)
        job.cancel()
    }

    @Test
    fun `startGoogleSignIn emits the auth URL to open`() = runTest {
        val viewModel = AuthViewModel(
            authRepository = FakeAuthRepository(Session("u1", "a@b.com", "Alex")),
            spaceRepository = FakeSpaceRepository(configured = true),
        )
        var url: String? = null
        val job = launch { viewModel.openOAuthUrl.collect { url = it } }

        viewModel.startGoogleSignIn()
        advanceUntilIdle()

        assertEquals("https://auth.url/redirect_uri=", url)
        job.cancel()
    }

    @Test
    fun `completeOAuth2 success routes with the resolved StartState`() = runTest {
        val viewModel = AuthViewModel(
            authRepository = FakeAuthRepository(Session("u1", "a@b.com", "Alex")),
            spaceRepository = FakeSpaceRepository(configured = true),
        )
        var emitted: StartState? = null
        val job = launch { viewModel.oauthVerified.collect { emitted = it } }

        viewModel.completeOAuth2(code = "c", state = "s")
        advanceUntilIdle()

        assertEquals(StartState.Ready, emitted)
        job.cancel()
    }

    @Test
    fun `completeOAuth2 failure surfaces an error and does not route`() = runTest {
        val viewModel = AuthViewModel(
            authRepository = FakeAuthRepository(
                Session("u1", "a@b.com", "Alex"),
                completeResult = Result.failure(RuntimeException("bad code")),
            ),
            spaceRepository = FakeSpaceRepository(configured = true),
        )
        var emitted: StartState? = null
        val job = launch { viewModel.oauthVerified.collect { emitted = it } }

        viewModel.completeOAuth2(code = "c", state = "s")
        advanceUntilIdle()

        assertEquals(null, emitted)
        assertEquals(false, viewModel.uiState.value.googleLoading)
        assertEquals(true, viewModel.uiState.value.authError != null)
        job.cancel()
    }
}
