package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.StatoLavoro
import it.unisannio.soscity.soscity_app.databinding.FragmentInterventiTabBinding
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InterventiTabFragment : Fragment() {

    private var _binding: FragmentInterventiTabBinding? = null
    private val binding get() = _binding!!

    /**
     * ViewModel condiviso con HomeTabFragment tramite lo scope del Fragment genitore
     * (TecnicoContainerFragment). Cosi' la lista non viene ricaricata quando si
     * cambia tab, e le due schermate riflettono sempre lo stesso dato.
     */
    private val viewModel: InterventionsViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { InterventionsViewModel.Factory(RepositoryProvider.provideRepository()) }
    )

    private lateinit var adapter: InterventionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterventiTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupBottoni()
        osservaStato()
        osservaAzione()
        osservaBanner()

        if (viewModel.uiState.value is UiState.Idle) {
            viewModel.caricaInterventi()
        }
    }

    override fun onResume() {
        super.onResume()
        // Ricarica solo se il ViewModel non ha ancora dati (primo avvio del tab)
        if (viewModel.uiState.value is UiState.Idle) {
            viewModel.caricaInterventi()
        }
    }

    private fun setupRecycler() {
        adapter = InterventionAdapter(
            onCardClick = { iv -> apriDettaglio(iv) },
            onAvvia     = { iv, nota -> viewModel.aggiornaStato(iv, StatoLavoro.IN_CORSO, nota) },
            onCompleta  = { iv, nota -> viewModel.aggiornaStato(iv, StatoLavoro.COMPLETATO, nota) }
        )
        binding.recyclerInterventi.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerInterventi.adapter = adapter
    }

    private fun setupBottoni() {
        binding.btnRiprova.setOnClickListener { viewModel.caricaInterventi() }
    }

    private fun apriDettaglio(iv: Intervention) {
        val sheet = InterventoBottomSheet.newInstance(iv)
        sheet.onAvvia    = { nota -> viewModel.aggiornaStato(iv, StatoLavoro.IN_CORSO, nota) }
        sheet.onCompleta = { nota -> viewModel.aggiornaStato(iv, StatoLavoro.COMPLETATO, nota) }
        sheet.show(parentFragmentManager, "dettaglio")
    }

    // ─── Osservazione ViewModel ───────────────────────────────────────────────

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { stato ->
                when (stato) {
                    is UiState.Idle    -> Unit
                    is UiState.Loading -> mostraSchermata(Schermata.CARICAMENTO)
                    is UiState.Success -> {
                        val lista = stato.data
                        if (lista.isEmpty()) {
                            mostraSchermata(Schermata.VUOTO)
                        } else {
                            mostraSchermata(Schermata.CONTENUTO)
                            adapter.submitList(lista)
                            aggiornaSubtitle(lista)
                            aggiornaContatori(lista)
                        }
                    }
                    is UiState.Error -> {
                        mostraSchermata(Schermata.ERRORE)
                        binding.textErrore.text = stato.message
                    }
                }
            }
        }
    }

    private fun osservaAzione() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.azioneState.collectLatest { azione ->
                when (azione) {
                    is AzioneUiState.Successo ->
                        adapter.mostraEsito(azione.interventionId, azione.messaggio)
                    is AzioneUiState.Errore ->
                        adapter.mostraEsito(azione.interventionId, azione.messaggio)
                    is AzioneUiState.Inattivo -> Unit
                }
            }
        }
    }

    private fun osservaBanner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bannerPromozioneState.collectLatest { banner ->
                when (banner) {
                    is BannerPromozione.NuovoAvviato -> {
                        binding.textBannerPromozione.text =
                            getString(R.string.banner_nuovo_avviato)
                        binding.bannerPromozione.visibility = View.VISIBLE
                    }
                    is BannerPromozione.TeamLibero -> {
                        binding.textBannerPromozione.text =
                            getString(R.string.banner_team_libero_interventi)
                        binding.bannerPromozione.visibility = View.VISIBLE
                    }
                    is BannerPromozione.Nascosto ->
                        binding.bannerPromozione.visibility = View.GONE
                }
            }
        }
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────

    private fun aggiornaSubtitle(lista: List<Intervention>) {
        val attivi = lista.count {
            it.statoLavoro == StatoLavoro.IN_CORSO.name
                || it.statoLavoro == StatoLavoro.PIANIFICATO.name
        }
        binding.textSubtitle.text = if (attivi > 0) {
            getString(R.string.interventi_da_gestire, attivi, lista.size)
        } else {
            getString(R.string.interventi_tutto_completato, lista.size)
        }
    }

    private fun aggiornaContatori(lista: List<Intervention>) {
        binding.textContatoreAttivi.text = lista.count {
            it.statoLavoro == StatoLavoro.IN_CORSO.name
                || it.statoLavoro == StatoLavoro.PIANIFICATO.name
        }.toString()
        binding.textContatoreCompletati.text =
            lista.count { it.statoLavoro == StatoLavoro.COMPLETATO.name }.toString()
    }

    private enum class Schermata { CARICAMENTO, CONTENUTO, VUOTO, ERRORE }

    private fun mostraSchermata(s: Schermata) {
        binding.progressBar.visibility         = vis(s == Schermata.CARICAMENTO)
        binding.recyclerInterventi.visibility  = vis(s == Schermata.CONTENUTO)
        binding.layoutVuoto.visibility         = vis(s == Schermata.VUOTO)
        binding.layoutErrore.visibility        = vis(s == Schermata.ERRORE)
        if (s != Schermata.CONTENUTO) {
            binding.bannerPromozione.visibility = View.GONE
        }
    }

    private fun vis(show: Boolean) = if (show) View.VISIBLE else View.GONE

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
