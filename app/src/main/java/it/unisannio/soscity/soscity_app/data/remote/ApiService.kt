package it.unisannio.soscity.soscity_app.data.remote

import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.data.model.VerifySessionResponse
import retrofit2.http.*

interface ApiService {

    // =========================
    // AUTH ENDPOINTS
    // =========================

    /**
     * Registra un nuovo utente nel backend dopo la creazione dell'account Firebase.
     * POST /users
     */
    @POST("users")
    suspend fun register(
        @Body request: RegisterRequest
    ): User

    /**
     * Verifica se l'utente esiste nel backend.
     * GET /users/verify-session/{uid}
     */
    @GET("users/verify-session/{uid}")
    suspend fun verifySession(
        @Path("uid") uid: String
    ): VerifySessionResponse

    // =========================
    // TICKET ENDPOINTS
    // =========================

    /**
     * Crea un nuovo ticket.
     * POST /tickets
     */
    @POST("tickets")
    suspend fun createTicket(
        @Body ticket: Ticket
    ): Ticket

    /**
     * Restituisce i ticket del cittadino autenticato.
     * GET /tickets/my
     */
    @GET("tickets/my")
    suspend fun getMyTickets(): List<Ticket>

    /**
     * Restituisce un ticket specifico per ID.
     * GET /tickets/{id}
     */
    @GET("tickets/{id}")
    suspend fun getTicketById(
        @Path("id") ticketId: String
    ): Ticket

    // =========================
    // NOTIFICATION ENDPOINTS
    // =========================

    /**
     * Restituisce le notifiche dell'utente autenticato.
     * GET /notifications
     */
    @GET("notifications")
    suspend fun getNotifications(): List<Notification>

    // =========================
    // INTERVENTION ENDPOINTS
    // =========================

    /**
     * Restituisce gli interventi assegnati al tecnico autenticato.
     * GET /interventions/my
     */
    @GET("interventions/my")
    suspend fun getMyInterventions(): List<Intervention>

    /**
     * Restituisce un intervento specifico per ID.
     * GET /interventions/{id}
     */
    @GET("interventions/{id}")
    suspend fun getInterventionById(
        @Path("id") interventionId: String
    ): Intervention

    /**
     * Aggiorna lo stato di un intervento.
     * PUT /interventions/{id}/stato
     */
    @PUT("interventions/{id}/stato")
    suspend fun updateInterventionStatus(
        @Path("id") interventionId: String,
        @Body statusRequest: Map<String, String>  // { "stato": "COMPLETATO" }
    ): Unit
}