package it.unisannio.soscity.soscity_app.data.model

data class RegisterRequest(

    val username: String,

    val password: String,

    val email: String,

    val nome: String,

    val telefono: String
)