package it.unisannio.soscity.soscity_app.e2e

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.unisannio.soscity.soscity_app.R
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginE2ETest : BaseE2ETest() {

    @Test
    fun login_cittadino_con_credenziali_valide_naviga_alla_home_cittadino() {
        activityRule.scenario.recreate() // riparte da loginFragment pulito

        onView(withId(R.id.editEmail)).perform(typeText("cittadino@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("Test1234!"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())

        // Arriva su CittadinoContainerFragment: la bottom nav con 3 tab è la prova
        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        onView(withId(R.id.tab_home_citt)).check(matches(isDisplayed()))
        onView(withId(R.id.fabNuovaSegnalazione)).check(matches(isDisplayed()))
    }

    @Test
    fun login_tecnico_con_credenziali_valide_naviga_alla_home_tecnico() {
        activityRule.scenario.recreate()

        onView(withId(R.id.editEmail)).perform(typeText("tecnicoY@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("Test1234!"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())

        onView(withId(R.id.bottomNav)).check(matches(isDisplayed()))
        onView(withId(R.id.tab_interventi)).check(matches(isDisplayed()))
    }

    @Test
    fun login_con_password_errata_mostra_errore_e_resta_su_login() {
        activityRule.scenario.recreate()

        onView(withId(R.id.editEmail)).perform(typeText("cittadino@test.com"), closeSoftKeyboard())
        onView(withId(R.id.editPassword)).perform(typeText("PasswordSbagliata!"), closeSoftKeyboard())
        onView(withId(R.id.buttonLogin)).perform(click())

        // Il Toast di errore arriva da Firebase ("The password is invalid...")
        // rimappato da LoginViewModel — non verifichiamo il testo esatto
        // (dipende dal messaggio Firebase, può cambiare), solo che l'app
        // resti sulla schermata di login.
        onView(withId(R.id.buttonLogin)).check(matches(isDisplayed()))
        onView(withId(R.id.bottomNav)).check(doesNotExist())
    }

    @Test
    fun tap_su_registrati_naviga_a_register_fragment() {
        activityRule.scenario.recreate()
        onView(withId(R.id.textRegister)).perform(click())
        onView(withId(R.id.buttonRegister)).check(matches(isDisplayed()))
    }
}