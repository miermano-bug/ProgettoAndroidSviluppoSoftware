package it.unisannio.soscity.soscity_app.ui.tecnico

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val STATO_PIANIFICATO = "PIANIFICATO"
private const val STATO_IN_CORSO = "IN_CORSO"
private const val STATO_COMPLETATO = "COMPLETATO"

class InterventionAdapter(
    private val onAvvia: (Intervention, note: String?) -> Unit,
    private val onCompleta: (Intervention, note: String?) -> Unit
) : RecyclerView.Adapter<InterventionAdapter.InterventionViewHolder>() {

    private val items = mutableListOf<Intervention>()
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

        private val textTicketRef: TextView? = itemView.findViewById(R.id.textTicketRef)
        private val textStatoBadge: TextView? = itemView.findViewById(R.id.textStatoBadge)
        private val textTeamInfo: TextView? = itemView.findViewById(R.id.textTeamInfo)
        private val layoutAzioni: LinearLayout? = itemView.findViewById(R.id.layoutAzioni)
        private val btnAvvia: Button? = itemView.findViewById(R.id.btnAvvia)
        private val btnCompleta: Button? = itemView.findViewById(R.id.btnCompleta)
        private val textEsitoAzione: TextView? = itemView.findViewById(R.id.textEsitoAzione)
        private val separatoreAzioni: View? = itemView.findViewById(R.id.separatoreAzioni)

        fun bind(intervention: Intervention, esito: String?) {
            textTicketRef?.text = "Ticket #${intervention.ticketId.ifBlank { intervention.id }}"
            textTeamInfo?.text = "Team ${intervention.teamId} · avviato il ${formattaData(intervention.dataInizio)}"

            applicaBadgeStato(intervention.statoLavoro)

            // Verifica se l'intervento è in uno stato gestibile rapidamente da card
            val azionabile = intervention.statoLavoro == STATO_PIANIFICATO ||
                    intervention.statoLavoro == STATO_IN_CORSO

            layoutAzioni?.visibility = if (azionabile) View.VISIBLE else View.GONE
            separatoreAzioni?.visibility = if (azionabile) View.VISIBLE else View.GONE

            // Il pulsante "Avvia" compare solo se è ancora pianificato
            btnAvvia?.visibility =
                if (intervention.statoLavoro == STATO_PIANIFICATO) View.VISIBLE else View.GONE

            btnAvvia?.setOnClickListener {
                // Sulla card non c'è la EditText delle note, passiamo null (le note si inseriscono dal BottomSheet)
                onAvvia(intervention, null)
            }

            btnCompleta?.setOnClickListener {
                onCompleta(intervention, null)
            }

            if (esito != null) {
                textEsitoAzione?.visibility = View.VISIBLE
                textEsitoAzione?.text = esito
                textEsitoAzione?.setTextColor(
                    if (esito.startsWith("❌")) 0xFFC62828.toInt() else 0xFF1F6B33.toInt()
                )
            } else {
                textEsitoAzione?.visibility = View.GONE
            }
        }

        private fun applicaBadgeStato(stato: String) {
            if (textStatoBadge == null) return
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