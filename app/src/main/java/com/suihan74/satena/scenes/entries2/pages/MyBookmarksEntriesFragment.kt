package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.showToast
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.activity_entries2.*

class MyBookmarksEntriesFragment : TwinTabsEntriesFragment() {
    companion object {
        fun createInstance() = MyBookmarksEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.MyBookmarks)
        }
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = MyBookmarksViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, MyBookmarksViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        // タグを選択している場合、戻るボタンでタグ選択を解除する
        val clearTagCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.tag.value != null) {
                    viewModel.tag.value = null
                }
            }
        }

        // タグ選択時にサブタイトルを表示する
        val toolbar = requireActivity().toolbar
        viewModel.tag.observe(viewLifecycleOwner) {
            toolbar.subtitle = it?.let { tag -> "${tag.text}(${tag.count})" }

            // 戻るボタンの処理に割り込む
            clearTagCallback.isEnabled = it != null
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, clearTagCallback)

        setHasOptionsMenu(true)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val viewModel = viewModel as MyBookmarksViewModel

        inflater.inflate(R.menu.my_bookmarks, menu)

        // TODO: ブクマ検索
        (menu.findItem(R.id.search_view)?.actionView as? SearchView)?.run {
            queryHint = getString(R.string.hint_search_my_bookmarks)

            // デフォルトでアイコン化する
            setIconifiedByDefault(true)
            isIconified = true

            // ツールバーアイコン長押しで説明を表示する
            findViewById<ImageView>(androidx.appcompat.R.id.search_button)?.run {
                setOnLongClickListener {
                    activity?.showToast(R.string.desc_search_mybookmarks)
                    true
                }
            }

            // 左端の余分なマージンを削るための設定
            arrayOf(
                androidx.appcompat.R.id.search_edit_frame,
                androidx.appcompat.R.id.search_mag_icon
            ).forEach { targetId ->
                findViewById<View>(targetId)?.updateLayoutParams<LinearLayout.LayoutParams> {
                    marginStart = 0
                    leftMargin = 0
                }
            }
        }

        viewModel.tags.observe(viewLifecycleOwner) { tags ->
            val activity = requireActivity()

            // タグ選択
            (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
                val spinnerItems = tags.map { "${it.text}(${it.count})" }
                initialize(
                    activity,
                    spinnerItems,
                    R.drawable.spinner_allow_tags,
                    getString(R.string.desc_tags_spinner)
                ) { position ->
                    val tag =
                        if (position == null) null
                        else tags[position]

                    viewModel.tag.value = tag
                }

                if (viewModel.tag.value != null) {
                    val currentIssueName = viewModel.tag.value?.text
                    val position = tags.indexOfFirst { it.text == currentIssueName }
                    if (position >= 0) {
                        setSelection(position + 1)
                    }
                }
            }
        }
    }
}
