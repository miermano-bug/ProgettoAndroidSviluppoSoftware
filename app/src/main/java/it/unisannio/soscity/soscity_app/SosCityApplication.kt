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
 * 2. Registrare i NotificationChannel richiesti su Android 8.0+ (API 26 =
 *    minSdk di questo progetto). Senza la registrazione del canale, nessuna
 *    notifica di sistema puo' essere mostrata, indipendentemente
 *    dall'utilizzo di NotificationCompat.Builder.
 *
 * NOTA "interventi_channel": le notifiche push per il tecnico non sono
 * supportate lato backend in questa versione. Il canale viene comunque
 * registrato per conformita' con i requisiti di sistema (minSdk 26) e per
 * dimostrare il pattern corretto. Il toggle nelle Impostazioni persiste la
 * preferenza utente in SharedPreferences ma non attiva polling reale.
 *
 * NOTA "notifiche_cittadino_channel": questo canale e' invece REALMENTE
 * utilizzato (vedi TicketClosedNotificationWorker) per mostrare una notifica
 * di sistema quando una segnalazione del cittadino viene chiusa da un tecnico.
 */
class SosCityApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inizializza la persistenza della sessione
        SessionManager.init(this)

        // Registra i NotificationChannel (richiesto da Android 8.0 / API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val canaleInterventi = NotificationChannel(
                CHANNEL_ID_INTERVENTI,
                "Interventi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Aggiornamenti sullo stato degli interventi assegnati"
            }

            val canaleNotificheCittadino = NotificationChannel(
                CHANNEL_ID_NOTIFICHE_CITTADINO,
                "Notifiche cittadino",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avvisi quando una tua segnalazione viene chiusa"
            }

            manager?.createNotificationChannel(canaleInterventi)
            manager?.createNotificationChannel(canaleNotificheCittadino)
        }
    }

    companion object {
        const val CHANNEL_ID_INTERVENTI = "interventi_channel"
        const val CHANNEL_ID_NOTIFICHE_CITTADINO = "notifiche_cittadino_channel"
    }
}
