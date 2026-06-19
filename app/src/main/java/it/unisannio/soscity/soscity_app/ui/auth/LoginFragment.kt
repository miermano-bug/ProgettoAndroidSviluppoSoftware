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
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null

    private val binding get() = _binding!!

    private val viewModel = LoginViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentLoginBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Avvia l'animazione dello sfondo della mappa
        setupMapAnimation(view)

        setupListeners()
        observeViewModel()
    }

    private fun setupMapAnimation(view: View) {
        val mapBackground = view.findViewById<android.widget.ImageView>(R.id.imageMapBackground)

        mapBackground?.post {
            // Spazio di movimento orizzontale e verticale
            val deltaX = (mapBackground.width - view.width).toFloat()
            val deltaY = (mapBackground.height - view.height).toFloat()

            if (deltaX > 0 && deltaY > 0) {
                // Animazione asse X (Orizzontale)
                val animX = android.animation.ObjectAnimator.ofFloat(
                    mapBackground,
                    "translationX",
                    0f,
                    -deltaX
                ).apply {
                    duration = 45000 // 45 secondi
                    repeatMode = android.animation.ValueAnimator.REVERSE
                    repeatCount = android.animation.ValueAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                }

                // Animazione asse Y (Verticale)
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

                // Unisce le due animazioni in diagonale/obliquo
                android.animation.AnimatorSet().apply {
                    playTogether(animX, animY)
                    start()
                }
            }
        }
    }

    private fun setupListeners() {

        binding.buttonLogin.setOnClickListener {

            val username =
                binding.editUsername.text.toString()

            val password =
                binding.editPassword.text.toString()

            viewModel.login(
                username,
                password
            )
        }

        binding.textRegister.setOnClickListener {

            findNavController().navigate(
                R.id.registerFragment
            )
        }
    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewModel.uiState.collect { state ->

                when (state) {

                    is UiState.Idle -> {}

                    is UiState.Loading -> {

                        binding.progressBar.visibility =
                            View.VISIBLE
                    }

                    is UiState.Success -> {

                        binding.progressBar.visibility =
                            View.GONE

                        val user = state.data

                        Toast.makeText(
                            requireContext(),
                            "Benvenuto ${user.nome}",
                            Toast.LENGTH_LONG
                        ).show()

                        when (user.ruolo) {

                            "CITTADINO" -> {

                                findNavController().navigate(
                                    R.id.citizenHomeFragment
                                )
                            }

                            "TECNICO" -> {

                                findNavController().navigate(
                                    R.id.technicianHomeFragment
                                )
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

                        binding.progressBar.visibility =
                            View.GONE

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