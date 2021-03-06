package com.suihan74.satena.scenes.preferences.userTag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.databinding.FragmentUserTagsListBinding
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.scenes.preferences.pages.UserTagsFragment
import com.suihan74.utilities.bindings.setDivider

class UserTagsListFragment : Fragment() {
    companion object {
        fun createInstance() = UserTagsListFragment()
    }

    // ------ //

    private val userTagsFragment: UserTagsFragment
        get() = requireParentFragment() as UserTagsFragment

    private val viewModel: UserTagViewModel
        get() = userTagsFragment.viewModel

    // ------ //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentUserTagsListBinding.inflate(
            inflater,
            container,
            false
        )

        val userTagsAdapter = object : UserTagsAdapter() {
            override fun onItemClicked(tag: TagAndUsers) {
                viewModel.currentTag.value = tag
                userTagsFragment.showTaggedUsersList()
            }

            override fun onItemLongClicked(tag: TagAndUsers): Boolean {
                viewModel.openTagMenuDialog(tag.userTag, userTagsFragment.childFragmentManager)
                return true
            }
        }

        binding.userTagsList.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = userTagsAdapter
        }

        // リストの変更を監視
        viewModel.tags.observe(viewLifecycleOwner, { tags ->
            userTagsAdapter.setItems(tags)
        })

        return binding.root
    }
}
