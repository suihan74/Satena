package com.suihan74.satena.scenes.preferences.userTag

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.UserTag
import com.suihan74.satena.scenes.preferences.pages.PreferencesUserTagsFragment
import com.suihan74.utilities.DividerItemDecorator

class UserTagsListFragment : Fragment() {
    private lateinit var mUserTagsAdapter : UserTagsAdapter

    companion object {
        fun createInstance() = UserTagsListFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_tags_list, container, false)

        val parentFragment = parentFragment as PreferencesUserTagsFragment
        val userTagsContainer = parentFragment.userTagsContainer
        mUserTagsAdapter = object : UserTagsAdapter(userTagsContainer.tags) {
            override fun onItemClicked(tag: UserTag) {
                parentFragment.showTaggedUsersList(tag)
            }

            override fun onItemLongClicked(tag: UserTag): Boolean {
                val items = arrayOf(
                    "編集" to { this@UserTagsListFragment.modifyItem(tag) },
                    "削除" to { this@UserTagsListFragment.removeItem(tag) }
                )

                AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle(tag.name)
                    .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                        items[which].second.invoke()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

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
        val parentFragment = parentFragment as PreferencesUserTagsFragment
        val dialog = UserTagDialogFragment.createInstance(tag) { fragment, name, _ ->
            if (tag.name != name) {
                val userTagsContainer = parentFragment.userTagsContainer
                if (userTagsContainer.getTag(name) != null) {
                    SatenaApplication.showToast("既に存在するタグ名です")
                    return@createInstance false
                }
                else {
                    fragment as UserTagsListFragment
                    val modifiedTag = userTagsContainer.changeTagName(tag, name)
                    fragment.updateItem(modifiedTag)
                    parentFragment.updatePrefs()
                }
            }
            return@createInstance true
        }
        dialog.show(fragmentManager!!, "dialog")
    }

    fun updateItem(tag: UserTag) {
        mUserTagsAdapter.updateItem(tag)
    }
}
