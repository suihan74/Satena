package com.suihan74.satena.scenes.entries2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.transition.Fade
import androidx.transition.TransitionSet
import com.suihan74.hatenaLib.Issue
import com.suihan74.satena.models.Category
import com.suihan74.utilities.getEnum
import kotlinx.android.synthetic.main.activity_entries2.*
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
    ) : EntriesFragmentViewModel

    /** タイトル */
    open val title : String?
        get() = getString(category.textId)

    /** サブタイトル */
    open val subtitle : String? = null

    //////////////////////////////////////////////////

    /**
     * フラグメント識別用のユニークID
     *
     * viewModelを子フラグメントから参照するために使用する
     * そのためBundleを使用して管理する
     */
    private lateinit var uuid: String

    /** EntriesActivityのViewModel */
    private val activityViewModel : EntriesViewModel by lazy {
        val activity = requireActivity() as EntriesActivity
        activity.viewModel
    }

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

    /** リストを再構成する */
    abstract fun refreshLists()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = requireActivity()

        val arguments = arguments ?: Bundle()
        this.arguments = arguments

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
        val activity = requireActivity() as EntriesActivity
        val toolbar = activity.toolbar

        // ツールバーを更新
        toolbar.also {
            it.title = title
            it.subtitle = subtitle
        }

        activity.showAppBar()

        return null
    }
}
