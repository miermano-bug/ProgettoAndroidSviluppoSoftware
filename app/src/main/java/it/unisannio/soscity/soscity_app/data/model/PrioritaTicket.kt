package it.unisannio.soscity.soscity_app.data.model

import it.unisannio.soscity.soscity_app.R

/**
 * Enum che rappresenta i livelli di priorita' di un ticket.
 * Centralizza i valori che prima erano stringhe letterali in HomeTabFragment.
 */
enum class PrioritaTicket {
    URGENTE,
    ALTA,
    MEDIA,
    BASSA;

    companion object {
        /**
         * Converte una stringa dal backend nel corrispondente enum.
         * Restituisce null se il valore non e' riconosciuto.
         */
        fun fromString(value: String): PrioritaTicket? = entries.find { it.name == value }
    }
}

/**
 * Restituisce il riferimento al colore associato a questa priorita',
 * leggendo da colors.xml.
 */
fun PrioritaTicket.toColorRes(): Int = when (this) {
    PrioritaTicket.URGENTE -> R.color.stato_sospeso
    PrioritaTicket.ALTA    -> R.color.stato_pianificato
    PrioritaTicket.MEDIA   -> R.color.stato_completato
    PrioritaTicket.BASSA   -> R.color.primary
}
