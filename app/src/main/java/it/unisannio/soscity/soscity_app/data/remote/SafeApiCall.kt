package it.unisannio.soscity.soscity_app.data.remote

import retrofit2.HttpException
import java.io.IOException

/**
 * Esegue una chiamata API in modo sicuro, traducendo le eccezioni in AppError.
 * Usata da RealRepository per uniformare la gestione degli errori.
 */
suspend fun <T> safeApiCall(
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: HttpException) {
        when (e.code()) {
            400 -> Result.failure(AppError.ValidationError(e.message ?: "Dati non validi"))
            401 -> Result.failure(AppError.SessionExpired())
            403 -> Result.failure(AppError.ValidationError("Non autorizzato"))
            404 -> Result.failure(AppError.ValidationError("Risorsa non trovata"))
            500, 502, 503 -> Result.failure(AppError.ServerError(e.code(), e.message ?: "Errore server"))
            else -> Result.failure(AppError.ServerError(e.code(), e.message ?: "Errore sconosciuto"))
        }
    } catch (e: IOException) {
        Result.failure(AppError.NetworkError(e))
    } catch (e: Exception) {
        Result.failure(AppError.ServerError(0, e.message ?: "Errore sconosciuto"))
    }
}