package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import it.unisannio.soscity.soscity_app.R

class TecnicoContainerFragment : Fragment(R.layout.fragment_tecnico_container) {

    private var currentTag = TAG_HOME

    companion object {
        const val TAG_HOME       = "tab_home"
        const val TAG_INTERVENTI = "tab_interventi"
        const val TAG_PROFILO    = "tab_profilo"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Avvia con la tab Home
        if (savedInstanceState == null) {
            switchTab(TAG_HOME) { HomeTabFragment() }
        }

        view.findViewById<BottomNavigationView>(R.id.bottomNav).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_home       -> { switchTab(TAG_HOME)       { HomeTabFragment()       }; true }
                R.id.tab_interventi -> { switchTab(TAG_INTERVENTI)  { InterventiTabFragment() }; true }
                R.id.tab_profilo    -> { switchTab(TAG_PROFILO)     { ProfiloTabFragment()    }; true }
                else -> false
            }
        }
    }

    /**
     * Switcha tra tab preservando lo stato dei fragment già creati
     * (hide/show invece di replace, come Gmail/WhatsApp).
     */
    private fun switchTab(tag: String, creator: () -> Fragment) {
        if (tag == currentTag && childFragmentManager.findFragmentByTag(tag) != null) return

        val fm      = childFragmentManager
        val current = fm.findFragmentByTag(currentTag)
        val next    = fm.findFragmentByTag(tag) ?: creator().also { new ->
            fm.beginTransaction()
                .add(R.id.navHostContainer, new, tag)
                .commit()
        }

        fm.beginTransaction()
            .apply { current?.let { hide(it) } }
            .show(next)
            .commit()

        currentTag = tag
    }
}
