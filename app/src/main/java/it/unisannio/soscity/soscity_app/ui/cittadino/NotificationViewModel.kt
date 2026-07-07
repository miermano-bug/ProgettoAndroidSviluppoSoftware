package it.unisannio.soscity.soscity_app.ui.cittadino

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.model.Notification
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * ViewModel per la schermata "Le mie notifiche" del Cittadino.
 *
 * Il backend (notification-service) non ha un canale push: le notifiche
 * vengono generate in modo asincrono in risposta al ciclo di vita del ticket
 * (TicketCreato/TicketAssegnato/TicketRisolto) e sono consultabili solo tramite
 * GET /notifications (polling). Questo ViewModel implementa quindi un
 * aggiornamento periodico "quasi in tempo reale" finche' la schermata e'
 * visibile, senza mai far sparire la lista gia' caricata durante il polling
 * silenzioso (solo il caricamento iniziale mostra la progress bar a pieno schermo).
 */
class NotificationsViewModel(
    private val repository: Repository
) : BaseViewModel<List<Notification>>() {

    private var pollingJob: Job? = null

    companion object {
        private const val POLLING_INTERVAL_MS = 15_000L
    }

    /** Caricamento iniziale: mostra la progress bar a schermo intero. */
    fun caricaNotifiche() {
        _uiState.value = UiState.Loading
        launchWithIdling {
            eseguiFetch()
        }
    }

    /** Usato dal pull-to-refresh: non passa da uno stato Loading "pieno". */
    fun refresh() {
        launchWithIdling {
            eseguiFetch()
        }
    }

    /**
     * Avvia il polling periodico. Va richiamato da onResume() della Fragment
     * e fermato in onPause() con fermaPolling(), per non consumare rete/batteria
     * quando la schermata non e' visibile.
     *
     * NON avvolgere con launchWithIdling: e' un loop che gira finche' la
     * schermata e' visibile (while (isActive)), il contatore dell'idling
     * resource non tornerebbe mai a zero e Espresso andrebbe in timeout.
     */
    fun avviaPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                eseguiFetch()
            }
        }
    }

    fun fermaPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun eseguiFetch() {
        repository.getNotifications()
            .onSuccess { lista ->
                _uiState.value = UiState.Success(ordinaPerData(lista))
            }
            .onFailure { e ->
                // Durante il polling silenzioso non sovrascriviamo una lista gia'
                // mostrata con uno stato di errore: solo il caricamento iniziale
                // (stato non ancora Success) propaga l'errore alla UI.
                if (_uiState.value !is UiState.Success) {
                    _uiState.value = UiState.Error(
                        e.message ?: "Errore nel recupero delle notifiche"
                    )
                }
            }
    }

    /** Notifiche piu' recenti prima. */
    private fun ordinaPerData(lista: List<Notification>): List<Notification> =
        lista.sortedByDescending { parseTimestamp(it.timestamp) }

    private fun parseTimestamp(isoDate: String): Instant {
        if (isoDate.isBlank()) return Instant.EPOCH
        return try {
            val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
            Instant.parse(src)
        } catch (e: Exception) {
            Instant.EPOCH
        }
    }

    override fun onCleared() {
        super.onCleared()
        fermaPolling()
    }

    class Factory(private val repository: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
                return NotificationsViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}