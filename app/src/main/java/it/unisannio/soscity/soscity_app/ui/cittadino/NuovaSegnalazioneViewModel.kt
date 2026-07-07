package it.unisannio.soscity.soscity_app.ui.cittadino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.model.Coordinate
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.launch

/**
 * ViewModel per la creazione di una nuova segnalazione.
 *
 * Sostituisce la logica che prima viveva direttamente in
 * NuovaSegnalazioneFragment (validazione + chiamata al Repository dentro
 * il click listener, violazione MVVM segnalata nell'analisi).
 *
 * Camera, permessi e posizione GPS restano nella Fragment perche' richiedono
 * API Android legate al ciclo di vita/Context (ActivityResultLauncher,
 * LocationManager); il ViewModel si occupa solo di validare i dati raccolti
 * e di eseguire la creazione del ticket.
 */
class NuovaSegnalazioneViewModel(
    private val repository: Repository
) : BaseViewModel<Ticket>() {

    fun inviaSegnalazione(
        titolo: String,
        descrizione: String,
        categoria: String,
        priorita: String,
        fotoBase64: String?,
        latitudine: Double?,
        longitudine: Double?
    ) {
        if (titolo.isBlank() || descrizione.isBlank()) {
            _uiState.value = UiState.Error("Compila i campi obbligatori")
            return
        }

        val ticket = Ticket(
            titolo = titolo,
            descrizione = descrizione,
            categoria = categoria,
            priorita = priorita,
            fotoAllegata = fotoBase64,
            coordinate = Coordinate(
                latitudine ?: 41.1279,
                longitudine ?: 14.7811
            )
        )

        _uiState.value = UiState.Loading
        launchWithIdling {
            repository.createTicket(ticket)
                .onSuccess { creato ->
                    _uiState.value = UiState.Success(creato)
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Errore durante l'invio della segnalazione"
                    )
                }
        }
    }

    class Factory(private val repository: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NuovaSegnalazioneViewModel::class.java)) {
                return NuovaSegnalazioneViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}