package com.suihan74.satena.scenes.browser.bookmarks

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.databinding.FragmentBrowserBookmarksConfirmationBinding
import com.suihan74.satena.scenes.browser.BrowserViewModel

/**
 * ブクマタブの内容を取得・表示するか確認する画面
 */
class ConfirmationFragment : Fragment() {
    companion object {
        fun createInstance() = ConfirmationFragment()
    }

    // ------ //

    private val viewModel by viewModels<ConfirmationViewModel>()
    private val browserViewModel by activityViewModels<BrowserViewModel>()

    private val frameFragment
        get() = requireParentFragment() as BookmarksFrameFragment

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBrowserBookmarksConfirmationBinding.inflate(inflater, container, false).also {
            it.vm = viewModel.apply {
                onCreateView(viewLifecycleOwner, browserViewModel)
            }
            it.browserVM = browserViewModel
            it.frameFragment = frameFragment
            it.lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }
}

// ------ //

class ConfirmationViewModel : ViewModel() {
    val url = MutableLiveData<String>()

    val notShowAgain = MutableLiveData<Boolean>()

    // ------- //

    fun onCreateView(owner: LifecycleOwner, browserViewModel: BrowserViewModel) {
        browserViewModel.url.observe(owner, {
            url.value = Uri.decode(it)
        })
    }

    // ------- //

    /**
     * 確認を完了し`BookmarksFragment`に遷移する
     */
    fun startBookmarksFragment(container: BookmarksFrameFragment, browserViewModel: BrowserViewModel) {
        if (notShowAgain.value == true) {
            browserViewModel.autoFetchBookmarks.value = true
        }
        container.startBookmarksFragment()
    }
}
