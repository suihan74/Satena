package com.suihan74.satena.scenes.entries2.pages

import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.observe
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.*
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

    override fun onResume() {
        super.onResume()

        activity?.alsoAs<EntriesActivity> {
            setHasOptionsMenu(!it.viewModel.isBottomLayoutMode)
        }
    }

    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ): Boolean {
        val result = super.updateActivityAppBar(activity, tabLayout, bottomAppBar)

        bottomAppBar?.let { appBar ->
//            activity.menuInflater.inflate(R.menu.my_bookmarks_bottom, activity.bottomMenu)
            appBar.inflateMenu(R.menu.my_bookmarks_bottom)
            initializeMenu(appBar.menu, appBar)
        }

        return result
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.my_bookmarks, menu)
        initializeMenu(menu)
    }

    /** メニュー初期化処理 */
    private fun initializeMenu(menu: Menu, bottomAppBar: BottomAppBar? = null) {
        val activity = requireActivity()
        val viewModel = viewModel as MyBookmarksViewModel

        // 検索窓
        val searchView = menu.findItem(R.id.search_view)?.actionView.alsoAs<SearchView> {
            initializeSearchView(it, viewModel, menu, bottomAppBar)
        }

        // タグ選択メニュー
        initializeTagsSpinner(menu, viewModel)

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
    private fun initializeSearchView(searchView: SearchView, viewModel: MyBookmarksViewModel, menu: Menu, bottomAppBar: BottomAppBar?) = searchView.run {
        val fragment = this@MyBookmarksEntriesFragment

        if (bottomAppBar != null) {
            val color = context.getThemeColor(R.attr.textColor)
            val editText = findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
            editText?.setTextColor(color)
        }

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
        findViewById<ImageView>(androidx.appcompat.R.id.search_button)?.let {
            TooltipCompat.setTooltipText(it, getString(R.string.desc_search_mybookmarks))
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
        stretchWidth(requireActivity(), menu, bottomAppBar != null)
    }

    /** タグ選択メニューの設定 */
    private fun initializeTagsSpinner(menu: Menu, viewModel: MyBookmarksViewModel) {
        val menuItem = menu.findItem(R.id.issues_spinner)
        val spinner = menuItem?.actionView.alsoAs<Spinner> {
            it.visibility = View.GONE
        } ?: return

        // タグ一覧のロード完了後に候補リストを作成する
        viewModel.tags.observe(viewLifecycleOwner) { tags ->
            val activity = requireActivity()

            if (tags.isNotEmpty()) {
                spinner.visibility = View.VISIBLE
            }

            // タグ選択
            spinner.run {
                val spinnerItems = tags.map { "${it.text}(${it.count})" }
                initialize(
                    activity,
                    menuItem,
                    spinnerItems,
                    R.string.desc_tags_spinner
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
