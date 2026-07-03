package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HomeTabFragment : Fragment(R.layout.fragment_home_tab) {

    private val repository: Repository = RepositoryProvider.provideRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        impostaHeader(view)

        view.findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Esci dall'app")
                .setMessage("Sei sicuro di voler effettuare il logout?")
                .setPositiveButton("Esci") { _, _ -> eseguiLogout() }
                .setNegativeButton("Annulla", null)
                .show()
        }

        view.findViewById<View>(R.id.btnImpostazioni).setOnClickListener {
            ImpostazioniBottomSheet().show(parentFragmentManager, "impostazioni")
        }

        caricaDashboard(view)
    }

    private fun impostaHeader(view: View) {
        val nome = SessionManager.getUser()?.nome?.split(" ")?.firstOrNull() ?: "Tecnico"
        val salutoText = when (LocalTime.now().hour) {
            in 6..11  -> "Buongiorno"
            in 12..17 -> "Buon pomeriggio"
            in 18..21 -> "Buona sera"
            else      -> "Buona notte"
        }
        view.findViewById<TextView>(R.id.textSaluto).text = "$salutoText, $nome!"
    }

    private fun caricaDashboard(view: View) {
        view.findViewById<ProgressBar>(R.id.progressHome).visibility = View.VISIBLE

        lifecycleScope.launch {
            repository.getMyInterventions()
                .onSuccess { interventi ->
                    view.findViewById<ProgressBar>(R.id.progressHome).visibility = View.GONE
                    aggiornaRiepilogo(view, interventi)
                    mostraProssimoIntervento(view, interventi)
                }
                .onFailure {
                    view.findViewById<ProgressBar>(R.id.progressHome).visibility = View.GONE
                    view.findViewById<TextView>(R.id.textDescrizioneGiornata).text =
                        "Impossibile caricare i dati"
                }
        }
    }

    private fun aggiornaRiepilogo(view: View, interventi: List<Intervention>) {
        val completati = interventi.count { it.statoLavoro == "COMPLETATO" }
        val mancanti   = interventi.count { it.statoLavoro == "IN_CORSO" || it.statoLavoro == "PIANIFICATO" }
        val sospesi    = interventi.count { it.statoLavoro == "SOSPESO" }
        val totale     = interventi.size

        view.findViewById<TextView>(R.id.textDescrizioneGiornata).text =
            when {
                totale == 0 -> "Nessun intervento assigned oggi"
                mancanti == 0 -> "Tutti gli interventi completati!"
                else -> "Oggi hai $mancanti interventi in programma"
            }

        view.findViewById<TextView>(R.id.countCompletati).text = completati.toString()
        view.findViewById<TextView>(R.id.countMancanti).text   = mancanti.toString()
        view.findViewById<TextView>(R.id.countSospesi).text    = sospesi.toString()
    }

    private fun mostraProssimoIntervento(view: View, interventi: List<Intervention>) {
        val prossimo = interventi.firstOrNull { it.statoLavoro == "IN_CORSO" }
            ?: interventi.firstOrNull { it.statoLavoro == "PIANIFICATO" }

        val card         = view.findViewById<CardView>(R.id.cardProssimoIntervento)
        val layoutVuoto  = view.findViewById<LinearLayout>(R.id.layoutNessunProssimo)

        if (prossimo == null) {
            card.visibility        = View.GONE
            layoutVuoto.visibility = View.VISIBLE
            return
        }

        card.visibility        = View.VISIBLE
        layoutVuoto.visibility = View.GONE

        val refCorta = prossimo.ticketId.takeLast(8).uppercase().ifBlank { prossimo.id.takeLast(8).uppercase() }
        view.findViewById<TextView>(R.id.cardTicketRef).text = "Ticket #$refCorta"
        view.findViewById<TextView>(R.id.cardDataInizio).text = formattaData(prossimo.dataInizio)

        val badge = view.findViewById<TextView>(R.id.cardStatoBadge)
        badge.text = prossimo.statoLavoro
        when (prossimo.statoLavoro) {
            "IN_CORSO"    -> { badge.setBackgroundResource(R.drawable.bg_status_in_corso);    badge.setTextColor(0xFF1B5E20.toInt()) }
            "PIANIFICATO" -> { badge.setBackgroundResource(R.drawable.bg_status_pianificato); badge.setTextColor(0xFFE65100.toInt()) }
            else          -> { badge.setBackgroundResource(R.drawable.bg_status_completato);  badge.setTextColor(0xFF757575.toInt()) }
        }

        val noteView = view.findViewById<TextView>(R.id.cardNote)
        if (prossimo.noteIntervento.isNotBlank()) {
            noteView.visibility = View.VISIBLE
            noteView.text = "${prossimo.noteIntervento}"
        }

        view.findViewById<MaterialButton>(R.id.btnApriDettaglio).setOnClickListener {
            val sheet = InterventoBottomSheet.newInstance(prossimo)
            sheet.onAvvia = { nota: String? ->
                lifecycleScope.launch {
                    repository.updateInterventionStatus(prossimo.id, "IN_CORSO", nota)
                        .onSuccess { caricaDashboard(view) }
                }
            }
            sheet.onCompleta = { nota: String? ->
                lifecycleScope.launch {
                    repository.updateInterventionStatus(prossimo.id, "COMPLETATO", nota)
                        .onSuccess { caricaDashboard(view) }
                }
            }
            sheet.show(parentFragmentManager, "dettaglio_home")
        }

        lifecycleScope.launch {
            repository.getTicketById(prossimo.ticketId)
                .onSuccess { ticket ->
                    popolaCampiTicket(view, ticket)
                }
                .onFailure {
                    view.findViewById<TextView>(R.id.cardCategoria).text = "Categoria: n.d."
                    view.findViewById<TextView>(R.id.cardPriorita).text = "—"
                }
        }
    }

    private fun popolaCampiTicket(view: View, ticket: Ticket) {
        view.findViewById<TextView>(R.id.cardCategoria).text =
            "Categoria: ${ticket.categoria.ifBlank { "n.d." }}"

        val priorita = ticket.priorita
        val (label, color) = when (priorita) {
            "URGENTE" -> "URGENTE" to 0xFFC62828.toInt()
            "ALTA"    -> "ALTA"    to 0xFFE65100.toInt()
            "MEDIA"   -> "MEDIA"   to 0xFF8A6D1D.toInt()
            "BASSA"   -> "BASSA"   to 0xFF2E7D32.toInt()
            else      -> "— n.d."     to 0xFF757575.toInt()
        }
        val priView = view.findViewById<TextView>(R.id.cardPriorita)
        priView.text = label
        priView.setTextColor(color)
    }

    private fun formattaData(isoDate: String): String {
        if (isoDate.isBlank()) return "n.d."
        return try {
            val src = if (isoDate.endsWith("Z")) isoDate else "${isoDate}Z"
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(Instant.parse(src))
        } catch (e: Exception) { isoDate }
    }

    private fun eseguiLogout() {
        FirebaseAuth.getInstance().signOut()
        SessionManager.clearSession()
        val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        findNavController().navigate(R.id.loginFragment, null, opts)
    }
}