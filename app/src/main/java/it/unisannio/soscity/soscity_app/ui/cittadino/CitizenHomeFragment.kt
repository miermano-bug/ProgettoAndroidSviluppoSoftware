package it.unisannio.soscity.soscity_app.ui.cittadino

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import it.unisannio.soscity.soscity_app.R

class CitizenHomeFragment : Fragment(R.layout.fragment_citizen_home) {

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val btnNuovaSegnalazione =
            view.findViewById<Button>(R.id.btnNuovaSegnalazione)

        val btnMieSegnalazioni =
            view.findViewById<Button>(R.id.btnMieSegnalazioni)

        val btnNotifiche =
            view.findViewById<Button>(R.id.btnNotifiche)

        btnNuovaSegnalazione.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Nuova Segnalazione",
                Toast.LENGTH_SHORT
            ).show()

        }

        btnMieSegnalazioni.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Le mie segnalazioni",
                Toast.LENGTH_SHORT
            ).show()

        }

        btnNotifiche.setOnClickListener {

            Toast.makeText(
                requireContext(),
                "Notifiche",
                Toast.LENGTH_SHORT
            ).show()

        }

    }
}