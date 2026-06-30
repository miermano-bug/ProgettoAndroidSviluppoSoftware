package it.unisannio.soscity.soscity_app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.databinding.FragmentRegisterBinding
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.roleHomeDestination
import it.unisannio.soscity.soscity_app.util.startMapBackgroundPan
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by lazy {
        RegisterViewModel(RepositoryProvider.provideRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startMapBackgroundPan(view, binding.imageMapBackground)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.buttonRegister.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString()
            val username = binding.editUsername.text.toString().trim()
            val nome = binding.editNome.text.toString().trim()
            val telefono = binding.editTelefono.text.toString().trim()

            if (validateInput(email, password, username, nome)) {
                viewModel.registerUser(email, password, username, nome, telefono)
            }
        }
    }

    private fun validateInput(
        email: String,
        password: String,
        username: String,
        nome: String
    ): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Inserisci l'email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty() || password.length < 6) {
            Toast.makeText(requireContext(), "La password deve contenere almeno 6 caratteri", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Inserisci lo username", Toast.LENGTH_SHORT).show()
            return false
        }
        if (nome.isEmpty()) {
            Toast.makeText(requireContext(), "Inserisci il nome completo", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Idle -> {}

                    is UiState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.buttonRegister.isEnabled = false
                    }

                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.buttonRegister.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            "Registrazione completata! Benvenuto ${state.data.nome}",
                            Toast.LENGTH_LONG
                        ).show()

                        // Naviga in base al ruolo
                        val destination = roleHomeDestination(state.data.ruolo)
                        if (destination != null) {
                            findNavController().navigate(destination)
                        } else {
                            findNavController().navigateUp()
                        }
                    }

                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.buttonRegister.isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            state.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}