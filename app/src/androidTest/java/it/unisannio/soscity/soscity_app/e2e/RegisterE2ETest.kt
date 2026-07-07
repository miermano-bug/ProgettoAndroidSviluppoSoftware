package it.unisannio.soscity.soscity_app.e2e

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RegisterE2ETest : BaseE2ETest() {

    private val emailUnivoca = "e2e.espresso.${System.currentTimeMillis()}@test.com"

    @Test
    fun registrazione_cittadino_con_dati_validi_naviga_alla_home_cittadino() {
        onView(withId(R.id.textRegister)).perform(click())

        onView(withId(R.id.editNome)).perform(typeText("Mario E2E"), closeSoftKeyboard())
        onView(withId(R.id.editEmail)).perform(typeText(emailUnivoca), closeSoftKeyboard())
        onView(withId(R.id.editTelefono)).perform(typeText("3331234567"), closeSoftKeyboard())
        onView(withId(R.id.editUsername)).perform(typeText("mario_e2e_${System.currentTimeMillis()}"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("Test1234!"), closeSoftKeyboard())
        onView(withId(R.id.buttonRegister)).perform(click())

        // roleHomeDestination("CITTADINO") -> cittadinoContainerFragment
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        onView(withId(R.id.tab_home_citt)).check(matches(isDisplayed()))
    }

    @Test
    fun registrazione_con_password_troppo_corta_viene_bloccata_lato_client() {
        onView(withId(R.id.textRegister)).perform(click())

        onView(withId(R.id.editNome)).perform(typeText("Mario Corto"), closeSoftKeyboard())
        onView(withId(R.id.editEmail)).perform(typeText("e2e.corto.${System.currentTimeMillis()}@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editUsername)).perform(typeText("mario_corto"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("123"), closeSoftKeyboard()) // < 6 caratteri
        onView(withId(R.id.buttonRegister)).perform(click())

        // validateInput() in RegisterFragment blocca PRIMA di chiamare il ViewModel:
        // nessuna chiamata di rete, si resta sulla schermata di registrazione.
        onView(withId(R.id.buttonRegister)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomNav)).check(doesNotExist())
    }

    /**
     * Pulizia: l'utente Firebase creato nel primo test va rimosso per non
     * accumulare account fittizi. Il backend (auth-service) non espone un
     * endpoint di cancellazione utente (vedi contratto API): il record in
     * MongoDB resta, l'account Firebase invece viene eliminato qui.
     *
     * Su device fisico la delete() è resa SINCRONA con Tasks.await(...) e un
     * timeout: senza, su rete reale (più lenta della rete locale
     * emulatore-host) il test framework potrebbe proseguire prima che la
     * cancellazione sia effettivamente arrivata a Firebase.
     */
    @After
    fun cleanupUtenteFirebase() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        try {
            Tasks.await(user.delete(), 10, TimeUnit.SECONDS)
        } catch (e: Exception) {
            // Non far fallire la suite per un cleanup non riuscito:
            // logghiamo soltanto, l'account fittizio andrà ripulito a mano.
            Log.w("RegisterE2ETest", "Cleanup utente Firebase fallito: ${e.message}")
        }
    }
}