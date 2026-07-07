package it.unisannio.soscity.soscity_app.ui.common
import it.unisannio.soscity.soscity_app.util.EspressoIdlingResource
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModel<T> : ViewModel() {

    protected val _uiState =
        MutableStateFlow<UiState<T>>(UiState.Idle)

    val uiState: StateFlow<UiState<T>> =
        _uiState

    protected fun launchWithIdling(block: suspend () -> Unit) {
        EspressoIdlingResource.increment()
        launchWithIdling {
            try {
                block()
            } finally {
                EspressoIdlingResource.decrement()
            }
        }
    }


}