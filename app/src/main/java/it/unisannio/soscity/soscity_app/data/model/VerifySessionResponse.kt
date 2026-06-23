package it.unisannio.soscity.soscity_app.data.model

data class VerifySessionResponse(
    val valida: Boolean,
    val userId: String? = null,
    val ruolo: String? = null
)