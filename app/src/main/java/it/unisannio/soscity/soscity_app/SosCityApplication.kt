package it.unisannio.soscity.soscity_app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import it.unisannio.soscity.soscity_app.util.SessionManager

/**
 * Application class di SOS City.
 *
 * Responsabilita':
 * 1. Inizializzare SessionManager con il Context dell'applicazione prima di
 *    qualunque Activity (necessario per la persistenza delle SharedPreferences).
 * 2. Registrare il NotificationChannel "interventi_channel", obbligatorio
 *    su Android 8.0+ (API 26 = minSdk di questo progetto).
 *    Senza la registrazione del canale, nessuna notifica di sistema puo' essere
 *    mostrata, indipendentemente dall'utilizzo di NotificationCompat.Builder.
 *
 * NOTA: le notifiche push per il tecnico non sono supportate lato backend in questa
 * versione. Il canale viene comunque registrato per conformita' con i requisiti
 * di sistema (minSdk 26) e per dimostrare il pattern corretto.
 * Il toggle nelle Impostazioni persiste la preferenza utente in SharedPreferences
 * ma non attiva polling reale.
 */
class SosCityApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inizializza la persistenza della sessione
        SessionManager.init(this)

        // Registra il NotificationChannel (richiesto da Android 8.0 / API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId   = CHANNEL_ID_INTERVENTI
            val channelName = "Interventi"
            val descrizione = "Aggiornamenti sullo stato degli interventi assegnati"
            val importanza  = NotificationManager.IMPORTANCE_DEFAULT

            val canale = NotificationChannel(channelId, channelName, importanza).apply {
                description = descrizione
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(canale)
        }
    }

    companion object {
        const val CHANNEL_ID_INTERVENTI = "interventi_channel"
    }
}
