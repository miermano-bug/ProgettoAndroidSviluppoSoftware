package it.unisannio.soscity.soscity_app.data.model

import com.google.gson.annotations.SerializedName

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
    val dataAggiornamento: String = "",
    // Il backend usa "id_cittadino" (snake_case) nella risposta JSON, non "idCittadino".
    // Senza @SerializedName questo campo sarebbe sempre rimasto vuoto in lettura.
    @SerializedName("id_cittadino")
    val idCittadino: String = ""
)

data class Coordinate(
    val latitudine: Double = 0.0,
    val longitudine: Double = 0.0
)