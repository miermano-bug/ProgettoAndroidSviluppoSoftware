package it.unisannio.soscity.soscity_app.util

import it.unisannio.soscity.soscity_app.R

/**
 * Restituisce l'id di destinazione (nav graph) corrispondente alla home del ruolo,
 * o null se il ruolo non è supportato.
 *
 * Punto unico dove vive la lista dei ruoli con una home dedicata: se in futuro si
 * aggiunge un quarto ruolo, va aggiornato solo qui (prima era duplicato in 3 punti
 * tra LoginFragment e RegisterFragment).
 *
 * NB: "OPERATORE" non è incluso deliberatamente, ha una dashboard web separata
 * e non una home in questa app (vedi OVERVIEW.md).
 */
fun roleHomeDestination(ruolo: String): Int? = when (ruolo) {
    "CITTADINO" -> R.id.cittadinoContainerFragment
    "TECNICO" -> R.id.technicianHomeFragment
    else -> null
}