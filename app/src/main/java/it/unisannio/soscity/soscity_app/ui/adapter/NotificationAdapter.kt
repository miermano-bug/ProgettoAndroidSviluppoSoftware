package it.unisannio.soscity.soscity_app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.model.NotificaTipo
import it.unisannio.soscity.soscity_app.data.model.toBadgeBackgroundRes
import it.unisannio.soscity.soscity_app.data.model.toBadgeTextColorRes
import it.unisannio.soscity.soscity_app.data.model.toEtichetta
import it.unisannio.soscity.soscity_app.data.model.toIconRes
import it.unisannio.soscity.soscity_app.databinding.ItemNotificaBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Adapter per la lista "Le mie notifiche" del Cittadino.
 * Stesso pattern (ViewBinding + DiffUtil + enum) di TicketAdapter/InterventionAdapter.
 */
class NotificationAdapter(
    private var notifications: List<Notification> = emptyList()
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    fun updateData(newNotifications: List<Notification>) {
        val diff = DiffUtil.calculateDiff(DiffCallback(notifications, newNotifications))
        notifications = newNotifications
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificaBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    inner class NotificationViewHolder(
        private val binding: ItemNotificaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            val ctx = itemView.context

            binding.textMessaggio.text = notification.messaggio
            binding.textTimestamp.text = formattaData(notification.timestamp)

            val tipoEnum = NotificaTipo.fromString(notification.tipo)
            if (tipoEnum != null) {
                binding.textTipo.text = tipoEnum.toEtichetta()
                binding.imageTipo.setImageResource(tipoEnum.toIconRes())
                binding.textTipo.setBackgroundResource(tipoEnum.toBadgeBackgroundRes())
                binding.textTipo.setTextColor(ContextCompat.getColor(ctx, tipoEnum.toBadgeTextColorRes()))
            } else {
                binding.textTipo.text = notification.tipo.ifBlank { "—" }
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
        private val old: List<Notification>,
        private val new: List<Notification>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition].id == new[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            old[oldItemPosition] == new[newItemPosition]
    }
}