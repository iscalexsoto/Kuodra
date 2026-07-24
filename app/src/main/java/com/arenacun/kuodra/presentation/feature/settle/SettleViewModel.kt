package com.arenacun.kuodra.presentation.feature.settle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementKind
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.usecase.CloseSettlement
import com.arenacun.kuodra.domain.usecase.RecordPayment
import com.arenacun.kuodra.domain.usecase.SettleSuggestions
import com.arenacun.kuodra.domain.usecase.SharedBalances
import com.arenacun.kuodra.domain.usecase.WhatsAppMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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

    /** Estado transitorio de la UI (no viene de repos): number pad de pago + confirmación del corte. */
    private data class Local(
        val payPersonId: String? = null,
        val payPad: CalcState = CalcState(),
        val confirmRegister: Boolean = false,
    )
    private val local = MutableStateFlow(Local())

    val uiState = combine(
        movementRepository.movements(spaceId),
        personRepository.persons(spaceId),
        settlementRepository.settlements(spaceId),
        local,
    ) { movements, people, settlements, l ->
        buildState(movements, people, settlements, l)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettleUiState())

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    /** URL `wa.me` lista para abrir con un intent (la construye el VM; la lanza la pantalla). */
    private val _whatsapp = Channel<String>(Channel.BUFFERED)
    val whatsapp = _whatsapp.receiveAsFlow()

    private fun livePayments(settlements: List<Settlement>): List<Settlement> =
        settlements.filter { it.kind == SettlementKind.Payment && it.settledBy.isEmpty() }

    private fun buildState(
        movements: List<Movement>,
        contacts: List<SpacePerson>,
        settlements: List<Settlement>,
        l: Local,
    ): SettleUiState {
        val byId = contacts.associateBy { it.id }
        val balances = SharedBalances.compute(movements, livePayments(settlements))
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
                    netCents = bal,
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
            canRegister = rows.isNotEmpty(),
            showRegisterConfirm = l.confirmRegister && rows.isNotEmpty(),
            payPadPersonId = l.payPersonId,
            payPad = l.payPad,
        )
    }

    // ---- Liquidación total (corte del periodo, con confirmación) ----
    /** Abre la confirmación del corte total (si hay saldos que liquidar). */
    fun onRegister() {
        if (uiState.value.people.isEmpty()) return
        local.update { it.copy(confirmRegister = true) }
    }
    fun onDismissRegister() = local.update { it.copy(confirmRegister = false) }

    /** Cierra el periodo: estampa movimientos y consume los pagos vivos. */
    fun onConfirmRegister() {
        local.update { it.copy(confirmRegister = false) }
        viewModelScope.launch {
            val movements = movementRepository.movements(spaceId).first()
            val contacts = personRepository.persons(spaceId).first().associateBy { it.id }
            val settlements = settlementRepository.settlements(spaceId).first()
            val result = CloseSettlement.build(spaceId, space.displayName, movements, contacts, today, settlements)
            if (result.movementIds.isNotEmpty() || result.paymentIds.isNotEmpty()) {
                settlementRepository.close(result.settlement, result.movementIds, result.paymentIds)
            }
            _done.send(Unit)
        }
    }

    // ---- Liquidar por persona (pago parcial/total con number pad) ----
    fun onOpenPay(personId: String) {
        val net = uiState.value.people.firstOrNull { it.personId == personId }?.netCents ?: 0L
        local.update { it.copy(payPersonId = personId, payPad = Calc.initial(kotlin.math.abs(net) / 100.0)) }
    }
    fun onPayKey(key: CalcKey) = local.update { it.copy(payPad = Calc.press(it.payPad, key)) }
    fun onDismissPay() = local.update { it.copy(payPersonId = null, payPad = CalcState()) }
    fun onConfirmPay() {
        val personId = local.value.payPersonId ?: return
        val amount = local.value.payPad.result?.let { Money.ofMajor(it) } ?: Money.Zero
        local.update { it.copy(payPersonId = null, payPad = CalcState()) }
        if (amount.cents <= 0L) return
        viewModelScope.launch {
            val contacts = personRepository.persons(spaceId).first().associateBy { it.id }
            val name = contacts[personId]?.name ?: "(eliminado)"
            val net = SharedBalances.compute(
                movementRepository.movements(spaceId).first(),
                livePayments(settlementRepository.settlements(spaceId).first()),
            )[personId] ?: 0L
            if (net == 0L) return@launch
            val payment = RecordPayment.build(spaceId, personId, name, amount, net, today)
            settlementRepository.record(payment)
        }
    }

    /** Construye el mensaje de deuda de la persona y emite la URL `wa.me`. */
    fun onWhatsApp(personId: String) {
        viewModelScope.launch {
            val contacts = personRepository.persons(spaceId).first().associateBy { it.id }
            val contact = contacts[personId] ?: return@launch
            if (contact.phone.isBlank()) return@launch
            val net = Money(
                SharedBalances.compute(
                    movementRepository.movements(spaceId).first(),
                    livePayments(settlementRepository.settlements(spaceId).first()),
                )[personId] ?: 0L,
            )
            val message = WhatsAppMessage.build(contact.name, net, space.displayName)
            val phone = contact.phone.filter { it.isDigit() }
            _whatsapp.send("https://wa.me/$phone?text=" + java.net.URLEncoder.encode(message, "UTF-8"))
        }
    }
}
