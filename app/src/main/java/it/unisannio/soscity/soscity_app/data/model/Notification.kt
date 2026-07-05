package it.unisannio.soscity.soscity_app.data.model

import it.unisannio.soscity.soscity_app.R

data class Notification(
    val id: String = "",
    val tipo: String = "",
    val destinatario: String = "",
    val messaggio: String = "",
    val ticketId: String = "",
    val timestamp: String = ""
)

/**
 * Enum che rappresenta il canale di consegna di una notifica.
 * Valori allineati al campo "tipo" restituito dal notification-service
 * ("PUSH" per notifiche in-app, "EMAIL" per notifiche via email).
 */
enum class NotificaTipo {
    PUSH,
    EMAIL;

    companion object {
        fun fromString(value: String): NotificaTipo? = entries.find { it.name == value }
    }
}

fun NotificaTipo.toEtichetta(): String = when (this) {
    NotificaTipo.PUSH  -> "In-app"
    NotificaTipo.EMAIL -> "Email"
}

fun NotificaTipo.toIconRes(): Int = when (this) {
    NotificaTipo.PUSH  -> android.R.drawable.ic_dialog_info
    NotificaTipo.EMAIL -> android.R.drawable.ic_dialog_email
}

fun NotificaTipo.toBadgeBackgroundRes(): Int = when (this) {
    NotificaTipo.PUSH  -> R.drawable.bg_status_aperto
    NotificaTipo.EMAIL -> R.drawable.bg_status_risolto
}

fun NotificaTipo.toBadgeTextColorRes(): Int = when (this) {
    NotificaTipo.PUSH  -> R.color.stato_aperto
    NotificaTipo.EMAIL -> R.color.stato_risolto
}