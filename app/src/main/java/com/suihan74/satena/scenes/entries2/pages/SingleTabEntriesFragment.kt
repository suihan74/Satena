package com.suihan74.satena.scenes.entries2.pages

import android.os.Bundle
import android.view.*
import android.widget.Spinner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.suihan74.satena.R
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries.initialize
import com.suihan74.satena.scenes.entries2.EntriesFragment
import com.suihan74.satena.scenes.entries2.EntriesFragmentViewModel
import com.suihan74.satena.scenes.entries2.EntriesRepository
import com.suihan74.satena.scenes.entries2.EntriesTabFragment
import com.suihan74.utilities.putEnum
import com.suihan74.utilities.withArguments
import kotlinx.android.synthetic.main.activity_entries2.*

class SingleTabEntriesFragment : EntriesFragment() {
    companion object {
        fun createInstance(category: Category) = SingleTabEntriesFragment().withArguments {
            putEnum(ARG_CATEGORY, category)
        }

        /** Category.Userを表示する */
        fun createUserEntriesInstance(user: String) = SingleTabEntriesFragment().withArguments {
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
        return when (category) {
            Category.User -> {
                val factory = UserEntriesViewModel.Factory(repository)
                ViewModelProvider(owner, factory)[viewModelKey, UserEntriesViewModel::class.java]
            }

            else -> {
                val factory = HatenaEntriesViewModel.Factory(repository)
                ViewModelProvider(owner, factory)[viewModelKey, HatenaEntriesViewModel::class.java]
            }
        }
    }

    override val title : String?
        get() = when(category) {
            Category.User -> "id:${viewModel.user.value}"
            else -> super.title
        }

    override val subtitle : String?
        get() = when(category) {
            Category.User -> viewModel.tag.value?.let { tag -> "${tag.text}(${tag.count})" }
            else -> super.subtitle
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val root = inflater.inflate(R.layout.fragment_entries2_single, container, false)

        if (savedInstanceState == null) {
            val fragment =
                when (category) {
                    Category.Notices -> NoticesFragment.createInstance(viewModelKey)

                    Category.User -> {
                        val user = requireArguments().getString(ARG_USER)!!
                        viewModel.user.value = user
                        UserEntriesTabFragment.createInstance(viewModelKey, user)
                    }

                    else -> EntriesTabFragment.createInstance(
                        viewModelKey,
                        category
                    )
                }

            childFragmentManager.beginTransaction()
                .replace(R.id.content_layout, fragment)
                .commit()
        }

        val toolbar = requireActivity().toolbar

        // タグ選択時にサブタイトルを表示する
        viewModel.tag.observe(viewLifecycleOwner, Observer {
            toolbar.subtitle = subtitle
        })

        // Category.UserではユーザーIDをタイトルに表示する
        viewModel.user.observe(viewLifecycleOwner, Observer {
            toolbar.title = title
        })

        setHasOptionsMenu(category == Category.User)

        return root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        when (category) {
            Category.User ->
                inflateUserEntriesMenu(viewModel as UserEntriesViewModel, menu, inflater)

            else -> throw NotImplementedError()
        }
    }

    /** Category.User画面用のメニュー */
    private fun inflateUserEntriesMenu(viewModel: UserEntriesViewModel, menu: Menu, inflater: MenuInflater) {
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
