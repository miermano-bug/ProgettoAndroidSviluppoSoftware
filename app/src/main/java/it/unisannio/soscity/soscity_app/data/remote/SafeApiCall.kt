package it.unisannio.soscity.soscity_app.data.remote

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException
import java.io.IOException

/**
 * Corpo d'errore restituito dal backend, es. {"errore": "..."}.
 * Non tutti i servizi lo garantiscono, quindi il parsing ha sempre un fallback.
 */
private data class ErrorBody(val errore: String?)

/**
 * Estrae il messaggio reale restituito dal backend nel body della risposta HTTP,
 * invece di limitarsi alla reason phrase HTTP (es. "Bad Request") che e.message()
 * restituirebbe da solo.
 */
private fun HttpException.parseServerMessage(): String {
    val raw = try {
        response()?.errorBody()?.string()
    } catch (e: IOException) {
        null
    }

    if (raw.isNullOrBlank()) return message() ?: "Errore sconosciuto"

    return try {
        Gson().fromJson(raw, ErrorBody::class.java)?.errore?.takeIf { it.isNotBlank() } ?: raw
    } catch (e: JsonSyntaxException) {
        // Il servizio non ha wrappato l'errore in JSON: usiamo la stringa cruda
        raw
    }
}

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
        val serverMessage = e.parseServerMessage()
        when (e.code()) {
            400 -> Result.failure(AppError.ValidationError(serverMessage))
            401 -> Result.failure(AppError.SessionExpired())
            403 -> Result.failure(AppError.ValidationError("Non autorizzato"))
            404 -> Result.failure(notFoundError?.invoke() ?: AppError.ValidationError("Risorsa non trovata"))
            500, 502, 503 -> Result.failure(AppError.ServerError(e.code(), serverMessage))
            else -> Result.failure(AppError.ServerError(e.code(), serverMessage))
        }
    } catch (e: IOException) {
        Result.failure(AppError.NetworkError(e))
    } catch (e: Exception) {
        Result.failure(AppError.ServerError(0, e.message ?: "Errore sconosciuto"))
    }
}