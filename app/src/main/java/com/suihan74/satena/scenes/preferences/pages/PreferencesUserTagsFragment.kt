package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.TaggedUserDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.*
import com.suihan74.satena.scenes.preferences.userTag.TaggedUsersListFragment
import com.suihan74.satena.scenes.preferences.userTag.UserTagsListFragment
import com.suihan74.utilities.BackPressable
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.get

class PreferencesUserTagsFragment : Fragment(), BackPressable {
    private lateinit var mUserTagsContainer : UserTagsContainer

    private var mDisplayedUserTag : UserTag? = null

    val userTagsContainer
        get() = mUserTagsContainer

    companion object {
        fun createInstance() =
            PreferencesUserTagsFragment()

        private const val BUNDLE_DISPLAYED_USER_TAG_NAME = "mDisplayedUserTag.name"
        private var savedUserTagsContainer : UserTagsContainer? = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_DISPLAYED_USER_TAG_NAME, mDisplayedUserTag?.name ?: "")
        savedUserTagsContainer = mUserTagsContainer
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SafeSharedPreferences.create<UserTagsKey>(context!!)
        mUserTagsContainer = savedInstanceState?.run {
            savedUserTagsContainer
        } ?: prefs.get(UserTagsKey.CONTAINER)

        // タグデータを最適化する
        optimize()

        prefs.edit {
            putObject(UserTagsKey.CONTAINER, mUserTagsContainer)
        }

        savedUserTagsContainer = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_user_tags, container, false)

        // タグ一覧を表示
        showUserTagsList()
        savedInstanceState?.run {
            val name = getString(BUNDLE_DISPLAYED_USER_TAG_NAME)
            val tag = mUserTagsContainer.getTag(name!!)
            if (tag != null) {
                showTaggedUsersList(tag)
            }
        }

        root.findViewById<FloatingActionButton>(R.id.add_button).setOnClickListener {
            val userTagsList = getCurrentFragment<UserTagsListFragment>()
            if (userTagsList != null) {
                showNewUserTagDialog()
            }
            else {
                val taggedUsersList = getCurrentFragment<TaggedUsersListFragment>()
                if (taggedUsersList != null) {
                    showNewTaggedUserDialog()
                }
            }
        }

        return root
    }

    private fun optimize() {
        // カスタムブコメタブの設定にも変更を反映する
        // タグIDが変更される可能性があるので，タグ名で記録しておき，最適化後にIDリストを再生成する
        val prefs = SafeSharedPreferences.create<PreferenceKey>(context)
        val activeTagIds = prefs.get<List<Int>>(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS)
        val activeTagNames = activeTagIds.mapNotNull { id -> mUserTagsContainer.tags.firstOrNull { it.id == id }?.name }

        mUserTagsContainer.optimize()

        val optimizedTagIds = mUserTagsContainer.tags
            .filter { activeTagNames.contains(it.name) }
            .map { it.id }
        prefs.edit {
            put(PreferenceKey.CUSTOM_BOOKMARKS_ACTIVE_TAG_IDS, optimizedTagIds)
        }
    }

    fun showTaggedUsersList(tag: UserTag) {
        mDisplayedUserTag = tag
        val fragment =
            TaggedUsersListFragment.createInstance(
                tag
            )
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showUserTagsList() {
        val fragment =
            UserTagsListFragment.createInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .commit()
    }

    private inline fun <reified T> getCurrentFragment() : T? {
        val fragment = childFragmentManager.findFragmentById(R.id.content_layout)
        @Suppress("UNCHECKED_CAST")
        return fragment as? T
    }

    private fun showNewUserTagDialog() {
        val dialog = UserTagDialogFragment.createInstance { fm, name, color ->
            if (mUserTagsContainer.containsTag(name)) {
                SatenaApplication.showToast("既に存在するタグです")
                return@createInstance false
            }
            else {
                val fragment = fm.get<UserTagsListFragment>()
                val item = mUserTagsContainer.addTag(name)

                updatePrefs()
                fragment?.addItem(item)

                SatenaApplication.showToast("タグ: $name を作成した")
                return@createInstance true
            }
        }
        dialog.show(childFragmentManager, "dialog")
    }

    private fun showNewTaggedUserDialog() {
        val dialog = TaggedUserDialogFragment.createInstance { fragment, name ->
            val tag = mDisplayedUserTag!!

            if (!mUserTagsContainer.containsUser(name)) {
                mUserTagsContainer.addUser(name)
            }
            val user = mUserTagsContainer.getUser(name)!!

            if (tag.contains(user)) {
                SatenaApplication.showToast("既に存在するユーザーです")
                return@createInstance false
            }
            else {
                mUserTagsContainer.tagUser(user, tag)

                updatePrefs()
                fragment.addItem(user)

                SatenaApplication.showToast("id:${name}にタグをつけました")
                return@createInstance true
            }
        }
        dialog.show(childFragmentManager, "dialog")
    }

    fun updatePrefs() {
        val context = SatenaApplication.instance.applicationContext
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
        prefs.edit {
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
