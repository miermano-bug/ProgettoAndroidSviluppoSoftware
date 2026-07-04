package it.unisannio.soscity.soscity_app.ui.tecnico

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import it.unisannio.soscity.soscity_app.databinding.BottomSheetNumeriEmergenzaBinding

/**
 * Bottom sheet dedicato ai numeri di emergenza.
 * Estratto dall'ex ImpostazioniBottomSheet per separazione concettuale:
 * i numeri di emergenza sono azioni immediate, non impostazioni.
 */
class NumeriEmergenzaBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetNumeriEmergenzaBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetNumeriEmergenzaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnChiama112.setOnClickListener { chiediConferma("112") }
        binding.btnChiama115.setOnClickListener { chiediConferma("115") }
        binding.btnChiama118.setOnClickListener { chiediConferma("118") }
        binding.btnChiama113.setOnClickListener { chiediConferma("113") }
    }

    private fun chiediConferma(numero: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(it.unisannio.soscity.soscity_app.R.string.emergenza_chiama_titolo, numero))
            .setMessage(getString(it.unisannio.soscity.soscity_app.R.string.emergenza_chiama_messaggio, numero))
            .setPositiveButton(it.unisannio.soscity.soscity_app.R.string.emergenza_chiama_conferma) { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$numero")))
            }
            .setNegativeButton(it.unisannio.soscity.soscity_app.R.string.logout_annulla, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
