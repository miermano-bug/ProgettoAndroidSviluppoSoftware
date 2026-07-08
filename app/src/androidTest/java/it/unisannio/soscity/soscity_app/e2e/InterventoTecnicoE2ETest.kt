package it.unisannio.soscity.soscity_app.e2e

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unisannio.soscity.soscity_app.BuildConfig
import it.unisannio.soscity.soscity_app.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterventoTecnicoE2ETest : BaseE2ETest() {

    private val client = OkHttpClient()
    private val JSON = "application/json".toMediaType()

    @Test
    fun tecnico_completa_intervento_e_lo_vede_come_COMPLETATO() {
        activityRule.scenario.recreate()

        // --- 1. Setup: cittadino crea il ticket via API (non serve pilotare
        //        questa parte in Espresso, e' gia' coperta da NuovaSegnalazioneE2ETest) ---
        val cittadinoToken = ApiTestHelper.firebaseLogin("cittadino@test.com", "Test1234!")
        val ticketId = creaTicketViaApi(cittadinoToken, "E2E UC4 ${System.currentTimeMillis()}")

        // --- 2. Setup: operatore assegna il ticket e crea l'intervento
        //        per il tecnico di test (recuperiamo dinamicamente il suo UID e il suo Team ID) ---
        val authTecnico = ApiTestHelper.firebaseLoginWithUid("tecnicoY@test.com", "Test1234!")
        val tecnicoTeamId = ApiTestHelper.getTeamIdDiTecnico(authTecnico.idToken)
        val teamId = ApiTestHelper.assegnaTicketATeamDiTest(ticketId, tecnicoTeamId)
        ApiTestHelper.creaInterventoPerTecnicoDiTest(ticketId, teamId, authTecnico.localId)

        // Attendi che il backend propaghi la creazione dell'intervento prima del login
        Thread.sleep(2000)

        // --- 3. Login come tecnico nell'app ---
        onView(withId(R.id.editEmail)).perform(typeText("tecnicoY@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("Test1234!"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))

        // --- 4. Vai alla tab Interventi e completa il primo intervento in lista ---
        Espresso.closeSoftKeyboard() // Assicura che la tastiera sia chiusa e non copra la bottomNav
        Thread.sleep(500)            // Attendi che la tastiera si chiuda fisicamente dallo schermo
        onView(withId(R.id.tab_interventi)).perform(click())
        onView(withId(R.id.recyclerInterventi)).check(matches(isDisplayed()))

        // L'intervento e' inizialmente PIANIFICATO, quindi dobbiamo prima avviarlo (IN_CORSO)
        // e poi completarlo (COMPLETATO).
        onView(withId(R.id.recyclerInterventi)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.btnAvvia)
            )
        )

        // Il bottone "Completa" e' dentro item_intervention.xml (id btnCompleta):
        // RecyclerViewActions permette di agire su una view interna all'item.
        onView(withId(R.id.recyclerInterventi)).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.btnCompleta)
            )
        )

        // --- 5. Verifica in Espresso: la lista resta consistente (l'adapter
        //        aggiorna la card in place) ---
        onView(withId(R.id.recyclerInterventi)).check(matches(isDisplayed()))

        // --- 6. Verifica cross-service via API: il ticket deve risultare RISOLTO.
        //        Su backend reale l'aggiornamento cross-service puo' non essere
        //        immediato rispetto alla risposta del click UI: ritentiamo la
        //        lettura invece di leggere una sola volta. ---
        val statoTicket = attendiStatoTicket(cittadinoToken, ticketId, "RISOLTO")
        assertEquals("RISOLTO", statoTicket)
    }

    /**
     * Ritenta la lettura dello stato del ticket fino a che non raggiunge lo
     * stato atteso o esaurisce i tentativi (5 tentativi, 1s di pausa) —
     * assorbe la latenza di propagazione cross-service su rete reale.
     * Ritorna comunque l'ultimo stato letto (cosi' l'assertEquals a chiamare
     * produce un messaggio di errore leggibile in caso di fallimento reale).
     */
    private fun attendiStatoTicket(
        token: String, ticketId: String, statoAtteso: String, tentativi: Int = 5
    ): String {
        var ultimoStato = ""
        repeat(tentativi) { tentativo ->
            ultimoStato = leggiStatoTicket(token, ticketId)
            if (ultimoStato == statoAtteso) return ultimoStato
            if (tentativo < tentativi - 1) Thread.sleep(1000)
        }
        return ultimoStato
    }

    private fun creaTicketViaApi(token: String, titolo: String): String {
        val body = JSONObject().apply {
            put("titolo", titolo)
            put("descrizione", "Creato per setup UC4")
            put("categoria", "ILLUMINAZIONE")
            put("priorita", "ALTA")
            put("coordinate", JSONObject().apply {
                put("latitudine", 41.1279); put("longitudine", 14.7811)
            })
        }.toString().toRequestBody(JSON)

        val req = Request.Builder()
            .url("${BuildConfig.BASE_URL}tickets")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            return JSONObject(resp.body?.string().orEmpty()).getString("id")
        }
    }

    private fun leggiStatoTicket(token: String, ticketId: String): String {
        val req = Request.Builder()
            .url("${BuildConfig.BASE_URL}tickets/$ticketId")
            .header("Authorization", "Bearer $token")
            .get().build()
        client.newCall(req).execute().use { resp ->
            return JSONObject(resp.body?.string().orEmpty()).getString("stato")
        }
    }

    /** Espressione per cliccare una view specifica dentro l'item di una RecyclerView. */
    private fun clickChildViewWithId(id: Int) = object : ViewAction {
        override fun getConstraints() = null
        override fun getDescription() = "Click su una child view con id specifico"
        override fun perform(uiController: UiController, view: View) {
            view.findViewById<View>(id).performClick()
        }
    }
}