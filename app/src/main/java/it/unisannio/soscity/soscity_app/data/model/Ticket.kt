package it.unisannio.soscity.soscity_app.data.model

data class Ticket(
    val id: String = "",
    val titolo: String = "",
    val descrizione: String = "",
    val categoria: String = "",
    val priorita: String = "",
    val stato: String = "",
    val coordinate: Coordinate = Coordinate(),
    val fotoAllegata: String? = null,
    val dataCreazione: String = "",
    val idCittadino: String = ""
)

data class Coordinate(
    val latitudine: Double = 0.0,
    val longitudine: Double = 0.0
)