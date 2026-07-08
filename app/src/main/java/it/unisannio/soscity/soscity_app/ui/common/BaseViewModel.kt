package it.unisannio.soscity.soscity_app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.util.EspressoIdlingResource
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<T> : ViewModel() {

    protected val _uiState =
        MutableStateFlow<UiState<T>>(UiState.Idle)

    val uiState: StateFlow<UiState<T>> =
        _uiState

    protected fun launchWithIdling(block: suspend () -> Unit) {
        EspressoIdlingResource.increment()
        viewModelScope.launch {
            try {
                block()
            } finally {
                EspressoIdlingResource.decrement()
            }
        }
    }
}