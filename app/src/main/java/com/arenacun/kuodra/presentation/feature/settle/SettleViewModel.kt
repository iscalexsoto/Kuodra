package com.arenacun.kuodra.presentation.feature.settle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.usecase.CloseSettlement
import com.arenacun.kuodra.domain.usecase.SettleSuggestions
import com.arenacun.kuodra.domain.usecase.SharedBalances
import com.arenacun.kuodra.domain.usecase.WhatsAppMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Liquidación (Gastos) — `scrSettle`. Calcula los balances reales por persona ([SharedBalances]) y
 * las transferencias sugeridas ([SettleSuggestions]) desde los movimientos vivos del espacio. Al
 * registrar cierra el periodo ([CloseSettlement] → [SettlementRepository.close]) estampando los
 * movimientos, y por persona ofrece enviar la deuda por WhatsApp.
 */
class SettleViewModel(
    private val spaceRepository: SpaceRepository,
    private val movementRepository: MovementRepository,
    private val personRepository: PersonRepository,
    private val settlementRepository: SettlementRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value
    private val spaceId = space.id
    private val today = LocalDate.now()

    val uiState = combine(
        movementRepository.movements(spaceId),
        personRepository.persons(spaceId),
    ) { movements, people ->
        buildState(movements, people)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettleUiState())

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    /** URL `wa.me` lista para abrir con un intent (la construye el VM; la lanza la pantalla). */
    private val _whatsapp = Channel<String>(Channel.BUFFERED)
    val whatsapp = _whatsapp.receiveAsFlow()

    private fun buildState(movements: List<Movement>, contacts: List<SpacePerson>): SettleUiState {
        val byId = contacts.associateBy { it.id }
        val balances = SharedBalances.compute(movements)
        val transfers = SettleSuggestions.compute(balances)
        val owed = transfers.filter { it.toId == PersonRef.ME }.sumOf { it.amount.cents }
        val owe = transfers.filter { it.fromId == PersonRef.ME }.sumOf { it.amount.cents }
        val net = owed - owe

        val rows = balances.filterKeys { it != PersonRef.ME }
            .entries.sortedByDescending { -it.value }
            .map { (id, bal) ->
                val name = byId[id]?.name ?: "(eliminado)"
                val owes = bal < 0L
                SettlePersonRow(
                    personId = id,
                    person = Person(
                        name = name,
                        sub = if (owes) "te debe" else "le debes",
                        amount = (if (owes) "+" else "−") + Calc.formatAmount(kotlin.math.abs(bal) / 100.0),
                        positive = owes,
                        initials = initialsOf(name),
                        tone = if (owes) AvatarTone.Pos else AvatarTone.Neg,
                    ),
                    hasPhone = byId[id]?.phone?.isNotBlank() == true,
                )
            }

        return SettleUiState(
            title = space.terminology.settleTitle.ifBlank { "Liquidación" },
            useCase = space.useCase,
            people = rows,
            heroLabel = "Saldo neto a tu favor",
            heroAmount = (if (net >= 0) "+" else "−") + Calc.formatAmount(kotlin.math.abs(net) / 100.0),
            owedAmount = Calc.formatAmount(owed / 100.0),
            oweAmount = Calc.formatAmount(owe / 100.0),
            confirmLabel = "Registrar liquidación",
        )
    }

    fun onRegister() {
        viewModelScope.launch {
            val movements = movementRepository.movements(spaceId).first()
            val contacts = personRepository.persons(spaceId).first().associateBy { it.id }
            val result = CloseSettlement.build(spaceId, space.displayName, movements, contacts, today)
            if (result.movementIds.isNotEmpty()) {
                settlementRepository.close(result.settlement, result.movementIds)
            }
            _done.send(Unit)
        }
    }

    /** Construye el mensaje de deuda de la persona y emite la URL `wa.me`. */
    fun onWhatsApp(personId: String) {
        viewModelScope.launch {
            val contacts = personRepository.persons(spaceId).first().associateBy { it.id }
            val contact = contacts[personId] ?: return@launch
            if (contact.phone.isBlank()) return@launch
            val balances = SharedBalances.compute(movementRepository.movements(spaceId).first())
            val net = Money(balances[personId] ?: 0L)
            val message = WhatsAppMessage.build(contact.name, net, space.displayName)
            val phone = contact.phone.filter { it.isDigit() }
            _whatsapp.send("https://wa.me/$phone?text=" + java.net.URLEncoder.encode(message, "UTF-8"))
        }
    }
}
