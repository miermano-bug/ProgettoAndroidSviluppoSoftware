package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Intervention
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

class InterventiTabFragment : Fragment(R.layout.fragment_interventi_tab) {

    private val repository: Repository = RepositoryProvider.provideRepository()
    private lateinit var adapter: InterventionAdapter
    private var ultimoSnapshot: List<Intervention> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerInterventi)
        adapter = InterventionAdapter(
            onCardClick = { iv -> apriDettaglio(iv) },
            onAvvia     = { iv, nota -> aggiornaStato(iv, "IN_CORSO", nota) },
            onCompleta  = { iv, nota -> aggiornaStato(iv, "COMPLETATO", nota) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        view.findViewById<MaterialButton>(R.id.btnRiprova).setOnClickListener { carica(view) }
        carica(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { carica(it) }
    }

    private fun carica(view: View) {
        mostraStato(view, Stato.CARICAMENTO)
        lifecycleScope.launch {
            repository.getMyInterventions()
                .onSuccess { list ->
                    ultimoSnapshot = list
                    if (list.isEmpty()) {
                        mostraStato(view, Stato.VUOTO)
                    } else {
                        mostraStato(view, Stato.CONTENUTO)
                        adapter.submitList(ordina(list))
                        aggiornaSubtitle(view, list)
                        aggiornaContatori(view, list)
                    }
                }
                .onFailure { e ->
                    mostraStato(view, Stato.ERRORE)
                    view.findViewById<TextView>(R.id.textErrore).text =
                        e.message ?: "Errore di rete"
                }
        }
    }

    private fun apriDettaglio(iv: Intervention) {
        val sheet = InterventoBottomSheet.newInstance(iv)
        sheet.onAvvia    = { nota -> aggiornaStato(iv, "IN_CORSO", nota) }
        sheet.onCompleta = { nota -> aggiornaStato(iv, "COMPLETATO", nota) }
        sheet.show(parentFragmentManager, "dettaglio")
    }

    private fun aggiornaStato(iv: Intervention, stato: String, nota: String?) {
        val v = view ?: return
        lifecycleScope.launch {
            repository.updateInterventionStatus(iv.id, stato, nota)
                .onSuccess {
                    adapter.mostraEsito(iv.id, "✅ Stato aggiornato a $stato")
                    ricarica(v, iv.teamId)
                }
                .onFailure { e ->
                    adapter.mostraEsito(iv.id, "❌ ${e.message ?: "Errore"}")
                }
        }
    }

    private fun ricarica(view: View, teamId: String) {
        val snap = ultimoSnapshot
        lifecycleScope.launch {
            repository.getMyInterventions().onSuccess { list ->
                ultimoSnapshot = list
                adapter.submitList(ordina(list))
                aggiornaSubtitle(view, list)
                aggiornaContatori(view, list)

                val promosso = list.firstOrNull { n ->
                    n.teamId == teamId && n.statoLavoro == "IN_CORSO" &&
                    snap.any { it.id == n.id && it.statoLavoro == "PIANIFICATO" }
                }
                val banner = view.findViewById<LinearLayout>(R.id.bannerPromozione)
                val bannerText = view.findViewById<TextView>(R.id.textBannerPromozione)
                when {
                    promosso != null -> { bannerText.text = "Nuovo intervento avviato"; banner.visibility = View.VISIBLE }
                    list.none { it.teamId == teamId && it.statoLavoro != "COMPLETATO" } ->
                        { bannerText.text = "Team libero — nessun intervento in coda"; banner.visibility = View.VISIBLE }
                    else -> banner.visibility = View.GONE
                }
            }
        }
    }

    private fun ordina(list: List<Intervention>): List<Intervention> {
        val p = mapOf("IN_CORSO" to 0, "PIANIFICATO" to 1, "SOSPESO" to 2, "COMPLETATO" to 3)
        return list.sortedBy { p[it.statoLavoro] ?: 2 }
    }

    private fun aggiornaSubtitle(view: View, list: List<Intervention>) {
        val attivi = list.count { it.statoLavoro == "IN_CORSO" || it.statoLavoro == "PIANIFICATO" }
        view.findViewById<TextView>(R.id.textSubtitle).text =
            if (attivi > 0) "$attivi da gestire · ${list.size} totali" else "Tutto completato · ${list.size} totali"
    }

    private fun aggiornaContatori(view: View, list: List<Intervention>) {
        view.findViewById<TextView>(R.id.textContatoreAttivi).text =
            list.count { it.statoLavoro == "IN_CORSO" || it.statoLavoro == "PIANIFICATO" }.toString()
        view.findViewById<TextView>(R.id.textContatoreCompletati).text =
            list.count { it.statoLavoro == "COMPLETATO" }.toString()
    }

    private enum class Stato { CARICAMENTO, CONTENUTO, VUOTO, ERRORE }

    private fun mostraStato(view: View, stato: Stato) {
        view.findViewById<ProgressBar>(R.id.progressBar).visibility         = vis(stato == Stato.CARICAMENTO)
        view.findViewById<RecyclerView>(R.id.recyclerInterventi).visibility = vis(stato == Stato.CONTENUTO)
        view.findViewById<LinearLayout>(R.id.layoutVuoto).visibility        = vis(stato == Stato.VUOTO)
        view.findViewById<LinearLayout>(R.id.layoutErrore).visibility       = vis(stato == Stato.ERRORE)
        if (stato != Stato.CONTENUTO) view.findViewById<LinearLayout>(R.id.bannerPromozione).visibility = View.GONE
    }

    private fun vis(show: Boolean) = if (show) View.VISIBLE else View.GONE
}
