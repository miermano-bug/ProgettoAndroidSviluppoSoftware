package it.unisannio.soscity.soscity_app.data.model

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val nome: String = "",
    val ruolo: String = "",
    val telefono: String? = null,
    val idTeam: String? = null,
    val competenze: List<String>? = null,
    val disponibile: Boolean? = null
)