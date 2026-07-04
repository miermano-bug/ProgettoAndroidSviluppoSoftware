package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * Schermata Impostazioni reale, implementata con PreferenceFragmentCompat.
 *
 * Sostituisce l'ex ImpostazioniBottomSheet che era un menu di azioni veloci
 * mascherato da "impostazioni". Questa implementazione rispetta il pattern
 * PreferenceFragmentCompat + PreferenceScreen XML (res/xml/preferences_tecnico.xml)
 * con persistenza automatica in SharedPreferences gestita dal framework Android.
 *
 * Preferenze reali persistite:
 * - pref_notifiche (SwitchPreferenceCompat): toggle notifiche (fittizio lato backend)
 * - pref_tema (ListPreference): tema chiaro/scuro/sistema, applicato immediatamente
 *
 * Preferenze-azione (non persistite, usano onPreferenceClickListener):
 * - pref_sincronizza: forza un refresh della lista interventi
 * - pref_numeri_emergenza: apre NumeriEmergenzaBottomSheet
 */
class ImpostazioniFragment : PreferenceFragmentCompat() {

    private val repository by lazy { RepositoryProvider.provideRepository() }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_tecnico, rootKey)

        setupToggleNotifiche()
        setupListaPreferenzaTema()
        setupSincronizza()
        setupNumeriEmergenza()
    }

    // ─── Toggle notifiche ────────────────────────────────────────────────────

    private fun setupToggleNotifiche() {
        findPreference<SwitchPreferenceCompat>("pref_notifiche")?.apply {
            // Il valore viene letto e scritto automaticamente da SharedPreferences.
            // Quando l'utente cambia il toggle, onPreferenceChangeListener viene
            // invocato con il nuovo valore prima che venga effettivamente salvato.
            setOnPreferenceChangeListener { _, _ ->
                // Nessuna azione reale: le notifiche push non sono supportate dal backend.
                // Il valore viene comunque persistito per mostrare lo stato coerente
                // all'utente tra sessioni.
                true
            }
        }
    }

    // ─── Tema (chiaro / scuro / sistema) ─────────────────────────────────────

    private fun setupListaPreferenzaTema() {
        findPreference<ListPreference>("pref_tema")?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                applicaTema(newValue as String)
                true
            }
        }
    }

    private fun applicaTema(valore: String) {
        val modalita = when (valore) {
            "light"  -> AppCompatDelegate.MODE_NIGHT_NO
            "dark"   -> AppCompatDelegate.MODE_NIGHT_YES
            else     -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(modalita)
    }

    // ─── Sincronizza dati ─────────────────────────────────────────────────────

    private fun setupSincronizza() {
        findPreference<Preference>("pref_sincronizza")?.apply {
            setOnPreferenceClickListener {
                summary = getString(R.string.impostazioni_sincronizzazione_in_corso)
                isEnabled = false
                lifecycleScope.launch {
                    repository.getMyInterventions()
                        .onSuccess {
                            summary = getString(R.string.impostazioni_dati_aggiornati)
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.impostazioni_sincronizzazione_ok),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .onFailure { e ->
                            summary = getString(R.string.impostazioni_sincronizza)
                            Toast.makeText(
                                requireContext(),
                                e.message ?: getString(R.string.errore_rete),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    isEnabled = true
                }
                true
            }
        }
    }

    // ─── Numeri di emergenza ──────────────────────────────────────────────────

    private fun setupNumeriEmergenza() {
        findPreference<Preference>("pref_numeri_emergenza")?.apply {
            setOnPreferenceClickListener {
                NumeriEmergenzaBottomSheet()
                    .show(parentFragmentManager, "numeri_emergenza")
                true
            }
        }
    }
}
