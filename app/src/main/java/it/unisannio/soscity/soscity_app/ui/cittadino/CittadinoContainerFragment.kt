package it.unisannio.soscity.soscity_app.ui.cittadino

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.databinding.FragmentCittadinoContainerBinding
import it.unisannio.soscity.soscity_app.notifications.NotificationWorkScheduler

/**
 * Container con bottom navigation per l'area Cittadino, sullo stesso pattern
 * di TecnicoContainerFragment: tre tab (Home, Segnalazioni, Profilo) gestiti con
 * hide/show sul child FragmentManager per preservare lo stato.
 *
 * Il FAB "Nuova segnalazione" resta sempre visibile sopra le tab e naviga alla
 * destinazione a schermo intero nuovaSegnalazioneFragment nel nav_graph esterno
 * (la stessa raggiungibile anche dal bottone dentro la tab Home).
 *
 * Da questa versione, all'ingresso nell'area Cittadino vengono anche:
 * 1. richiesto (su Android 13+/API 33+) il permesso POST_NOTIFICATIONS,
 *    necessario per poter mostrare le notifiche di sistema di chiusura ticket;
 * 2. pianificato il controllo periodico in background (vedi
 *    NotificationWorkScheduler / TicketClosedNotificationWorker) che mostra
 *    una vera notifica Android quando una segnalazione viene chiusa.
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

    // Esito ignorato volutamente: se il permesso viene negato, l'app continua
    // a funzionare normalmente, semplicemente non potra' mostrare la
    // notifica di sistema di chiusura ticket (TicketClosedNotificationWorker
    // controlla comunque il permesso prima di provare a notificare).
    private val richiediPermessoNotifiche =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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

        richiediPermessoNotificheSeNecessario()
        NotificationWorkScheduler.schedule(requireContext().applicationContext)
    }

    private fun richiediPermessoNotificheSeNecessario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val giaConcesso = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!giaConcesso) {
                richiediPermessoNotifiche.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
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