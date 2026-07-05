package it.unisannio.soscity.soscity_app.data.model

import it.unisannio.soscity.soscity_app.R

/**
 * Enum che rappresenta gli stati del ciclo di vita di un ticket.
 * Valori allineati all'enum StatoTicket del modulo shared-common del backend
 * (vedi docs/API_Contract.md - Ticket Lifecycle).
 */
enum class StatoTicket {
    APERTO,
    IN_VALUTAZIONE,
    ASSEGNATO,
    IN_LAVORAZIONE,
    RISOLTO,
    RESPINTO,
    CHIUSO;

    companion object {
        fun fromString(value: String): StatoTicket? = entries.find { it.name == value }
    }
}

fun StatoTicket.toEtichetta(): String = when (this) {
    StatoTicket.APERTO         -> "Aperto"
    StatoTicket.IN_VALUTAZIONE -> "In valutazione"
    StatoTicket.ASSEGNATO      -> "Assegnato"
    StatoTicket.IN_LAVORAZIONE -> "In lavorazione"
    StatoTicket.RISOLTO        -> "Risolto"
    StatoTicket.RESPINTO       -> "Respinto"
    StatoTicket.CHIUSO         -> "Chiuso"
}

fun StatoTicket.toBadgeBackgroundRes(): Int = when (this) {
    StatoTicket.APERTO         -> R.drawable.bg_status_aperto
    StatoTicket.IN_VALUTAZIONE -> R.drawable.bg_status_in_valutazione
    StatoTicket.ASSEGNATO      -> R.drawable.bg_status_assegnato
    StatoTicket.IN_LAVORAZIONE -> R.drawable.bg_status_in_lavorazione
    StatoTicket.RISOLTO        -> R.drawable.bg_status_risolto
    StatoTicket.RESPINTO       -> R.drawable.bg_status_respinto
    StatoTicket.CHIUSO         -> R.drawable.bg_status_chiuso
}

fun StatoTicket.toBadgeTextColorRes(): Int = when (this) {
    StatoTicket.APERTO         -> R.color.stato_aperto
    StatoTicket.IN_VALUTAZIONE -> R.color.stato_in_valutazione
    StatoTicket.ASSEGNATO      -> R.color.stato_assegnato
    StatoTicket.IN_LAVORAZIONE -> R.color.stato_in_lavorazione
    StatoTicket.RISOLTO        -> R.color.stato_risolto
    StatoTicket.RESPINTO       -> R.color.stato_respinto
    StatoTicket.CHIUSO         -> R.color.stato_chiuso
}