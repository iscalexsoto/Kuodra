package com.arenacun.kuodra.presentation.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Captura el nombre con el que el usuario quiere ser identificado y lo persiste. */
class NameViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NameUiState())
    val uiState = _uiState.asStateFlow()

    /** Evento de una sola vez: el nombre se guardó con éxito ⇒ continuar al onboarding. */
    private val _nameSaved = Channel<Unit>(Channel.BUFFERED)
    val nameSaved = _nameSaved.receiveAsFlow()

    init {
        // Sugerencia inicial a partir del correo (parte local, primera letra en mayúscula).
        val suggestion = authRepository.session.value?.email
            ?.substringBefore('@')
            ?.replaceFirstChar { it.uppercase() }
            .orEmpty()
        _uiState.update { it.copy(name = suggestion, valid = suggestion.isNotBlank()) }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, valid = value.isNotBlank(), error = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.saving || !state.valid) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            val result = authRepository.updateName(state.name)
            _uiState.update { it.copy(saving = false) }
            if (result.isSuccess) {
                _nameSaved.send(Unit)
            } else {
                _uiState.update {
                    it.copy(error = "No pudimos guardar tu nombre. Revisa tu conexión e inténtalo de nuevo.")
                }
            }
        }
    }
}

data class NameUiState(
    val name: String = "",
    val valid: Boolean = false,
    /** Guardando el nombre contra el servidor (deshabilita el botón). */
    val saving: Boolean = false,
    /** Mensaje de error al guardar (red/servidor). */
    val error: String? = null,
)
