package it.unisannio.soscity.soscity_app.ui.cittadino

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import it.unisannio.soscity.soscity_app.R
import it.unisannio.soscity.soscity_app.data.model.Coordinate
import it.unisannio.soscity.soscity_app.data.model.Ticket
import it.unisannio.soscity.soscity_app.util.RepositoryProvider
import kotlinx.coroutines.launch
import java.io.File

class NuovaSegnalazioneFragment :
    Fragment(R.layout.fragment_nuova_segnalazione) {

    private val repository = RepositoryProvider.provideRepository()

    private var latitudine: Double? = null
    private var longitudine: Double? = null

    private var fotoUri: Uri? = null

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success) {
                Toast.makeText(
                    requireContext(),
                    "Foto salvata correttamente",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Scatto annullato",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val titolo = view.findViewById<EditText>(R.id.editTitolo)
        val descrizione = view.findViewById<EditText>(R.id.editDescrizione)

        val categoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val priorita = view.findViewById<Spinner>(R.id.spinnerPriorita)

        val btnFoto = view.findViewById<Button>(R.id.btnFoto)
        val btnInvia = view.findViewById<Button>(R.id.btnInvia)
        val btnPosizione = view.findViewById<Button>(R.id.btnPosizione)

        val textPosizione = view.findViewById<TextView>(R.id.textPosizione)

        // =========================
        // SPINNER CATEGORIA
        // =========================

        categoria.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "ILLUMINAZIONE",
                "STRADA",
                "RIFIUTI",
                "VANDALISMO",
                "ALTRO"
            )
        )

        // =========================
        // SPINNER PRIORITA'
        // =========================

        priorita.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf(
                "BASSA",
                "MEDIA",
                "ALTA"
            )
        )

        // =========================
        // GPS
        // =========================

        btnPosizione.setOnClickListener {

            ottieniPosizione()

            if (latitudine != null && longitudine != null) {

                textPosizione.text =
                    "Lat: $latitudine\nLng: $longitudine"

            } else {

                textPosizione.text =
                    "Posizione non disponibile"

            }

        }

        // =========================
        // FOTO
        // =========================

        btnFoto.setOnClickListener {

            fotoUri = creaFileImmagine()

            cameraLauncher.launch(fotoUri)

        }

        // =========================
        // INVIO TICKET
        // =========================

        btnInvia.setOnClickListener {

            val titoloStr = titolo.text.toString().trim()
            val descrizioneStr = descrizione.text.toString().trim()

            if (titoloStr.isEmpty() || descrizioneStr.isEmpty()) {

                Toast.makeText(
                    requireContext(),
                    "Compila tutti i campi",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val ticket = Ticket(

                titolo = titoloStr,

                descrizione = descrizioneStr,

                categoria = categoria.selectedItem.toString(),

                priorita = priorita.selectedItem.toString(),

                fotoAllegata = fotoUri?.path,

                coordinate = Coordinate(

                    latitudine = latitudine ?: 41.1279,

                    longitudine = longitudine ?: 14.7811

                )

            )

            lifecycleScope.launch {

                val result = repository.createTicket(ticket)

                result.onSuccess {

                    Toast.makeText(
                        requireContext(),
                        "Segnalazione inviata con successo!",
                        Toast.LENGTH_SHORT
                    ).show()

                }

                result.onFailure {

                    Toast.makeText(
                        requireContext(),
                        "Errore: ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()

                }

            }

        }

    }

    // =========================
    // GPS
    // =========================

    private fun ottieniPosizione() {

        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )

            return

        }

        val location =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {

            latitudine = location.latitude
            longitudine = location.longitude

        }

    }

    // =========================
    // CREA FILE FOTO
    // =========================

    private fun creaFileImmagine(): Uri {

        val file = File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "IMG_${System.currentTimeMillis()}.jpg"
        )

        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

    }

}