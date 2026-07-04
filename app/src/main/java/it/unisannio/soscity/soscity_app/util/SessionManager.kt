package it.unisannio.soscity.soscity_app.util

import android.content.Context
import android.content.SharedPreferences
import it.unisannio.soscity.soscity_app.data.model.User

/**
 * Gestisce la sessione utente.
 *
 * Rispetto alla versione precedente (solo in-memory), aggiunge persistenza minima:
 * uid e ruolo vengono salvati in SharedPreferences in modo che, dopo un kill di
 * processo da parte del sistema Android, l'app possa determinare che l'utente era
 * loggato senza richiedere un nuovo login (FirebaseAuth mantiene la sessione lato SDK).
 *
 * Il token Firebase NON viene persistito (e' volatile per sicurezza): viene riottenuto
 * da AuthInterceptor a ogni richiesta tramite FirebaseAuth.currentUser.getIdToken().
 *
 * init(context) deve essere chiamato da SosCityApplication.onCreate() prima di
 * qualunque altro uso di SessionManager.
 */
object SessionManager {

    private const val PREFS_NAME  = "soscity_session"
    private const val KEY_UID     = "uid"
    private const val KEY_RUOLO   = "ruolo"

    private var prefs: SharedPreferences? = null
    private var firebaseToken: String? = null
    private var currentUser: User? = null

    /**
     * Inizializza le SharedPreferences. Deve essere chiamato una sola volta
     * da SosCityApplication.onCreate().
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isLoggedIn: Boolean
        get() = firebaseToken != null && currentUser != null

    /**
     * Restituisce true se esiste un uid salvato nelle SharedPreferences,
     * utile per decidere se ri-verificare la sessione al cold start
     * prima che setSession() venga chiamato.
     */
    fun hasSavedSession(): Boolean = prefs?.contains(KEY_UID) == true

    /**
     * Restituisce l'uid salvato nelle SharedPreferences (puo' essere null
     * se l'utente non ha mai fatto login o ha fatto logout).
     */
    fun getSavedUid(): String? = prefs?.getString(KEY_UID, null)

    /**
     * Imposta la sessione dopo login/registrazione.
     * @param firebaseToken Firebase ID Token (ottenuto da FirebaseAuth)
     * @param user Dati dell'utente (dal backend /users)
     */
    fun setSession(firebaseToken: String, user: User) {
        this.firebaseToken = firebaseToken
        this.currentUser = user
        prefs?.edit()
            ?.putString(KEY_UID, user.id)
            ?.putString(KEY_RUOLO, user.ruolo)
            ?.apply()
    }

    /**
     * Recupera il Firebase ID Token corrente.
     * @return Token come stringa, o null se non loggato
     */
    fun getToken(): String? = firebaseToken

    /**
     * Aggiorna il Firebase ID Token (usato da AuthInterceptor dopo un rinnovo).
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
     * Pulisce sia la memoria che le SharedPreferences.
     */
    fun clearSession() {
        firebaseToken = null
        currentUser = null
        prefs?.edit()?.clear()?.apply()
    }
}