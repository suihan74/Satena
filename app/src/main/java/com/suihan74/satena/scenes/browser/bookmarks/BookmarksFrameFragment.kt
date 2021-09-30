package com.suihan74.satena.scenes.browser.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBrowserBookmarksFrameBinding
import com.suihan74.satena.scenes.browser.BrowserActivity
import com.suihan74.utilities.ScrollableToTop
import com.suihan74.utilities.TabItem
import com.suihan74.utilities.extensions.alsoAs

/**
 * ブラウザのドロワタブ上のブクマ画面か，それを表示するか確認する画面の表示領域
 */
class BookmarksFrameFragment : Fragment(), ScrollableToTop, TabItem {
    companion object {
        fun createInstance() = BookmarksFrameFragment()
    }

    // ------ //

    private val browserActivity
        get() = requireActivity() as BrowserActivity

    private val browserViewModel
        get() = browserActivity.viewModel

    private val bodyFragment : Fragment?
        get() = childFragmentManager.findFragmentById(R.id.contentsFrame)

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBrowserBookmarksFrameBinding.inflate(inflater, container, false)
        if (savedInstanceState == null) {
            initializeFragment()
        }

        var firstObservingSkipped = false
        browserViewModel.entryUrl.observe(viewLifecycleOwner, { entryUrl ->
            if (browserViewModel.autoFetchBookmarks.value == true) return@observe
            if (firstObservingSkipped) {
                val prevUrl = browserViewModel.bookmarksRepo.url
                lifecycleScope.launchWhenResumed {
                    if (entryUrl != prevUrl) {
                        browserViewModel.bookmarksRepo.clear()
                        startConfirmFragment()
                    }
                }
            }
            firstObservingSkipped = true
        })

        return binding.root
    }

    // ------ //

    fun startBookmarksFragment() {
        childFragmentManager.beginTransaction()
            .replace(R.id.contentsFrame, BookmarksFragment.createInstance())
            .commit()
    }

    fun startConfirmFragment() {
        childFragmentManager.beginTransaction()
            .replace(R.id.contentsFrame, ConfirmationFragment.createInstance())
            .commit()
    }

    private fun initializeFragment() {
        if (browserViewModel.autoFetchBookmarks.value == true) {
            startBookmarksFragment()
        }
        else {
            startConfirmFragment()
        }
    }

    // ------ //

    override fun onTabSelected() {
        bodyFragment.alsoAs<TabItem> { f -> f.onTabSelected() }
    }

    override fun onTabUnselected() {
        bodyFragment.alsoAs<TabItem> { f -> f.onTabUnselected() }
    }

    override fun onTabReselected() {
        bodyFragment.alsoAs<TabItem> { f -> f.onTabReselected() }
    }

    // ------ //

    override fun scrollToTop() {
        bodyFragment.alsoAs<ScrollableToTop> { f -> f.scrollToTop() }
    }
}
