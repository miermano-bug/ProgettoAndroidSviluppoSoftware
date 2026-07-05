package it.unisannio.soscity.soscity_app.ui.cittadino

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.ui.common.UiState
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * La Fragment gestisce solo permessi, fotocamera e posizione (API legate ad
 * Activity/Context). La validazione dei dati e la creazione del ticket sono
 * delegate a NuovaSegnalazioneViewModel (violazione MVVM corretta).
 */
class NuovaSegnalazioneFragment : Fragment(R.layout.fragment_nuova_segnalazione) {

    private val viewModel: NuovaSegnalazioneViewModel by viewModels {
        NuovaSegnalazioneViewModel.Factory(RepositoryProvider.provideRepository())
    }

    private var latitudine: Double? = null
    private var longitudine: Double? = null
    private var fotoFile: File? = null
    private var fotoBase64Str: String? = null
    private var imageAnteprima: ImageView? = null

    private lateinit var editTitolo: EditText
    private lateinit var editDescrizione: EditText
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerPriorita: Spinner
    private lateinit var btnInvia: Button
    private lateinit var textPosizione: TextView

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            ottieniPosizione()
            textPosizione.text = "Lat: $latitudine, Lng: $longitudine"
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) eseguiScatto()
        else Toast.makeText(requireContext(), "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && fotoFile != null) {
                val bitmapOriginale = BitmapFactory.decodeFile(fotoFile!!.absolutePath)

                // Rotazione di 90 gradi
                val matrix = Matrix()
                matrix.postRotate(90f)
                val bitmapRuotata = Bitmap.createBitmap(
                    bitmapOriginale, 0, 0,
                    bitmapOriginale.width, bitmapOriginale.height,
                    matrix, true
                )

                imageAnteprima?.setImageBitmap(bitmapRuotata)
                imageAnteprima?.visibility = View.VISIBLE

                lifecycleScope.launch(Dispatchers.Default) {
                    fotoBase64Str = convertiBitmapInBase64(bitmapRuotata)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editTitolo = view.findViewById(R.id.editTitolo)
        editDescrizione = view.findViewById(R.id.editDescrizione)
        spinnerCategoria = view.findViewById(R.id.spinnerCategoria)
        spinnerPriorita = view.findViewById(R.id.spinnerPriorita)
        val btnFoto = view.findViewById<Button>(R.id.btnFoto)
        btnInvia = view.findViewById(R.id.btnInvia)
        val btnPosizione = view.findViewById<Button>(R.id.btnPosizione)
        textPosizione = view.findViewById(R.id.textPosizione)
        imageAnteprima = view.findViewById(R.id.imageAnteprima)

        spinnerCategoria.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("ILLUMINAZIONE", "VERDE_URBANO", "ARREDO_URBANO", "EDIFICI", "EMERGENZA")
        )
        spinnerPriorita.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            listOf("BASSA", "MEDIA", "ALTA", "URGENTE")
        )

        btnPosizione.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            } else {
                ottieniPosizione()
                textPosizione.text = "Lat: $latitudine, Lng: $longitudine"
            }
        }

        btnFoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                eseguiScatto()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        btnInvia.setOnClickListener {
            btnInvia.isEnabled = false
            viewModel.inviaSegnalazione(
                titolo = editTitolo.text.toString().trim(),
                descrizione = editDescrizione.text.toString().trim(),
                categoria = spinnerCategoria.selectedItem.toString(),
                priorita = spinnerPriorita.selectedItem.toString(),
                fotoBase64 = fotoBase64Str,
                latitudine = latitudine,
                longitudine = longitudine
            )
        }

        osservaStato()
    }

    private fun osservaStato() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { stato ->
                when (stato) {
                    is UiState.Idle -> Unit

                    is UiState.Loading -> {
                        btnInvia.isEnabled = false
                    }

                    is UiState.Success -> {
                        Toast.makeText(requireContext(), "Inviato!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }

                    is UiState.Error -> {
                        btnInvia.isEnabled = true
                        Toast.makeText(requireContext(), "Errore: ${stato.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun ottieniPosizione() {
        try {
            val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (loc != null) { latitudine = loc.latitude; longitudine = loc.longitude }
        } catch (e: SecurityException) { e.printStackTrace() }
    }

    private fun eseguiScatto() {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        fotoFile = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", storageDir)
        val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", fotoFile!!)
        cameraLauncher.launch(uri)
    }

    private suspend fun convertiBitmapInBase64(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val os = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, os)
        Base64.encodeToString(os.toByteArray(), Base64.DEFAULT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageAnteprima = null
    }
}