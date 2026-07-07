package it.unisannio.soscity.soscity_app.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.StatoLavoro
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.model.ordinePriorita
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.ui.common.BaseViewModel
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Stato puntuale per l'esito dell'aggiornamento di un singolo intervento.
 * Separato da UiState (che rappresenta lo stato della lista) per evitare
 * che un'azione su una card resetti la visibilita' dell'intera lista.
 */
sealed class AzioneUiState {
    object Inattivo : AzioneUiState()
    data class Successo(val interventionId: String, val messaggio: String) : AzioneUiState()
    data class Errore(val interventionId: String, val messaggio: String) : AzioneUiState()
}

/**
 * ViewModel condiviso tra HomeTabFragment, InterventiTabFragment e HomeTabFragment.
 * Estende BaseViewModel<List<Intervention>> che espone il campo _uiState / uiState.
 *
 * Sostituisce la logica di caricamento che prima era duplicata identicamente in
 * TecnicoHomeFragment e InterventiTabFragment (violazione DRY + assenza MVVM).
 *
 * Il ViewModel sopravvive ai cambi di configurazione (rotazione schermo): la lista
 * non viene ricaricata dalla rete a ogni ricreazione del Fragment.
 */
class InterventionsViewModel(
    private val repository: Repository
) : BaseViewModel<List<Intervention>>() {

    /** Stato puntuale per l'esito di un aggiornamento singolo. */
    private val _azioneState = MutableStateFlow<AzioneUiState>(AzioneUiState.Inattivo)
    val azioneState: StateFlow<AzioneUiState> = _azioneState

    /**
     * Stato del dettaglio ticket (categoria/priorita') associato al prossimo
     * intervento mostrato in HomeTabFragment. Prima questa chiamata veniva
     * fatta direttamente dalla Fragment tramite RepositoryProvider
     * (violazione MVVM segnalata nell'analisi); ora passa dal ViewModel.
     */
    private val _dettaglioTicketState = MutableStateFlow<UiState<Ticket>>(UiState.Idle)
    val dettaglioTicketState: StateFlow<UiState<Ticket>> = _dettaglioTicketState

    /** Snapshot dell'ultima lista caricata, usato per rilevare la promozione automatica. */
    private var ultimoSnapshot: List<Intervention> = emptyList()

    /**
     * Carica gli interventi del tecnico dal repository.
     * Emette Loading -> Success(lista ordinata) oppure Loading -> Error.
     */
    fun caricaInterventi() {
        _uiState.value = UiState.Loading
        launchWithIdling {
            repository.getMyInterventions()
                .onSuccess { lista ->
                    ultimoSnapshot = lista
                    _uiState.value = UiState.Success(ordinaPerRilevanza(lista))
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(
                        e.message ?: "Errore nel recupero degli interventi"
                    )
                }
        }
    }

    /**
     * Carica il ticket associato a un intervento, per mostrare categoria e
     * priorita' nella card "prossimo intervento" della Home.
     */
    fun caricaDettaglioTicket(ticketId: String) {
        _dettaglioTicketState.value = UiState.Loading
        launchWithIdling {
            repository.getTicketById(ticketId)
                .onSuccess { ticket ->
                    _dettaglioTicketState.value = UiState.Success(ticket)
                }
                .onFailure { e ->
                    _dettaglioTicketState.value = UiState.Error(
                        e.message ?: "Errore nel recupero del ticket"
                    )
                }
        }
    }

    /**
     * Aggiorna lo stato di un intervento e ricarica la lista.
     * Emette su azioneState per fornire feedback puntuale alla card corrispondente.
     */
    fun aggiornaStato(intervention: Intervention, nuovoStato: StatoLavoro, nota: String?) {
        launchWithIdling {
            repository.updateInterventionStatus(intervention.id, nuovoStato.name, nota)
                .onSuccess {
                    _azioneState.value = AzioneUiState.Successo(
                        interventionId = intervention.id,
                        messaggio = "Stato aggiornato a ${nuovoStato.name}"
                    )
                    ricaricaEControllaPromozione(intervention.teamId)
                }
                .onFailure { e ->
                    _azioneState.value = AzioneUiState.Errore(
                        interventionId = intervention.id,
                        messaggio = e.message ?: "Aggiornamento non riuscito"
                    )
                }
        }
    }

    /**
     * Ricarica la lista dopo un aggiornamento di stato e rileva
     * la promozione automatica di un intervento PIANIFICATO -> IN_CORSO.
     * Il risultato e' esposto tramite uiState (Success con lista aggiornata)
     * e tramite bannerPromozioneState.
     */
    private suspend fun ricaricaEControllaPromozione(teamId: String) {
        val snapshotPrecedente = ultimoSnapshot
        repository.getMyInterventions().onSuccess { aggiornati ->
            ultimoSnapshot = aggiornati
            _uiState.value = UiState.Success(ordinaPerRilevanza(aggiornati))

            val promosso = aggiornati.firstOrNull { nuovo ->
                nuovo.teamId == teamId
                        && nuovo.statoLavoro == StatoLavoro.IN_CORSO.name
                        && snapshotPrecedente.any { it.id == nuovo.id && it.statoLavoro == StatoLavoro.PIANIFICATO.name }
            }

            _bannerPromozioneState.value = when {
                promosso != null ->
                    BannerPromozione.NuovoAvviato
                aggiornati.none { it.teamId == teamId && it.statoLavoro != StatoLavoro.COMPLETATO.name } ->
                    BannerPromozione.TeamLibero
                else ->
                    BannerPromozione.Nascosto
            }
        }
    }

    /** Stato del banner promozione visualizzato dopo un completamento. */
    private val _bannerPromozioneState = MutableStateFlow<BannerPromozione>(BannerPromozione.Nascosto)
    val bannerPromozioneState: StateFlow<BannerPromozione> = _bannerPromozioneState

    fun nascondiBanner() {
        _bannerPromozioneState.value = BannerPromozione.Nascosto
    }

    // ─── Funzione di ordinamento centralizzata ────────────────────────────────

    /**
     * Ordina la lista per rilevanza: IN_CORSO prima, COMPLETATO alla fine.
     * Era duplicata identicamente in TecnicoHomeFragment e InterventiTabFragment.
     */
    private fun ordinaPerRilevanza(lista: List<Intervention>): List<Intervention> {
        return lista.sortedBy { iv ->
            StatoLavoro.fromString(iv.statoLavoro)?.ordinePriorita() ?: 2
        }
    }

    // ─── Factory ─────────────────────────────────────────────────────────────

    /**
     * Factory scritta a mano (senza framework DI) che riceve Repository come
     * parametro del costruttore, consentendo al Fragment di richiedere solo
     * il ViewModel senza conoscere il Repository direttamente.
     */
    class Factory(private val repository: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InterventionsViewModel::class.java)) {
                return InterventionsViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}

/** Stato del banner di promozione automatica. */
sealed class BannerPromozione {
    object Nascosto     : BannerPromozione()
    object NuovoAvviato : BannerPromozione()
    object TeamLibero   : BannerPromozione()
}