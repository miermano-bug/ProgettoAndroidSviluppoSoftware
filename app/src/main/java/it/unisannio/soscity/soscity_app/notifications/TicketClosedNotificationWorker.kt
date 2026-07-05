package it.unisannio.soscity.soscity_app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.MainActivity
import it.unisannio.soscity.soscity_app.SosCityApplication
import it.unisannio.soscity.soscity_app.util.NetworkClient
import it.unisannio.soscity.soscity_app.util.SessionManager

/**
 * Worker periodico che controlla se sono arrivate nuove notifiche di chiusura
 * ticket (evento "TicketRisolto", salvato dal notification-service con
 * tipo = "EMAIL") e, in tal caso, mostra una vera notifica di sistema Android.
 *
 * Il backend non implementa un invio push reale (nessun FirebaseMessaging.send
 * da nessuna parte in notification-service): questo worker e', senza toccare
 * il backend, l'unico modo per avvisare il cittadino con una notifica di
 * sistema anche a schermata "Notifiche"/Home chiusa.
 *
 * IMPORTANTE: non viene inviata alcuna email reale. Il campo tipo="EMAIL" nel
 * backend identifica semplicemente, ad oggi, l'evento di chiusura ticket:
 * questo worker lo usa solo come "marcatore" per riconoscere quell'evento,
 * l'invio email vero e proprio resta (volutamente) non implementato.
 *
 * WorkManager impone un intervallo minimo di 15 minuti per il lavoro
 * periodico in background (limite di sistema Android per il risparmio
 * batteria): e' il modo piu' vicino a un push reale senza un canale FCM lato
 * server. Quando l'app e' aperta in Home/Notifiche, l'aggiornamento resta
 * invece quasi immediato grazie al polling ogni 15 secondi gia' presente in
 * NotificationsViewModel.
 */
class TicketClosedNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Nessun utente loggato (o logout effettuato): non fare nulla.
        if (FirebaseAuth.getInstance().currentUser == null) {
            return Result.success()
        }

        // Questo worker serve solo per i cittadini: se il ruolo salvato e'
        // TECNICO (o comunque diverso da CITTADINO), non chiamare l'endpoint.
        val ruolo = SessionManager.getUserRole()
        if (ruolo != null && ruolo != "CITTADINO") {
            return Result.success()
        }

        return try {
            val notifiche = NetworkClient.apiService.getNotifications()

            // Le notifiche di chiusura ticket sono salvate dal backend con
            // tipo = "EMAIL" (vedi NotificationService.riceviEventoEInviaNotifica,
            // case "TicketRisolto"): e' l'unico segnale disponibile senza
            // modificare il backend.
            val notificheChiusura = notifiche.filter { it.tipo == "EMAIL" }

            val prefs = applicationContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE
            )
            val giaNotificate = prefs.getStringSet(KEY_ID_NOTIFICATE, emptySet()) ?: emptySet()
            val nuoveIdNotificate = giaNotificate.toMutableSet()

            var mostrataAlmenoUna = false
            notificheChiusura.forEach { notifica ->
                if (notifica.id.isNotBlank() && !giaNotificate.contains(notifica.id)) {
                    mostraNotificaSistema(notifica.messaggio, notifica.ticketId)
                    nuoveIdNotificate.add(notifica.id)
                    mostrataAlmenoUna = true
                }
            }

            if (mostrataAlmenoUna) {
                prefs.edit().putStringSet(KEY_ID_NOTIFICATE, nuoveIdNotificate).apply()
            }

            Result.success()
        } catch (e: Exception) {
            // Nessuna connessione o errore di rete: si ritentera' al giro successivo.
            Result.success()
        }
    }

    private fun mostraNotificaSistema(messaggio: String, ticketId: String) {
        val permessoConcesso = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!permessoConcesso) return

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            ticketId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            SosCityApplication.CHANNEL_ID_NOTIFICHE_CITTADINO
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Segnalazione chiusa")
            .setContentText(messaggio)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messaggio))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(ticketId.hashCode(), notification)
    }

    companion object {
        private const val PREFS_NAME = "soscity_ticket_notifications"
        private const val KEY_ID_NOTIFICATE = "id_notifiche_mostrate"
    }
}