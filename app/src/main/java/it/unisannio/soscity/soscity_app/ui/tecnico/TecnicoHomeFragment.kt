package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.launch

class TecnicoHomeFragment : Fragment(R.layout.fragment_tecnico_home) {

    private val repository: Repository = RepositoryProvider.provideRepository()
    private lateinit var adapter: InterventionAdapter
    private var ultimoSnapshot: List<Intervention> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHeader(view)
        setupRecycler(view)
        view.findViewById<MaterialButton>(R.id.btnRiprova).setOnClickListener { caricaInterventi(view) }
        caricaInterventi(view)
    }

    // ─── Header ────────────────────────────────────────────────────────────────

    private fun setupHeader(view: View) {
        val user = SessionManager.getUser()
        val nome = user?.nome ?: "Tecnico"
        view.findViewById<TextView>(R.id.textNomeTecnico).text = nome
        view.findViewById<TextView>(R.id.textInitials).text = nome.firstOrNull()?.uppercase() ?: "T"

        // Avatar → bottom sheet profilo
        view.findViewById<FrameLayout>(R.id.btnProfilo).setOnClickListener {
            ProfiloBottomSheet().show(parentFragmentManager, "profilo")
        }

        // Logout
        view.findViewById<LinearLayout>(R.id.btnLogout).setOnClickListener { mostraDialogLogout() }
    }

    private fun mostraDialogLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Esci dall'app")
            .setMessage("Sei sicuro di voler effettuare il logout?")
            .setPositiveButton("Esci") { _, _ -> eseguiLogout() }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun eseguiLogout() {
        FirebaseAuth.getInstance().signOut()
        SessionManager.clearSession()
        val navOptions = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        findNavController().navigate(R.id.loginFragment, null, navOptions)
    }

    // ─── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecycler(view: View) {
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerInterventi)
        adapter = InterventionAdapter(
            onCardClick = { intervention -> apriDettaglio(intervention) },
            onAvvia     = { intervention, nota -> aggiornaStato(intervention, "IN_CORSO", nota) },
            onCompleta  = { intervention, nota -> aggiornaStato(intervention, "COMPLETATO", nota) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
    }

    private fun apriDettaglio(intervention: Intervention) {
        val sheet = InterventoBottomSheet.newInstance(intervention)
        sheet.onAvvia    = { nota -> aggiornaStato(intervention, "IN_CORSO", nota) }
        sheet.onCompleta = { nota -> aggiornaStato(intervention, "COMPLETATO", nota) }
        sheet.show(parentFragmentManager, "dettaglio_intervento")
    }

    // ─── Caricamento ───────────────────────────────────────────────────────────

    private fun caricaInterventi(view: View) {
        mostraStato(view, Stato.CARICAMENTO)
        lifecycleScope.launch {
            repository.getMyInterventions()
                .onSuccess { interventi ->
                    ultimoSnapshot = interventi
                    if (interventi.isEmpty()) {
                        mostraStato(view, Stato.VUOTO)
                    } else {
                        mostraStato(view, Stato.CONTENUTO)
                        adapter.submitList(ordinaPerRilevanza(interventi))
                        aggiornaSottotitolo(view, interventi)
                        aggiornaContatori(view, interventi)
                    }
                }
                .onFailure { e ->
                    mostraStato(view, Stato.ERRORE)
                    view.findViewById<TextView>(R.id.textErrore).text =
                        e.message ?: "Errore nel recupero degli interventi"
                }
        }
    }

    // ─── Aggiornamento stato ────────────────────────────────────────────────────

    private fun aggiornaStato(intervention: Intervention, nuovoStato: String, nota: String?) {
        val v = view ?: return
        lifecycleScope.launch {
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
                }
        }
    }

    private fun ricaricaERilevaPromozione(view: View, teamId: String) {
        val snapshotPrecedente = ultimoSnapshot
        lifecycleScope.launch {
            repository.getMyInterventions().onSuccess { aggiornati ->
                ultimoSnapshot = aggiornati
                adapter.submitList(ordinaPerRilevanza(aggiornati))
                aggiornaSottotitolo(view, aggiornati)
                aggiornaContatori(view, aggiornati)

                val promosso = aggiornati.firstOrNull { nuovo ->
                    nuovo.teamId == teamId && nuovo.statoLavoro == "IN_CORSO" &&
                    snapshotPrecedente.any { it.id == nuovo.id && it.statoLavoro == "PIANIFICATO" }
                }

                val banner     = view.findViewById<LinearLayout>(R.id.bannerPromozione)
                val bannerText = view.findViewById<TextView>(R.id.textBannerPromozione)

                when {
                    promosso != null -> {
                        bannerText.text = "Nuovo intervento avviato per il team"
                        banner.visibility = View.VISIBLE
                    }
                    aggiornati.none { it.teamId == teamId && it.statoLavoro != "COMPLETATO" } -> {
                        bannerText.text = "Nessun altro intervento in coda — team libero"
                        banner.visibility = View.VISIBLE
                    }
                    else -> banner.visibility = View.GONE
                }
            }
        }
    }

    private fun ricaricaSilenziosamente(view: View) {
        lifecycleScope.launch {
            repository.getMyInterventions().onSuccess { interventi ->
                ultimoSnapshot = interventi
                adapter.submitList(ordinaPerRilevanza(interventi))
                aggiornaSottotitolo(view, interventi)
                aggiornaContatori(view, interventi)
            }
        }
    }

    // ─── UI helpers ────────────────────────────────────────────────────────────

    private fun ordinaPerRilevanza(interventi: List<Intervention>): List<Intervention> {
        val peso = mapOf("IN_CORSO" to 0, "PIANIFICATO" to 1, "SOSPESO" to 2, "COMPLETATO" to 3)
        return interventi.sortedBy { peso[it.statoLavoro] ?: 2 }
    }

    private fun aggiornaSottotitolo(view: View, interventi: List<Intervention>) {
        val attivi = interventi.count { it.statoLavoro == "IN_CORSO" || it.statoLavoro == "PIANIFICATO" }
        view.findViewById<TextView>(R.id.textSubtitle).text =
            if (attivi > 0) "$attivi da gestire · ${interventi.size} totali"
            else "Tutto completato · ${interventi.size} totali"
    }

    private fun aggiornaContatori(view: View, interventi: List<Intervention>) {
        view.findViewById<TextView>(R.id.textContatoreAttivi).text =
            interventi.count { it.statoLavoro == "IN_CORSO" || it.statoLavoro == "PIANIFICATO" }.toString()
        view.findViewById<TextView>(R.id.textContatoreCompletati).text =
            interventi.count { it.statoLavoro == "COMPLETATO" }.toString()
    }

    private enum class Stato { CARICAMENTO, CONTENUTO, VUOTO, ERRORE }

    private fun mostraStato(view: View, stato: Stato) {
        view.findViewById<ProgressBar>(R.id.progressBar).visibility          = if (stato == Stato.CARICAMENTO) View.VISIBLE else View.GONE
        view.findViewById<RecyclerView>(R.id.recyclerInterventi).visibility  = if (stato == Stato.CONTENUTO)   View.VISIBLE else View.GONE
        view.findViewById<LinearLayout>(R.id.layoutVuoto).visibility         = if (stato == Stato.VUOTO)       View.VISIBLE else View.GONE
        view.findViewById<LinearLayout>(R.id.layoutErrore).visibility        = if (stato == Stato.ERRORE)      View.VISIBLE else View.GONE
        if (stato != Stato.CONTENUTO) view.findViewById<LinearLayout>(R.id.bannerPromozione).visibility = View.GONE
    }
}
