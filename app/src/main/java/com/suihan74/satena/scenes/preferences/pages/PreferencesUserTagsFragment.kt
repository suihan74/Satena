package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.userTag.TaggedUsersListFragment
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagViewModel
import com.suihan74.satena.scenes.preferences.userTag.UserTagsListFragment
import com.suihan74.utilities.BackPressable
import com.suihan74.utilities.provideViewModel
import kotlinx.android.synthetic.main.fragment_preferences_user_tags.view.*


class PreferencesUserTagsFragment :
        PreferencesFragmentBase(),
        BackPressable
{
    val viewModel: UserTagViewModel by lazy {
        provideViewModel(this) {
            UserTagViewModel(
                UserTagRepository(SatenaApplication.instance.userTagDao)
            )
        }
    }

    companion object {
        fun createInstance() =
            PreferencesUserTagsFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_user_tags, container, false)

        viewModel.init(
            onFinally = { initializeViews(root) }
        )

        return root
    }

    /** ビューの初期化 */
    private fun initializeViews(root: View) {
        // タグ一覧を表示
        showUserTagsList()
        if (viewModel.currentTag.value != null) {
            showTaggedUsersList()
        }

        root.add_button.setOnClickListener {
            val userTagsList = getCurrentFragment<UserTagsListFragment>()
            if (userTagsList != null) {
                showNewUserTagDialog()
            }
            else {
                val taggedUsersList = getCurrentFragment<TaggedUsersListFragment>()
                if (taggedUsersList != null) {
                    viewModel.openTagUserDialog(childFragmentManager)
                }
            }
        }
    }

    fun showTaggedUsersList() {
        val fragment = TaggedUsersListFragment.createInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun showUserTagsList() {
        val fragment = UserTagsListFragment.createInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .commitAllowingStateLoss()
    }

    private inline fun <reified T> getCurrentFragment() : T? {
        val fragment = childFragmentManager.findFragmentById(R.id.content_layout)
        @Suppress("UNCHECKED_CAST")
        return fragment as? T
    }

    private fun showNewUserTagDialog() {
        viewModel.openUserTagDialog(null, childFragmentManager)
    }

    override fun onBackPressed(): Boolean {
        val fragment = getCurrentFragment<TaggedUsersListFragment>()
        return if (fragment != null) {
            viewModel.currentTag.postValue(null)
            childFragmentManager.popBackStack()
            true
        }
        else false
    }
}
