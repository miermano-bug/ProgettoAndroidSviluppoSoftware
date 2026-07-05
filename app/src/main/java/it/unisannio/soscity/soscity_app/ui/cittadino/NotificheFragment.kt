package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.databinding.FragmentNotificheBinding
import it.unisannio.soscity.soscity_app.ui.adapter.NotificationAdapter
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * Schermata "Le mie notifiche" del Cittadino, raggiunta dal bottone Notifiche
 * nella Home. Il backend non supporta push: la lista viene aggiornata con un
 * caricamento iniziale, pull-to-refresh manuale e un polling automatico ogni
 * 15 secondi finche' la schermata resta visibile (vedi NotificationsViewModel).
 */
class NotificheFragment : Fragment() {

    private var _binding: FragmentNotificheBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels {
        NotificationsViewModel.Factory(RepositoryProvider.provideRepository())
    }

    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = NotificationAdapter()
        binding.recyclerNotifiche.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifiche.adapter = adapter

        binding.swipeRefreshNotifiche.setOnRefreshListener {
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
                        binding.progressNotifiche.visibility = View.VISIBLE
                        binding.emptyStateNotifiche.visibility = View.GONE
                    }

                    is UiState.Success -> {
                        binding.progressNotifiche.visibility = View.GONE
                        binding.swipeRefreshNotifiche.isRefreshing = false
                        mostraRisultato(stato.data)
                    }

                    is UiState.Error -> {
                        binding.progressNotifiche.visibility = View.GONE
                        binding.swipeRefreshNotifiche.isRefreshing = false
                        Toast.makeText(requireContext(), "Errore: ${stato.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun mostraRisultato(notifiche: List<Notification>) {
        adapter.updateData(notifiche)
        binding.emptyStateNotifiche.visibility =
            if (notifiche.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}