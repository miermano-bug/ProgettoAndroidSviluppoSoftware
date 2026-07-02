package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.util.SessionManager

class ImpostazioniBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.bottom_sheet_impostazioni, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
