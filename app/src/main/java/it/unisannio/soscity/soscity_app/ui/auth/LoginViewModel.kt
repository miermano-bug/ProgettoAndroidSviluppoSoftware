package it.unisannio.soscity.soscity_app.ui.auth

import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.data.repository.FakeRepository
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.launch

class LoginViewModel(

    private val repository: Repository =
        FakeRepository()

) : BaseViewModel<User>() {

    fun login(
        username: String,
        password: String
    ) {

        viewModelScope.launch {

            _uiState.value = UiState.Loading

            repository.login(username, password)

                .onSuccess { user ->

                    SessionManager.login(
                        user = user,
                        username = username,
                        password = password
                    )

                    _uiState.value =
                        UiState.Success(user)
                }

                .onFailure { exception ->

                    _uiState.value =
                        UiState.Error(
                            exception.message
                                ?: "Errore sconosciuto"
                        )
                }
        }
    }
}