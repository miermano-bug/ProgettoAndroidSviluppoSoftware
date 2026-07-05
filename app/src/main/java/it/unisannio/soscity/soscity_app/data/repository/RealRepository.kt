package it.unisannio.soscity.soscity_app.data.repository

import it.unisannio.soscity.soscity_app.data.remote.AppError
import it.unisannio.soscity.soscity_app.data.remote.safeApiCall
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.util.NetworkClient
import it.unisannio.soscity.soscity_app.data.model.NuovoTicketRequest
import retrofit2.HttpException

/**
 * Implementazione concreta di Repository che usa Retrofit per chiamare il backend.
 *
 * Tutta la gestione errori è centralizzata in safeApiCall(): qui ogni metodo si
 * limita a invocare l'endpoint e, dove serve un 404 con messaggio contestuale
 * (es. "Intervento non trovato: <id>"), passa un notFoundError dedicato.
 *
 * login() e verifySession() sono le uniche due eccezioni: un 404 HTTP, per questi
 * due endpoint, non è un errore applicativo (significa "utente/sessione non
 * trovata", un risultato legittimo), quindi vanno gestiti con un try/catch
 * dedicato invece che delegati a safeApiCall.
 */
class RealRepository : Repository {

    private val apiService = NetworkClient.apiService

    // =========================
    // AUTH
    // =========================

    override suspend fun login(
        firebaseToken: String,
        uid: String
    ): Result<User> {
        return try {
            // 1. Verifica che l'utente esista nel backend
            val verifyResponse = apiService.verifySession(uid, "Bearer $firebaseToken")

            if (verifyResponse.valida && verifyResponse.userId != null) {
                // 2. Scarica i dati completi dell'utente da MongoDB
                val userCompleto = apiService.getMyDetails("Bearer $firebaseToken")

                // 3. Controllo di sicurezza sui dati
                if (userCompleto.nome.isBlank() || userCompleto.email.isBlank()) {
                    Result.failure(AppError.ValidationError("Dati profilo incompleti, contatta l'assistenza"))
                } else {
                    Result.success(userCompleto)
                }
            } else {
                Result.failure(AppError.ValidationError("Utente non trovato nel backend"))
            }
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.failure(AppError.ValidationError("Utente non trovato nel backend"))
                401 -> Result.failure(AppError.SessionExpired())
                500, 502, 503 -> Result.failure(AppError.ServerError(e.code(), "Impossibile recuperare i dati del profilo. Riprova tra poco."))
                else -> Result.failure(AppError.ServerError(e.code(), e.message() ?: "Errore di rete"))
            }
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e))
        }
    }

    override suspend fun register(
        request: RegisterRequest,
        firebaseToken: String
    ): Result<User> =
    // Il filtro lato server richiede Authorization: Bearer <idToken> su
    // QUALSIASI path autenticato, incluso /users — anche per la
    // registrazione CITTADINO "pubblica" (pubblica nel senso che non
        // serve essere OPERATORE, non nel senso che non serve un token).
        safeApiCall { apiService.register(request, "Bearer $firebaseToken") }

    override suspend fun verifySession(uid: String): Result<Boolean> {
        // Qui, a differenza di login(), la sessione è già attiva e
        // AuthInterceptor aggiunge già l'header Authorization in automatico:
        // passiamo null per non sovrascriverlo.
        //
        // Un 404 qui NON è un errore applicativo: significa semplicemente
        // "sessione non valida", quindi il ramo va gestito fuori da
        // safeApiCall (che altrimenti lo tradurrebbe in un Result.failure).
        return try {
            val response = apiService.verifySession(uid, null)
            Result.success(response.valida)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.success(false)
                401 -> Result.failure(AppError.SessionExpired())
                else -> Result.failure(AppError.ServerError(e.code(), e.message() ?: "Errore di rete"))
            }
        } catch (e: Exception) {
            Result.failure(AppError.NetworkError(e))
        }
    }

    // =========================
    // TICKETS
    // =========================

    override suspend fun createTicket(ticket: Ticket): Result<Ticket> =
        safeApiCall {
            apiService.createTicket(
                NuovoTicketRequest(
                    titolo = ticket.titolo,
                    descrizione = ticket.descrizione,
                    categoria = ticket.categoria,
                    priorita = ticket.priorita,
                    coordinate = ticket.coordinate,
                    fotoAllegata = ticket.fotoAllegata
                )
            )
        }

    override suspend fun getMyTickets(): Result<List<Ticket>> =
        safeApiCall { apiService.getMyTickets() }

    override suspend fun getTicketById(ticketId: String): Result<Ticket> =
        safeApiCall(
            notFoundError = { AppError.ValidationError("Ticket non trovato: $ticketId") }
        ) { apiService.getTicketById(ticketId) }

    // =========================
    // NOTIFICATIONS
    // =========================

    override suspend fun getNotifications(): Result<List<Notification>> =
        safeApiCall { apiService.getNotifications() }

    // =========================
    // INTERVENTIONS
    // =========================

    override suspend fun getMyInterventions(): Result<List<Intervention>> =
        safeApiCall { apiService.getMyInterventions() }

    override suspend fun getInterventionById(interventionId: String): Result<Intervention> =
        safeApiCall(
            notFoundError = { AppError.InterventionNotFound(interventionId) }
        ) { apiService.getInterventionById(interventionId) }

    override suspend fun updateInterventionStatus(
        interventionId: String,
        status: String,
        note: String?
    ): Result<Unit> =
    // "stato" e "note" vanno passati come query parameter, non come body JSON
        // (vedi nota in ApiService.kt)
        safeApiCall(
            notFoundError = { AppError.InterventionNotFound(interventionId) }
        ) { apiService.updateInterventionStatus(interventionId, status, note) }
}