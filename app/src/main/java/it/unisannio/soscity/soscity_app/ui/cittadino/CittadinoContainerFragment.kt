package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.databinding.FragmentCittadinoContainerBinding

/**
 * Container con bottom navigation per l'area Cittadino, sullo stesso pattern
 * di TecnicoContainerFragment: tre tab (Home, Segnalazioni, Profilo) gestiti con
 * hide/show sul child FragmentManager per preservare lo stato.
 *
 * Il FAB "Nuova segnalazione" resta sempre visibile sopra le tab e naviga alla
 * destinazione a schermo intero nuovaSegnalazioneFragment nel nav_graph esterno
 * (la stessa raggiungibile anche dal bottone dentro la tab Home).
 */
class CittadinoContainerFragment : Fragment() {

    private var _binding: FragmentCittadinoContainerBinding? = null
    private val binding get() = _binding!!

    private var currentTag = TAG_HOME

    companion object {
        const val TAG_HOME         = "tab_home_citt"
        const val TAG_SEGNALAZIONI = "tab_segnalazioni_citt"
        const val TAG_PROFILO      = "tab_profilo_citt"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCittadinoContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            switchTab(TAG_HOME) { CitizenHomeFragment() }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home_citt         -> { switchTab(TAG_HOME)         { CitizenHomeFragment() }; true }
                R.id.tab_segnalazioni_citt -> { switchTab(TAG_SEGNALAZIONI) { LeMieSegnalazioniFragment() }; true }
                R.id.tab_profilo_citt      -> { switchTab(TAG_PROFILO)      { ProfiloCittadinoFragment() }; true }
                else -> false
            }
        }

        binding.fabNuovaSegnalazione.setOnClickListener {
            findNavController().navigate(R.id.nuovaSegnalazioneFragment)
        }
    }

    /**
     * Switcha tra tab preservando lo stato dei fragment gia' creati
     * (hide/show invece di replace, come Gmail/WhatsApp).
     */
    private fun switchTab(tag: String, creator: () -> Fragment) {
        if (tag == currentTag && childFragmentManager.findFragmentByTag(tag) != null) return

        val fm      = childFragmentManager
        val current = fm.findFragmentByTag(currentTag)
        val next    = fm.findFragmentByTag(tag) ?: creator().also { new ->
            fm.beginTransaction()
                .add(R.id.navHostContainer, new, tag)
                .commit()
        }

        fm.beginTransaction()
            .apply { current?.let { hide(it) } }
            .show(next)
            .commit()

        currentTag = tag
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}