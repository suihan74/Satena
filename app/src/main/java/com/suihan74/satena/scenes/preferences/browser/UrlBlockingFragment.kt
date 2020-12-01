package com.suihan74.satena.scenes.preferences.browser

import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentUrlBlockingBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.pages.PreferencesBrowserFragment
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.letAs
import com.suihan74.utilities.lazyProvideViewModel

/** URLブロック設定を編集する */
class UrlBlockingFragment : Fragment() {
    companion object {
        fun createInstance() = UrlBlockingFragment()
    }

    val viewModel by lazyProvideViewModel {
        val context = requireContext()
        val repository =
            parentFragment.letAs<PreferencesBrowserFragment, BrowserRepository> {
                it.viewModel.browserRepo
            } ?: BrowserRepository(
                client = HatenaClient,
                prefs = SafeSharedPreferences.create(context),
                browserSettings =  SafeSharedPreferences.create(context)
            )

        UrlBlockingViewModel(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentUrlBlockingBinding>(
            inflater,
            R.layout.fragment_url_blocking,
            null,
            false
        ).also {
            it.vm = viewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        binding.recyclerView.adapter = BlockUrlSettingsAdapter(viewLifecycleOwner).also { adapter ->
            adapter.setOnLongLickItemListener { item ->
                val model = item.model ?: return@setOnLongLickItemListener
                viewModel.openItemMenuDialog(model, childFragmentManager)
            }
        }

        binding.addButton.let {
            it.size =
                if (activity is PreferencesActivity) FloatingActionButton.SIZE_NORMAL
                else FloatingActionButton.SIZE_MINI

            it.setOnClickListener {
                val activity = activity
                if (activity is BrowserActivity) {
                    // ブラウザから呼ばれている場合、
                    // 現在表示しているページのリソース情報を含めたダイアログを表示させる
                    activity.viewModel.openBlockUrlDialog(childFragmentManager)
                }
                else {
                    viewModel.openBlockUrlDialog(childFragmentManager)
                }
            }
        }

        return binding.root
    }
}
