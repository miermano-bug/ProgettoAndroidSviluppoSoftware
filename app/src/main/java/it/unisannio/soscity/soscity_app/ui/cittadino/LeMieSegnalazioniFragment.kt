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
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.databinding.FragmentMieSegnalazioniBinding
import it.unisannio.soscity.soscity_app.ui.adapter.TicketAdapter
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * Tab "Segnalazioni" dell'area Cittadino: elenco dei ticket creati dall'utente,
 * con pull-to-refresh, stato di caricamento iniziale ed empty state.
 *
 * Al tap su una segnalazione si apre SegnalazioneDettaglioBottomSheet con
 * tutte le informazioni del ticket (tranne la foto allegata).
 *
 * La Fragment non parla piu' col Repository: osserva solo LeMieSegnalazioniViewModel
 * (violazione MVVM corretta: la logica di caricamento vive nel ViewModel).
 */
class LeMieSegnalazioniFragment : Fragment() {

    private var _binding: FragmentMieSegnalazioniBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeMieSegnalazioniViewModel by viewModels {
        LeMieSegnalazioniViewModel.Factory(RepositoryProvider.provideRepository())
    }

    private lateinit var adapter: TicketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMieSegnalazioniBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TicketAdapter(onItemClick = { ticket ->
            SegnalazioneDettaglioBottomSheet.newInstance(ticket)
                .show(childFragmentManager, "dettaglio_segnalazione")
        })
        binding.recyclerTickets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTickets.adapter = adapter

        binding.swipeRefreshSegnalazioni.setOnRefreshListener {
            viewModel.refresh()
        }

        osservaStato()

        if (viewModel.uiState.value is UiState.Idle) {
            viewModel.caricaSegnalazioni()
        }
    }

    override fun onResume() {
        super.onResume()
        // Ricarica ogni volta che la tab torna visibile, cosi' una nuova
        // segnalazione appena creata compare subito nell'elenco.
        if (adapter.itemCount == 0) {
            viewModel.caricaSegnalazioni()
        } else {
            viewModel.refresh()
        }
    }

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { stato ->
                when (stato) {
                    is UiState.Idle -> Unit

                    is UiState.Loading -> {
                        binding.progressSegnalazioni.visibility = View.VISIBLE
                        binding.emptyStateSegnalazioni.visibility = View.GONE
                    }

                    is UiState.Success -> {
                        binding.progressSegnalazioni.visibility = View.GONE
                        binding.swipeRefreshSegnalazioni.isRefreshing = false
                        mostraRisultato(stato.data)
                    }

                    is UiState.Error -> {
                        binding.progressSegnalazioni.visibility = View.GONE
                        binding.swipeRefreshSegnalazioni.isRefreshing = false
                        Toast.makeText(requireContext(), "Errore: ${stato.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun mostraRisultato(tickets: List<Ticket>) {
        adapter.updateData(tickets)
        binding.emptyStateSegnalazioni.visibility =
            if (tickets.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}