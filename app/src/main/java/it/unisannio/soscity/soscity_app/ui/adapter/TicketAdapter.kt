package it.unisannio.soscity.soscity_app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Ticket

class TicketAdapter(private var tickets: List<Ticket>) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    class TicketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titolo: TextView = view.findViewById(R.id.textTitolo)
        val categoria: TextView = view.findViewById(R.id.textCategoria)
        val stato: TextView = view.findViewById(R.id.textStato)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val ticket = tickets[position]
        holder.titolo.text = ticket.titolo
        holder.categoria.text = ticket.categoria
        holder.stato.text = ticket.stato
    }

    override fun getItemCount() = tickets.size

    fun updateData(newTickets: List<Ticket>) {
        tickets = newTickets
        notifyDataSetChanged()
    }
}