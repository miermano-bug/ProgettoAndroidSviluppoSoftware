package it.unisannio.soscity.soscity_app.data.remote

/**
 * Errori specifici dell'applicazione.
 * Estende Exception per essere compatibile con Result.failure().
 */
sealed class AppError(
    message: String
) : Exception(message) {

    /**
     * Errore quando un intervento non viene trovato (404).
     */
    class InterventionNotFound(
        interventionId: String
    ) : AppError("Intervento non trovato: $interventionId")

    /**
     * Errore quando la sessione è scaduta (token Firebase non valido).
     */
    class SessionExpired : AppError("Sessione scaduta, riautenticati")

    /**
     * Errore di rete generico.
     */
    class NetworkError(
        cause: Throwable
    ) : AppError("Errore di rete: ${cause.message}")

    /**
     * Errore del server (5xx).
     */
    class ServerError(
        code: Int,
        message: String
    ) : AppError("Errore server ($code): $message")

    /**
     * Errore di validazione (400).
     */
    class ValidationError(
        message: String
    ) : AppError("Dati non validi: $message")
}