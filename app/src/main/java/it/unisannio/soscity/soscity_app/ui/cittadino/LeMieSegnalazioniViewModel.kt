package it.unisannio.soscity.soscity_app.ui.cittadino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.launch

/**
 * ViewModel per la tab "Segnalazioni" del Cittadino.
 *
 * Sostituisce la logica che prima viveva direttamente in
 * LeMieSegnalazioniFragment (chiamata al Repository dentro la View,
 * violazione MVVM segnalata nell'analisi): la Fragment ora si limita a
 * osservare uiState e a invocare caricaSegnalazioni()/refresh(), sullo
 * stesso pattern gia' usato da InterventionsViewModel lato Tecnico.
 */
class LeMieSegnalazioniViewModel(
    private val repository: Repository
) : BaseViewModel<List<Ticket>>() {

    /**
     * Carica le segnalazioni del cittadino autenticato.
     * Emette Loading -> Success(lista) oppure Loading -> Error.
     */
    fun caricaSegnalazioni() {
        _uiState.value = UiState.Loading
        launchWithIdling {
            repository.getMyTickets()
                .onSuccess { lista ->
                    _uiState.value = UiState.Success(ordinaPerData(lista))
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Errore nel recupero delle segnalazioni"
                    )
                }
        }
    }

    /**
     * Usato dal pull-to-refresh: ricarica senza passare da uno stato Loading
     * "pieno" (che nasconderebbe la lista attuale sotto una progress bar),
     * lasciando lo SwipeRefreshLayout mostrare il proprio indicatore.
     */
    fun refresh() {
        launchWithIdling {
            repository.getMyTickets()
                .onSuccess { lista ->
                    _uiState.value = UiState.Success(ordinaPerData(lista))
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Errore nel recupero delle segnalazioni"
                    )
                }
        }
    }

    /** Segnalazioni piu' recenti prima, in base alla data di creazione ISO. */
    private fun ordinaPerData(lista: List<Ticket>): List<Ticket> =
        lista.sortedByDescending { it.dataCreazione }

    class Factory(private val repository: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LeMieSegnalazioniViewModel::class.java)) {
                return LeMieSegnalazioniViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}