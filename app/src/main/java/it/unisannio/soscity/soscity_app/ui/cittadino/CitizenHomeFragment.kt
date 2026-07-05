package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R

class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Troviamo le card usando View (visto che ora sono MaterialCardView nell'XML)
        val btnNuovaSegnalazione = view.findViewById<View>(R.id.btnNuovaSegnalazione)
        val btnMieSegnalazioni = view.findViewById<View>(R.id.btnMieSegnalazioni)
        val btnNotifiche = view.findViewById<View>(R.id.btnNotifiche)

        // Navigazione nativa pulita basata sul Navigation Architecture Component
        btnNuovaSegnalazione.setOnClickListener {
            findNavController().navigate(R.id.action_citizenHome_to_nuovaSegnalazione)
        }

        btnMieSegnalazioni.setOnClickListener {
            findNavController().navigate(R.id.action_citizenHome_to_mieSegnalazioni)
        }

        btnNotifiche.setOnClickListener {
            Toast.makeText(requireContext(), "Notifiche (Sezione in sviluppo)", Toast.LENGTH_SHORT).show()
        }
    }
}