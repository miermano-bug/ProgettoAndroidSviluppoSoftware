package it.unisannio.soscity.soscity_app.e2e

import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.MainActivity
import it.unisannio.soscity.soscity_app.util.EspressoIdlingResource
import it.unisannio.soscity.soscity_app.util.SessionManager
import org.junit.After
import org.junit.Before
import org.junit.Rule

/**
 * Base comune a tutti i test E2E:
 * - lancia MainActivity (parte sempre da loginFragment, vedi nav_graph.xml)
 * - registra la IdlingResource globale
 * - garantisce che ogni test parta da "nessuno loggato", indipendentemente
 *   da cosa abbia lasciato il test precedente
 */
abstract class BaseE2ETest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    open fun setUp() {
        // Registriamo la risorsa prima del test
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        // Sganciamo Firebase e Sessione in sicurezza sul Thread dell'Activity,
        // assicurandoci che l'Activity sia nata e pronta
        activityRule.scenario.onActivity { activity ->
            FirebaseAuth.getInstance().signOut()
            SessionManager.clearSession()
        }
    }

    @After
    open fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        try {
            FirebaseAuth.getInstance().signOut()
            SessionManager.clearSession()
        } catch (e: Exception) {
            // Previene crash nel tearDown se il processo è già terminato
        }
    }
}