package com.suihan74.satena.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.satena.R
import com.suihan74.satena.dialogs.AddTaggedUserDialogFragment
import com.suihan74.satena.dialogs.NewUserTagDialogFragment
import com.suihan74.satena.models.TaggedUser
import com.suihan74.satena.models.UserTag
import com.suihan74.satena.models.UserTagsContainer
import com.suihan74.satena.models.UserTagsKey
import com.suihan74.utilities.BackPressable
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.showToast

class PreferencesUserTagsFragment : Fragment(), BackPressable {
    private lateinit var mUserTagsContainer : UserTagsContainer
    private lateinit var mPrefs : SafeSharedPreferences<UserTagsKey>

    private var mDisplayedUserTag : UserTag? = null

    companion object {
        fun createInstance() = PreferencesUserTagsFragment()

        private const val BUNDLE_DISPLAYED_USER_TAG = "mDisplayedUserTag"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(BUNDLE_DISPLAYED_USER_TAG, mDisplayedUserTag)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_user_tags, container, false)

        mPrefs = SafeSharedPreferences.create(context!!)
        mUserTagsContainer = mPrefs.get(UserTagsKey.CONTAINER)

        // タグ一覧を表示
        showUserTagsList()
        savedInstanceState?.run {
            val tag = getSerializable(BUNDLE_DISPLAYED_USER_TAG) as? UserTag
            if (tag != null) {
                showTaggedUsersList(tag)
            }
        }

        root.findViewById<FloatingActionButton>(R.id.add_button).setOnClickListener {
            val userTagsList = getCurrentFragment<UserTagsListFragment>()
            if (userTagsList != null) {
                showNewUserTagDialog(userTagsList)
            }
            else {
                val taggedUsersList = getCurrentFragment<TaggedUsersListFragment>()
                if (taggedUsersList != null) {
                    showNewTaggedUserDialog(taggedUsersList)
                }
            }
        }

        retainInstance = true
        return root
    }

    fun showTaggedUsersList(tag: UserTag) {
        mDisplayedUserTag = tag
        val fragment = TaggedUsersListFragment.createInstance(this, mUserTagsContainer.getUsersOfTag(tag), tag)
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showUserTagsList() {
        val fragment = UserTagsListFragment.createInstance(this, mUserTagsContainer.tags)
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .commit()
    }

    private inline fun <reified T> getCurrentFragment() : T? {
        val fragment = childFragmentManager.findFragmentById(R.id.content_layout)
        @Suppress("UNCHECKED_CAST")
        return fragment as? T
    }

    private fun showNewUserTagDialog(fragment: UserTagsListFragment) {
        val dialog = NewUserTagDialogFragment.createInstance { name, color ->
            if (mUserTagsContainer.containsTag(name)) {
                context!!.showToast("既に存在するタグです")
                return@createInstance false
            }
            else {
                val item = mUserTagsContainer.addTag(name)

                updatePrefs()
                fragment.addItem(item)

                context!!.showToast("タグ: $name を作成した")
                return@createInstance true
            }
        }
        dialog.show(fragmentManager!!, "dialog")
    }

    private fun showNewTaggedUserDialog(fragment: TaggedUsersListFragment) {
        val dialog = AddTaggedUserDialogFragment.createInstance { name ->
            val tag = mDisplayedUserTag!!

            if (!mUserTagsContainer.containsUser(name)) {
                mUserTagsContainer.addUser(name)
            }
            val user = mUserTagsContainer.getUser(name)!!

            if (tag.contains(user)) {
                context!!.showToast("既に存在するユーザーです")
                return@createInstance false
            }
            else {
                mUserTagsContainer.tagUser(user, tag)

                updatePrefs()
                fragment.addItem(user)

                context!!.showToast("id:${name}にタグをつけました")
                return@createInstance true
            }
        }
        dialog.show(fragmentManager!!, "dialog")
    }

    fun updatePrefs() {
        mPrefs.edit {
            putObject(UserTagsKey.CONTAINER, mUserTagsContainer)
        }
    }

    fun removeUserFromTag(tag: UserTag, user: TaggedUser) {
        mUserTagsContainer.unTagUser(user, tag)
        updatePrefs()
    }

    fun removeTag(tag: UserTag) {
        mUserTagsContainer.removeTag(tag)
        updatePrefs()
    }

    override fun onBackPressed(): Boolean {
        val fragment = getCurrentFragment<TaggedUsersListFragment>()
        if (fragment != null) {
            childFragmentManager.popBackStack()
            val userTagsList = getCurrentFragment<UserTagsListFragment>()
            userTagsList?.updateItem(mDisplayedUserTag!!)
            mDisplayedUserTag = null
            return true
        }
        else {
            return false
        }
    }
}
