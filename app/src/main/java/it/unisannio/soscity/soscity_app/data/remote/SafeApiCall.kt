package it.unisannio.soscity.soscity_app.data.remote

import retrofit2.HttpException
import java.io.IOException

/**
 * Esegue una chiamata API in modo sicuro, traducendo le eccezioni in AppError.
 * Usata da RealRepository per uniformare la gestione degli errori.
 *
 * @param notFoundError lambda opzionale per costruire un AppError specifico in caso
 *   di 404 (es. AppError.InterventionNotFound(id)). Se non fornita, un 404 produce
 *   un generico AppError.ValidationError("Risorsa non trovata"). Permette a ogni
 *   chiamante di mantenere un messaggio/tipo contestuale senza duplicare il resto
 *   della mappatura codice HTTP -> AppError.
 */
suspend fun <T> safeApiCall(
    notFoundError: (() -> AppError)? = null,
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: HttpException) {
        when (e.code()) {
            400 -> Result.failure(AppError.ValidationError(e.message() ?: "Dati non validi"))
            401 -> Result.failure(AppError.SessionExpired())
            403 -> Result.failure(AppError.ValidationError("Non autorizzato"))
            404 -> Result.failure(notFoundError?.invoke() ?: AppError.ValidationError("Risorsa non trovata"))
            500, 502, 503 -> Result.failure(AppError.ServerError(e.code(), e.message() ?: "Errore server"))
            else -> Result.failure(AppError.ServerError(e.code(), e.message() ?: "Errore sconosciuto"))
        }
    } catch (e: IOException) {
        Result.failure(AppError.NetworkError(e))
    } catch (e: Exception) {
        Result.failure(AppError.ServerError(0, e.message ?: "Errore sconosciuto"))
    }
}