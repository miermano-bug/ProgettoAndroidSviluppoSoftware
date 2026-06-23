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
     *
     * Il filtro FirebaseAuthFilter lato server richiede Authorization: Bearer <idToken>
     * su QUALSIASI path diverso da q/health e q/metrics (vedi FirebaseAuthFilter.java,
     * punto 3 "Verifica token Firebase Bearer") — non ci sono eccezioni per /users
     * a livello di filtro: il fatto che la registrazione CITTADINO sia "pubblica" è
     * una logica di autorizzazione applicata dentro UserResource (più a valle), che
     * però viene raggiunta solo se il filtro a monte ha già verificato un token Bearer
     * valido. Va quindi sempre passato il token Firebase ottenuto subito dopo
     * createUserWithEmailAndPassword(), anche per la registrazione CITTADINO.
     */
    @POST("users")
    suspend fun register(
        @Body request: RegisterRequest,
        @Header("Authorization") authHeader: String
    ): User

    /**
     * Verifica se l'utente esiste nel backend.
     * GET /users/verify-session/{uid}
     *
     * Il backend richiede l'header Authorization (Bearer <idToken>) anche su questo
     * endpoint: serve al filtro lato server per derivare X-Firebase-UID. Durante il
     * LOGIN, SessionManager non ha ancora una sessione attiva, quindi AuthInterceptor
     * non aggiungerebbe l'header in automatico — per questo lo passiamo qui in modo
     * esplicito. Nota: Retrofit non legge i valori di default Kotlin sui metodi di
     * interfaccia (il proxy dinamico richiede sempre l'argomento), quindi il
     * parametro è nullable ma OBBLIGATORIO da passare ad ogni chiamata (usare null
     * se non serve un override esplicito, es. quando si chiama a sessione già attiva
     * e si vuole lasciare fare tutto ad AuthInterceptor).
     */
    @GET("users/verify-session/{uid}")
    suspend fun verifySession(
        @Path("uid") uid: String,
        @Header("Authorization") authHeader: String?
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
     * PUT /interventions/{id}/stato?stato=<valore>
     *
     * Il backend legge "stato" come QUERY PARAMETER, non come body JSON
     * (vedi contratto §9.6: "PUT /interventions/{id}/stato?stato=<valore>").
     * La versione precedente mandava un body {"stato": "..."}, che il backend
     * avrebbe semplicemente ignorato (nessun parametro stato sulla query string
     * → 400 "Parametro stato mancante").
     */
    @PUT("interventions/{id}/stato")
    suspend fun updateInterventionStatus(
        @Path("id") interventionId: String,
        @Query("stato") stato: String
    ): Unit
}