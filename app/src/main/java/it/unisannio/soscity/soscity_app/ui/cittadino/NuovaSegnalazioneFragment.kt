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
import androidx.lifecycle.lifecycleScope
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Coordinate
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

class NuovaSegnalazioneFragment : Fragment(R.layout.fragment_nuova_segnalazione) {

    private val repository = RepositoryProvider.provideRepository()
    private var latitudine: Double? = null
    private var longitudine: Double? = null
    private var fotoFile: File? = null
    private var fotoBase64Str: String? = null
    private var imageAnteprima: ImageView? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            ottieniPosizione()
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

        val titolo = view.findViewById<EditText>(R.id.editTitolo)
        val descrizione = view.findViewById<EditText>(R.id.editDescrizione)
        val categoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val priorita = view.findViewById<Spinner>(R.id.spinnerPriorita)
        val btnFoto = view.findViewById<Button>(R.id.btnFoto)
        val btnInvia = view.findViewById<Button>(R.id.btnInvia)
        val btnPosizione = view.findViewById<Button>(R.id.btnPosizione)
        val textPosizione = view.findViewById<TextView>(R.id.textPosizione)
        imageAnteprima = view.findViewById(R.id.imageAnteprima)

        categoria.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("ILLUMINAZIONE", "VERDE_URBANO", "ARREDO_URBANO", "EDIFICI", "EMERGENZA"))
        priorita.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("BASSA", "MEDIA", "ALTA", "URGENTE"))

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
            val titoloStr = titolo.text.toString().trim()
            val descrizioneStr = descrizione.text.toString().trim()

            if (titoloStr.isEmpty() || descrizioneStr.isEmpty()) {
                Toast.makeText(requireContext(), "Compila i campi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnInvia.isEnabled = false
            val ticket = Ticket(
                titolo = titoloStr,
                descrizione = descrizioneStr,
                categoria = categoria.selectedItem.toString(),
                priorita = priorita.selectedItem.toString(),
                fotoAllegata = fotoBase64Str,
                coordinate = Coordinate(latitudine ?: 41.1279, longitudine ?: 14.7811)
            )

            lifecycleScope.launch {
                // Aggiungi questo log appena prima di repository.createTicket(ticket)
                android.util.Log.d("API_DEBUG", "Invio Ticket: $ticket")
                repository.createTicket(ticket).onSuccess {
                    Toast.makeText(requireContext(), "Inviato!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }.onFailure {
                    Toast.makeText(requireContext(), "Errore: ${it.message}", Toast.LENGTH_SHORT).show()
                    btnInvia.isEnabled = true
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