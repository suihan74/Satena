package com.suihan74.satena.scenes.entries2

import android.content.Context
import androidx.lifecycle.*
import com.suihan74.hatenaLib.Issue
import com.suihan74.hatenaLib.Tag
import com.suihan74.satena.models.Category
import com.suihan74.utilities.OnError
import com.suihan74.utilities.SingleUpdateMutableLiveData

abstract class EntriesFragmentViewModel : ViewModel() {
    /** この画面で表示しているCategory */
    val category by lazy {
        MutableLiveData<Category>()
    }

    /** この画面で表示しているIssue */
    val issue by lazy {
        SingleUpdateMutableLiveData<Issue?>(
            selector = { it?.code }
        )
    }

    /** この画面で表示しているタグ(Category.MyBookmarks, Category.User) */
    val tag by lazy {
        SingleUpdateMutableLiveData<Tag?>(
            selector = { it?.text }
        )
    }

    /** エントリリストを取得するサイトURL */
    val siteUrl by lazy {
        SingleUpdateMutableLiveData<String?>()
    }

    /** Category.Userで表示しているユーザー */
    val user by lazy {
        SingleUpdateMutableLiveData<String?>()
    }

    // タブ管理に関する設定

    /** 位置に対応するタブタイトル */
    abstract fun getTabTitle(context: Context, position: Int) : String
    /** タブ数 */
    abstract val tabCount: Int

    // タブ設定に関する設定ここまで

    /** タブ用ViewModelへの値変更の伝播 */
    open fun connectToTab(
        fragment: EntriesTabFragment,
        entriesAdapter: EntriesAdapter,
        viewModel: EntriesTabFragmentViewModel,
        onError: OnError?
    ) {
        val owner = fragment.viewLifecycleOwner
        val lifecycleScope = fragment.lifecycleScope

        // Issueの変更を監視する
        // Issueの選択を監視している親のEntriesFragmentから状態をもらってくる
        viewModel.issue = issue.value
        issue.observe(owner, Observer {
            if (viewModel.issue == it) return@Observer
            viewModel.issue = it
            // 一度クリアしておかないとスクロール位置が滅茶苦茶になる
            entriesAdapter.clearEntries {
                lifecycleScope.launchWhenResumed {
                    runCatching { viewModel.reloadLists() }
                        .onFailure { e -> onError?.invoke(e) }
                }
            }
        })

        // Tagの変更を監視する
        viewModel.tag = tag.value
        tag.observe(owner, Observer {
            if (viewModel.tag == it) return@Observer
            viewModel.tag = it
            entriesAdapter.clearEntries {
                lifecycleScope.launchWhenResumed {
                    runCatching { viewModel.reloadLists() }
                        .onFailure { e -> onError?.invoke(e) }
                }
            }
        })

        // サイトURL指定
        viewModel.siteUrl = siteUrl.value
        siteUrl.observe(owner, Observer {
            if (category.value != Category.Site && (it == null || viewModel.siteUrl == it)) return@Observer
            viewModel.siteUrl = it
            lifecycleScope.launchWhenResumed {
                runCatching { viewModel.reloadLists() }
                    .onFailure { e -> onError?.invoke(e) }
            }
        })
    }
}
