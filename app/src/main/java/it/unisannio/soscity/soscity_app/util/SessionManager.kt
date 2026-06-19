package it.unisannio.soscity.soscity_app.util

import it.unisannio.soscity.soscity_app.data.model.User

object SessionManager {

    private var currentUser: User? = null

    var username: String? = null
        private set

    var password: String? = null
        private set

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun login(
        user: User,
        username: String,
        password: String
    ) {
        currentUser = user
        this.username = username
        this.password = password
    }

    fun logout() {
        currentUser = null
        username = null
        password = null
    }

    fun getUser(): User? = currentUser

    fun getUserId(): String? = currentUser?.id

    fun getUserRole(): String? = currentUser?.ruolo

    fun getUserName(): String? = currentUser?.nome
}