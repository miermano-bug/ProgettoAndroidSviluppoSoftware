package it.unisannio.soscity.soscity_app.data.model

import it.unisannio.soscity.soscity_app.R

/**
 * Enum che rappresenta le categorie di una segnalazione (ticket).
 * Valori allineati all'enum Categoria del modulo shared-common del backend.
 */
enum class Categoria {
    ILLUMINAZIONE,
    VERDE_URBANO,
    ARREDO_URBANO,
    EDIFICI,
    EMERGENZA;

    companion object {
        fun fromString(value: String): Categoria? = entries.find { it.name == value }
    }
}

fun Categoria.toEtichetta(): String = when (this) {
    Categoria.ILLUMINAZIONE -> "Illuminazione"
    Categoria.VERDE_URBANO  -> "Verde Urbano"
    Categoria.ARREDO_URBANO -> "Arredo Urbano"
    Categoria.EDIFICI       -> "Edifici"
    Categoria.EMERGENZA     -> "Emergenza"
}

fun Categoria.toIconRes(): Int = when (this) {
    Categoria.ILLUMINAZIONE -> R.drawable.ic_categoria_illuminazione
    Categoria.VERDE_URBANO  -> R.drawable.ic_categoria_verde_urbano
    Categoria.ARREDO_URBANO -> R.drawable.ic_categoria_arredo_urbano
    Categoria.EDIFICI       -> R.drawable.ic_categoria_edifici
    Categoria.EMERGENZA     -> R.drawable.ic_categoria_emergenza
}