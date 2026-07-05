package it.unisannio.soscity.soscity_app.ui.cittadino

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.ui.adapter.NotificationAdapter
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.SessionManager
import it.unisannio.soscity.soscity_app.util.performLogout
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Tab "Home" dell'area Cittadino, ospitata dentro CittadinoContainerFragment.
 * "Le mie segnalazioni" non e' piu' una card qui: e' diventata una tab della
 * bottom navigation (vedi CittadinoContainerFragment). "Nuova segnalazione"
 * non e' piu' una card qui: l'unico punto di accesso e' il FAB "+" del
 * container, per evitare due modi diversi di fare la stessa azione.
 *
 * "Notifiche" non e' piu' una card che rimanda a un'altra schermata: la
 * sezione un tempo intitolata "Seleziona un'operazione" e' stata sostituita
 * da "Notifiche" e la lista delle notifiche del cittadino (recuperata dal
 * notification-service, stesso ViewModel/adapter di NotificheFragment) e'
 * mostrata direttamente qui in Home, con pull-to-refresh e polling ogni 15
 * secondi mentre la schermata e' visibile.
 */
class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    private val viewModel: NotificationsViewModel by viewModels {
        NotificationsViewModel.Factory(RepositoryProvider.provideRepository())
    }

    private lateinit var adapter: NotificationAdapter

    private lateinit var recyclerNotifiche: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var emptyState: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textSaluto = view.findViewById<TextView>(R.id.textSaluto)
        val btnLogout = view.findViewById<View>(R.id.btnLogout)
        val btnImpostazioni = view.findViewById<View>(R.id.btnImpostazioni)

        recyclerNotifiche = view.findViewById(R.id.recyclerNotificheHome)
        swipeRefresh = view.findViewById(R.id.swipeRefreshNotificheHome)
        progress = view.findViewById(R.id.progressNotificheHome)
        emptyState = view.findViewById(R.id.emptyStateNotificheHome)

        impostaHeader(textSaluto)

        btnImpostazioni.setOnClickListener {
            findNavController().navigate(R.id.impostazioniCittadinoFragment)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_titolo)
                .setMessage(R.string.logout_messaggio)
                .setPositiveButton(R.string.logout_conferma) { _, _ ->
                    findNavController().performLogout()
                }
                .setNegativeButton(R.string.logout_annulla, null)
                .show()
        }

        adapter = NotificationAdapter()
        recyclerNotifiche.layoutManager = LinearLayoutManager(requireContext())
        recyclerNotifiche.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        osservaStato()

        if (viewModel.uiState.value is UiState.Idle) {
            viewModel.caricaNotifiche()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.avviaPolling()
    }

    override fun onPause() {
        super.onPause()
        viewModel.fermaPolling()
    }

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { stato ->
                when (stato) {
                    is UiState.Idle -> Unit

                    is UiState.Loading -> {
                        progress.visibility = View.VISIBLE
                        emptyState.visibility = View.GONE
                    }

                    is UiState.Success -> {
                        progress.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        mostraRisultato(stato.data)
                    }

                    is UiState.Error -> {
                        progress.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), "Errore: ${stato.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun mostraRisultato(notifiche: List<Notification>) {
        adapter.updateData(notifiche)
        emptyState.visibility = if (notifiche.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun impostaHeader(textSaluto: TextView) {
        val nome = SessionManager.getUser()?.nome?.split(" ")?.firstOrNull() ?: "Cittadino"
        val saluto = when (LocalTime.now().hour) {
            in 6..11  -> getString(R.string.saluto_mattina)
            in 12..17 -> getString(R.string.saluto_pomeriggio)
            in 18..21 -> getString(R.string.saluto_sera)
            else      -> getString(R.string.saluto_notte)
        }
        textSaluto.text = "$saluto, $nome!"
    }
}