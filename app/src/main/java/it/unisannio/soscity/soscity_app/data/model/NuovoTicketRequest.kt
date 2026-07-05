package it.unisannio.soscity.soscity_app.data.model

/**
 * Body inviato in POST /tickets. Contiene solo i campi che il contratto
 * backend si aspetta in creazione (vedi docs/API_Contract.md).
 * Ticket.kt NON va usato come body di richiesta: contiene anche id/stato/date,
 * che sono generati dal server e la cui deserializzazione (UUID, enum, LocalDateTime)
 * fallisce con 400 se il client manda stringhe vuote.
 */
data class NuovoTicketRequest(
    val titolo: String,
    val descrizione: String,
    val categoria: String,
    val priorita: String,
    val coordinate: Coordinate,
    val fotoAllegata: String? = null
)