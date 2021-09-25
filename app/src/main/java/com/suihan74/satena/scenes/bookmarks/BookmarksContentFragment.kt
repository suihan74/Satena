package com.suihan74.satena.scenes.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentBookmarksContentBinding
import com.suihan74.utilities.extensions.dp2px

class BookmarksContentFragment : Fragment() {
    companion object {
        fun createInstance() = BookmarksContentFragment()
    }

    // ------ //

    private val bookmarksActivity
        get() = requireActivity() as BookmarksActivity

    private val bookmarksViewModel
        get() = bookmarksActivity.bookmarksViewModel

    private val contentsViewModel
        get() = bookmarksActivity.contentsViewModel

    private var _binding : FragmentBookmarksContentBinding? = null
    private val binding get() = _binding!!

    // ------ //

    private val tileHeight : Int
        get() = requireContext().dp2px(80)

    private val extraMargin : Int
        get() = binding.tabPager.measuredHeight - tileHeight * 3

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksContentBinding.inflate(inflater, container, false).also {
            it.bookmarksViewModel = bookmarksViewModel
            it.contentsViewModel = contentsViewModel
            it.lifecycleOwner = viewLifecycleOwner
        }

        // イベントリスナの設定
        bookmarksViewModel.initializeListeners(bookmarksActivity)

        // タブ制御の初期化
        contentsViewModel.initializeTabPager(
            requireActivity(),
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
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.buttons_layout, FloatingActionButtonsFragment.createInstance())
                .commitAllowingStateLoss()
        }

        // ツールバータップ
        binding.toolbar.setOnClickListener {
            bookmarksViewModel.onClickToolbar(bookmarksActivity)
        }
        // ツールバーロングタップ
        binding.toolbar.setOnLongClickListener {
            bookmarksViewModel.onLongClickToolbar(bookmarksActivity)
            true
        }

        // エクストラスクロールによる上部マージン設定
        contentsViewModel.extraScrollProgress.observe(viewLifecycleOwner, Observer {
            binding.tabPager.updatePadding(
                top = (extraMargin * it).toInt()
            )
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.toolbar.startMarquee()

        lifecycleScope.launchWhenCreated {
            runCatching {
                contentsViewModel.updateExtraScrollBarVisibility(
                    extraMargin > tileHeight
                )
            }
        }
    }
}
