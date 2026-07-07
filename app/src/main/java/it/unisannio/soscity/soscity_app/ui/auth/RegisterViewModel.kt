package it.unisannio.soscity.soscity_app.ui.auth

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.data.remote.AppError
import it.unisannio.soscity.soscity_app.data.repository.FakeRepository
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel(
    private val repository: Repository = FakeRepository()
) : BaseViewModel<User>() {

    private val auth: FirebaseAuth = Firebase.auth

    fun registerUser(
        email: String,
        password: String,
        username: String,
        nome: String,
        telefono: String?
    ) {
        launchWithIdling {
            _uiState.value = UiState.Loading

            try {
                // 1. Creazione account Firebase
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser == null) {
                    _uiState.value = UiState.Error("Errore: utente Firebase non creato")
                    return@launchWithIdling
                }

                // 2. Ottieni Firebase ID Token
                val firebaseToken = firebaseUser.getIdToken(true).await().token
                if (firebaseToken == null) {
                    _uiState.value = UiState.Error("Impossibile ottenere il token Firebase")
                    return@launchWithIdling
                }

                // 3. Registrazione nel backend
                val registerRequest = RegisterRequest(
                    uid = firebaseUser.uid,
                    username = username,
                    email = email,
                    nome = nome,
                    ruolo = "CITTADINO",
                    telefono = telefono
                )

                val result = repository.register(registerRequest, firebaseToken)

                result.onSuccess { user ->
                    SessionManager.setSession(firebaseToken, user)
                    _uiState.value = UiState.Success(user)
                }.onFailure { exception ->
                    // Rollback: elimina account Firebase se la registrazione nel backend fallisce.
                    // Va eseguito a prescindere dal tipo di errore: anche se il backend ha
                    // rifiutato per sessione/token scaduto, l'account Firebase appena creato
                    // resterebbe comunque "orfano" (Firebase OK, backend KO) se non lo rimuoviamo.
                    firebaseUser.delete().await()

                    // SessionExpired qui significa che il token Firebase appena ottenuto
                    // non è stato accettato dal backend: puliamo eventuali residui di
                    // sessione prima di mostrare l'errore, così l'utente riparte da uno
                    // stato pulito al prossimo tentativo.
                    if (exception is AppError.SessionExpired) {
                        SessionManager.clearSession()
                    }
                    _uiState.value = UiState.Error(
                        exception.message ?: "Errore durante la registrazione"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    when {
                        e.message?.contains("The email address is already in use") == true ->
                            "Email già registrata"
                        e.message?.contains("The email address is badly formatted") == true ->
                            "Email non valida"
                        e.message?.contains("Password should be at least 6 characters") == true ->
                            "La password deve contenere almeno 6 caratteri"
                        else -> e.message ?: "Errore di connessione"
                    }
                )
            }
        }
    }
}