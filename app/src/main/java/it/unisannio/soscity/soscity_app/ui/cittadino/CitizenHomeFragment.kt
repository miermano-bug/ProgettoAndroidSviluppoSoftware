package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Coordinate
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

/**
 * SCHERMATA DI TEST GREZZA — non è UI definitiva.
 * Serve solo a verificare manualmente che gli endpoint ticket/notifications
 * comunichino correttamente col backend, con risultati mostrati a schermo
 * invece che solo su Logcat.
 */
class CitizenHomeFragment : Fragment(
    R.layout.fragment_citizen_home
) {

    private val repository = RepositoryProvider.provideRepository()

    // Conserva l'id dell'ultimo ticket creato, per poter testare subito
    // GET /tickets/{id} senza doverlo copiare a mano da un altro test.
    private var ultimoTicketId: String? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val textRisultato = view.findViewById<TextView>(R.id.textRisultato)

        view.findViewById<Button>(R.id.btnCreateTicket).setOnClickListener {
            mostraCaricamento(textRisultato, "POST /tickets")
            lifecycleScope.launch {
                val ticketTest = Ticket(
                    titolo = "Lampione spento — TEST",
                    descrizione = "Ticket di test generato dalla schermata diagnostica",
                    categoria = "ILLUMINAZIONE",
                    priorita = "MEDIA",
                    coordinate = Coordinate(latitudine = 41.1279, longitudine = 14.7811)
                )
                val result = repository.createTicket(ticketTest)
                result.onSuccess { ticket ->
                    ultimoTicketId = ticket.id
                    textRisultato.text = """
                        ✅ POST /tickets OK

                        id: ${ticket.id}
                        titolo: ${ticket.titolo}
                        stato: ${ticket.stato}
                        idCittadino: ${ticket.idCittadino}
                        dataCreazione: ${ticket.dataCreazione}
                    """.trimIndent()
                }.onFailure { e ->
                    textRisultato.text = "❌ POST /tickets FALLITO\n\n${e.message}"
                }
            }
        }

        view.findViewById<Button>(R.id.btnGetMyTickets).setOnClickListener {
            mostraCaricamento(textRisultato, "GET /tickets/my")
            lifecycleScope.launch {
                val result = repository.getMyTickets()
                result.onSuccess { tickets ->
                    textRisultato.text = buildString {
                        append("✅ GET /tickets/my OK — ${tickets.size} ticket trovati\n\n")
                        tickets.forEach { t ->
                            append("• [${t.stato}] ${t.titolo} (id=${t.id})\n")
                            append("  idCittadino=${t.idCittadino}\n")
                        }
                        if (tickets.isEmpty()) append("(lista vuota)")
                    }
                }.onFailure { e ->
                    textRisultato.text = "❌ GET /tickets/my FALLITO\n\n${e.message}"
                }
            }
        }

        view.findViewById<Button>(R.id.btnGetTicketById).setOnClickListener {
            val id = ultimoTicketId
            if (id == null) {
                textRisultato.text = "⚠️ Crea prima un ticket col bottone 1, così ho un id da usare qui."
                return@setOnClickListener
            }
            mostraCaricamento(textRisultato, "GET /tickets/$id")
            lifecycleScope.launch {
                val result = repository.getTicketById(id)
                result.onSuccess { ticket ->
                    textRisultato.text = """
                        ✅ GET /tickets/{id} OK

                        id: ${ticket.id}
                        titolo: ${ticket.titolo}
                        descrizione: ${ticket.descrizione}
                        categoria: ${ticket.categoria}
                        priorita: ${ticket.priorita}
                        stato: ${ticket.stato}
                        idCittadino: ${ticket.idCittadino}
                        coordinate: (${ticket.coordinate.latitudine}, ${ticket.coordinate.longitudine})
                    """.trimIndent()
                }.onFailure { e ->
                    textRisultato.text = "❌ GET /tickets/{id} FALLITO\n\n${e.message}"
                }
            }
        }

        view.findViewById<Button>(R.id.btnGetNotifications).setOnClickListener {
            mostraCaricamento(textRisultato, "GET /notifications")
            lifecycleScope.launch {
                val result = repository.getNotifications()
                result.onSuccess { notifiche ->
                    textRisultato.text = buildString {
                        append("✅ GET /notifications OK — ${notifiche.size} notifiche\n\n")
                        notifiche.forEach { n ->
                            append("• [${n.tipo}] ${n.messaggio}\n")
                            append("  ticketId=${n.ticketId} timestamp=${n.timestamp}\n")
                        }
                        if (notifiche.isEmpty()) append("(lista vuota)")
                    }
                }.onFailure { e ->
                    textRisultato.text = "❌ GET /notifications FALLITO\n\n${e.message}"
                }
            }
        }
    }

    private fun mostraCaricamento(textView: TextView, endpoint: String) {
        textView.text = "⏳ Chiamata in corso: $endpoint ..."
    }
}