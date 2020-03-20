package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.pages.HatenaEntriesViewModel
import com.suihan74.satena.scenes.entries2.pages.MyBookmarksViewModel
import com.suihan74.satena.scenes.entries2.pages.UserEntriesViewModel
import com.suihan74.utilities.getEnum
import kotlinx.android.synthetic.main.activity_entries2.*
import java.util.*

abstract class EntriesFragment : Fragment() {
    companion object {
        const val ARG_CATEGORY = "ARG_CATEGORY"

        private const val ARG_UUID = "ARG_UUID"
    }

    /**
     * フラグメント識別用のユニークID
     *
     * viewModelを子フラグメントから参照するために使用する
     * そのためBundleを使用して管理する
     */
    private lateinit var uuid: String

    /** EntriesActivityのViewModel */
    protected lateinit var activityViewModel : EntriesViewModel

    protected lateinit var viewModel : EntriesFragmentViewModel

    /** タブ側からこのフラグメントのVMにアクセスするためのキー */
    val viewModelKey: String
        get() = "EntriesFragment_${uuid}"

    /** この画面のCategory */
    val category : Category
        get() = viewModel.category.value!!

    /** この画面のIssue */
    val issue : Issue?
        get() = viewModel.issue.value

    /** タブタイトルを取得する */
    fun getTabTitle(position: Int) = viewModel.getTabTitle(requireContext(), position)
    val tabCount get() = viewModel.tabCount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()

        val arguments = arguments ?: Bundle.EMPTY
        this.arguments = arguments

        // UUIDを生成
        uuid = arguments.getString(ARG_UUID) ?: UUID.randomUUID().toString()
        arguments.putString(ARG_UUID, uuid)

        activityViewModel = ViewModelProvider(activity)[EntriesViewModel::class.java]
        val category = arguments.getEnum<Category>(ARG_CATEGORY)!!
        val repository = activityViewModel.repository

        val viewModelType =
            when (category) {
                Category.MyBookmarks -> MyBookmarksViewModel::class.java
                Category.User -> UserEntriesViewModel::class.java
                else -> HatenaEntriesViewModel::class.java
            }

        viewModel =
            if (savedInstanceState != null) {
                ViewModelProvider(activity)[viewModelKey, viewModelType]
            }
            else {
                val factory =
                    when (category) {
                        Category.MyBookmarks -> MyBookmarksViewModel.Factory(repository) // TODO
                        Category.User -> UserEntriesViewModel.Factory(repository)
                        else -> HatenaEntriesViewModel.Factory(repository)
                    }
                ViewModelProvider(activity, factory)[viewModelKey, viewModelType]
            }
        viewModel.category.value = category

        // 画面遷移時にフェードする
        enterTransition = TransitionSet().addTransition(Fade())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val activity = requireActivity() as EntriesActivity
        val toolbar = activity.toolbar

        // ツールバーを更新
        toolbar.also {
            it.title = title
            it.subtitle = viewModel.issue.value?.name
        }

        // Issue選択時にサブタイトルを表示する
        viewModel.issue.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = it?.name
        })

        // タグ選択時にサブタイトルを表示する
        viewModel.tag.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = it?.let { tag -> "${tag.text}(${tag.count})" }
        })

        // Category.SiteではサイトURLをタイトルに表示する
        viewModel.siteUrl.observe(viewLifecycleOwner, Observer {
            toolbar.title = title
        })

        // Category.UserではユーザーIDをタイトルに表示する
        viewModel.user.observe(viewLifecycleOwner, Observer {
            toolbar.title = title
        })

        activity.showAppBar()

        return null
    }

    /** ツールバーのタイトル部分に表示する内容 */
    private val title : String?
        get() = when (viewModel.category.value) {
            Category.Site -> viewModel.siteUrl.value
            Category.User -> "id:${viewModel.user.value}"
            else -> getString(viewModel.category.value?.textId!!)
        }
}
