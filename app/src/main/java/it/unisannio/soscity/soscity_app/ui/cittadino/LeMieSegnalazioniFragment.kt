package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.ui.adapter.TicketAdapter
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch

class LeMieSegnalazioniFragment : Fragment(R.layout.fragment_mie_segnalazioni) {

    private val repository = RepositoryProvider.provideRepository()
    private lateinit var adapter: TicketAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerTickets)
        adapter = TicketAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        caricaSegnalazioni()
    }

    private fun caricaSegnalazioni() {
        lifecycleScope.launch {
            repository.getMyTickets().onSuccess { tickets ->
                adapter.updateData(tickets)
            }.onFailure {
                Toast.makeText(requireContext(), "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}