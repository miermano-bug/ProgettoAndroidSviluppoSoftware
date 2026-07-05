package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.databinding.FragmentImpostazioniCittadinoBinding
import it.unisannio.soscity.soscity_app.ui.tecnico.NumeriEmergenzaBottomSheet
import it.unisannio.soscity.soscity_app.util.mostraDialogoLogout
import it.unisannio.soscity.soscity_app.util.performLogout

/**
 * Schermata Impostazioni del Cittadino.
 *
 * Stesso identico pattern grafico e funzionale della schermata Impostazioni
 * del Tecnico (ImpostazioniFragment): header blu con card arrotondate,
 * preferenze persistite in SharedPreferences (pref_notifiche / pref_tema).
 *
 * L'unica differenza rispetto alla versione Tecnico e' l'assenza della
 * sezione "Sincronizza dati", che ha senso solo per il flusso di lavoro del
 * tecnico (aggiornamento della lista interventi).
 *
 * Raggiunta dal bottone impostazioni nell'header della Home Cittadino.
 */
class ImpostazioniCittadinoFragment : Fragment() {

    private var _binding: FragmentImpostazioniCittadinoBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImpostazioniCittadinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnIndietro.setOnClickListener { findNavController().navigateUp() }

        setupNotifiche()
        setupTema()
        setupNumeriEmergenza()
        setupLogout()
    }

    // ─── Toggle notifiche ────────────────────────────────────────────────────

    private fun setupNotifiche() {
        binding.switchNotifiche.isChecked = prefs.getBoolean(PREF_NOTIFICHE, true)
        binding.switchNotifiche.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean(PREF_NOTIFICHE, checked) }
        }
    }

    // ─── Tema (chiaro / scuro / sistema) ─────────────────────────────────────

    private fun setupTema() {
        aggiornaSelezioneTema(prefs.getString(PREF_TEMA, "system") ?: "system")

        binding.chipTemaSistema.setOnClickListener { selezionaTema("system") }
        binding.chipTemaChiaro.setOnClickListener { selezionaTema("light") }
        binding.chipTemaScuro.setOnClickListener { selezionaTema("dark") }
    }

    private fun selezionaTema(valore: String) {
        prefs.edit { putString(PREF_TEMA, valore) }
        aggiornaSelezioneTema(valore)

        val modalita = when (valore) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
            else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(modalita)
    }

    private fun aggiornaSelezioneTema(valore: String) {
        evidenziaChip(binding.chipTemaSistema, valore == "system")
        evidenziaChip(binding.chipTemaChiaro, valore == "light")
        evidenziaChip(binding.chipTemaScuro, valore == "dark")
    }

    private fun evidenziaChip(chip: TextView, selezionato: Boolean) {
        if (selezionato) {
            chip.setBackgroundResource(R.drawable.bg_theme_chip_selected)
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            chip.background = null
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
    }

    // ─── Numeri di emergenza ──────────────────────────────────────────────────

    private fun setupNumeriEmergenza() {
        binding.rowNumeriEmergenza.setOnClickListener {
            NumeriEmergenzaBottomSheet().show(parentFragmentManager, "numeri_emergenza")
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    private fun setupLogout() {
        binding.btnLogoutImpostazioni.setOnClickListener {
            mostraDialogoLogout(requireContext()) { findNavController().performLogout() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PREF_NOTIFICHE = "pref_notifiche"
        private const val PREF_TEMA = "pref_tema"
    }
}