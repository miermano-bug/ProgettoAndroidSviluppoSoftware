package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.StatoLavoro
import it.unisannio.soscity.soscity_app.data.model.applicaBadge
import it.unisannio.soscity.soscity_app.databinding.BottomSheetInterventoBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class InterventoBottomSheet : BottomSheetDialogFragment() {

    var onAvvia:    ((note: String?) -> Unit)? = null
    var onCompleta: ((note: String?) -> Unit)? = null

    private var intervention: Intervention? = null

    private var _binding: BottomSheetInterventoBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance(intervention: Intervention): InterventoBottomSheet {
            return InterventoBottomSheet().apply {
                this.intervention = intervention
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetInterventoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val iv = intervention ?: return

        val refCorta = iv.ticketId.takeLast(8).uppercase()
            .ifBlank { iv.id.takeLast(8).uppercase() }
        binding.detailTicketRef.text = getString(R.string.ticket_ref, refCorta)

        // Badge stato tramite enum (nessun literal esadecimale)
        val statoEnum = StatoLavoro.fromString(iv.statoLavoro)
        if (statoEnum != null) {
            statoEnum.applicaBadge(binding.detailStatoBadge, requireContext())
        } else {
            binding.detailStatoBadge.text = iv.statoLavoro
        }

        // Campi info
        binding.detailTicketId.text =
            iv.ticketId.ifBlank { getString(R.string.dettaglio_valore_vuoto) }
        binding.detailTeamId.text =
            iv.teamId.ifBlank { getString(R.string.dettaglio_valore_vuoto) }
        binding.detailDataInizio.text    = formattaData(iv.dataInizio)
        binding.detailDataFine.text      =
            if (iv.dataFine.isNullOrBlank()) getString(R.string.dettaglio_fine_in_corso)
            else formattaData(iv.dataFine)
        binding.detailDataCreazione.text = formattaData(iv.dataCreazione)
        binding.detailNote.text =
            iv.noteIntervento.ifBlank { getString(R.string.dettaglio_nessuna_nota) }

        // Bottoni azioni
        val azionabile = iv.statoLavoro == StatoLavoro.PIANIFICATO.name
                || iv.statoLavoro == StatoLavoro.IN_CORSO.name

        if (azionabile) {
            binding.layoutAzioniDetail.visibility = View.VISIBLE
            binding.layoutNotaDetail.visibility   = View.VISIBLE
            binding.detailCompletato.visibility   = View.GONE
            binding.btnAvviaDetail.visibility =
                if (iv.statoLavoro == StatoLavoro.PIANIFICATO.name) View.VISIBLE else View.GONE

            binding.btnAvviaDetail.setOnClickListener {
                val nota = binding.editNotaDetail.text?.toString()?.trim()?.ifEmpty { null }
                onAvvia?.invoke(nota)
                dismiss()
            }
            binding.btnCompletaDetail.setOnClickListener {
                val nota = binding.editNotaDetail.text?.toString()?.trim()?.ifEmpty { null }
                onCompleta?.invoke(nota)
                dismiss()
            }
        } else {
            binding.layoutAzioniDetail.visibility = View.GONE
            binding.layoutNotaDetail.visibility   = View.GONE
            binding.detailCompletato.visibility   =
                if (iv.statoLavoro == StatoLavoro.COMPLETATO.name) View.VISIBLE else View.GONE
        }
    }

    private fun formattaData(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return getString(R.string.dettaglio_valore_vuoto)
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
