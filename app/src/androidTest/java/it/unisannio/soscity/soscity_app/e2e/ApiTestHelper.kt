package it.unisannio.soscity.soscity_app.e2e

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Helper HTTP "grezzo", indipendente dall'ApiService/Retrofit dell'app e
 * dalla sessione Firebase corrente dell'app: serve a simulare, prima del
 * test, le azioni che nell'app reale farebbe l'OPERATORE dalla dashboard web
 * Non sostituisce nessuna asserzione: quelle restano
 * tutte espresse in Espresso sulla UI del Cittadino/Tecnico.
 *
 * Firebase qui e' usato SOLO per ottenere un idToken (autenticazione),
 * mai per salvare dati: tutte le operazioni (team, assignment, intervention)
 * vengono create tramite le API REST del backend Quarkus.
 */
object ApiTestHelper {

    private const val BASE_URL = "http://172.31.6.15:9090"
    private const val FIREBASE_API_KEY = "AIzaSyC5_eCF_hN55io-6BohgdzXNISyDwKGARU"


    const val OPERATORE_EMAIL = "operatore@test.com"
    const val OPERATORE_PASSWORD = "Test1234!"
    const val TECNICO_UID = "BFO9oebrmfUpkptOGO5mwkQoRkt2"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json".toMediaType()

    /** Login Firebase via REST — ritorna l'idToken. */
    fun firebaseLogin(email: String, password: String): String {
        val body = JSONObject().apply {
            put("email", email); put("password", password); put("returnSecureToken", true)
        }.toString().toRequestBody(JSON)

        val req = Request.Builder()
            .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$FIREBASE_API_KEY")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body?.string() ?: "{}")
            return json.getString("idToken")
        }
    }

    data class AuthResult(val idToken: String, val localId: String)

    /** Login Firebase via REST — ritorna sia l'idToken che il localId (UID). */
    fun firebaseLoginWithUid(email: String, password: String): AuthResult {
        val body = JSONObject().apply {
            put("email", email); put("password", password); put("returnSecureToken", true)
        }.toString().toRequestBody(JSON)

        val req = Request.Builder()
            .url("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$FIREBASE_API_KEY")
            .post(body)
            .build()

        client.newCall(req).execute().use { resp ->
            val json = JSONObject(resp.body?.string() ?: "{}")
            return AuthResult(json.getString("idToken"), json.getString("localId"))
        }
    }

    private fun call(method: String, path: String, token: String, jsonBody: JSONObject? = null): JSONObject {
        val builder = Request.Builder()
            .url("$BASE_URL$path")
            .header("Authorization", "Bearer $token")
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post((jsonBody ?: JSONObject()).toString().toRequestBody(JSON))
            "PUT" -> builder.put((jsonBody ?: JSONObject()).toString().toRequestBody(JSON))
        }
        client.newCall(builder.build()).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code} su $method $path: $raw")
            }
            return if (raw.isBlank()) JSONObject() else JSONObject(raw)
        }
    }

    fun getTeamIdDiTecnico(token: String): String {
        val user = call("GET", "/users/me", token)
        return user.getString("idTeam")
    }

    /** UC3 di setup: crea un team disponibile e assegna il ticket, come farebbe l'operatore. */
    fun assegnaTicketATeamDiTest(ticketId: String, teamId: String? = null): String {
        val opToken = firebaseLogin(OPERATORE_EMAIL, OPERATORE_PASSWORD)

        val targetTeamId = if (teamId != null) {
            teamId
        } else {
            val team = call("POST", "/teams", opToken, JSONObject().apply {
                put("nome", "Team E2E Espresso"); put("area", "Area Test")
            })
            team.getString("id")
        }

        call("POST", "/assignments", opToken, JSONObject().apply {
            put("ticketId", ticketId)
            put("teamId", targetTeamId)
            put("dataInterventoPrevista", "2026-12-01T09:00:00")
            put("noteOperative", "Setup automatico test Espresso")
        })
        return targetTeamId
    }

    /** UC4 di setup: crea l'intervento per il tecnico di test (l'app non lo fa: lo fa l'operatore). */
    fun creaInterventoPerTecnicoDiTest(ticketId: String, teamId: String, tecnicoUid: String): String {
        val opToken = firebaseLogin(OPERATORE_EMAIL, OPERATORE_PASSWORD)
        val interv = call("POST", "/interventions", opToken, JSONObject().apply {
            put("ticketId", ticketId)
            put("teamId", teamId)
            put("technicianId", tecnicoUid)
            put("interventionNotes", "Intervento di test Espresso")
            put("startDate", "2026-12-01T09:00:00")
        })
        return interv.getString("id")
    }
}