package it.unisannio.soscity.soscity_app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.RegisterRequest
import it.unisannio.soscity.soscity_app.databinding.FragmentRegisterBinding
import it.unisannio.soscity.soscity_app.ui.common.UiState
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding:
            FragmentRegisterBinding? = null

    private val binding
        get() = _binding!!

    private val viewModel =
        RegisterViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentRegisterBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Avvia l'animazione obliqua dello sfondo
        setupMapAnimation(view)

        // I tuoi metodi esistenti
        setupListeners()
        observeViewModel()
    }

    // 2. Incolla questa funzione subito sotto, prima della chiusura della classe
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

        binding.buttonRegister.setOnClickListener {

            val request = RegisterRequest(

                username =
                    binding.editUsername.text
                        .toString(),

                password =
                    binding.editPassword.text
                        .toString(),

                email =
                    binding.editEmail.text
                        .toString(),

                nome =
                    binding.editNome.text
                        .toString(),

                telefono =
                    binding.editTelefono.text
                        .toString()
            )

            viewModel.register(request)
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

                        Toast.makeText(
                            requireContext(),
                            "Registrazione completata",
                            Toast.LENGTH_LONG
                        ).show()

                        findNavController()
                            .navigateUp()
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