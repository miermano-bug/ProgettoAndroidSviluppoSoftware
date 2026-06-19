package it.unisannio.soscity.soscity_app.ui.auth

import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.model.User
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.data.repository.FakeRepository
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.launch

class RegisterViewModel(

    private val repository: Repository =
        FakeRepository()

) : BaseViewModel<User>() {

    fun register(
        request: RegisterRequest
    ) {

        viewModelScope.launch {

            _uiState.value =
                UiState.Loading

            repository.register(request)

                .onSuccess { user ->

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