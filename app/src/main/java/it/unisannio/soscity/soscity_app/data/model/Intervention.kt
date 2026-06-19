package it.unisannio.soscity.soscity_app.data.model

data class Intervention(
    val id: String = "",
    val ticketId: String = "",
    val teamId: String = "",
    val tecnicoId: String = "",
    val statoLavoro: String = "",
    val noteIntervento: String = "",
    val dataInizio: String = "",
    val dataFine: String? = null,
    val dataCreazione: String = ""
)