package it.unisannio.soscity.soscity_app.ui.tecnico

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class InterventionAdapter(
    private val onCardClick: (Intervention) -> Unit,
    private val onAvvia:    (Intervention, note: String?) -> Unit,
    private val onCompleta: (Intervention, note: String?) -> Unit
) : RecyclerView.Adapter<InterventionAdapter.ViewHolder>() {

    private val items = mutableListOf<Intervention>()
    private val esitoPerCardId = mutableMapOf<String, String>()

    fun submitList(nuovaLista: List<Intervention>) {
        val diff = DiffUtil.calculateDiff(DiffCallback(items, nuovaLista))
        items.clear()
        items.addAll(nuovaLista)
        diff.dispatchUpdatesTo(this)
    }

    fun mostraEsito(interventionId: String, messaggio: String) {
        esitoPerCardId[interventionId] = messaggio
        val idx = items.indexOfFirst { it.id == interventionId }
        if (idx >= 0) notifyItemChanged(idx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_intervention, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], esitoPerCardId[items[position].id])

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardView:        View       = itemView.findViewById(R.id.cardIntervento)
        private val viewStatusStrip: View       = itemView.findViewById(R.id.viewStatusStrip)
        private val textTicketRef:   TextView   = itemView.findViewById(R.id.textTicketRef)
        private val textStatoBadge:  TextView   = itemView.findViewById(R.id.textStatoBadge)
        private val textTeamInfo:    TextView   = itemView.findViewById(R.id.textTeamInfo)
        private val layoutAzioni:    View       = itemView.findViewById(R.id.layoutAzioni)
        private val btnAvvia:        MaterialButton = itemView.findViewById(R.id.btnAvvia)
        private val btnCompleta:     MaterialButton = itemView.findViewById(R.id.btnCompleta)
        private val textEsitoAzione: TextView   = itemView.findViewById(R.id.textEsitoAzione)
        private val separatore:      View       = itemView.findViewById(R.id.separatoreAzioni)

        fun bind(intervention: Intervention, esito: String?) {
            // Ticket ref abbreviata
            val ref = intervention.ticketId.takeLast(8).uppercase().ifBlank { intervention.id.takeLast(8).uppercase() }
            textTicketRef.text = "Ticket #$ref"

            // Team + data
            val dataFormattata = formattaData(intervention.dataInizio)
            textTeamInfo.text = "Team ${intervention.teamId.takeLast(6)} · dal $dataFormattata"

            // Stile badge + striscia
            applicaStile(intervention.statoLavoro)

            // Bottoni azione rapida (senza campo nota — quello è nel bottom sheet)
            val azionabile = intervention.statoLavoro == "PIANIFICATO" || intervention.statoLavoro == "IN_CORSO"
            layoutAzioni.visibility  = if (azionabile) View.VISIBLE else View.GONE
            separatore.visibility    = if (azionabile) View.VISIBLE else View.GONE
            btnAvvia.visibility      = if (intervention.statoLavoro == "PIANIFICATO") View.VISIBLE else View.GONE

            btnAvvia.setOnClickListener    { onAvvia(intervention, null) }
            btnCompleta.setOnClickListener { onCompleta(intervention, null) }

            // Tutta la card apre il dettaglio
            cardView.setOnClickListener { onCardClick(intervention) }

            // Esito
            if (esito != null) {
                textEsitoAzione.visibility = View.VISIBLE
                textEsitoAzione.text = esito
                textEsitoAzione.setTextColor(
                    if (esito.startsWith("❌")) 0xFFC62828.toInt() else 0xFF1B5E20.toInt()
                )
            } else {
                textEsitoAzione.visibility = View.GONE
            }
        }

        private fun applicaStile(stato: String) {
            textStatoBadge.text = stato.ifBlank { "—" }
            when (stato) {
                "IN_CORSO"    -> { textStatoBadge.setBackgroundResource(R.drawable.bg_status_in_corso);    textStatoBadge.setTextColor(0xFF1B5E20.toInt()); viewStatusStrip.setBackgroundColor(0xFF2E7D32.toInt()) }
                "PIANIFICATO" -> { textStatoBadge.setBackgroundResource(R.drawable.bg_status_pianificato); textStatoBadge.setTextColor(0xFFE65100.toInt()); viewStatusStrip.setBackgroundColor(0xFFE65100.toInt()) }
                "COMPLETATO"  -> { textStatoBadge.setBackgroundResource(R.drawable.bg_status_completato);  textStatoBadge.setTextColor(0xFF1565C0.toInt()); viewStatusStrip.setBackgroundColor(0xFF1565C0.toInt()) }
                "SOSPESO"     -> { textStatoBadge.setBackgroundResource(R.drawable.bg_status_sospeso);     textStatoBadge.setTextColor(0xFFBF360C.toInt()); viewStatusStrip.setBackgroundColor(0xFFBF360C.toInt()) }
                else          -> { textStatoBadge.setBackgroundResource(R.drawable.bg_status_completato);  textStatoBadge.setTextColor(0xFF757575.toInt()); viewStatusStrip.setBackgroundColor(0xFF9E9E9E.toInt()) }
            }
        }

        private fun formattaData(isoDate: String): String {
            if (isoDate.isBlank()) return "n.d."
            return try {
                val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
                DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(src))
            } catch (e: Exception) { isoDate }
        }
    }

    private class DiffCallback(private val old: List<Intervention>, private val new: List<Intervention>) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(o: Int, n: Int) = old[o].id == new[n].id
        override fun areContentsTheSame(o: Int, n: Int) = old[o] == new[n]
    }
}
