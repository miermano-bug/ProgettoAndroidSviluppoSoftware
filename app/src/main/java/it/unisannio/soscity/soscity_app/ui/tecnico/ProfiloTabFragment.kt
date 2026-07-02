package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.util.SessionManager

class ProfiloTabFragment : Fragment(R.layout.fragment_profilo_tab) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = SessionManager.getUser()
        val nome = user?.nome ?: "Tecnico"

        view.findViewById<TextView>(R.id.profiloInitials).text =
            nome.firstOrNull()?.uppercase() ?: "T"
        view.findViewById<TextView>(R.id.profiloNome).text = nome
        view.findViewById<TextView>(R.id.profiloUsername).text =
            user?.username?.let { "@$it" } ?: "@—"
        view.findViewById<TextView>(R.id.profiloEmail).text =
            user?.email ?: "—"
        view.findViewById<TextView>(R.id.profiloDisponibilita).text =
            when (user?.disponibile) {
                true  -> "✅ Disponibile"
                false -> "🔴 Non disponibile"
                null  -> "—"
            }
        view.findViewById<TextView>(R.id.profiloTeam).text =
            user?.idTeam ?: "Nessun team assegnato"
        view.findViewById<TextView>(R.id.profiloCompetenze).text =
            user?.competenze?.joinToString(" · ") ?: "Nessuna competenza registrata"

        view.findViewById<MaterialButton>(R.id.btnLogoutProfilo).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Esci dall'app")
                .setMessage("Sei sicuro di voler effettuare il logout?")
                .setPositiveButton("Esci") { _, _ -> eseguiLogout() }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private fun eseguiLogout() {
        FirebaseAuth.getInstance().signOut()
        SessionManager.clearSession()
        val opts = NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
        findNavController().navigate(R.id.loginFragment, null, opts)
    }
}
