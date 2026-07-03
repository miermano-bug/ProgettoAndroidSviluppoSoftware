package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.repository.Repository
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.SessionManager
import kotlinx.coroutines.launch

class ImpostazioniBottomSheet : BottomSheetDialogFragment() {

    private val repository: Repository by lazy { RepositoryProvider.provideRepository() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.bottom_sheet_impostazioni, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSincronizza(view)
        setupNumeriEmergenza(view)
        setupLogout(view)
    }

    private fun setupSincronizza(view: View) {
        val btn = view.findViewById<MaterialButton>(R.id.btnSincronizza)
        btn.setOnClickListener {
            btn.isEnabled = false
            btn.text = "Sincronizzazione in corso…"
            lifecycleScope.launch {
                try {
                    repository.getMyInterventions()
                        .onSuccess {
                            btn.text = "Dati aggiornati"
                            Toast.makeText(requireContext(), "Dati sincronizzati con successo", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure {
                            btn.text = "Sincronizza dati"
                            Toast.makeText(requireContext(), "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } finally {
                    btn.isEnabled = true
                    view.postDelayed({ btn.text = "Sincronizza dati" }, 2000)
                }
            }
        }
    }

    private fun setupNumeriEmergenza(view: View) {
        mapOf(
            R.id.btnChiama112 to "112",
            R.id.btnChiama115 to "115",
            R.id.btnChiama118 to "118",
            R.id.btnChiama113 to "113"
        ).forEach { (id, numero) ->
            view.findViewById<LinearLayout>(id).setOnClickListener {
                chiediConfermaChiamata(numero)
            }
        }
    }

    private fun chiediConfermaChiamata(numero: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Chiama $numero")
            .setMessage("Vuoi chiamare il numero di emergenza $numero?")
            .setPositiveButton("Chiama") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")))
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    private fun setupLogout(view: View) {
        view.findViewById<MaterialButton>(R.id.btnLogoutImpostazioni).setOnClickListener {
            dismiss()
            AlertDialog.Builder(requireContext())
                .setTitle("Esci dall'app")
                .setMessage("Sei sicuro di voler effettuare il logout?")
                .setPositiveButton("Esci") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    SessionManager.clearSession()
                    val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                    findNavController().navigate(R.id.loginFragment, null, opts)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }
}
