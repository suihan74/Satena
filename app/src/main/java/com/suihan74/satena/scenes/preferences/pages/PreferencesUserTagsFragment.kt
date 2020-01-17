package com.suihan74.satena.scenes.preferences.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.AlertDialogFragment
import com.suihan74.satena.dialogs.TagUserDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.scenes.preferences.PreferencesFragmentBase
import com.suihan74.satena.scenes.preferences.userTag.TaggedUsersListFragment
import com.suihan74.satena.scenes.preferences.userTag.UserTagRepository
import com.suihan74.satena.scenes.preferences.userTag.UserTagViewModel
import com.suihan74.satena.scenes.preferences.userTag.UserTagsListFragment
import com.suihan74.utilities.BackPressable
import com.suihan74.utilities.get
import com.suihan74.utilities.showToast
import kotlinx.android.synthetic.main.fragment_preferences_user_tags.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class PreferencesUserTagsFragment :
        PreferencesFragmentBase(),
        BackPressable,
        UserTagDialogFragment.Listener,
        TagUserDialogFragment.Listener
{
    private lateinit var viewModel: UserTagViewModel

    companion object {
        fun createInstance() =
            PreferencesUserTagsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = UserTagViewModel.Factory(
            UserTagRepository(SatenaApplication.instance.userTagDao)
        )

        viewModel = ViewModelProviders.of(this, factory)[UserTagViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_preferences_user_tags, container, false)

        // 初期値をロード
        launch(Dispatchers.Main) {
            viewModel.loadTags()

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
                        showNewTaggedUserDialog()
                    }
                }
            }
        }

        return root
    }

    fun showTaggedUsersList() {
        val fragment = TaggedUsersListFragment.createInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.content_layout, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showUserTagsList() {
        val fragment = UserTagsListFragment.createInstance()
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
        UserTagDialogFragment.Builder(R.style.AlertDialogStyle)
            .show(childFragmentManager, "create_tag_dialog")
    }

    private fun showNewTaggedUserDialog() {
        TagUserDialogFragment.Builder(R.style.AlertDialogStyle)
            .show(childFragmentManager, "tag_user_dialog")
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

    /**
     * 子フラグメントのメニューダイアログを処理する
     *
     * 各子フラグメントで別々に処理するようにしない理由は、
     * 子のchildFragmentManagerを渡す場合は画面回転時にダイアログが閉じるが
     * 一番上にある親（このフラグメント）のchildFragmentManagerを使う場合は画面回転後もダイアログ表示が継続するため
     */
    override fun onSelectItem(dialog: AlertDialogFragment, which: Int) {
        when (dialog.tag) {
            UserTagsListFragment.DIALOG_TAG_MENU -> {
                val childFragment = childFragmentManager.get<UserTagsListFragment>()
                val tag = dialog.getAdditionalData<TagAndUsers>("tag") ?: return
                childFragment?.menuItems?.get(which)?.second?.invoke(tag)
            }

            TaggedUsersListFragment.DIALOG_TAG_MENU -> {
                val childFragment = childFragmentManager.get<TaggedUsersListFragment>()
                val user = dialog.getAdditionalData<User>("user") ?: return
                childFragment?.menuItems?.get(which)?.second?.invoke(user)
            }
        }
    }

    /**
     * タグを新規作成orタグ名を編集するダイアログの結果を処理する
     */
    override suspend fun onCompletedEditTagName(tagName: String, dialog: UserTagDialogFragment): Boolean {
        if (dialog.isModifyMode) {
            val tag = dialog.editingUserTag!!

            if (tag.name != tagName) {
                if (viewModel.containsTag(tagName)) {
                    context?.showToast(R.string.msg_user_tag_existed)
                    return false
                }
                else {
                    val prevName = tag.name
                    val modified = tag.copy(name = tagName)
                    viewModel.updateTag(modified)
                    context?.showToast(R.string.msg_user_tag_updated, prevName, tagName)
                }
            }
            return true
        }
        else {
            return if (viewModel.containsTag(tagName)) {
                context?.showToast(R.string.msg_user_tag_existed)
                false
            }
            else {
                viewModel.addTag(tagName)
                context?.showToast(R.string.msg_user_tag_created, tagName)
                true
            }
        }
    }

    /**
     * 現在選択中のタグにユーザーを追加する
     */
    override suspend fun onCompleteTaggedUser(userName: String, dialog: TagUserDialogFragment): Boolean {
        val tag = viewModel.currentTag.value ?: return true

        return if (tag.users.none { it.name == userName }) {
            viewModel.addRelation(tag.userTag, userName)
            context?.showToast(R.string.msg_user_tagged_single, userName)
            true
        }
        else {
            context?.showToast(R.string.msg_user_has_already_tagged, tag.userTag.name, userName)
            false
        }
    }
}
