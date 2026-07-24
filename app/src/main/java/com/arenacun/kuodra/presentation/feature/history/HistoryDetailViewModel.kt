package com.arenacun.kuodra.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.data.mapper.toSettlementRecord
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.usecase.WhatsAppMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sheet del flujo de reenvío de un corte/pago. */
enum class ReshareSheet { None, Options }

/** Fila de reenvío: una persona con deuda congelada y su acción de WhatsApp. */
data class ReshareRow(
    val personId: String,
    val name: String,
    val hasPhone: Boolean,
    val amount: String,
    val positive: Boolean,
)

data class HistoryDetailUiState(
    val record: SettlementRecord? = null,
    val sheet: ReshareSheet = ReshareSheet.None,
    /** Solo Gastos: deudores congelados del corte/pago (para reenviar por WhatsApp). */
    val reshareRows: List<ReshareRow> = emptyList(),
)

class HistoryDetailViewModel(
    id: String,
    spaceRepository: SpaceRepository,
    settingsRepository: SettingsRepository,
    private val settlementRepository: SettlementRepository,
    private val personRepository: PersonRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState = _uiState.asStateFlow()

    /** Corte/pago de dominio (Gastos) para reconstruir los saldos congelados al reenviar. */
    private var settlement: Settlement? = null

    private val _whatsapp = Channel<String>(Channel.BUFFERED)
    val whatsapp = _whatsapp.receiveAsFlow()

    private val _share = Channel<String>(Channel.BUFFERED)
    val share = _share.receiveAsFlow()

    init {
        if (space.useCase == UseCase.Personal) {
            _uiState.update { it.copy(record = settingsRepository.historyEntry(id)) }
        } else viewModelScope.launch {
            val s = settlementRepository.settlements(space.id).first().firstOrNull { it.id == id }
            settlement = s
            val contacts = personRepository.persons(space.id).first().associateBy { it.id }
            val rows = s?.lines.orEmpty()
                .filter { it.personId != PersonRef.ME && it.net.cents != 0L }
                .map { line ->
                    val owes = line.net.cents < 0L
                    ReshareRow(
                        personId = line.personId,
                        name = line.name,
                        hasPhone = contacts[line.personId]?.phone?.isNotBlank() == true,
                        amount = (if (owes) "+" else "−") + Calc.formatAmount(kotlin.math.abs(line.net.cents) / 100.0),
                        positive = owes,
                    )
                }
            _uiState.update { it.copy(record = s?.toSettlementRecord(), reshareRows = rows) }
        }
    }

    fun onReshare() = _uiState.update { it.copy(sheet = ReshareSheet.Options) }
    fun onCloseSheet() = _uiState.update { it.copy(sheet = ReshareSheet.None) }

    /** Reenvía por WhatsApp la deuda congelada de una persona. */
    fun onWhatsApp(personId: String) {
        val s = settlement ?: return
        viewModelScope.launch {
            val contact = personRepository.persons(space.id).first().firstOrNull { it.id == personId } ?: return@launch
            if (contact.phone.isBlank()) return@launch
            val net = Money(s.lines.firstOrNull { it.personId == personId }?.net?.cents ?: 0L)
            val message = WhatsAppMessage.build(contact.name, net, space.displayName)
            val phone = contact.phone.filter { it.isDigit() }
            _whatsapp.send("https://wa.me/$phone?text=" + java.net.URLEncoder.encode(message, "UTF-8"))
        }
    }

    /** Comparte el resumen del corte/pago como texto por el share nativo. */
    fun onShareText() {
        val record = _uiState.value.record ?: return
        val sb = StringBuilder(record.title)
        sb.append("\n${record.periodLabel} · ${record.total}")
        record.lines.forEach { sb.append("\n• ${it.name}: ${it.detail} ${it.amount}") }
        _uiState.update { it.copy(sheet = ReshareSheet.None) }
        viewModelScope.launch { _share.send(sb.toString()) }
    }
}
