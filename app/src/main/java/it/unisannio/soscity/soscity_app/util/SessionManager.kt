package it.unisannio.soscity.soscity_app.util

import it.unisannio.soscity.soscity_app.data.model.User

/**
 * Gestisce la sessione utente.
 * NOTA: il token è il Firebase ID Token, non un JWT generato dal backend.
 */
object SessionManager {

    private var firebaseToken: String? = null  // Firebase ID Token
    private var currentUser: User? = null

    val isLoggedIn: Boolean
        get() = firebaseToken != null && currentUser != null

    /**
     * Imposta la sessione dopo login/registrazione.
     * @param firebaseToken Firebase ID Token (ottenuto da FirebaseAuth)
     * @param user Dati dell'utente (dal backend /users)
     */
    fun setSession(firebaseToken: String, user: User) {
        this.firebaseToken = firebaseToken
        this.currentUser = user
    }

    /**
     * Recupera il Firebase ID Token corrente.
     * @return Token come stringa, o null se non loggato
     */
    fun getToken(): String? = firebaseToken

    /**
     * Aggiorna il Firebase ID Token (es. dopo refresh).
     */
    fun updateToken(newToken: String) {
        this.firebaseToken = newToken
    }

    /**
     * Recupera l'utente corrente.
     */
    fun getUser(): User? = currentUser

    /**
     * Recupera l'ID dell'utente corrente (UID Firebase).
     */
    fun getUserId(): String? = currentUser?.id

    /**
     * Recupera il ruolo dell'utente corrente.
     */
    fun getUserRole(): String? = currentUser?.ruolo

    /**
     * Recupera il nome dell'utente corrente.
     */
    fun getUserName(): String? = currentUser?.nome

    /**
     * Termina la sessione (logout).
     */
    fun clearSession() {
        firebaseToken = null
        currentUser = null
    }
}