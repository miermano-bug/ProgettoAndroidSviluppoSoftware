package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class InterventoBottomSheet : BottomSheetDialogFragment() {

    // callback invocati quando il tecnico preme Avvia/Completa nel bottom sheet
    var onAvvia:   ((note: String?) -> Unit)? = null
    var onCompleta: ((note: String?) -> Unit)? = null

    private var intervention: Intervention? = null

    companion object {
        fun newInstance(intervention: Intervention): InterventoBottomSheet {
            return InterventoBottomSheet().apply {
                this.intervention = intervention
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.bottom_sheet_intervento, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val iv = intervention ?: return

        // Riferimento ticket
        val refCorta = iv.ticketId.takeLast(8).uppercase().ifBlank { iv.id.takeLast(8).uppercase() }
        view.findViewById<TextView>(R.id.detailTicketRef).text = "Ticket #$refCorta"

        // Badge stato
        val badge = view.findViewById<TextView>(R.id.detailStatoBadge)
        badge.text = iv.statoLavoro
        applicaStileBadge(badge, iv.statoLavoro)

        // Campi info
        view.findViewById<TextView>(R.id.detailTicketId).text =
            iv.ticketId.ifBlank { "—" }
        view.findViewById<TextView>(R.id.detailTeamId).text =
            iv.teamId.ifBlank { "—" }
        view.findViewById<TextView>(R.id.detailDataInizio).text =
            formattaData(iv.dataInizio)
        view.findViewById<TextView>(R.id.detailDataFine).text =
            if (iv.dataFine.isNullOrBlank()) "In corso" else formattaData(iv.dataFine)
        view.findViewById<TextView>(R.id.detailDataCreazione).text =
            formattaData(iv.dataCreazione)
        view.findViewById<TextView>(R.id.detailNote).text =
            iv.noteIntervento.ifBlank { "Nessuna nota dell'operatore" }

        // Bottoni azioni
        val azionabile = iv.statoLavoro == "PIANIFICATO" || iv.statoLavoro == "IN_CORSO"
        val layoutAzioni  = view.findViewById<View>(R.id.layoutAzioniDetail)
        val layoutNota    = view.findViewById<TextInputLayout>(R.id.layoutNotaDetail)
        val completato    = view.findViewById<TextView>(R.id.detailCompletato)
        val btnAvvia      = view.findViewById<MaterialButton>(R.id.btnAvviaDetail)
        val btnCompleta   = view.findViewById<MaterialButton>(R.id.btnCompletaDetail)
        val editNota      = view.findViewById<TextInputEditText>(R.id.editNotaDetail)

        if (azionabile) {
            layoutAzioni.visibility = View.VISIBLE
            layoutNota.visibility   = View.VISIBLE
            completato.visibility   = View.GONE
            btnAvvia.visibility = if (iv.statoLavoro == "PIANIFICATO") View.VISIBLE else View.GONE

            btnAvvia.setOnClickListener {
                val nota = editNota.text?.toString()?.trim()?.ifEmpty { null }
                onAvvia?.invoke(nota)
                dismiss()
            }
            btnCompleta.setOnClickListener {
                val nota = editNota.text?.toString()?.trim()?.ifEmpty { null }
                onCompleta?.invoke(nota)
                dismiss()
            }
        } else {
            layoutAzioni.visibility = View.GONE
            layoutNota.visibility   = View.GONE
            completato.visibility   = if (iv.statoLavoro == "COMPLETATO") View.VISIBLE else View.GONE
        }
    }

    private fun applicaStileBadge(badge: TextView, stato: String) {
        when (stato) {
            "IN_CORSO"    -> { badge.setBackgroundResource(R.drawable.bg_status_in_corso);    badge.setTextColor(0xFF1B5E20.toInt()) }
            "PIANIFICATO" -> { badge.setBackgroundResource(R.drawable.bg_status_pianificato); badge.setTextColor(0xFFE65100.toInt()) }
            "COMPLETATO"  -> { badge.setBackgroundResource(R.drawable.bg_status_completato);  badge.setTextColor(0xFF1565C0.toInt()) }
            "SOSPESO"     -> { badge.setBackgroundResource(R.drawable.bg_status_sospeso);     badge.setTextColor(0xFFBF360C.toInt()) }
            else          -> { badge.setBackgroundResource(R.drawable.bg_status_completato);  badge.setTextColor(0xFF757575.toInt()) }
        }
    }

    private fun formattaData(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return "—"
        return try {
            val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(src))
        } catch (e: Exception) {
            isoDate
        }
    }
}
