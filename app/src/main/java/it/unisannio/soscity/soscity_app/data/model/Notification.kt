package it.unisannio.soscity.soscity_app.data.model

data class Notification(
    val id: String = "",
    val tipo: String = "",
    val destinatario: String = "",
    val messaggio: String = "",
    val ticketId: String = "",
    val timestamp: String = ""
)