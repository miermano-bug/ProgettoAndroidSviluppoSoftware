package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import it.unisannio.soscity.soscity_app.data.model.Categoria
import it.unisannio.soscity.soscity_app.data.model.PrioritaTicket
import it.unisannio.soscity.soscity_app.data.model.StatoTicket
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.toBadgeBackgroundRes
import it.unisannio.soscity.soscity_app.data.model.toBadgeTextColorRes
import it.unisannio.soscity.soscity_app.data.model.toColorRes
import it.unisannio.soscity.soscity_app.data.model.toEtichetta
import it.unisannio.soscity.soscity_app.databinding.BottomSheetSegnalazioneBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bottom sheet di sola lettura con il dettaglio di una segnalazione del
 * cittadino, mostrato al tap su un elemento della lista in
 * LeMieSegnalazioniFragment (tab "Segnalazioni").
 *
 * Mostra tutte le informazioni del ticket TRANNE la foto allegata
 * (fotoAllegata), volutamente esclusa su richiesta.
 */
class SegnalazioneDettaglioBottomSheet : BottomSheetDialogFragment() {

    private var ticket: Ticket? = null

    private var _binding: BottomSheetSegnalazioneBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(ticket: Ticket): SegnalazioneDettaglioBottomSheet {
            return SegnalazioneDettaglioBottomSheet().apply {
                this.ticket = ticket
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSegnalazioneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val t = ticket ?: return

        binding.detailTitolo.text = t.titolo.ifBlank { "Senza titolo" }

        // Badge stato tramite enum (nessun literal esadecimale)
        val stato = StatoTicket.fromString(t.stato)
        if (stato != null) {
            binding.detailStatoBadge.text = stato.toEtichetta().uppercase()
            binding.detailStatoBadge.setBackgroundResource(stato.toBadgeBackgroundRes())
            binding.detailStatoBadge.setTextColor(
                ContextCompat.getColor(requireContext(), stato.toBadgeTextColorRes())
            )
        } else {
            binding.detailStatoBadge.text = t.stato.ifBlank { "—" }
        }

        val categoria = Categoria.fromString(t.categoria)
        binding.detailCategoria.text = categoria?.toEtichetta() ?: t.categoria.ifBlank { "—" }

        val priorita = PrioritaTicket.fromString(t.priorita)
        if (priorita != null) {
            binding.detailPriorita.text = priorita.name
            binding.detailPriorita.setTextColor(
                ContextCompat.getColor(requireContext(), priorita.toColorRes())
            )
        } else {
            binding.detailPriorita.text = t.priorita.ifBlank { "—" }
        }

        binding.detailDescrizione.text = t.descrizione.ifBlank { "Nessuna descrizione" }

        binding.detailPosizione.text = String.format(
            "%.5f, %.5f", t.coordinate.latitudine, t.coordinate.longitudine
        )

        binding.detailDataCreazione.text = formattaData(t.dataCreazione)
        binding.detailDataAggiornamento.text =
            if (t.dataAggiornamento.isBlank()) "—" else formattaData(t.dataAggiornamento)
    }

    private fun formattaData(isoDate: String): String {
        if (isoDate.isBlank()) return "—"
        return try {
            val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(src))
        } catch (e: Exception) {
            isoDate
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}