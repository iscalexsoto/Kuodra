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

    /** Fake que simula el canje del OTP: al verificar, publica la sesión que trae el backend. */
    private class FakeAuthRepository(private val verifiedSession: Session) : AuthRepository {
        private val _session = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = _session
        override fun isValidEmail(email: String): Boolean = true
        override suspend fun requestOtp(email: String): Result<Unit> = Result.success(Unit)
        override suspend fun verifyOtp(code: String): Result<Unit> {
            _session.value = verifiedSession
            return Result.success(Unit)
        }
        override suspend fun restoreSession(): Session? = _session.value
        override suspend fun updateName(name: String): Result<Unit> = Result.success(Unit)
        override suspend fun signOut() { _session.value = null }
    }

    private class FakeSpaceRepository(private val configured: Boolean) : SpaceRepository {
        override val activeSpace: StateFlow<Space> = MutableStateFlow(Space(UseCase.Personal))
        override fun selectUseCase(useCase: UseCase) = Unit
        override fun createSpace(useCase: UseCase, name: String) = Unit
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
}
