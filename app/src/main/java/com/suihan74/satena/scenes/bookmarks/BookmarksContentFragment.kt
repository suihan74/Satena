package com.suihan74.satena.scenes.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarksContentBinding

class BookmarksContentFragment : Fragment() {
    companion object {
        fun createInstance() = BookmarksContentFragment()
    }

    // ------ //

    val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    val bookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    val contentsViewModel
        get() = bookmarksActivity.contentsViewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBookmarksContentBinding.inflate(inflater, container, false).also {
            it.bookmarksViewModel = bookmarksViewModel
            it.contentsViewModel = contentsViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        // イベントリスナの設定
        bookmarksViewModel.initializeListeners(bookmarksActivity)

        // タブ制御の初期化
        contentsViewModel.initializeTabPager(
            requireContext(),
            childFragmentManager,
            binding.tabPager,
            binding.tabLayout
        )

        // スクロールにあわせてビューを隠す設定を反映させる
        contentsViewModel.setScrollingBehavior(
            requireContext(),
            binding.toolbar,
            binding.buttonsLayout
        )

        // 下部ボタンエリアを生成
        childFragmentManager.beginTransaction()
            .replace(R.id.buttons_layout, FloatingActionButtonsFragment.createInstance())
            .commitAllowingStateLoss()

        return binding.root
    }
}
