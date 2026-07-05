package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.PrioritaTicket
import it.unisannio.soscity.soscity_app.data.model.StatoLavoro
import it.unisannio.soscity.soscity_app.data.model.applicaBadge
import it.unisannio.soscity.soscity_app.data.model.toColorRes
import it.unisannio.soscity.soscity_app.databinding.FragmentHomeTabBinding
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.performLogout
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeTabFragment : Fragment() {

    private var _binding: FragmentHomeTabBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InterventionsViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { InterventionsViewModel.Factory(RepositoryProvider.provideRepository()) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        impostaHeader()
        setupBottoni()
        osservaStato()
        osservaDettaglioTicket()

        if (viewModel.uiState.value is UiState.Idle) {
            viewModel.caricaInterventi()
        }
    }

    // ─── Header ──────────────────────────────────────────────────────────────

    private fun impostaHeader() {
        val nome = SessionManager.getUser()?.nome?.split(" ")?.firstOrNull() ?: "Tecnico"
        val saluto = when (LocalTime.now().hour) {
            in 6..11  -> getString(R.string.saluto_mattina)
            in 12..17 -> getString(R.string.saluto_pomeriggio)
            in 18..21 -> getString(R.string.saluto_sera)
            else      -> getString(R.string.saluto_notte)
        }
        binding.textSaluto.text = "$saluto, $nome!"
    }

    private fun setupBottoni() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_titolo)
                .setMessage(R.string.logout_messaggio)
                .setPositiveButton(R.string.logout_conferma) { _, _ ->
                    findNavController().performLogout()
                }
                .setNegativeButton(R.string.logout_annulla, null)
                .show()
        }

        // Il bottone impostazioni apre direttamente ImpostazioniFragment tramite Navigation
        binding.btnImpostazioni.setOnClickListener {
            findNavController().navigate(R.id.impostazioniFragment)
        }
    }

    // ─── Osservazione ViewModel ───────────────────────────────────────────────

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { stato ->
                when (stato) {
                    is UiState.Idle    -> Unit
                    is UiState.Loading -> binding.progressHome.visibility = View.VISIBLE
                    is UiState.Success -> {
                        binding.progressHome.visibility = View.GONE
                        aggiornaRiepilogo(stato.data)
                        mostraProssimoIntervento(stato.data)
                    }
                    is UiState.Error -> {
                        binding.progressHome.visibility = View.GONE
                        binding.textDescrizioneGiornata.text =
                            getString(R.string.interventi_caricamento_errore)
                    }
                }
            }
        }
    }

    private fun osservaDettaglioTicket() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dettaglioTicketState.collectLatest { stato ->
                if (_binding == null) return@collectLatest
                when (stato) {
                    is UiState.Idle, is UiState.Loading -> Unit

                    is UiState.Success -> {
                        val ticket = stato.data
                        val cat = ticket.categoria.ifBlank { null }
                        if (cat != null) {
                            binding.cardCategoria.visibility = View.VISIBLE
                            binding.cardCategoria.text = getString(R.string.categoria_formato, cat)
                        }

                        val prioritaEnum = PrioritaTicket.fromString(ticket.priorita)
                        if (prioritaEnum != null) {
                            binding.cardPriorita.visibility = View.VISIBLE
                            binding.cardPriorita.text = ticket.priorita
                            binding.cardPriorita.setTextColor(
                                ContextCompat.getColor(requireContext(), prioritaEnum.toColorRes())
                            )
                        }
                    }

                    is UiState.Error -> {
                        binding.cardCategoria.text = getString(R.string.categoria_nd)
                    }
                }
            }
        }
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────

    private fun aggiornaRiepilogo(interventi: List<Intervention>) {
        val completati = interventi.count { it.statoLavoro == StatoLavoro.COMPLETATO.name }
        val mancanti   = interventi.count {
            it.statoLavoro == StatoLavoro.IN_CORSO.name
                    || it.statoLavoro == StatoLavoro.PIANIFICATO.name
        }
        val sospesi = interventi.count { it.statoLavoro == StatoLavoro.SOSPESO.name }
        val totale  = interventi.size

        binding.textDescrizioneGiornata.text = when {
            totale == 0   -> getString(R.string.interventi_nessuno)
            mancanti == 0 -> getString(R.string.interventi_tutti_completati)
            else          -> getString(R.string.interventi_in_programma, mancanti)
        }

        binding.countCompletati.text = completati.toString()
        binding.countMancanti.text   = mancanti.toString()
        binding.countSospesi.text    = sospesi.toString()
    }

    private fun mostraProssimoIntervento(interventi: List<Intervention>) {
        val prossimo = interventi.firstOrNull { it.statoLavoro == StatoLavoro.IN_CORSO.name }
            ?: interventi.firstOrNull { it.statoLavoro == StatoLavoro.PIANIFICATO.name }

        if (prossimo == null) {
            binding.cardProssimoIntervento.visibility = View.GONE
            binding.layoutNessunProssimo.visibility   = View.VISIBLE
            return
        }

        binding.cardProssimoIntervento.visibility = View.VISIBLE
        binding.layoutNessunProssimo.visibility   = View.GONE

        val refCorta = prossimo.ticketId.takeLast(8).uppercase()
            .ifBlank { prossimo.id.takeLast(8).uppercase() }
        binding.cardTicketRef.text    = getString(R.string.ticket_ref, refCorta)
        binding.cardDataInizio.text   = formattaData(prossimo.dataInizio)

        // Badge stato tramite enum (nessun colore hardcoded)
        val statoEnum = StatoLavoro.fromString(prossimo.statoLavoro)
        if (statoEnum != null) {
            statoEnum.applicaBadge(binding.cardStatoBadge, requireContext())
        } else {
            binding.cardStatoBadge.text = prossimo.statoLavoro
        }

        // Note
        if (prossimo.noteIntervento.isNotBlank()) {
            binding.cardNote.visibility = View.VISIBLE
            binding.cardNote.text = prossimo.noteIntervento
        } else {
            binding.cardNote.visibility = View.GONE
        }

        // Bottone dettaglio
        binding.btnApriDettaglio.setOnClickListener {
            val sheet = InterventoBottomSheet.newInstance(prossimo)
            sheet.onAvvia    = { nota -> viewModel.aggiornaStato(prossimo, StatoLavoro.IN_CORSO, nota) }
            sheet.onCompleta = { nota -> viewModel.aggiornaStato(prossimo, StatoLavoro.COMPLETATO, nota) }
            sheet.show(parentFragmentManager, "dettaglio_home")
        }

        // Carica ticket per categoria + priorita' tramite il ViewModel
        viewModel.caricaDettaglioTicket(prossimo.ticketId)
    }

    private fun formattaData(isoDate: String): String {
        if (isoDate.isBlank()) return getString(R.string.dato_nd)
        return try {
            val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(src))
        } catch (e: Exception) { isoDate }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}