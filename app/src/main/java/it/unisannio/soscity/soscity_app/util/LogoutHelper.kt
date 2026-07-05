package it.unisannio.soscity.soscity_app.util

import android.app.AlertDialog
import android.content.Context
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.google.firebase.auth.FirebaseAuth
import it.unisannio.soscity.soscity_app.R

/**
 * Unico punto di codice per il logout del tecnico.
 * Sostituisce le 4+ copie identiche del blocco logout sparse tra Fragment diversi.
 * Richiamato da tutti i Fragment che espongono un'azione di logout.
 */
fun NavController.performLogout() {
    FirebaseAuth.getInstance().signOut()
    SessionManager.clearSession()
    val opts = NavOptions.Builder()
        .setPopUpTo(R.id.nav_graph, true)
        .build()
    navigate(R.id.loginFragment, null, opts)
}

/**
 * Dialogo di conferma logout, unico anche questo (era duplicato identico in
 * ogni schermata Impostazioni/Profilo). [onConferma] viene eseguito solo se
 * l'utente conferma l'uscita.
 */
fun mostraDialogoLogout(context: Context, onConferma: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle(R.string.logout_titolo)
        .setMessage(R.string.logout_messaggio)
        .setPositiveButton(R.string.logout_conferma) { _, _ -> onConferma() }
        .setNegativeButton(R.string.logout_annulla, null)
        .show()
}