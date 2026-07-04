package it.unisannio.soscity.soscity_app.util

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor che allega il Firebase ID Token a ogni richiesta HTTP.
 *
 * Rispetto alla versione precedente che leggeva un token statico da SessionManager,
 * questa versione richiede sempre un token fresco a FirebaseAuth prima di ogni chiamata.
 * Il metodo getIdToken(false) restituisce il token corrente se ancora valido (scadenza
 * Firebase: 1 ora), oppure ne ottiene automaticamente uno nuovo senza bisogno di logout.
 * Tasks.await() blocca il thread OkHttp corrente (non il Main thread): e' il pattern
 * documentato per gli Interceptor che necessitano di operazioni asincrone.
 */
class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val token: String? = try {
                // false = usa il token in cache se ancora valido, rinnova solo se scaduto
                Tasks.await(currentUser.getIdToken(false)).token
            } catch (e: Exception) {
                // In caso di errore (es. nessuna connessione) ricade sul token memorizzato
                SessionManager.getToken()
            }

            if (token != null) {
                builder.header("Authorization", "Bearer $token")
                // Aggiorna SessionManager con il token piu' recente
                SessionManager.updateToken(token)
            }
        }

        builder.header("Content-Type", "application/json")
        return chain.proceed(builder.build())
    }
}