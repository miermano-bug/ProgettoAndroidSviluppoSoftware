package it.unisannio.soscity.soscity_app.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Impostazioni. Sostituisce la chiamata diretta
 * al Repository che prima avveniva dentro il click listener di "Sincronizza"
 * in ImpostazioniFragment (violazione MVVM segnalata nell'analisi).
 */
class ImpostazioniViewModel(
    private val repository: Repository
) : BaseViewModel<Unit>() {

    fun sincronizzaDati() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            repository.getMyInterventions()
                .onSuccess { _uiState.value = UiState.Success(Unit) }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Errore di rete"
                    )
                }
        }
    }

    class Factory(private val repository: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ImpostazioniViewModel::class.java)) {
                return ImpostazioniViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}