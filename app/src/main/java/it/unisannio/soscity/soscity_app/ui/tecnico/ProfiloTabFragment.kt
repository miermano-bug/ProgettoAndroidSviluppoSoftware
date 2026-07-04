package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.databinding.FragmentProfiloTabBinding
import it.unisannio.soscity.soscity_app.util.performLogout
import it.unisannio.soscity.soscity_app.util.SessionManager

class ProfiloTabFragment : Fragment() {

    private var _binding: FragmentProfiloTabBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfiloTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = SessionManager.getUser()
        val nome = user?.nome ?: "Tecnico"

        binding.profiloInitials.text   = nome.firstOrNull()?.uppercase() ?: "T"
        binding.profiloNome.text       = nome
        binding.profiloUsername.text   = user?.username?.let { "@$it" } ?: "@—"
        binding.profiloEmail.text      = user?.email ?: getString(R.string.dettaglio_valore_vuoto)
        binding.profiloDisponibilita.text = when (user?.disponibile) {
            true  -> getString(R.string.profilo_disponibile)
            false -> getString(R.string.profilo_non_disponibile)
            null  -> getString(R.string.dettaglio_valore_vuoto)
        }
        binding.profiloTeam.text       = user?.idTeam
            ?: getString(R.string.profilo_nessun_team)
        binding.profiloCompetenze.text = user?.competenze?.joinToString(" · ")
            ?: getString(R.string.profilo_nessuna_competenza)

        binding.btnLogoutProfilo.setOnClickListener {
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
