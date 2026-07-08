package it.unisannio.soscity.soscity_app.e2e

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unisannio.soscity.soscity_app.BuildConfig
import it.unisannio.soscity.soscity_app.R
import org.hamcrest.CoreMatchers.containsString
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssegnazioneNotificaE2ETest : BaseE2ETest() {

    private fun loginCittadino() {
        onView(withId(R.id.editEmail)).perform(typeText("cittadino@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("Test1234!"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        Espresso.closeSoftKeyboard() // Assicura che la tastiera sia chiusa
        Thread.sleep(500)            // Attendi la chiusura fisica dello schermo per liberare i bottoni in basso
    }

    @Test
    fun ticket_assegnato_dall_operatore_appare_come_ASSEGNATO_e_genera_notifica() {
        activityRule.scenario.recreate()
        loginCittadino()

        // --- 1. Crea il ticket dall'app (stessa interazione di NuovaSegnalazioneE2ETest) ---
        val titolo = "E2E UC3 ${System.currentTimeMillis()}"
        onView(withId(R.id.fabNuovaSegnalazione)).perform(click())
        onView(withId(R.id.editTitolo)).perform(typeText(titolo), closeSoftKeyboard())
        onView(withId(R.id.editDescrizione)).perform(typeText("Test assegnazione"), closeSoftKeyboard())
        onView(withId(R.id.btnInvia)).perform(click())
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))

        // --- 2. Recupera l'id del ticket appena creato dalla lista (il testo del
        //        titolo e' univoco, cerchiamo il ticket corrispondente via API
        //        di sola lettura con lo stesso token dell'utente cittadino) ---
        val cittadinoToken = ApiTestHelper.firebaseLogin("cittadino@test.com", "Test1234!")
        val ticketId = trovaTicketIdPerTitolo(cittadinoToken, titolo)

        // --- 3. Setup fuori dalla UI: l'operatore assegna il ticket a un team ---
        ApiTestHelper.assegnaTicketATeamDiTest(ticketId)

        // --- 4. Verifica in Espresso: il dettaglio del ticket mostra ASSEGNATO.
        //        Su backend reale la propagazione dell'assegnazione tra servizi
        //        non e' garantita istantanea: proviamo il refresh piu' volte
        //        prima di considerare il test fallito. ---
        onView(withId(R.id.tab_segnalazioni_citt)).perform(click())
        attendiTicketAssegnato(titolo)

        // Chiude il bottom sheet prima di navigare altrove
        pressBack()

        // --- 5. Verifica la notifica PUSH nella schermata Notifiche ---
        onView(withId(R.id.tab_home_citt)).perform(click())
        onView(withId(R.id.recyclerNotificheHome)).check(matches(isDisplayed()))
        onView(first(withText(containsString("assegnat")))).check(matches(isDisplayed()))
    }

    private fun <T> first(matcher: org.hamcrest.Matcher<T>): org.hamcrest.Matcher<T> {
        return object : org.hamcrest.BaseMatcher<T>() {
            var isFirst = true
            override fun matches(item: Any?): Boolean {
                if (isFirst && matcher.matches(item)) {
                    isFirst = false
                    return true
                }
                return false
            }
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("first matching: ")
                matcher.describeTo(description)
            }
        }
    }

    /**
     * Ritenta lo swipe-to-refresh + apertura dettaglio + controllo badge fino
     * a 5 volte (con pausa breve), per assorbire la latenza di propagazione
     * dell'assegnazione lato backend su rete reale. Se dopo 5 tentativi il
     * badge non e' ancora "ASSEGNATO", il test fallisce con l'errore reale
     * dell'ultimo tentativo (piu' utile in debug che un timeout generico).
     */
    private fun attendiTicketAssegnato(titolo: String, tentativi: Int = 5) {
        var ultimoErrore: Throwable? = null
        repeat(tentativi) { tentativo ->
            try {
                onView(withId(R.id.swipeRefreshSegnalazioni)).perform(swipeDown())
                onView(withText(titolo)).perform(click()) // apre SegnalazioneDettaglioBottomSheet
                onView(withId(R.id.detailStatoBadge)).check(matches(withText("ASSEGNATO")))
                return
            } catch (e: Throwable) {
                ultimoErrore = e
                if (tentativo < tentativi - 1) {
                    pressBack() // richiude il bottom sheet se si e' aperto con stato vecchio
                    Thread.sleep(1000)
                }
            }
        }
        throw AssertionError(
            "Il ticket '$titolo' non risulta ASSEGNATO dopo $tentativi tentativi", ultimoErrore
        )
    }

    /** Lettura di sola query per trovare l'id del ticket appena creato, per titolo. */
    private fun trovaTicketIdPerTitolo(token: String, titolo: String): String {
        val client = okhttp3.OkHttpClient()
        val req = okhttp3.Request.Builder()
            .url("${BuildConfig.BASE_URL}tickets/my")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            val arr = org.json.JSONArray(resp.body?.string().orEmpty())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("titolo") == titolo) return obj.getString("id")
            }
        }
        throw IllegalStateException("Ticket con titolo '$titolo' non trovato in /tickets/my")
    }
}