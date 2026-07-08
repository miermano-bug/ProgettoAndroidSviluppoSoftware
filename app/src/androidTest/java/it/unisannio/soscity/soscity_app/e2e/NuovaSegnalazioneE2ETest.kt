package it.unisannio.soscity.soscity_app.e2e

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unisannio.soscity.soscity_app.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NuovaSegnalazioneE2ETest : BaseE2ETest() {

    private val titoloUnivoco = "E2E lampione ${System.currentTimeMillis()}"

    private fun loginCittadino() {
        onView(withId(R.id.editEmail)).perform(typeText("cittadino@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("Test1234!"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        Espresso.closeSoftKeyboard() // Assicura che la tastiera sia chiusa
        Thread.sleep(500)            // Attendi la chiusura fisica dello schermo per liberare i bottoni in basso
    }

    @Test
    fun creazione_segnalazione_completa_e_appare_nella_lista() {
        activityRule.scenario.recreate()
        loginCittadino()

        // FAB -> nuovaSegnalazioneFragment (schermo intero, vedi nav_graph.xml)
        onView(withId(R.id.fabNuovaSegnalazione)).perform(click())

        onView(withId(R.id.editTitolo)).perform(typeText(titoloUnivoco), closeSoftKeyboard())
        onView(withId(R.id.editDescrizione)).perform(
            typeText("Segnalazione creata dal test Espresso"), closeSoftKeyboard()
        )
        // Spinner categoria/priorita' hanno già un valore selezionato di default
        // (il primo dell'array: ILLUMINAZIONE / BASSA) — sufficiente per il test,
        // NuovaSegnalazioneViewModel non richiede altro (coordinate hanno un default).
        onView(withId(R.id.btnInvia)).perform(click())

        // Su successo: Toast "Inviato!" + popBackStack() -> si torna alla Home Cittadino
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))

        // Verifica che compaia in "Le mie segnalazioni"
        onView(withId(R.id.tab_segnalazioni_citt)).perform(click())
        onView(withId(R.id.recyclerTickets)).check(matches(isDisplayed()))
        onView(withText(titoloUnivoco)).check(matches(isDisplayed()))
    }
}