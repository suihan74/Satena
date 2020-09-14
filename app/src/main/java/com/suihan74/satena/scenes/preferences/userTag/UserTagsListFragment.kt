package com.suihan74.satena.scenes.preferences.userTag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.suihan74.satena.R
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.bindings.setDivider
import kotlinx.android.synthetic.main.fragment_user_tags_list.view.*

class UserTagsListFragment : Fragment() {
    private val userTagsFragment: PreferencesUserTagsFragment
        get() = requireParentFragment() as PreferencesUserTagsFragment

    private val viewModel: UserTagViewModel
        get() = userTagsFragment.viewModel

    companion object {
        fun createInstance() = UserTagsListFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_tags_list, container, false)

        val userTagsAdapter = object : UserTagsAdapter() {
            override fun onItemClicked(tag: TagAndUsers) {
                viewModel.currentTag.postValue(tag)
                userTagsFragment.showTaggedUsersList()
            }

            override fun onItemLongClicked(tag: TagAndUsers): Boolean {
                viewModel.openTagMenuDialog(tag.userTag, userTagsFragment.childFragmentManager)
                return true
            }
        }

        root.user_tags_list?.apply {
            setDivider(R.drawable.recycler_view_item_divider)
            layoutManager = LinearLayoutManager(context)
            adapter = userTagsAdapter
        }

        // リストの変更を監視
        viewModel.tags.observe(viewLifecycleOwner) { tags ->
            userTagsAdapter.setItems(tags)
        }

        return root
    }
}
