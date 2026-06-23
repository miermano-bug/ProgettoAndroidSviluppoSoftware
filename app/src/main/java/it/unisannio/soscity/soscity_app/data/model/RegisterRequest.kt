package it.unisannio.soscity.soscity_app.data.model

data class RegisterRequest(
    val uid: String,                // Firebase UID
    val username: String,
    val email: String,
    val nome: String,
    val ruolo: String? = "CITTADINO",
    val telefono: String? = null
)