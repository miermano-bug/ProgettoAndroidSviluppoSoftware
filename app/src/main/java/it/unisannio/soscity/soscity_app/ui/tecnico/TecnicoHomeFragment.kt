package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * SCHERMATA DI TEST GREZZA — non è UI definitiva.
 * Serve solo a verificare manualmente che gli endpoint interventions
 * comunichino correttamente col backend.
 *
 * NOTA: non esiste nell'app un modo per CREARE un Intervention di test
 * (POST /interventions non è esposto in ApiService). Se il bottone 1
 * restituisce una lista vuota, non significa necessariamente un errore:
 * potrebbe semplicemente non esistere ancora nessun intervento assegnato
 * a questo tecnico nel database. Per creare un intervento di test, usa
 * una chiamata diretta (curl/Postman) con POST :8085/interventions,
 * passando il technicianId = uid Firebase di questo account TECNICO.
 */
class TecnicoHomeFragment : Fragment(
    R.layout.fragment_tecnico_home
) {

    private val repository = RepositoryProvider.provideRepository()

    // Conserva l'id dell'ultimo intervento letto, per testare subito
    // GET /interventions/{id} e l'update di stato senza copiarlo a mano.
    private var ultimoInterventionId: String? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val textRisultato = view.findViewById<TextView>(R.id.textRisultatoTecnico)

        view.findViewById<Button>(R.id.btnGetMyInterventions).setOnClickListener {
            mostraCaricamento(textRisultato, "GET /interventions/my")
            lifecycleScope.launch {
                val result = repository.getMyInterventions()
                result.onSuccess { interventi ->
                    if (interventi.isNotEmpty()) {
                        ultimoInterventionId = interventi.first().id
                    }
                    textRisultato.text = buildString {
                        append("✅ GET /interventions/my OK — ${interventi.size} interventi\n\n")
                        interventi.forEach { i ->
                            append("• [${i.statoLavoro}] id=${i.id}\n")
                            append("  ticketId=${i.ticketId} tecnicoId=${i.tecnicoId}\n")
                            append("  note=${i.noteIntervento}\n\n")
                        }
                        if (interventi.isEmpty()) {
                            append("(lista vuota — non è necessariamente un errore: ")
                            append("potrebbe non esistere ancora nessun intervento assegnato ")
                            append("a questo tecnico. Vedi nota nel codice del Fragment.)")
                        }
                    }
                }.onFailure { e ->
                    textRisultato.text = "❌ GET /interventions/my FALLITO\n\n${e.message}"
                }
            }
        }

        view.findViewById<Button>(R.id.btnGetInterventionById).setOnClickListener {
            val id = ultimoInterventionId
            if (id == null) {
                textRisultato.text = "⚠️ Premi prima il bottone 1 per ottenere un id da usare qui."
                return@setOnClickListener
            }
            mostraCaricamento(textRisultato, "GET /interventions/$id")
            lifecycleScope.launch {
                val result = repository.getInterventionById(id)
                result.onSuccess { i ->
                    textRisultato.text = """
                        ✅ GET /interventions/{id} OK

                        id: ${i.id}
                        ticketId: ${i.ticketId}
                        teamId: ${i.teamId}
                        tecnicoId: ${i.tecnicoId}
                        statoLavoro: ${i.statoLavoro}
                        noteIntervento: ${i.noteIntervento}
                        dataInizio: ${i.dataInizio}
                        dataFine: ${i.dataFine ?: "(non ancora conclusa)"}
                        dataCreazione: ${i.dataCreazione}
                    """.trimIndent()
                }.onFailure { e ->
                    textRisultato.text = "❌ GET /interventions/{id} FALLITO\n\n${e.message}"
                }
            }
        }

        view.findViewById<Button>(R.id.btnSetStatoInCorso).setOnClickListener {
            aggiornaStato(textRisultato, "IN_CORSO")
        }

        view.findViewById<Button>(R.id.btnSetStatoCompletato).setOnClickListener {
            aggiornaStato(textRisultato, "COMPLETATO")
        }
    }

    private fun aggiornaStato(textView: TextView, nuovoStato: String) {
        val id = ultimoInterventionId
        if (id == null) {
            textView.text = "⚠️ Premi prima il bottone 1 per ottenere un id da usare qui."
            return
        }
        mostraCaricamento(textView, "PUT /interventions/$id/stato?stato=$nuovoStato")
        lifecycleScope.launch {
            val result = repository.updateInterventionStatus(id, nuovoStato)
            result.onSuccess {
                textView.text = "✅ PUT /interventions/$id/stato?stato=$nuovoStato OK\n\n" +
                        "Premi di nuovo il bottone 1 o 2 per verificare che lo stato sia cambiato " +
                        "(se nuovoStato == COMPLETATO, controlla anche che il Ticket collegato " +
                        "sia passato a RISOLTO, secondo §13 UC4 del contratto)."
            }.onFailure { e ->
                textView.text = "❌ PUT .../stato FALLITO\n\n${e.message}"
            }
        }
    }

    private fun mostraCaricamento(textView: TextView, endpoint: String) {
        textView.text = "⏳ Chiamata in corso: $endpoint ..."
    }
}