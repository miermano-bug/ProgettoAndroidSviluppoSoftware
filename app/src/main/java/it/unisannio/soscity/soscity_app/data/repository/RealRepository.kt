package it.unisannio.soscity.soscity_app.data.repository

import it.unisannio.soscity.soscity_app.data.remote.ApiService
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.util.NetworkClient
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementazione concreta di Repository che usa Retrofit per chiamare il backend.
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
            // Il login consiste nel verificare che l'utente esista nel backend
            // e salvare il token in SessionManager (gestito dal chiamante).
            // Header Authorization passato esplicitamente: a questo punto
            // SessionManager non ha ancora una sessione attiva, quindi
            // AuthInterceptor non aggiungerebbe l'header in automatico, e il
            // backend richiede comunque un Bearer token valido su questo endpoint.
            val response = apiService.verifySession(uid, "Bearer $firebaseToken")
            if (response.valida && response.userId != null) {
                // Restituiamo un User minimale (il backend non restituisce tutti i dettagli qui)
                // Il chiamante (ViewModel) ha già i dati dal token Firebase
                val user = User(
                    id = response.userId,
                    username = "",  // Non disponibile da verify-session
                    email = "",     // Non disponibile da verify-session
                    nome = "",      // Non disponibile da verify-session
                    ruolo = response.ruolo ?: "CITTADINO",
                    telefono = null
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Utente non trovato nel backend"))
            }
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.failure(Exception("Utente non trovato nel backend"))
                401 -> Result.failure(Exception("Token Firebase non valido"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(
        request: RegisterRequest,
        firebaseToken: String
    ): Result<User> {
        return try {
            // Il filtro lato server richiede Authorization: Bearer <idToken> su
            // QUALSIASI path autenticato, incluso /users — anche per la
            // registrazione CITTADINO "pubblica" (pubblica nel senso che non
            // serve essere OPERATORE, non nel senso che non serve un token).
            // firebaseToken arriva già come parametro da RegisterViewModel: prima
            // veniva ignorato qui, causando 401 "Token mancante".
            val user = apiService.register(request, "Bearer $firebaseToken")
            Result.success(user)
        } catch (e: HttpException) {
            when (e.code()) {
                400 -> Result.failure(Exception("Dati di registrazione non validi"))
                409 -> Result.failure(Exception("Utente già registrato nel backend"))
                401 -> Result.failure(Exception("Token Firebase non valido"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifySession(uid: String): Result<Boolean> {
        return try {
            // Qui, a differenza di login(), la sessione è già attiva e
            // AuthInterceptor aggiunge già l'header Authorization in automatico:
            // passiamo null per non sovrascriverlo.
            val response = apiService.verifySession(uid, null)
            Result.success(response.valida)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.success(false)
                401 -> Result.failure(Exception("Token Firebase non valido"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================
    // TICKETS
    // =========================

    override suspend fun createTicket(ticket: Ticket): Result<Ticket> {
        return try {
            val result = apiService.createTicket(ticket)
            Result.success(result)
        } catch (e: HttpException) {
            when (e.code()) {
                400 -> Result.failure(Exception("Dati ticket non validi"))
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                403 -> Result.failure(Exception("Non autorizzato a creare ticket"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyTickets(): Result<List<Ticket>> {
        return try {
            val result = apiService.getMyTickets()
            Result.success(result)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                403 -> Result.failure(Exception("Non autorizzato"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTicketById(ticketId: String): Result<Ticket> {
        return try {
            val result = apiService.getTicketById(ticketId)
            Result.success(result)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.failure(Exception("Ticket non trovato"))
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                403 -> Result.failure(Exception("Non autorizzato a visualizzare questo ticket"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================
    // NOTIFICATIONS
    // =========================

    override suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val result = apiService.getNotifications()
            Result.success(result)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================
    // INTERVENTIONS
    // =========================

    override suspend fun getMyInterventions(): Result<List<Intervention>> {
        return try {
            val result = apiService.getMyInterventions()
            Result.success(result)
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                403 -> Result.failure(Exception("Non autorizzato"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInterventionById(interventionId: String): Result<Intervention> {
        return try {
            val result = apiService.getInterventionById(interventionId)
            Result.success(result)
        } catch (e: HttpException) {
            when (e.code()) {
                404 -> Result.failure(Exception("Intervento non trovato"))
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                403 -> Result.failure(Exception("Non autorizzato a visualizzare questo intervento"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateInterventionStatus(
        interventionId: String,
        status: String
    ): Result<Unit> {
        return try {
            // "stato" va passato come query parameter, non come body JSON
            // (vedi nota in ApiService.kt)
            apiService.updateInterventionStatus(interventionId, status)
            Result.success(Unit)
        } catch (e: HttpException) {
            when (e.code()) {
                400 -> Result.failure(Exception("Stato non valido"))
                404 -> Result.failure(Exception("Intervento non trovato"))
                401 -> Result.failure(Exception("Sessione scaduta, riautenticati"))
                403 -> Result.failure(Exception("Non autorizzato a modificare questo intervento"))
                else -> Result.failure(Exception("Errore di rete: ${e.message}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Errore di connessione: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}