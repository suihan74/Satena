package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelStoreOwner
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.tabs.TabLayout
import com.suihan74.hatenaLib.BookmarkResult
import com.suihan74.hatenaLib.Entry
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.models.Category
import com.suihan74.utilities.extensions.getEnum
import com.suihan74.utilities.extensions.requireActivity
import com.suihan74.utilities.extensions.toVisibility
import java.util.*

abstract class EntriesFragment : Fragment() {
    companion object {
        const val ARG_CATEGORY = "ARG_CATEGORY"

        private const val ARG_UUID = "ARG_UUID"
    }

    //////////////////////////////////////////////////

    /** ViewModelを生成する */
    abstract fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel

    //////////////////////////////////////////////////

    /**
     * フラグメント識別用のユニークID
     *
     * viewModelを子フラグメントから参照するために使用する
     * そのためBundleを使用して管理する
     */
    private lateinit var uuid: String

    private val entriesActivity
        get() = requireActivity() as EntriesActivity

    /** EntriesActivityのViewModel */
    private val activityViewModel by lazy {
        requireActivity<EntriesActivity>().viewModel
    }

    protected lateinit var viewModel: EntriesFragmentViewModel

    /** タブ側からこのフラグメントのVMにアクセスするためのキー */
    val viewModelKey: String
        get() = "EntriesFragment_${uuid}"

    /** この画面のCategory */
    val category: Category
        get() = viewModel.category.value!!

    /** この画面のIssue */
    val issue: Issue?
        get() = viewModel.issue.value

    /** タブタイトルを取得する */
    fun getTabTitle(position: Int) = viewModel.getTabTitle(requireContext(), position)
    val tabCount get() = viewModel.tabCount

    /** リストを再構成する */
    abstract fun reloadLists()

    /** リストを再構成する(取得を行わない単なる再配置) */
    abstract fun refreshLists()

    /** エントリに付けたブクマを削除 */
    abstract fun removeBookmark(entry: Entry)

    /** エントリに付けたブクマを更新する */
    abstract fun updateBookmark(entry: Entry, bookmarkResult: BookmarkResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()
        val arguments = requireArguments()

        // UUIDを生成
        uuid = arguments.getString(ARG_UUID) ?: UUID.randomUUID().toString()
        arguments.putString(ARG_UUID, uuid)

        val category = arguments.getEnum<Category>(ARG_CATEGORY)!!
        val repository = activityViewModel.repository

        viewModel = generateViewModel(activity, viewModelKey, repository, category).also {
            it.category.value = category
        }

        // 画面遷移時にフェードする
        enterTransition = TransitionSet().addTransition(Fade())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        activityViewModel.toolbarTitle.value = getString(category.textId)
        activityViewModel.toolbarSubTitle.value = ""
        entriesActivity.showAppBar()
        return null
    }

    override fun onResume() {
        super.onResume()

        // EntriesActivityのTabLayoutとBottomAppBarをFragment側に公開する
        val tabLayout = entriesActivity.initializeTabLayout()

        val bottomAppBar = entriesActivity.initializeBottomAppBar()

        tabLayout.visibility =
            updateActivityAppBar(entriesActivity, tabLayout, bottomAppBar).toVisibility(defaultInvisible = View.GONE)
    }

    /**
     * BottomAppBarやTabLayoutをフラグメントに合わせた内容に更新する
     *
     * タブレイアウトの表示状態を返す
     */
    open fun updateActivityAppBar(activity: EntriesActivity, tabLayout: TabLayout, bottomAppBar: BottomAppBar?) : Boolean = false

    /** 一番上までスクロール */
    abstract fun scrollToTop()
}
