package it.unisannio.soscity.soscity_app.data.model

import androidx.core.content.ContextCompat
import android.content.Context
import it.unisannio.soscity.soscity_app.R

/**
 * Enum che rappresenta i possibili stati di un intervento.
 * Centralizza i valori che prima erano stringhe letterali ripetute in piu' file.
 */
enum class StatoLavoro {
    PIANIFICATO,
    IN_CORSO,
    COMPLETATO,
    SOSPESO;

    companion object {
        /**
         * Converte una stringa dal backend/JSON nel corrispondente enum.
         * Restituisce null se il valore non e' riconosciuto.
         */
        fun fromString(value: String): StatoLavoro? = entries.find { it.name == value }
    }
}

/**
 * Restituisce il riferimento al colore del testo del badge per questo stato,
 * leggendo da colors.xml invece di usare literal esadecimali.
 */
fun StatoLavoro.toBadgeTextColorRes(): Int = when (this) {
    StatoLavoro.IN_CORSO    -> R.color.stato_in_corso
    StatoLavoro.PIANIFICATO -> R.color.stato_pianificato
    StatoLavoro.COMPLETATO  -> R.color.stato_completato
    StatoLavoro.SOSPESO     -> R.color.stato_sospeso
}

/**
 * Restituisce il riferimento al drawable di sfondo del badge per questo stato.
 */
fun StatoLavoro.toBadgeBackgroundRes(): Int = when (this) {
    StatoLavoro.IN_CORSO    -> R.drawable.bg_status_in_corso
    StatoLavoro.PIANIFICATO -> R.drawable.bg_status_pianificato
    StatoLavoro.COMPLETATO  -> R.drawable.bg_status_completato
    StatoLavoro.SOSPESO     -> R.drawable.bg_status_sospeso
}

/**
 * Restituisce il colore della striscia laterale nella card intervento.
 */
fun StatoLavoro.toStripColorRes(): Int = when (this) {
    StatoLavoro.IN_CORSO    -> R.color.stato_in_corso
    StatoLavoro.PIANIFICATO -> R.color.stato_pianificato
    StatoLavoro.COMPLETATO  -> R.color.stato_completato
    StatoLavoro.SOSPESO     -> R.color.stato_sospeso
}

/**
 * Peso per l'ordinamento della lista interventi: IN_CORSO prima, COMPLETATO ultimo.
 */
fun StatoLavoro.ordinePriorita(): Int = when (this) {
    StatoLavoro.IN_CORSO    -> 0
    StatoLavoro.PIANIFICATO -> 1
    StatoLavoro.SOSPESO     -> 2
    StatoLavoro.COMPLETATO  -> 3
}

/**
 * Applica testo, colore testo e sfondo badge a una TextView in modo centralizzato.
 */
fun StatoLavoro.applicaBadge(badge: android.widget.TextView, context: Context) {
    badge.text = this.name
    badge.setBackgroundResource(this.toBadgeBackgroundRes())
    badge.setTextColor(ContextCompat.getColor(context, this.toBadgeTextColorRes()))
}
