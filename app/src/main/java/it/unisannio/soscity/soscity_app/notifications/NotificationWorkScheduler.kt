package it.unisannio.soscity.soscity_app.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Punto unico per avviare il controllo periodico in background delle
 * notifiche di chiusura ticket (vedi TicketClosedNotificationWorker).
 *
 * schedule() va chiamato quando un cittadino entra nell'area Cittadino
 * (CittadinoContainerFragment). ExistingPeriodicWorkPolicy.KEEP fa si' che
 * chiamate ripetute (es. ogni volta che si riapre l'app o si torna alla
 * Home) non creino lavori duplicati ne' resettino l'intervallo gia'
 * pianificato.
 *
 * Non serve una cancel() esplicita al logout: il Worker stesso, ad ogni
 * esecuzione, controlla se esiste ancora un utente Firebase loggato e non
 * fa nulla in caso contrario (vedi TicketClosedNotificationWorker.doWork()).
 */
object NotificationWorkScheduler {

    private const val WORK_NAME = "ticket_closed_notification_worker"
    private const val INTERVALLO_MINUTI = 15L // minimo consentito da WorkManager

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<TicketClosedNotificationWorker>(
            INTERVALLO_MINUTI, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}