package it.unisannio.soscity.soscity_app.data.repository

import it.unisannio.soscity.soscity_app.data.model.Coordinate
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User
import kotlinx.coroutines.delay

class FakeRepository : Repository {

    // =========================
    // MOCK USERS
    // =========================

    private val users = mutableListOf(
        User(
            id = "firebase_uid_1",
            username = "mario",
            email = "mario@soscity.it",
            nome = "Mario Rossi",
            ruolo = "CITTADINO",
            telefono = "3331234567"
        ),
        User(
            id = "firebase_uid_2",
            username = "tecnico1",
            email = "tecnico@soscity.it",
            nome = "Luigi Verdi",
            ruolo = "TECNICO",
            idTeam = "64f1a2b3c4d5e6f7a8b9c0d1",
            competenze = listOf("ILLUMINAZIONE"),
            disponibile = true
        ),
        User(
            id = "firebase_uid_3",
            username = "operatore1",
            email = "operatore@soscity.it",
            nome = "Anna Neri",
            ruolo = "OPERATORE",
            telefono = "3339876543"
        )
    )

    private val userSessions = mutableMapOf<String, String>() // uid -> firebaseToken

    // =========================
    // MOCK TICKETS
    // =========================

    private val tickets = mutableListOf(
        Ticket(
            id = "T1",
            titolo = "Lampione rotto",
            descrizione = "Lampione spento in via Roma",
            categoria = "ILLUMINAZIONE",
            priorita = "MEDIA",
            stato = "APERTO",
            coordinate = Coordinate(
                latitudine = 41.123,
                longitudine = 14.456
            ),
            fotoAllegata = null,
            dataCreazione = "2026-05-26T10:00:00",
            idCittadino = "firebase_uid_1"
        ),
        Ticket(
            id = "T2",
            titolo = "Buca in strada",
            descrizione = "Buca pericolosa in via Garibaldi",
            categoria = "STRADE",
            priorita = "ALTA",
            stato = "IN_VALUTAZIONE",
            coordinate = Coordinate(
                latitudine = 41.124,
                longitudine = 14.457
            ),
            fotoAllegata = null,
            dataCreazione = "2026-05-27T14:30:00",
            idCittadino = "firebase_uid_1"
        )
    )

    // =========================
    // MOCK NOTIFICATIONS
    // =========================

    private val notifications = mutableListOf(
        Notification(
            id = "N1",
            tipo = "INFO",
            destinatario = "firebase_uid_1",
            messaggio = "La tua segnalazione è stata presa in carico",
            ticketId = "T1",
            timestamp = "2026-05-26T11:00:00"
        ),
        Notification(
            id = "N2",
            tipo = "PUSH",
            destinatario = "firebase_uid_2",
            messaggio = "Nuovo intervento assegnato",
            ticketId = "T1",
            timestamp = "2026-05-26T12:00:00"
        )
    )

    // =========================
    // MOCK INTERVENTIONS
    // =========================

    private val interventions = mutableListOf(
        Intervention(
            id = "I1",
            ticketId = "T1",
            teamId = "TEAM_A",
            tecnicoId = "firebase_uid_2",
            statoLavoro = "IN_CORSO",
            noteIntervento = "Verifica impianto",
            dataInizio = "2026-05-26T12:00:00",
            dataFine = null,
            dataCreazione = "2026-05-26T12:00:00"
        ),
        Intervention(
            id = "I1B",
            ticketId = "T1B",
            teamId = "TEAM_A",
            tecnicoId = "firebase_uid_2",
            statoLavoro = "PIANIFICATO",
            noteIntervento = "",
            dataInizio = "2026-05-26T12:05:00",
            dataFine = null,
            dataCreazione = "2026-05-26T12:05:00"
        ),
        Intervention(
            id = "I2",
            ticketId = "T2",
            teamId = "TEAM_B",
            tecnicoId = "firebase_uid_3",
            statoLavoro = "IN_CORSO",
            noteIntervento = "Riparazione buca",
            dataInizio = "2026-05-27T15:00:00",
            dataFine = null,
            dataCreazione = "2026-05-27T15:00:00"
        )
    )

    // =========================
    // AUTH
    // =========================

    override suspend fun login(
        firebaseToken: String,
        uid: String
    ): Result<User> {
        delay(800)

        val user = users.find { it.id == uid }
        return if (user != null) {
            userSessions[uid] = firebaseToken
            Result.success(user)
        } else {
            Result.failure(Exception("Utente non trovato nel backend"))
        }
    }

    override suspend fun register(
        request: RegisterRequest,
        firebaseToken: String
    ): Result<User> {
        delay(800)

        val user = User(
            id = request.uid ?: "firebase_uid_${System.currentTimeMillis()}",
            username = request.username,
            email = request.email,
            nome = request.nome,
            ruolo = request.ruolo ?: "CITTADINO",
            telefono = request.telefono
        )

        users.add(user)
        userSessions[user.id] = firebaseToken
        return Result.success(user)
    }

    override suspend fun verifySession(uid: String): Result<Boolean> {
        delay(500)
        val user = users.find { it.id == uid }
        return if (user != null) {
            Result.success(true)
        } else {
            Result.success(false)
        }
    }

    // =========================
    // TICKETS
    // =========================

    override suspend fun createTicket(ticket: Ticket): Result<Ticket> {
        delay(800)
        val newTicket = ticket.copy(
            id = "T${System.currentTimeMillis()}",
            dataCreazione = java.time.Instant.now().toString(),
            stato = "APERTO"
        )
        tickets.add(newTicket)
        return Result.success(newTicket)
    }

    override suspend fun getMyTickets(): Result<List<Ticket>> {
        delay(800)
        // In un'implementazione reale, prenderebbe l'utente da SessionManager
        // Per il mock, restituiamo tutti i ticket
        return Result.success(tickets.toList())
    }

    override suspend fun getTicketById(ticketId: String): Result<Ticket> {
        delay(500)
        val ticket = tickets.find { it.id == ticketId }
        return if (ticket != null) {
            Result.success(ticket)
        } else {
            Result.failure(Exception("Ticket non trovato"))
        }
    }

    // =========================
    // NOTIFICATIONS
    // =========================

    override suspend fun getNotifications(): Result<List<Notification>> {
        delay(800)
        // In un'implementazione reale, filtra per l'utente corrente
        return Result.success(notifications.toList())
    }

    // =========================
    // INTERVENTIONS
    // =========================

    override suspend fun getMyInterventions(): Result<List<Intervention>> {
        delay(800)
        // In un'implementazione reale, filtra per il tecnico corrente
        return Result.success(interventions.toList())
    }

    override suspend fun getInterventionById(interventionId: String): Result<Intervention> {
        delay(500)
        val intervention = interventions.find { it.id == interventionId }
        return if (intervention != null) {
            Result.success(intervention)
        } else {
            Result.failure(Exception("Intervento non trovato"))
        }
    }

    override suspend fun updateInterventionStatus(
        interventionId: String,
        status: String,
        note: String?
    ): Result<Unit> {
        delay(800)

        val intervention = interventions.find { it.id == interventionId }
        return if (intervention != null) {
            val updated = intervention.copy(
                statoLavoro = status,
                noteIntervento = note ?: intervention.noteIntervento,
                dataFine = if (status == "COMPLETATO") java.time.Instant.now().toString() else intervention.dataFine
            )
            interventions.removeIf { it.id == interventionId }
            interventions.add(updated)

            // Se l'intervento è COMPLETATO, aggiorna anche il ticket a RISOLTO
            if (status == "COMPLETATO") {
                val ticket = tickets.find { it.id == intervention.ticketId }
                ticket?.let {
                    val updatedTicket = it.copy(stato = "RISOLTO")
                    tickets.removeIf { t -> t.id == it.id }
                    tickets.add(updatedTicket)
                }

                // Simula la promozione automatica (Modifica 4): se esiste un altro
                // intervento PIANIFICATO sullo stesso team, lo promuove a IN_CORSO.
                val prossimo = interventions
                    .filter { it.teamId == intervention.teamId && it.statoLavoro == "PIANIFICATO" }
                    .minByOrNull { it.dataCreazione }

                if (prossimo != null) {
                    val promosso = prossimo.copy(statoLavoro = "IN_CORSO")
                    interventions.removeIf { it.id == prossimo.id }
                    interventions.add(promosso)
                }
                // Se non c'è nessun prossimo in coda, nel backend reale qui verrebbe
                // liberato il team (disponibile = true) — il mock non modella i team,
                // quindi questo passaggio non ha un equivalente diretto qui.
            }

            Result.success(Unit)
        } else {
            Result.failure(Exception("Intervento non trovato"))
        }
    }
}