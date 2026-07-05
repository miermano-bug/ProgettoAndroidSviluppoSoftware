package it.unisannio.soscity.soscity_app.ui.cittadino

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.util.SessionManager
import it.unisannio.soscity.soscity_app.util.performLogout
import java.time.LocalTime

/**
 * Tab "Home" dell'area Cittadino, ospitata dentro CittadinoContainerFragment.
 * "Le mie segnalazioni" non e' piu' una card qui: e' diventata una tab della
 * bottom navigation (vedi CittadinoContainerFragment). "Nuova segnalazione"
 * non e' piu' una card qui: l'unico punto di accesso e' il FAB "+" del
 * container, per evitare due modi diversi di fare la stessa azione.
 * "Notifiche" resta un placeholder in attesa del prossimo step del piano.
 */
class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textSaluto = view.findViewById<TextView>(R.id.textSaluto)
        val btnLogout = view.findViewById<View>(R.id.btnLogout)
        val btnNotifiche = view.findViewById<View>(R.id.btnNotifiche)

        impostaHeader(textSaluto)

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_titolo)
                .setMessage(R.string.logout_messaggio)
                .setPositiveButton(R.string.logout_conferma) { _, _ ->
                    findNavController().performLogout()
                }
                .setNegativeButton(R.string.logout_annulla, null)
                .show()
        }

        btnNotifiche.setOnClickListener {
            Toast.makeText(requireContext(), "Notifiche (Sezione in sviluppo)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun impostaHeader(textSaluto: TextView) {
        val nome = SessionManager.getUser()?.nome?.split(" ")?.firstOrNull() ?: "Cittadino"
        val saluto = when (LocalTime.now().hour) {
            in 6..11  -> getString(R.string.saluto_mattina)
            in 12..17 -> getString(R.string.saluto_pomeriggio)
            in 18..21 -> getString(R.string.saluto_sera)
            else      -> getString(R.string.saluto_notte)
        }
        textSaluto.text = "$saluto, $nome!"
    }
}