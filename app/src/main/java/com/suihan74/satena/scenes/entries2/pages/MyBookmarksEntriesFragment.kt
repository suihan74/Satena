package com.suihan74.satena.scenes.entries2.pages

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabAdapter
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.*
import kotlinx.android.synthetic.main.activity_entries2.*
import kotlinx.android.synthetic.main.fragment_entries2.view.*


class MyBookmarksEntriesFragment : TwinTabsEntriesFragment() {
    companion object {
        fun createInstance() = MyBookmarksEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.MyBookmarks)
        }
    }

    private var onBackPressedCallback : OnBackPressedCallback? = null

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
        setHasOptionsMenu(true)
        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val activity = requireActivity()
        val viewModel = viewModel as MyBookmarksViewModel

        inflater.inflate(R.menu.my_bookmarks, menu)

        // 検索窓
        val searchView = (menu.findItem(R.id.search_view)?.actionView as? SearchView)?.also {
            initializeSearchView(it, viewModel)
        }

        // タグ選択メニュー
        val tagsSpinner = (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.also {
            initializeTagsSpinner(it, viewModel)
        }

        // 戻るボタンに割り込む
        onBackPressedCallback?.remove()
        onBackPressedCallback = activity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            detectBackPressedCallbackStatus(viewModel)
        ) {
            if (viewModel.tag.value != null) {
                viewModel.tag.value = null
            }

            if (searchView != null && !searchView.isIconified) {
                searchView.isIconified = true
            }

            isEnabled = detectBackPressedCallbackStatus(viewModel)
        }
    }

    /** 戻るボタン割り込みを有効にするべきかを判別する */
    private fun detectBackPressedCallbackStatus(viewModel: MyBookmarksViewModel) =
        viewModel.isSearchViewExpanded || viewModel.tag.value != null

    /** SearchViewを設定 */
    private fun initializeSearchView(searchView: SearchView, viewModel: MyBookmarksViewModel) = searchView.run {
        val fragment = this@MyBookmarksEntriesFragment

        queryHint = getString(R.string.hint_search_my_bookmarks)
        setQuery(viewModel.searchQuery.value, false)

        // デフォルトでアイコン化する
        setIconifiedByDefault(true)
        if (query.isNullOrBlank()) {
            isIconified = true
        }
        else {
            isIconified = false
            // 画面遷移や回転ごとにキーボードを表示しないようにする
            requireActivity().hideSoftInputMethod(fragment.view?.contentLayout)
            clearFocus()
        }
        viewModel.isSearchViewExpanded = !isIconified

        // ツールバーアイコン長押しで説明を表示する
        findViewById<ImageView>(androidx.appcompat.R.id.search_button)?.run {
            setOnLongClickListener {
                activity?.showToast(R.string.desc_search_mybookmarks)
                true
            }
        }

        // クエリ文字列の変更を監視する
        setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchQuery.value = newText
                return true
            }
            // 検索ボタン押下時にロードを行う
            override fun onQueryTextSubmit(query: String?): Boolean {
                val root = fragment.view

                (root?.entries_tab_pager?.adapter as? EntriesTabAdapter)?.run {
                    refreshLists()
                }

                return (!query.isNullOrBlank()).also {
                    if (it) requireActivity().hideSoftInputMethod(root?.contentLayout)
                }
            }
        })

        // 検索窓を開いたら戻るボタンの割込みをONにする
        setOnSearchClickListener {
            viewModel.isSearchViewExpanded = true
            onBackPressedCallback?.isEnabled = true
        }

        // 検索窓を閉じたらクエリを除去する
        setOnCloseListener {
            viewModel.isSearchViewExpanded = false
            viewModel.searchQuery.value = null
            onBackPressedCallback?.isEnabled = detectBackPressedCallbackStatus(viewModel)

            (fragment.view?.entries_tab_pager?.adapter as? EntriesTabAdapter)?.run {
                refreshLists()
            }
            return@setOnCloseListener false
        }

        // 横幅を最大化
        stretchWidth(requireActivity(), 1)
    }

    /** タグ選択メニューの設定 */
    private fun initializeTagsSpinner(spinner: Spinner, viewModel: MyBookmarksViewModel) {
        // TODO: ロード失敗時にアイコンが表示されないので、暫定的な処置
        spinner.background = requireContext().getDrawable(R.drawable.spinner_allow_tags)
        spinner.backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.colorPrimaryText))

        // タグ一覧のロード完了後に候補リストを作成する
        viewModel.tags.observe(viewLifecycleOwner) { tags ->
            val activity = requireActivity()

            // タグ選択
            spinner.run {
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

        // タグ選択時にサブタイトルを表示する
        val toolbar = requireActivity().toolbar
        viewModel.tag.observe(viewLifecycleOwner) {
            toolbar.subtitle = it?.let { tag -> "${tag.text}(${tag.count})" }

            if (it == null) {
                // リセット時
                spinner.setSelection(0)
                onBackPressedCallback?.isEnabled = detectBackPressedCallbackStatus(viewModel)
            }
            else {
                // 戻るボタンの割り込みを有効化する
                onBackPressedCallback?.isEnabled = true
            }
        }
    }
}
