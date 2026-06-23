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

        setupMapAnimation(view)
        setupListeners()
        observeViewModel()
    }

    private fun setupMapAnimation(view: View) {
        val mapBackground = view.findViewById<android.widget.ImageView>(R.id.imageMapBackground)

        mapBackground?.post {
            val deltaX = (mapBackground.width - view.width).toFloat()
            val deltaY = (mapBackground.height - view.height).toFloat()

            if (deltaX > 0 && deltaY > 0) {
                val animX = android.animation.ObjectAnimator.ofFloat(
                    mapBackground,
                    "translationX",
                    0f,
                    -deltaX
                ).apply {
                    duration = 45000
                    repeatMode = android.animation.ValueAnimator.REVERSE
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                }

                val animY = android.animation.ObjectAnimator.ofFloat(
                    mapBackground,
                    "translationY",
                    0f,
                    -deltaY
                ).apply {
                    duration = 45000
                    repeatMode = android.animation.ValueAnimator.REVERSE
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                }

                android.animation.AnimatorSet().apply {
                    playTogether(animX, animY)
                    start()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.buttonLogin.setOnClickListener {
            // CAMBIO QUI: ora è email, non username
            val email = binding.editUsername.text.toString().trim()
            val password = binding.editPassword.text.toString()

            if (validateInput(email, password)) {
                // CAMBIO QUI: uso loginWithEmail
                viewModel.loginWithEmail(email, password)
            }
        }

        binding.textRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    // AGGIUNTO: validazione per email e password
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

                        when (user.ruolo) {
                            "CITTADINO" -> {
                                findNavController().navigate(R.id.citizenHomeFragment)
                            }
                            "TECNICO" -> {
                                findNavController().navigate(R.id.technicianHomeFragment)
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    "Ruolo non supportato",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
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