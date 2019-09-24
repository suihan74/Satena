package com.suihan74.satena.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.suihan74.satena.R
import com.suihan74.satena.adapters.TaggedUsersAdapter
import com.suihan74.satena.models.TaggedUser
import com.suihan74.satena.models.UserTag
import com.suihan74.satena.models.UserTagsContainer
import com.suihan74.utilities.DividerItemDecorator

class TaggedUsersListFragment : Fragment() {
    private lateinit var mRoot: View
    private var mParentFragment: PreferencesUserTagsFragment? = null
    private lateinit var mTaggedUsersAdapter : TaggedUsersAdapter
    private lateinit var mUserTag : UserTag
    private lateinit var mContainer : UserTagsContainer

    companion object {
        fun createInstance(parentFragment: PreferencesUserTagsFragment, container: UserTagsContainer, userTag: UserTag) = TaggedUsersListFragment().apply {
            mParentFragment = parentFragment
            mUserTag = userTag
            mContainer = container

            enterTransition = TransitionSet()
                .addTransition(Fade())
                .addTransition(Slide(GravityCompat.END))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tagged_users_list, container, false)
        mRoot = root

        root.findViewById<TextView>(R.id.tag_name).text = mUserTag.name
        updateCounter()

        mTaggedUsersAdapter = object : TaggedUsersAdapter(mContainer.getUsersOfTag(mUserTag)) {
            override fun onItemClicked(user: TaggedUser) {
            }

            override fun onItemLongClicked(user: TaggedUser): Boolean {
                val items = arrayOf(
/*                    "編集" to {  },*/
                    "削除" to { this@TaggedUsersListFragment.removeItem(user) }
                )

                AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setTitle(user.name)
                    .setItems(items.map { it.first }.toTypedArray()) { _, which ->
                        items[which].second.invoke()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()

                return true
            }
        }

        root.findViewById<RecyclerView>(R.id.users_list).apply {
            val dividerItemDecoration = DividerItemDecorator(ContextCompat.getDrawable(this@TaggedUsersListFragment.requireContext(),
                R.drawable.recycler_view_item_divider
            )!!)
            addItemDecoration(dividerItemDecoration)
            layoutManager = LinearLayoutManager(context)
            adapter = mTaggedUsersAdapter
        }

        retainInstance = true
        return root
    }

    private fun removeItem(user: TaggedUser) {
        mTaggedUsersAdapter.removeItem(user)
        mParentFragment?.removeUserFromTag(mUserTag, user)
        updateCounter()
    }

    fun addItem(user: TaggedUser) {
        mTaggedUsersAdapter.addItem(user)
        updateCounter()
    }

    fun updateCounter() {
        mRoot.findViewById<TextView>(R.id.users_count).text = String.format("%d users", mUserTag.count)
    }
}
