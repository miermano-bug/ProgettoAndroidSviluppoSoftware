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
            id = "1",
            username = "mario",
            email = "mario@soscity.it",
            nome = "Mario Rossi",
            ruolo = "CITTADINO",
            telefono = "3331234567"
        ),

        User(
            id = "2",
            username = "tecnico1",
            email = "tecnico@soscity.it",
            nome = "Luigi Verdi",
            ruolo = "TECNICO",
            idTeam = 1,
            competenze = listOf("ILLUMINAZIONE"),
            disponibile = true
        )
    )

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
            idCittadino = "1"
        )
    )

    // =========================
    // MOCK NOTIFICATIONS
    // =========================

    private val notifications = mutableListOf(

        Notification(
            id = "N1",
            tipo = "INFO",
            destinatario = "1",
            messaggio = "La tua segnalazione è stata presa in carico",
            ticketId = "T1",
            timestamp = "2026-05-26T11:00:00"
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
            tecnicoId = "2",
            statoLavoro = "ASSEGNATO",
            noteIntervento = "Verifica impianto",
            dataInizio = "2026-05-26T12:00:00",
            dataFine = null,
            dataCreazione = "2026-05-26T12:00:00"
        )
    )

    // =========================
    // AUTH
    // =========================

    override suspend fun login(
        username: String,
        password: String
    ): Result<User> {

        delay(1000)

        val user = users.find {
            it.username == username
        }

        return if (user != null && password == "1234") {

            Result.success(user)

        } else {

            Result.failure(
                Exception("Credenziali non valide")
            )
        }
    }

    override suspend fun register(
        request: RegisterRequest
    ): Result<User> {

        val user = User(

            id = System.currentTimeMillis().toString(),

            username = request.username,

            email = request.email,

            nome = request.nome,

            ruolo = "CITTADINO",

            telefono = request.telefono
        )

        return Result.success(user)
    }

    // =========================
    // TICKETS
    // =========================

    override suspend fun createTicket(
        ticket: Ticket
    ): Result<Ticket> {

        delay(1000)

        tickets.add(ticket)

        return Result.success(ticket)
    }

    override suspend fun getCitizenTickets(
        citizenId: String
    ): Result<List<Ticket>> {

        delay(1000)

        return Result.success(
            tickets.filter {
                it.idCittadino == citizenId
            }
        )
    }

    // =========================
    // NOTIFICATIONS
    // =========================

    override suspend fun getNotifications(
        userId: String
    ): Result<List<Notification>> {

        delay(1000)

        return Result.success(
            notifications.filter {
                it.destinatario == userId
            }
        )
    }

    // =========================
    // INTERVENTIONS
    // =========================

    override suspend fun getTechnicianInterventions(
        technicianId: String
    ): Result<List<Intervention>> {

        delay(1000)

        return Result.success(
            interventions.filter {
                it.tecnicoId == technicianId
            }
        )
    }

    override suspend fun updateInterventionStatus(
        interventionId: String,
        status: String
    ): Result<Unit> {

        delay(1000)

        val intervention = interventions.find {
            it.id == interventionId
        }

        return if (intervention != null) {

            val updated = intervention.copy(
                statoLavoro = status
            )

            interventions.removeIf {
                it.id == interventionId
            }

            interventions.add(updated)

            Result.success(Unit)

        } else {

            Result.failure(
                Exception("Intervento non trovato")
            )
        }
    }
}