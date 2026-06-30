package it.unisannio.soscity.soscity_app.ui.tecnico

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Stato testuale puro dei possibili valori di "statoLavoro".
 * Se il backend introduce nuovi stati, qui vanno aggiunti i relativi
 * colori/badge — altrimenti finiscono nel ramo "else" (badge neutro).
 */
private const val STATO_PIANIFICATO = "PIANIFICATO"
private const val STATO_IN_CORSO = "IN_CORSO"
private const val STATO_COMPLETATO = "COMPLETATO"

class InterventionAdapter(
    private val onAvvia: (Intervention, note: String?) -> Unit,
    private val onCompleta: (Intervention, note: String?) -> Unit
) : RecyclerView.Adapter<InterventionAdapter.InterventionViewHolder>() {

    private val items = mutableListOf<Intervention>()

    // Messaggi di esito temporanei per-card (es. "✅ Stato aggiornato"),
    // mantenuti qui (non nel ViewHolder) perché la view viene riciclata.
    private val esitoPerCardId = mutableMapOf<String, String>()

    fun submitList(nuovaLista: List<Intervention>) {
        val diff = DiffUtil.calculateDiff(
            InterventionDiffCallback(items, nuovaLista)
        )
        items.clear()
        items.addAll(nuovaLista)
        diff.dispatchUpdatesTo(this)
    }

    fun mostraEsito(interventionId: String, messaggio: String) {
        esitoPerCardId[interventionId] = messaggio
        val index = items.indexOfFirst { it.id == interventionId }
        if (index >= 0) notifyItemChanged(index)
    }

    fun impostaCaricamento(interventionId: String, inCorso: Boolean) {
        val index = items.indexOfFirst { it.id == interventionId }
        if (index >= 0) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): InterventionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_intervention, parent, false)
        return InterventionViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterventionViewHolder, position: Int) {
        holder.bind(items[position], esitoPerCardId[items[position].id])
    }

    override fun getItemCount(): Int = items.size

    inner class InterventionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textTicketRef: TextView = itemView.findViewById(R.id.textTicketRef)
        private val textStatoBadge: TextView = itemView.findViewById(R.id.textStatoBadge)
        private val textTeamInfo: TextView = itemView.findViewById(R.id.textTeamInfo)
        private val textNoteIntervento: TextView = itemView.findViewById(R.id.textNoteIntervento)
        private val layoutNota: View = itemView.findViewById(R.id.layoutNota)
        private val editNota: TextInputEditText = itemView.findViewById(R.id.editNota)
        private val layoutAzioni: LinearLayout = itemView.findViewById(R.id.layoutAzioni)
        private val btnAvvia: Button = itemView.findViewById(R.id.btnAvvia)
        private val btnCompleta: Button = itemView.findViewById(R.id.btnCompleta)
        private val textEsitoAzione: TextView = itemView.findViewById(R.id.textEsitoAzione)

        fun bind(intervention: Intervention, esito: String?) {
            textTicketRef.text = "Ticket #${intervention.ticketId.ifBlank { intervention.id }}"
            textTeamInfo.text = "Team ${intervention.teamId} · avviato il ${formattaData(intervention.dataInizio)}"

            if (intervention.noteIntervento.isNotBlank()) {
                textNoteIntervento.visibility = View.VISIBLE
                textNoteIntervento.text = intervention.noteIntervento
            } else {
                textNoteIntervento.visibility = View.GONE
            }

            applicaBadgeStato(intervention.statoLavoro)

            // Le azioni (e il campo nota) sono disponibili solo per gli stati
            // su cui il tecnico può ancora intervenire. Un intervento COMPLETATO
            // resta visibile in lista ma è di sola consultazione.
            val azionabile = intervention.statoLavoro == STATO_PIANIFICATO ||
                    intervention.statoLavoro == STATO_IN_CORSO

            layoutAzioni.visibility = if (azionabile) View.VISIBLE else View.GONE
            layoutNota.visibility = if (azionabile) View.VISIBLE else View.GONE

            // "Avvia" ha senso solo da PIANIFICATO; da IN_CORSO è già avviato.
            btnAvvia.visibility =
                if (intervention.statoLavoro == STATO_PIANIFICATO) View.VISIBLE else View.GONE
            btnAvvia.setOnClickListener {
                val nota = editNota.text?.toString()?.trim()?.ifEmpty { null }
                onAvvia(intervention, nota)
            }
            btnCompleta.setOnClickListener {
                val nota = editNota.text?.toString()?.trim()?.ifEmpty { null }
                onCompleta(intervention, nota)
            }

            if (esito != null) {
                textEsitoAzione.visibility = View.VISIBLE
                textEsitoAzione.text = esito
                textEsitoAzione.setTextColor(
                    if (esito.startsWith("❌")) 0xFFC62828.toInt() else 0xFF1F6B33.toInt()
                )
            } else {
                textEsitoAzione.visibility = View.GONE
            }
        }

        private fun applicaBadgeStato(stato: String) {
            textStatoBadge.text = stato.ifBlank { "—" }
            when (stato) {
                STATO_IN_CORSO -> {
                    textStatoBadge.setBackgroundResource(R.drawable.bg_status_in_corso)
                    textStatoBadge.setTextColor(0xFF1F6B33.toInt())
                }
                STATO_PIANIFICATO -> {
                    textStatoBadge.setBackgroundResource(R.drawable.bg_status_pianificato)
                    textStatoBadge.setTextColor(0xFF8A6D1D.toInt())
                }
                STATO_COMPLETATO -> {
                    textStatoBadge.setBackgroundResource(R.drawable.bg_status_completato)
                    textStatoBadge.setTextColor(0xFF3949AB.toInt())
                }
                else -> {
                    textStatoBadge.setBackgroundResource(R.drawable.bg_status_completato)
                    textStatoBadge.setTextColor(0xFF424242.toInt())
                }
            }
        }

        /**
         * Le date arrivano come stringa ISO-8601 (es. "2026-05-26T12:00:00").
         * Se il formato non fosse parsabile (es. il backend lo cambia), si
         * mostra la stringa originale invece di far crashare la UI.
         */
        private fun formattaData(isoDate: String): String {
            if (isoDate.isBlank()) return "data non disponibile"
            return try {
                val instant = Instant.parse(
                    if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
                )
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            } catch (e: Exception) {
                isoDate
            }
        }
    }

    private class InterventionDiffCallback(
        private val old: List<Intervention>,
        private val new: List<Intervention>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition].id == new[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            old[oldItemPosition] == new[newItemPosition]
    }
}
