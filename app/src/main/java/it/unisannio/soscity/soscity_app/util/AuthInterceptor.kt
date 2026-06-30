package it.unisannio.soscity.soscity_app.util

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor che aggiunge il Firebase ID Token a tutte le richieste.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // Aggiunge il Firebase ID Token se l'utente è loggato
        val token = SessionManager.getToken()
        if (SessionManager.isLoggedIn && token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        // Aggiunge Content-Type per tutte le richieste
        builder.header("Content-Type", "application/json")

        return chain.proceed(builder.build())
    }
}