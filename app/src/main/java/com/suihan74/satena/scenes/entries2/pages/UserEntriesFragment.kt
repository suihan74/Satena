package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragmentBase
import com.suihan74.satena.scenes.entries2.initialize
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.activity_entries2.*

class UserEntriesFragment : SingleTabEntriesFragment() {
    companion object {
        fun createInstance(user: String) = UserEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, Category.User)
            putString(ARG_USER, user)
        }

        private const val ARG_USER = "ARG_USER"
    }

    override fun generateViewModel(
        owner: ViewModelStoreOwner,
        viewModelKey: String,
        repository: EntriesRepository,
        category: Category
    ): EntriesFragmentViewModel {
        val factory = UserEntriesViewModel.Factory(repository)
        return ViewModelProvider(owner, factory)[viewModelKey, UserEntriesViewModel::class.java]
    }

    override fun generateContentFragment(viewModelKey: String) : EntriesTabFragmentBase {
        val user = requireArguments().getString(ARG_USER)!!
        viewModel.user.value = user
        return UserEntriesTabFragment.createInstance(viewModelKey, user)
    }

    override val title : String?
        get() = "id:${viewModel.user.value}"

    override val subtitle : String?
        get() = viewModel.tag.value?.let { tag -> "${tag.text}(${tag.count})" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState)

        val toolbar = requireActivity().toolbar

        // タグを選択している場合、戻るボタンでタグ選択を解除する
        val clearTagCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.tag.value != null) {
                    viewModel.tag.value = null
                }
            }
        }

        // タグ選択時にサブタイトルを表示する
        viewModel.tag.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = subtitle

            clearTagCallback.isEnabled = it != null
        })

        // ユーザーIDをタイトルに表示する
        viewModel.user.observe(viewLifecycleOwner, Observer {
            toolbar.title = title
        })

        setHasOptionsMenu(category == Category.User)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, clearTagCallback)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        val viewModel = viewModel as UserEntriesViewModel
        var inflated = false
        viewModel.tags.observe(viewLifecycleOwner, Observer { tags ->
            val activity = requireActivity()

            if (!inflated) {
                inflater.inflate(R.menu.spinner_issues, menu)
                inflated = true
            }

            (menu.findItem(R.id.spinner)?.actionView as? Spinner)?.run {
                val spinnerItems = tags.map { "${it.text}(${it.count})" }
                initialize(
                    activity,
                    spinnerItems,
                    R.drawable.spinner_allow_tags,
                    getString(R.string.desc_issues_spinner)
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
    }
}
