package com.suihan74.satena.scenes.bookmarks2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.suihan74.satena.R
import com.suihan74.satena.models.PreferenceKey
import com.suihan74.satena.scenes.bookmarks2.tab.BookmarksTabViewModel
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.setOnTabLongClickListener
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_bookmarks2.view.*

class BookmarksFragmentViewModel : ViewModel() {
    /** 選択されたタブのポジション */
    val selectedTab by lazy {
        MutableLiveData<Int>()
    }

    /** 選択されたタブがもつViewModel */
    val selectedTabViewModel by lazy {
        MutableLiveData<BookmarksTabViewModel>()
    }
}

class BookmarksFragment : Fragment() {
    /** BookmarksFragmentの状態管理用ViewModel */
    private val viewModel: BookmarksFragmentViewModel by lazy {
        ViewModelProvider(this)[BookmarksFragmentViewModel::class.java]
    }

    companion object {
        fun createInstance() = BookmarksFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bookmarks2, container, false)

        val selectedTab =
            viewModel.selectedTab.value
            ?: SafeSharedPreferences.create<PreferenceKey>(context).run {
                getInt(PreferenceKey.BOOKMARKS_INITIAL_TAB)
            }

        // TabFragment表示部分
        val tabAdapter = BookmarksTabAdapter(this)
        val viewPager = view.tab_pager.apply {
            adapter = tabAdapter
            currentItem = selectedTab
        }

        // Tabセレクタ
        view.tab_layout.apply {
            setupWithViewPager(viewPager)
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.position?.let {
                        viewModel.selectedTab.postValue(it)
                    }
                    // タブを切り替えたら画面下部のボタンを再表示する
                    (activity as? BookmarksActivity)?.showButtons()
                }
                override fun onTabUnselected(p0: TabLayout.Tab?) {
                }
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    viewModel.selectedTabViewModel.value?.scrollToTop()
                }
            })

            // タブを長押しで最初に表示するタブを変更
            setOnTabLongClickListener { idx ->
                val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
                val key = PreferenceKey.BOOKMARKS_INITIAL_TAB
                if (prefs.getInt(key) != idx) {
                    prefs.edit {
                        put(key, idx)
                    }
                    context.showToast(R.string.msg_bookmarks_initial_tab_changed, getString(BookmarksTabType.fromInt(idx).textId))
                }
                return@setOnTabLongClickListener true
            }
        }

        return view
    }
}
