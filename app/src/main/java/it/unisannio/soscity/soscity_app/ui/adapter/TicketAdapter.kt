package it.unisannio.soscity.soscity_app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Categoria
import it.unisannio.soscity.soscity_app.data.model.StatoTicket
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.toBadgeBackgroundRes
import it.unisannio.soscity.soscity_app.data.model.toBadgeTextColorRes
import it.unisannio.soscity.soscity_app.data.model.toEtichetta
import it.unisannio.soscity.soscity_app.data.model.toIconRes
import it.unisannio.soscity.soscity_app.databinding.ItemTicketBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Adapter per la lista "Le mie segnalazioni" del Cittadino.
 * Usa ViewBinding (ItemTicketBinding) e le enum Categoria/StatoTicket per
 * evitare literal di stringa sparsi nel bind (stesso pattern di InterventionAdapter).
 *
 * onItemClick viene invocato al tap su una card, per mostrare il dettaglio
 * della segnalazione (vedi SegnalazioneDettaglioBottomSheet).
 */
class TicketAdapter(
    private var tickets: List<Ticket> = emptyList(),
    private val onItemClick: (Ticket) -> Unit = {}
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    fun updateData(newTickets: List<Ticket>) {
        val diff = DiffUtil.calculateDiff(DiffCallback(tickets, newTickets))
        tickets = newTickets
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val binding = ItemTicketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TicketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        holder.bind(tickets[position])
    }

    override fun getItemCount() = tickets.size

    inner class TicketViewHolder(
        private val binding: ItemTicketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ticket: Ticket) {
            val ctx = itemView.context

            binding.textTitolo.text = ticket.titolo
            binding.textData.text = "Creato il ${formattaData(ticket.dataCreazione)}"

            val categoria: Categoria? = Categoria.fromString(ticket.categoria)
            if (categoria != null) {
                binding.textCategoria.text = categoria.toEtichetta()
                binding.imageCategoria.setImageResource(categoria.toIconRes())
            } else {
                binding.textCategoria.text = ticket.categoria.ifBlank { "—" }
                binding.imageCategoria.setImageResource(R.drawable.ic_nav_segnalazioni)
            }

            val stato: StatoTicket? = StatoTicket.fromString(ticket.stato)
            if (stato != null) {
                binding.textStato.text = stato.toEtichetta()
                binding.textStato.setBackgroundResource(stato.toBadgeBackgroundRes())
                binding.textStato.setTextColor(ContextCompat.getColor(ctx, stato.toBadgeTextColorRes()))
            } else {
                binding.textStato.text = ticket.stato.ifBlank { "—" }
            }

            itemView.setOnClickListener {
                onItemClick(ticket)
            }
        }

        private fun formattaData(isoDate: String): String {
            if (isoDate.isBlank()) return "n.d."
            return try {
                val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
                DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(src))
            } catch (e: Exception) {
                isoDate
            }
        }
    }

    private class DiffCallback(
        private val old: List<Ticket>,
        private val new: List<Ticket>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition].id == new[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] == new[newItemPosition]
    }
}