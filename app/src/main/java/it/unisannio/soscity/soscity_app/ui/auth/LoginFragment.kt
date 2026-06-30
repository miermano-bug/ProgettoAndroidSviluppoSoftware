package it.unisannio.soscity.soscity_app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import androidx.lifecycle.lifecycleScope
import it.unisannio.soscity.soscity_app.databinding.FragmentLoginBinding
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import it.unisannio.soscity.soscity_app.util.roleHomeDestination
import it.unisannio.soscity.soscity_app.util.startMapBackgroundPan
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by lazy {
        LoginViewModel(RepositoryProvider.provideRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startMapBackgroundPan(view, binding.imageMapBackground)
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            val email = binding.editEmail.text.toString().trim()
            val password = binding.editPassword.text.toString()

            if (validateInput(email, password)) {
                viewModel.loginWithEmail(email, password)
            }
        }

        binding.textRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Inserisci l'email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(requireContext(), "Inserisci la password", Toast.LENGTH_SHORT).show()
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
                    }

                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE

                        val user = state.data

                        Toast.makeText(
                            requireContext(),
                            "Benvenuto ${user.nome}",
                            Toast.LENGTH_LONG
                        ).show()

                        val destination = roleHomeDestination(user.ruolo)
                        if (destination != null) {
                            findNavController().navigate(destination)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Ruolo non supportato",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE

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