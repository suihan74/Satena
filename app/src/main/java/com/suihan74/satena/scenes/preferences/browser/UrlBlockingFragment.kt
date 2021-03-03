package com.suihan74.satena.scenes.preferences.browser

import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.*
import androidx.activity.addCallback
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.hatenaLib.HatenaClient
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentUrlBlockingBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.satena.scenes.browser.BrowserRepository
import com.suihan74.satena.scenes.browser.DrawerTab
import com.suihan74.satena.scenes.preferences.PreferencesActivity
import com.suihan74.satena.scenes.preferences.PreferencesTabMode
import com.suihan74.satena.scenes.preferences.pages.BrowserFragment
import com.suihan74.utilities.DrawableCompat
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.extensions.letAs
import com.suihan74.utilities.lazyProvideViewModel

/** URLブロック設定を編集する */
class UrlBlockingFragment : Fragment() {
    companion object {
        fun createInstance() = UrlBlockingFragment()
    }

    // ------ //

    private val preferencesActivity : PreferencesActivity?
        get() = requireActivity() as? PreferencesActivity

    private val browserActivity : BrowserActivity?
        get() = requireActivity() as? BrowserActivity

    val viewModel by lazyProvideViewModel {
        val context = requireContext()
        val repository =
            parentFragment.letAs<BrowserFragment, BrowserRepository> {
                it.viewModel.browserRepo
            } ?: BrowserRepository(
                client = HatenaClient,
                prefs = SafeSharedPreferences.create(context),
                browserSettings =  SafeSharedPreferences.create(context)
            )

        UrlBlockingViewModel(repository)
    }

    // ------ //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))

        exitTransition = TransitionSet()
            .addTransition(Fade())
            .addTransition(Slide(Gravity.END))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.beginTransaction()
                .remove(this@UrlBlockingFragment)
                .commit()
        }
        preferencesActivity?.viewModel?.currentTab?.observe(viewLifecycleOwner, {
            callback.isEnabled = it == PreferencesTabMode.BROWSER
        })
        browserActivity?.viewModel?.let { vm ->
            vm.currentDrawerTab.observe(viewLifecycleOwner, {
                callback.isEnabled = it == DrawerTab.SETTINGS && vm.drawerOpened.value == true
            })
            vm.drawerOpened.observe(viewLifecycleOwner, {
                callback.isEnabled = vm.currentDrawerTab.value == DrawerTab.SETTINGS && it == true
            })
        }

        binding.backButton.setOnClickListener {
            callback.handleOnBackPressed()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (preferencesActivity != null) {
            setHasOptionsMenu(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.browser_url_blocking_list, menu)

        menu.findItem(R.id.button).apply {
            val color = ActivityCompat.getColor(requireActivity(), R.color.colorPrimaryText)
            DrawableCompat.setColorFilter(icon.mutate(), color)

            setOnMenuItemClickListener {
                activity?.onBackPressed()
                true
            }
        }
    }
}
