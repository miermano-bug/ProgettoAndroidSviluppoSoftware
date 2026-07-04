package it.unisannio.soscity.soscity_app.ui.tecnico

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.StatoLavoro
import it.unisannio.soscity.soscity_app.data.model.applicaBadge
import it.unisannio.soscity.soscity_app.data.model.toStripColorRes
import it.unisannio.soscity.soscity_app.databinding.ItemInterventionBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class InterventionAdapter(
    private val onCardClick: (Intervention) -> Unit,
    private val onAvvia:     (Intervention, note: String?) -> Unit,
    private val onCompleta:  (Intervention, note: String?) -> Unit
) : RecyclerView.Adapter<InterventionAdapter.ViewHolder>() {

    private val items           = mutableListOf<Intervention>()
    private val esitoPerCardId  = mutableMapOf<String, String>()

    fun submitList(nuovaLista: List<Intervention>) {
        val diff = DiffUtil.calculateDiff(DiffCallback(items, nuovaLista))
        items.clear()
        items.addAll(nuovaLista)
        diff.dispatchUpdatesTo(this)
    }

    /**
     * Mostra un messaggio di esito nella card corrispondente.
     * I messaggi non usano emoji: testo semplice.
     */
    fun mostraEsito(interventionId: String, messaggio: String) {
        esitoPerCardId[interventionId] = messaggio
        val idx = items.indexOfFirst { it.id == interventionId }
        if (idx >= 0) notifyItemChanged(idx)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInterventionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], esitoPerCardId[items[position].id])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(
        private val binding: ItemInterventionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(intervention: Intervention, esito: String?) {
            val ctx = itemView.context

            // Ticket ref abbreviata
            val ref = intervention.ticketId.takeLast(8).uppercase()
                .ifBlank { intervention.id.takeLast(8).uppercase() }
            binding.textTicketRef.text = "Ticket #$ref"

            // Team + data
            val dataFormattata = formattaData(intervention.dataInizio)
            binding.textTeamInfo.text =
                "Team ${intervention.teamId.takeLast(6)} · dal $dataFormattata"

            // Badge + striscia laterale tramite enum (nessun literal esadecimale)
            val statoEnum = StatoLavoro.fromString(intervention.statoLavoro)
            if (statoEnum != null) {
                statoEnum.applicaBadge(binding.textStatoBadge, ctx)
                binding.viewStatusStrip.setBackgroundColor(
                    ContextCompat.getColor(ctx, statoEnum.toStripColorRes())
                )
            } else {
                binding.textStatoBadge.text = intervention.statoLavoro.ifBlank { "—" }
            }

            // Bottoni azione rapida
            val azionabile = intervention.statoLavoro == StatoLavoro.PIANIFICATO.name
                    || intervention.statoLavoro == StatoLavoro.IN_CORSO.name
            binding.layoutAzioni.visibility = if (azionabile) android.view.View.VISIBLE else android.view.View.GONE
            binding.separatoreAzioni.visibility = binding.layoutAzioni.visibility
            binding.btnAvvia.visibility =
                if (intervention.statoLavoro == StatoLavoro.PIANIFICATO.name)
                    android.view.View.VISIBLE else android.view.View.GONE

            binding.btnAvvia.setOnClickListener    { onAvvia(intervention, null) }
            binding.btnCompleta.setOnClickListener { onCompleta(intervention, null) }

            // Tutta la card apre il dettaglio
            binding.cardIntervento.setOnClickListener { onCardClick(intervention) }

            // Esito (senza emoji)
            if (esito != null) {
                binding.textEsitoAzione.visibility = android.view.View.VISIBLE
                binding.textEsitoAzione.text       = esito
                // Errori contengono "non riuscito", successi contengono "aggiornato"
                val coloreEsito = if (esito.contains("non riuscito", ignoreCase = true))
                    ContextCompat.getColor(ctx, it.unisannio.soscity.soscity_app.R.color.error)
                else
                    ContextCompat.getColor(ctx, it.unisannio.soscity.soscity_app.R.color.success)
                binding.textEsitoAzione.setTextColor(coloreEsito)
            } else {
                binding.textEsitoAzione.visibility = android.view.View.GONE
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

    private class DiffCallback(
        private val old: List<Intervention>,
        private val new: List<Intervention>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(o: Int, n: Int) = old[o].id == new[n].id
        override fun areContentsTheSame(o: Int, n: Int) = old[o] == new[n]
    }
}
