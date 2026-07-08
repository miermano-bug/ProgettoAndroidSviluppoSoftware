package it.unisannio.soscity.soscity_app.ui.tecnico

import it.unisannio.soscity.soscity_app.data.model.Coordinate
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.StatoLavoro
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.EspressoIdlingResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test unitari per InterventionsViewModel.
 * Usa UnconfinedTestDispatcher per eseguire le coroutine in modo sincrono.
 *
 * Il Repository e' sostituito da un oggetto anonimo che implementa l'interfaccia
 * senza dipendenze Firebase/Retrofit: i test sono completamente isolati dalla rete.
 */
@ExperimentalCoroutinesApi
class InterventionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        EspressoIdlingResource.isTestEnvironment = true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private val interventiMock = listOf(
        Intervention(id = "I1", ticketId = "T1", teamId = "TA",
            statoLavoro = "IN_CORSO",    dataInizio = "", dataCreazione = ""),
        Intervention(id = "I2", ticketId = "T2", teamId = "TA",
            statoLavoro = "PIANIFICATO", dataInizio = "", dataCreazione = ""),
        Intervention(id = "I3", ticketId = "T3", teamId = "TB",
            statoLavoro = "COMPLETATO",  dataInizio = "", dataCreazione = "")
    )

    /** Crea un Repository stub che restituisce sempre la lista fornita. */
    private fun stubSuccesso(lista: List<Intervention>): Repository = repositoryStub(
        getMyInterventions = { Result.success(lista) }
    )

    /** Crea un Repository stub che fallisce sempre con il messaggio fornito. */
    private fun stubErrore(msg: String): Repository = repositoryStub(
        getMyInterventions = { Result.failure(Exception(msg)) }
    )

    // ─── Test 1: caricamento con successo ─────────────────────────────────────

    @Test
    fun `caricaInterventi - lista non vuota - emette Success con lista ordinata`() = runTest {
        val repo = stubSuccesso(interventiMock)
        val vm   = InterventionsViewModel(repo)

        vm.caricaInterventi()

        val stato = vm.uiState.first { it is UiState.Success }
        assertTrue(stato is UiState.Success)
        val lista = (stato as UiState.Success).data
        // IN_CORSO deve venire prima di PIANIFICATO, che precede COMPLETATO
        assertEquals("I1", lista[0].id)
        assertEquals("I2", lista[1].id)
        assertEquals("I3", lista[2].id)
    }

    // ─── Test 2: caricamento con lista vuota ──────────────────────────────────

    @Test
    fun `caricaInterventi - lista vuota - emette Success con lista vuota`() = runTest {
        val repo = stubSuccesso(emptyList())
        val vm   = InterventionsViewModel(repo)

        vm.caricaInterventi()

        val stato = vm.uiState.first { it is UiState.Success }
        assertTrue(stato is UiState.Success)
        assertTrue((stato as UiState.Success).data.isEmpty())
    }

    // ─── Test 3: aggiornamento stato fallisce ─────────────────────────────────

    @Test
    fun `aggiornaStato - repository fallisce - emette AzioneUiState Errore`() = runTest {
        val errMsg = "Connessione assente"
        val repo = repositoryStub(
            getMyInterventions = { Result.success(interventiMock) },
            updateInterventionStatus = { _, _, _ -> Result.failure(Exception(errMsg)) }
        )
        val vm = InterventionsViewModel(repo)
        vm.caricaInterventi()

        val target = interventiMock[0]
        vm.aggiornaStato(target, StatoLavoro.COMPLETATO, null)

        val azione = vm.azioneState.first { it is AzioneUiState.Errore }
        assertTrue(azione is AzioneUiState.Errore)
        assertEquals(target.id, (azione as AzioneUiState.Errore).interventionId)
        assertTrue(azione.messaggio.contains(errMsg))
    }

    // ─── Stub factory ─────────────────────────────────────────────────────────

    /**
     * Factory per creare implementazioni stub di Repository con un minimo di boilerplate.
     * Solo i metodi forniti vengono sovrascritti; gli altri restituiscono Result.failure
     * con UnsupportedOperationException per rilevare chiamate non previste nel test.
     */
    private fun repositoryStub(
        getMyInterventions: (suspend () -> Result<List<Intervention>>)? = null,
        updateInterventionStatus: (suspend (String, String, String?) -> Result<Unit>)? = null
    ): Repository = object : Repository {

        override suspend fun getMyInterventions() =
            getMyInterventions?.invoke()
                ?: Result.failure(UnsupportedOperationException("non previsto"))

        override suspend fun updateInterventionStatus(id: String, status: String, note: String?) =
            updateInterventionStatus?.invoke(id, status, note)
                ?: Result.failure(UnsupportedOperationException("non previsto"))

        override suspend fun login(firebaseToken: String, uid: String) =
            Result.failure<User>(UnsupportedOperationException("non previsto"))

        override suspend fun register(request: RegisterRequest, firebaseToken: String) =
            Result.failure<User>(UnsupportedOperationException("non previsto"))

        override suspend fun verifySession(uid: String) =
            Result.failure<Boolean>(UnsupportedOperationException("non previsto"))

        override suspend fun createTicket(ticket: Ticket) =
            Result.failure<Ticket>(UnsupportedOperationException("non previsto"))

        override suspend fun getMyTickets() =
            Result.failure<List<Ticket>>(UnsupportedOperationException("non previsto"))

        override suspend fun getTicketById(ticketId: String) =
            Result.failure<Ticket>(UnsupportedOperationException("non previsto"))

        override suspend fun getNotifications() =
            Result.failure<List<Notification>>(UnsupportedOperationException("non previsto"))

        override suspend fun getInterventionById(interventionId: String) =
            Result.failure<Intervention>(UnsupportedOperationException("non previsto"))
    }
}
