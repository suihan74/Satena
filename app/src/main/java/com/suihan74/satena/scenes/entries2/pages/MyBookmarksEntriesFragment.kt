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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesViewModel
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.bindings.setVisibility
import com.suihan74.utilities.extensions.*
import com.suihan74.utilities.provideViewModel

class MyBookmarksEntriesFragment : MultipleTabsEntriesFragment() {
    companion object {
        fun createInstance() = MyBookmarksEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.MyBookmarks)
        }
    }

    // ------ //

    private val activityViewModel by activityViewModels<EntriesViewModel>()

    private val fragmentViewModel: MyBookmarksViewModel
        get() = viewModel as MyBookmarksViewModel

    private var onBackPressedCallback : OnBackPressedCallback? = null

    // ------ //

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ) = provideViewModel(owner, viewModelKey) {
        MyBookmarksViewModel(repository)
    }

    override fun updateActivityAppBar(
        activity: EntriesActivity,
        tabLayout: TabLayout,
        bottomAppBar: BottomAppBar?
    ): Boolean {
        val result = super.updateActivityAppBar(activity, tabLayout, bottomAppBar)

        if (bottomAppBar == null) {
            setHasOptionsMenu(true)
        }
        else {
            setHasOptionsMenu(false)
            activity.inflateExtraBottomMenu(R.menu.my_bookmarks_bottom)
            initializeMenu(bottomAppBar.menu, bottomAppBar)
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
        val activity = requireActivity() as EntriesActivity
        val viewModel = fragmentViewModel

        // 検索窓
        val searchView =
            if (bottomAppBar == null) menu.findItem(R.id.search_view)?.actionView as? SearchView
            else activity.bottomSearchView?.also { searchView ->
                bottomAppBar.menu.findItem(R.id.search_view).setOnMenuItemClickListener {
                    viewModel.isSearchViewExpanded = !viewModel.isSearchViewExpanded
                    searchView.setVisibility(viewModel.isSearchViewExpanded)
                    onBackPressedCallback?.isEnabled = detectBackPressedCallbackStatus(viewModel)
                    true
                }
            }

        searchView?.let {
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

            if (searchView != null) {
                if (activity.viewModel.isBottomLayoutMode) {
                    if (searchView.visibility == View.VISIBLE) {
                        searchView.visibility = View.GONE
                        viewModel.isSearchViewExpanded = false
                        searchView.setQuery("", false)
                        viewModel.searchQuery.value = ""
                        setSubTitle(viewModel)
                        reloadLists()
                    }
                }
                else {
                    if (!searchView.isIconified) {
                        searchView.isIconified = true
                    }
                }
            }

            isEnabled = detectBackPressedCallbackStatus(viewModel)
        }
    }

    /** 戻るボタン割り込みを有効にするべきかを判別する */
    private fun detectBackPressedCallbackStatus(viewModel: MyBookmarksViewModel) =
        viewModel.isSearchViewExpanded || viewModel.tag.value != null

    /** SearchViewを設定 */
    private fun initializeSearchView(
        searchView: SearchView,
        viewModel: MyBookmarksViewModel,
        menu: Menu,
        bottomAppBar: BottomAppBar?
    ) = searchView.run {
        val fragment = this@MyBookmarksEntriesFragment

        queryHint = getString(R.string.hint_search_my_bookmarks)
        setQuery(viewModel.searchQuery.value, false)

        if (bottomAppBar == null) {
            // デフォルトでアイコン化する
            setIconifiedByDefault(true)
            findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)?.run {
                val color = context.getColor(R.color.colorPrimaryText)
                setTextColor(color)
                setHintTextColor(color)
            }
            if (query.isNullOrBlank()) {
                isIconified = true
            }
            else {
                isIconified = false
                // 画面遷移や回転ごとにキーボードを表示しないようにする
                requireActivity().hideSoftInputMethod(fragment.contentLayout)
                clearFocus()
            }
            viewModel.isSearchViewExpanded = !isIconified
        }
        else {
            findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)?.run {
                setTextColor(context.getThemeColor(R.attr.textColor))
            }

            if (!query.isNullOrBlank()) {
                // 画面遷移や回転ごとにキーボードを表示しないようにする
                requireActivity().hideSoftInputMethod(fragment.contentLayout)
                clearFocus()
            }
            viewModel.isSearchViewExpanded = false
        }

        // ツールバーアイコン長押しで説明を表示する
        findViewById<ImageView>(androidx.appcompat.R.id.search_button)?.let {
            TooltipCompat.setTooltipText(it, getString(R.string.desc_search_mybookmarks))
        }

        // クエリ文字列の変更を監視する
        setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
            // 検索ボタン押下時にロードを行う
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchQuery.value = query
                setSubTitle(viewModel)
                reloadLists()
                requireActivity().hideSoftInputMethod(fragment.contentLayout)
                return true
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
            setQuery("", true)
            onBackPressedCallback?.isEnabled = detectBackPressedCallbackStatus(viewModel)

            return@setOnCloseListener false
        }

        // 横幅を最大化
        if (bottomAppBar == null) {
            stretchWidth(menu)
        }
    }

    /** タグ選択メニューの設定 */
    private fun initializeTagsSpinner(menu: Menu, viewModel: MyBookmarksViewModel) {
        val menuItem = menu.findItem(R.id.issues_spinner)
        val spinner = menuItem?.actionView.alsoAs<Spinner> {
            it.visibility = View.GONE
        } ?: return

        // タグ一覧のロード完了後に候補リストを作成する
        viewModel.tags.observe(viewLifecycleOwner, { tags ->
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
        })

        // タグ選択時にサブタイトルを表示する
        viewModel.tag.observe(viewLifecycleOwner, {
            setSubTitle(fragmentViewModel)

            if (it == null) {
                // リセット時
                spinner.setSelection(0)
                onBackPressedCallback?.isEnabled = detectBackPressedCallbackStatus(viewModel)
            }
            else {
                // 戻るボタンの割り込みを有効化する
                onBackPressedCallback?.isEnabled = true
            }
        })
    }

    /** 検索情報をサブタイトルに表示する */
    private fun setSubTitle(viewModel: MyBookmarksViewModel) {
        val tag = viewModel.tag.value
        val query = viewModel.searchQuery.value
        val isQueryBlank = query.isNullOrBlank()

        activityViewModel.toolbarSubTitle.value =
            when {
                tag == null && isQueryBlank -> null
                tag == null -> query
                isQueryBlank -> "${tag.text}(${tag.count})"
                else -> "${tag.text}(${tag.count}),$query"
            }
    }
}
