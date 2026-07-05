package it.unisannio.soscity.soscity_app.ui.cittadino

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.databinding.FragmentProfiloCittadinoBinding
import it.unisannio.soscity.soscity_app.util.SessionManager
import it.unisannio.soscity.soscity_app.util.performLogout

/**
 * Tab "Profilo" dell'area Cittadino, ospitata dentro CittadinoContainerFragment.
 * Stesso pattern di ProfiloTabFragment lato tecnico.
 */
class ProfiloCittadinoFragment : Fragment() {

    private var _binding: FragmentProfiloCittadinoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfiloCittadinoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = SessionManager.getUser()
        val nome = user?.nome ?: "Cittadino"

        binding.profiloCittInitials.text = nome.firstOrNull()?.uppercase() ?: "C"
        binding.profiloCittNome.text     = nome
        binding.profiloCittUsername.text = user?.username?.let { "@$it" } ?: "@—"
        binding.profiloCittEmail.text    = user?.email ?: getString(R.string.dettaglio_valore_vuoto)
        binding.profiloCittTelefono.text = user?.telefono ?: getString(R.string.dettaglio_valore_vuoto)

        binding.btnLogoutProfiloCitt.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_titolo)
                .setMessage(R.string.logout_messaggio)
                .setPositiveButton(R.string.logout_conferma) { _, _ ->
                    findNavController().performLogout()
                }
                .setNegativeButton(R.string.logout_annulla, null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}