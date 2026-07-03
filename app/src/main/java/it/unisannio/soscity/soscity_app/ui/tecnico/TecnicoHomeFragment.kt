package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * Home del TECNICO: mostra tutti gli interventi assegnati (Modifica 3 —
 * coda multipla sul team: il tecnico ora può avere più di un intervento
 * relativo allo stesso team, non solo quello "attivo").
 *
 * Per ogni intervento azionabile (PIANIFICATO o IN_CORSO) sono disponibili
 * i bottoni Avvia/Completa con un campo nota opzionale (Modifica 5).
 *
 * Dopo un COMPLETATO riuscito, ricarica la lista e confronta lo snapshot
 * precedente con quello nuovo per segnalare se un intervento PIANIFICATO
 * è stato promosso automaticamente a IN_CORSO sullo stesso team
 * (Modifica 4 — promozione automatica della coda).
 */
class TecnicoHomeFragment : Fragment(
    R.layout.fragment_tecnico_home
) {

    private val repository: Repository = RepositoryProvider.provideRepository()
    private lateinit var adapter: InterventionAdapter

    // Ultimo snapshot caricato, usato per rilevare la promozione automatica
    // dopo un COMPLETATO (Modifica 4).
    private var ultimoSnapshot: List<Intervention> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerInterventi)
        adapter = InterventionAdapter(
            onAvvia = { intervention, nota -> aggiornaStato(intervention, "IN_CORSO", nota) },
            onCompleta = { intervention, nota -> aggiornaStato(intervention, "COMPLETATO", nota) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<Button>(R.id.btnRiprova).setOnClickListener {
            caricaInterventi(view)
        }

        caricaInterventi(view)
    }

    private fun caricaInterventi(view: View) {
        mostraStato(view, Stato.CARICAMENTO)
        lifecycleScope.launch {
            val result = repository.getMyInterventions()
            result.onSuccess { interventi ->
                ultimoSnapshot = interventi
                if (interventi.isEmpty()) {
                    mostraStato(view, Stato.VUOTO)
                } else {
                    mostraStato(view, Stato.CONTENUTO)
                    adapter.submitList(ordinaPerRilevanza(interventi))
                    aggiornaSottotitolo(view, interventi)
                }
            }.onFailure { e ->
                mostraStato(view, Stato.ERRORE)
                view.findViewById<TextView>(R.id.textErrore).text =
                    e.message ?: "Errore nel recupero degli interventi"
            }
        }
    }

    private fun aggiornaStato(intervention: Intervention, nuovoStato: String, nota: String?) {
        val view = view ?: return
        lifecycleScope.launch {
<<<<<<< Updated upstream
            val result = repository.updateInterventionStatus(intervention.id, nuovoStato, nota)
            result.onSuccess {
                adapter.mostraEsito(intervention.id, "✅ Stato aggiornato a $nuovoStato")
                if (nuovoStato == "COMPLETATO") {
                    ricaricaERilevaPromozione(view, teamId = intervention.teamId)
                } else {
                    ricaricaSilenziosamente(view)
=======
            repository.updateInterventionStatus(intervention.id, nuovoStato, nota)
                .onSuccess {
                    adapter.mostraEsito(intervention.id, "Stato aggiornato a $nuovoStato")
                    if (nuovoStato == "COMPLETATO") {
                        ricaricaERilevaPromozione(v, intervention.teamId)
                    } else {
                        ricaricaSilenziosamente(v)
                    }
                }
                .onFailure { e ->
                    adapter.mostraEsito(intervention.id, "❌ ${e.message ?: "Aggiornamento non riuscito"}")
>>>>>>> Stashed changes
                }
            }.onFailure { e ->
                adapter.mostraEsito(intervention.id, "❌ ${e.message ?: "Aggiornamento non riuscito"}")
            }
        }
    }

    /**
     * Dopo un COMPLETATO, ricarica la lista e confronta lo snapshot precedente
     * con quello nuovo: se un intervento dello stesso teamId che era
     * PIANIFICATO è ora IN_CORSO, significa che il backend l'ha promosso
     * automaticamente (Modifica 4) — mostriamo il banner.
     * Se invece nessun intervento dello stesso team risulta più
     * PIANIFICATO né IN_CORSO, il team è stato liberato.
     */
    private fun ricaricaERilevaPromozione(view: View, teamId: String) {
        val snapshotPrecedente = ultimoSnapshot
        lifecycleScope.launch {
            val result = repository.getMyInterventions()
            result.onSuccess { interventiAggiornati ->
                ultimoSnapshot = interventiAggiornati
                adapter.submitList(ordinaPerRilevanza(interventiAggiornati))
                aggiornaSottotitolo(view, interventiAggiornati)

                val promosso = interventiAggiornati.firstOrNull { nuovo ->
                    nuovo.teamId == teamId &&
                            nuovo.statoLavoro == "IN_CORSO" &&
                            snapshotPrecedente.any { it.id == nuovo.id && it.statoLavoro == "PIANIFICATO" }
                }

                val bannerView = view.findViewById<LinearLayout>(R.id.bannerPromozione)
                val bannerText = view.findViewById<TextView>(R.id.textBannerPromozione)

                when {
                    promosso != null -> {
<<<<<<< Updated upstream
                        bannerText.text = "Nuovo intervento avviato per il team $teamId (ticket #${promosso.ticketId})"
                        bannerView.visibility = View.VISIBLE
                    }
                    interventiAggiornati.none { it.teamId == teamId && it.statoLavoro != "COMPLETATO" } -> {
                        bannerText.text = "Nessun altro intervento in coda: il team $teamId è stato liberato"
                        bannerView.visibility = View.VISIBLE
=======
                        bannerText.text = "Nuovo intervento avviato per il team"
                        banner.visibility = View.VISIBLE
                    }
                    aggiornati.none { it.teamId == teamId && it.statoLavoro != "COMPLETATO" } -> {
                        bannerText.text = "Nessun altro intervento in coda — team libero"
                        banner.visibility = View.VISIBLE
>>>>>>> Stashed changes
                    }
                    else -> bannerView.visibility = View.GONE
                }
            }.onFailure {
                // La lista locale resta quella precedente all'update: non blocchiamo
                // l'utente con un errore, dato che l'update di stato è già riuscito.
            }
        }
    }

    private fun ricaricaSilenziosamente(view: View) {
        lifecycleScope.launch {
            repository.getMyInterventions().onSuccess { interventi ->
                ultimoSnapshot = interventi
                adapter.submitList(ordinaPerRilevanza(interventi))
                aggiornaSottotitolo(view, interventi)
            }
        }
    }

    /** IN_CORSO e PIANIFICATO in alto (sono quelli su cui agire), COMPLETATO in fondo. */
    private fun ordinaPerRilevanza(interventi: List<Intervention>): List<Intervention> {
        val peso = mapOf("IN_CORSO" to 0, "PIANIFICATO" to 1, "COMPLETATO" to 2)
        return interventi.sortedBy { peso[it.statoLavoro] ?: 1 }
    }

    private fun aggiornaSottotitolo(view: View, interventi: List<Intervention>) {
        val attivi = interventi.count { it.statoLavoro == "IN_CORSO" || it.statoLavoro == "PIANIFICATO" }
        view.findViewById<TextView>(R.id.textSubtitle).text =
            if (attivi > 0) "$attivi da gestire · ${interventi.size} totali"
            else "Tutto completato · ${interventi.size} totali"
    }

    private enum class Stato { CARICAMENTO, CONTENUTO, VUOTO, ERRORE }

    private fun mostraStato(view: View, stato: Stato) {
        view.findViewById<ProgressBar>(R.id.progressBar).visibility =
            if (stato == Stato.CARICAMENTO) View.VISIBLE else View.GONE
        view.findViewById<RecyclerView>(R.id.recyclerInterventi).visibility =
            if (stato == Stato.CONTENUTO) View.VISIBLE else View.GONE
        view.findViewById<LinearLayout>(R.id.layoutVuoto).visibility =
            if (stato == Stato.VUOTO) View.VISIBLE else View.GONE
        view.findViewById<LinearLayout>(R.id.layoutErrore).visibility =
            if (stato == Stato.ERRORE) View.VISIBLE else View.GONE
        if (stato != Stato.CONTENUTO) {
            view.findViewById<LinearLayout>(R.id.bannerPromozione).visibility = View.GONE
        }
    }
}
