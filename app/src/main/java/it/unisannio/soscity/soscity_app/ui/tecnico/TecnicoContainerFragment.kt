package it.unisannio.soscity.soscity_app.ui.tecnico

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import it.unisannio.soscity.soscity_app.databinding.FragmentTecnicoContainerBinding

class TecnicoContainerFragment : Fragment() {

    private var _binding: FragmentTecnicoContainerBinding? = null
    private val binding get() = _binding!!

    private var currentTag = TAG_HOME

    companion object {
        const val TAG_HOME       = "tab_home"
        const val TAG_INTERVENTI = "tab_interventi"
        const val TAG_PROFILO    = "tab_profilo"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTecnicoContainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            switchTab(TAG_HOME) { HomeTabFragment() }
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                it.unisannio.soscity.soscity_app.R.id.tab_home       -> { switchTab(TAG_HOME)       { HomeTabFragment()       }; true }
                it.unisannio.soscity.soscity_app.R.id.tab_interventi  -> { switchTab(TAG_INTERVENTI)  { InterventiTabFragment() }; true }
                it.unisannio.soscity.soscity_app.R.id.tab_profilo     -> { switchTab(TAG_PROFILO)     { ProfiloTabFragment()    }; true }
                else -> false
            }
        }
    }

    /**
     * Switcha tra tab preservando lo stato dei fragment gia' creati
     * (hide/show invece di replace, come Gmail/WhatsApp).
     */
    private fun switchTab(tag: String, creator: () -> Fragment) {
        if (tag == currentTag && childFragmentManager.findFragmentByTag(tag) != null) return

        val fm      = childFragmentManager
        val current = fm.findFragmentByTag(currentTag)
        val next    = fm.findFragmentByTag(tag) ?: creator().also { new ->
            fm.beginTransaction()
                .add(it.unisannio.soscity.soscity_app.R.id.navHostContainer, new, tag)
                .commit()
        }

        fm.beginTransaction()
            .apply { current?.let { hide(it) } }
            .show(next)
            .commit()

        currentTag = tag
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
