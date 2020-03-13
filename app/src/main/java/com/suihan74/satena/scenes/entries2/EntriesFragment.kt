package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import com.suihan74.satena.scenes.entries2.pages.HatenaEntriesViewModel
import com.suihan74.satena.scenes.entries2.pages.MyBookmarksViewModel
import com.suihan74.utilities.getEnum
import kotlinx.android.synthetic.main.activity_entries2.*
import java.util.*

abstract class EntriesFragment : Fragment() {
    companion object {
        @JvmStatic
        protected val ARG_CATEGORY = "ARG_CATEGORY"

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
    fun getTabTitleId(position: Int) = viewModel.getTabTitleId(position)
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

        val viewModelType =
            if (category == Category.MyBookmarks) MyBookmarksViewModel::class.java
            else HatenaEntriesViewModel::class.java

        viewModel = ViewModelProvider(activity)[viewModelKey, viewModelType]
        viewModel.category.value = category
        setHasOptionsMenu(category == Category.MyBookmarks || category.hasIssues)

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
        toolbar.apply {
            setTitle(viewModel.category.value?.textId ?: 0)
            subtitle = viewModel.issue.value?.name
        }

        // Issue選択時にサブタイトルを表示する
        viewModel.issue.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = it?.name
        })

        activity.showAppBar()

        return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        viewModel.issues.observe(viewLifecycleOwner, Observer { issues ->
            if (issues != null) {
                inflateIssuesMenu(menu, inflater, issues)
            }
        })
    }

    /** カテゴリごとの特集を選択する追加メニュー */
    private fun inflateIssuesMenu(menu: Menu, inflater: MenuInflater, issues: List<Issue>) {
        val activity = requireActivity() as EntriesActivity
        val spinnerItems = issues.map { it.name }

        inflater.inflate(R.menu.spinner_issues, menu)

        (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
            initialize(activity, spinnerItems, R.drawable.spinner_allow_issues, getString(R.string.desc_issues_spinner)) { position ->
                viewModel.issue.value =
                    if (position == null) null
                    else {
                        val item = spinnerItems[position]
                        issues.firstOrNull { it.name == item }
                    }
            }

            if (viewModel.issue.value != null) {
                val currentIssueName = viewModel.issue.value?.name
                val position = spinnerItems.indexOfFirst { it == currentIssueName }
                if (position >= 0) {
                    setSelection(position + 1)
                }
            }
        }
    }
}
