package com.suihan74.satena.fragments

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
import com.suihan74.satena.adapters.UserTagsAdapter
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.UserTag
import com.suihan74.utilities.DividerItemDecorator
import com.suihan74.utilities.showToast

class UserTagsListFragment : Fragment() {
    private lateinit var mUserTagsAdapter : UserTagsAdapter
    private lateinit var mUserTags : Collection<UserTag>

    private var mParentFragment: PreferencesUserTagsFragment? = null

    companion object {
        fun createInstance(parentFragment: PreferencesUserTagsFragment, tags: Collection<UserTag>) = UserTagsListFragment().apply {
            mParentFragment = parentFragment
            mUserTags = tags
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_tags_list, container, false)

        mUserTagsAdapter = object : UserTagsAdapter(mUserTags) {
            override fun onItemClicked(tag: UserTag) {
                mParentFragment?.showTaggedUsersList(tag)
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

        retainInstance = true
        return root
    }

    private fun removeItem(tag: UserTag) {
        mUserTagsAdapter.removeItem(tag)
        mParentFragment?.removeTag(tag)
    }

    fun addItem(tag: UserTag) {
        mUserTagsAdapter.addItem(tag)
    }

    private fun modifyItem(tag: UserTag) {
        val dialog = UserTagDialogFragment.createInstance(tag) { name, _ ->
            if (tag.name != name) {
                if (mUserTags.any { it.name == name }) {
                    activity?.showToast("既に存在するタグ名です")
                    return@createInstance false
                }
                else {
                    tag.name = name
                    updateItem(tag)
                    mParentFragment?.updatePrefs()
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
