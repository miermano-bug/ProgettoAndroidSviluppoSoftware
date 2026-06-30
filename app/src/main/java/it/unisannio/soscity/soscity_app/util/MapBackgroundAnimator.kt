package it.unisannio.soscity.soscity_app.util

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import it.unisannio.soscity.soscity_app.R

/**
 * Avvia l'animazione di "pan" continuo dello sfondo mappa (un'immagine più
 * grande della view contenitore, che scorre avanti e indietro all'infinito).
 *
 * In precedenza questa logica era duplicata identica in LoginFragment e
 * RegisterFragment (vedi punto #2 del documento di refactoring): stesso
 * calcolo dei delta, stessi parametri (45000ms, REVERSE, INFINITE,
 * LinearInterpolator). I parametri costanti vivono ora in
 * res/animator/map_pan.xml; qui resta solo il calcolo runtime dei delta,
 * che non può vivere in XML perché dipende dalle dimensioni effettive
 * della view a schermo.
 *
 * @param container la view che fa da "finestra" (tipicamente la root del Fragment)
 * @param mapBackground l'ImageView di sfondo, più grande del container, su cui
 *   applicare la traslazione
 */
fun startMapBackgroundPan(container: View, mapBackground: ImageView) {
    mapBackground.post {
        val deltaX = (mapBackground.width - container.width).toFloat()
        val deltaY = (mapBackground.height - container.height).toFloat()

        if (deltaX <= 0 || deltaY <= 0) return@post

        val animatorSet = AnimatorInflater.loadAnimator(
            mapBackground.context,
            R.animator.map_pan
        ) as AnimatorSet

        // I valori reali (0 -> -delta) sono noti solo a runtime: l'XML
        // definisce solo i parametri costanti (durata, repeat, interpolator).
        animatorSet.childAnimations.forEach { animator ->
            val objectAnimator = animator as ObjectAnimator
            when (objectAnimator.propertyName) {
                "translationX" -> objectAnimator.setFloatValues(0f, -deltaX)
                "translationY" -> objectAnimator.setFloatValues(0f, -deltaY)
            }
        }

        animatorSet.setTarget(mapBackground)
        animatorSet.start()
    }
}