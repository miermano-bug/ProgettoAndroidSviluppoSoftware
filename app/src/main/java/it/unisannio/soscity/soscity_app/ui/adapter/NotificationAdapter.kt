package it.unisannio.soscity.soscity_app.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Notification

class NotificationAdapter(private var notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tipo: TextView = view.findViewById(R.id.textTipo)
        val messaggio: TextView = view.findViewById(R.id.textMessaggio)
        val timestamp: TextView = view.findViewById(R.id.textTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notifica, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.tipo.text = notification.tipo
        holder.messaggio.text = notification.messaggio
        holder.timestamp.text = notification.timestamp
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}