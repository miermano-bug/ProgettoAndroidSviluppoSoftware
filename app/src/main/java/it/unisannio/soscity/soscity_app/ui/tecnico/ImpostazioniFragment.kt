package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * Schermata Impostazioni reale, implementata con PreferenceFragmentCompat.
 *
 * La logica di sincronizzazione ora passa da ImpostazioniViewModel invece di
 * chiamare il Repository direttamente dal click listener (violazione MVVM
 * corretta).
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

    private val viewModel: ImpostazioniViewModel by viewModels {
        ImpostazioniViewModel.Factory(RepositoryProvider.provideRepository())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_tecnico, rootKey)

        setupToggleNotifiche()
        setupListaPreferenzaTema()
        setupSincronizza()
        setupNumeriEmergenza()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        osservaStato()
    }

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { stato ->
                val prefSincronizza = findPreference<Preference>("pref_sincronizza") ?: return@collect
                when (stato) {
                    is UiState.Idle -> Unit

                    is UiState.Loading -> {
                        prefSincronizza.summary = getString(R.string.impostazioni_sincronizzazione_in_corso)
                        prefSincronizza.isEnabled = false
                    }

                    is UiState.Success -> {
                        prefSincronizza.summary = getString(R.string.impostazioni_dati_aggiornati)
                        prefSincronizza.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.impostazioni_sincronizzazione_ok),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is UiState.Error -> {
                        prefSincronizza.summary = getString(R.string.impostazioni_sincronizza)
                        prefSincronizza.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            stato.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // ─── Toggle notifiche ────────────────────────────────────────────────────

    private fun setupToggleNotifiche() {
        findPreference<SwitchPreferenceCompat>("pref_notifiche")?.apply {
            // Il valore viene letto e scritto automaticamente da SharedPreferences.
            setOnPreferenceChangeListener { _, _ ->
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
                viewModel.sincronizzaDati()
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