package it.unisannio.soscity.soscity_app.ui.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<T> : ViewModel() {

    protected val _uiState =
        MutableStateFlow<UiState<T>>(UiState.Idle)

    val uiState: StateFlow<UiState<T>> =
        _uiState
}