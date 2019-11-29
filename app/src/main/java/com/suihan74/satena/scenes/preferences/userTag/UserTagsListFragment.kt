package com.suihan74.satena.scenes.preferences.userTag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.AlertDialogListener
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.UserTag
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.DividerItemDecorator

class UserTagsListFragment : Fragment() {
    private lateinit var mUserTagsAdapter : UserTagsAdapter
    var menuItems: Array<out Pair<String, (UserTag)->Unit>>? = null
        private set

    companion object {
        fun createInstance() = UserTagsListFragment()

        const val DIALOG_TAG_MENU = "UserTagsListFragment.DIALOG_TAG_MENU"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_tags_list, container, false)

        menuItems = arrayOf(
            getString(R.string.pref_user_tags_tag_menu_edit) to { t -> this@UserTagsListFragment.modifyItem(t) },
            getString(R.string.pref_user_tags_tag_menu_remove) to { t -> this@UserTagsListFragment.removeItem(t) }
        )

        val parentFragment = parentFragment as PreferencesUserTagsFragment
        val userTagsContainer = parentFragment.userTagsContainer
        mUserTagsAdapter = object : UserTagsAdapter(userTagsContainer.tags) {
            override fun onItemClicked(tag: UserTag) {
                parentFragment.showTaggedUsersList(tag)
            }

            override fun onItemLongClicked(tag: UserTag): Boolean {
                AlertDialogFragment.Builder(R.style.AlertDialogStyle)
                    .setTitle(tag.name)
                    .setNegativeButton(R.string.dialog_cancel)
                    .setItems(menuItems!!.map { it.first })
                    .setAdditionalData("tag", tag)
                    .show(parentFragment.childFragmentManager, DIALOG_TAG_MENU)

                return true
            }
        }

        root.findViewById<RecyclerView>(R.id.user_tags_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(this@UserTagsListFragment.requireContext(),
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = mUserTagsAdapter
        }

        return root
    }

    private fun removeItem(tag: UserTag) {
        val parentFragment = parentFragment as PreferencesUserTagsFragment
        mUserTagsAdapter.removeItem(tag)
        parentFragment.removeTag(tag)
    }

    fun addItem(tag: UserTag) {
        mUserTagsAdapter.addItem(tag)
    }

    private fun modifyItem(tag: UserTag) {
        UserTagDialogFragment.Builder(R.style.AlertDialogStyle)
            .setUserTag(tag)
            .show(parentFragment!!.childFragmentManager, "modify_tag_dialog")
    }

    fun updateItem(tag: UserTag) {
        mUserTagsAdapter.updateItem(tag)
    }
}
