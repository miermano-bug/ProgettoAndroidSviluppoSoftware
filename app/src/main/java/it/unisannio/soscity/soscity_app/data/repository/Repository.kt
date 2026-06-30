package it.unisannio.soscity.soscity_app.data.repository

import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User

interface Repository {

    // =========================
    // AUTH (Firebase)
    // =========================

    /**
     * Login con Firebase ID Token.
     * @param firebaseToken Firebase ID Token (ottenuto da FirebaseAuth)
     * @param uid Firebase UID dell'utente
     */
    suspend fun login(
        firebaseToken: String,
        uid: String
    ): Result<User>

    suspend fun register(
        request: RegisterRequest,
        firebaseToken: String
    ): Result<User>

    /**
     * Verifica se l'utente esiste nel backend.
     */
    suspend fun verifySession(uid: String): Result<Boolean>

    // =========================
    // TICKETS
    // =========================

    suspend fun createTicket(
        ticket: Ticket
    ): Result<Ticket>

    /**
     * Restituisce i ticket del cittadino autenticato.
     * Equivalente a GET /tickets/my
     */
    suspend fun getMyTickets(): Result<List<Ticket>>

    /**
     * Restituisce un ticket specifico per ID.
     * GET /tickets/{id}
     */
    suspend fun getTicketById(
        ticketId: String
    ): Result<Ticket>

    // =========================
    // NOTIFICATIONS
    // =========================

    suspend fun getNotifications(): Result<List<Notification>>

    // =========================
    // INTERVENTIONS
    // =========================

    /**
     * Restituisce gli interventi assegnati al tecnico autenticato.
     * GET /interventions/my
     */
    suspend fun getMyInterventions(): Result<List<Intervention>>

    /**
     * Restituisce un intervento specifico per ID.
     * GET /interventions/{id}
     */
    suspend fun getInterventionById(
        interventionId: String
    ): Result<Intervention>

    suspend fun updateInterventionStatus(
        interventionId: String,
        status: String,
        note: String? = null
    ): Result<Unit>
}