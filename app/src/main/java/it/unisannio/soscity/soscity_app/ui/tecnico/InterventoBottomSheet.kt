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

class InterventoBottomSheet : BottomSheetDialogFragment() {

    var onAvvia: ((String?) -> Unit)? = null
    var onCompleta: ((String?) -> Unit)? = null
    private var intervention: Intervention? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Safe cast per recuperare l'intervento dai parametri del fragment
            intervention = it.getSerializable(ARG_INTERVENTION) as? Intervention
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_intervento, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textRef = view.findViewById<TextView>(R.id.detailTicketRef)
        val textId = view.findViewById<TextView>(R.id.detailTicketId)
        val textTeam = view.findViewById<TextView>(R.id.detailTeamId)
        val textInizio = view.findViewById<TextView>(R.id.detailDataInizio)
        val textFine = view.findViewById<TextView>(R.id.detailDataFine)
        val textNote = view.findViewById<TextView>(R.id.detailNote)
        val textBadge = view.findViewById<TextView>(R.id.detailStatoBadge)

        val layoutNota = view.findViewById<TextInputLayout>(R.id.layoutNotaDetail)
        val editNota = view.findViewById<TextInputEditText>(R.id.editNotaDetail)
        val layoutAzioni = view.findViewById<View>(R.id.layoutAzioniDetail)
        val btnAvvia = view.findViewById<MaterialButton>(R.id.btnAvviaDetail)
        val btnCompleta = view.findViewById<MaterialButton>(R.id.btnCompletaDetail)
        val textCompletato = view.findViewById<TextView>(R.id.detailCompletato)

        intervention?.let { iv ->
            // CORREZIONE 1: Gestione sicura del ticketId se nullable usando il safe call (?.) o un fallback
            val safeTicketId = iv.ticketId ?: iv.id ?: ""
            val visualId = if (safeTicketId.length >= 8) safeTicketId.takeLast(8).uppercase() else safeTicketId.uppercase()

            textRef?.text = "Intervento #$visualId"
            textId?.text = safeTicketId
            textTeam?.text = iv.teamId
            textInizio?.text = iv.dataInizio
            textFine?.text = if (iv.dataFine.isNullOrBlank()) "In corso" else iv.dataFine
            textNote?.text = if (iv.noteIntervento.isNullOrBlank()) "Nessuna nota" else iv.noteIntervento
            textBadge?.text = iv.statoLavoro

            if (iv.statoLavoro == "COMPLETATO") {
                layoutNota?.visibility = View.GONE
                layoutAzioni?.visibility = View.GONE
                textCompletato?.visibility = View.VISIBLE
            } else {
                layoutNota?.visibility = View.VISIBLE
                layoutAzioni?.visibility = View.VISIBLE
                textCompletato?.visibility = View.GONE

                if (iv.statoLavoro == "IN_CORSO") {
                    btnAvvia?.visibility = View.GONE
                } else {
                    btnAvvia?.visibility = View.VISIBLE
                }
            }
        }

        btnAvvia?.setOnClickListener {
            val nota = editNota?.text?.toString()?.trim()?.ifEmpty { null }
            onAvvia?.invoke(nota)
            dismiss()
        }

        btnCompleta?.setOnClickListener {
            val nota = editNota?.text?.toString()?.trim()?.ifEmpty { null }
            onCompleta?.invoke(nota)
            dismiss()
        }
    }

    companion object {
        private const val ARG_INTERVENTION = "intervention"

        fun newInstance(intervention: Intervention): InterventoBottomSheet {
            val fragment = InterventoBottomSheet()
            val args = Bundle()
            // CORREZIONE 2: Cast esplicito a java.io.Serializable richiesto da putSerializable
            args.putSerializable(ARG_INTERVENTION, intervention as java.io.Serializable)
            fragment.arguments = args
            return fragment
        }
    }
}