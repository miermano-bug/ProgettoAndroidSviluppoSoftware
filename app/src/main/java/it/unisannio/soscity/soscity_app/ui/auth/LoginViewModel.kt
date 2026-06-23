package it.unisannio.soscity.soscity_app.ui.auth

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.data.repository.FakeRepository
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel(
    private val repository: Repository = FakeRepository()
) : BaseViewModel<User>() {

    private val auth: FirebaseAuth = Firebase.auth

    fun loginWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            try {
                // 1. Login con Firebase
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    _uiState.value = UiState.Error("Errore: utente Firebase non trovato")
                    return@launch
                }

                // 2. Ottieni il token Firebase
                val firebaseToken = firebaseUser.getIdToken(true).await().token
                if (firebaseToken == null) {
                    _uiState.value = UiState.Error("Impossibile ottenere il token Firebase")
                    return@launch
                }

                // 3. Login nel backend con token e uid
                val result = repository.login(firebaseToken, firebaseUser.uid)

                result.onSuccess { user ->
                    SessionManager.setSession(firebaseToken, user)
                    _uiState.value = UiState.Success(user)
                }.onFailure { exception ->
                    _uiState.value = UiState.Error(
                        exception.message ?: "Errore durante il login"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    when {
                        e.message?.contains("The email address is badly formatted") == true ->
                            "Email non valida"
                        e.message?.contains("There is no user record") == true ->
                            "Utente non trovato"
                        e.message?.contains("The password is invalid") == true ->
                            "Password errata"
                        else -> e.message ?: "Errore di connessione"
                    }
                )
            }
        }
    }

    // Per ora lasciamo questo metodo ma lo segniamo come deprecato
    @Deprecated("Usa loginWithEmail invece", ReplaceWith("loginWithEmail(username, password)"))
    fun login(username: String, password: String) {
        loginWithEmail(username, password)
    }
}