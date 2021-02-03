package com.suihan74.satena.scenes.preferences.userTag

import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.TagUserDialogFragment
import com.suihan74.satena.dialogs.UserTagDialogFragment
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.User
import com.suihan74.satena.scenes.entries2.EntriesActivity
import com.suihan74.utilities.OnError
import com.suihan74.utilities.OnFinally
import com.suihan74.utilities.OnSuccess
import com.suihan74.utilities.extensions.ContextExtensions.showToast
import com.suihan74.utilities.showAllowingStateLoss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserTagViewModel(
    private val repository: UserTagRepository
) : ViewModel() {

    val DIALOG_TAG_USER by lazy { "DIALOG_TAG_USER" }
    val DIALOG_TAG_MENU by lazy { "DIALOG_TAG_MENU" }
    val DIALOG_USER_MENU by lazy { "DIALOG_USER_MENU" }
    val DIALOG_CREATE_TAG by lazy { "DIALOG_CREATE_TAG" }
    val DIALOG_MODIFY_TAG by lazy { "DIALOG_MODIFY_TAG" }

    /** 全てのタグとユーザーのリスト */
    val tags = MutableLiveData<List<TagAndUsers>>().apply {
        observeForever {
            updateCurrentTag()
        }
    }

    /** 現在表示中のタグ */
    val currentTag by lazy { MutableLiveData<TagAndUsers?>() }

    fun init(
        onSuccess: OnSuccess<Unit>? = null,
        onError: OnError? = null,
        onFinally: OnFinally? = null
    ) = viewModelScope.launch(Dispatchers.Main) {
        try {
            loadTags()
            onSuccess?.invoke(Unit)
        }
        catch (e: Throwable) {
            onError?.invoke(e)
        }
        finally {
            onFinally?.invoke()
        }
    }

    /** リストを取得 */
    suspend fun loadTags() = withContext(Dispatchers.Main) {
        tags.value = repository.loadTags()
    }

    /** 選択中の現在表示中のタグ情報を更新 */
    private fun updateCurrentTag() {
        val id = currentTag.value?.userTag?.id
        currentTag.value =
            if (id != null) tags.value?.firstOrNull { it.userTag.id == id }
            else null
    }

    /** タグを追加 */
    suspend fun addTag(tagName: String) {
        if (repository.addTag(tagName)) {
            loadTags()
        }
    }

    /** タグを削除 */
    suspend fun deleteTag(tag: TagAndUsers) {
        repository.deleteTag(tag)
        loadTags()
    }

    /** タグを更新 */
    suspend fun updateTag(tag: Tag) {
        repository.updateTag(tag)
        loadTags()
    }

    /** タグが存在するかを確認 */
    suspend fun containsTag(tagName: String) =
        repository.containsTag(tagName)

    /** ユーザーにタグをつける */
    suspend fun addRelation(tag: Tag, user: User) {
        repository.addRelation(tag, user)
        updateTag(tag)
    }

    /** ユーザーにタグをつける */
    suspend fun addRelation(tag: Tag, userName: String) {
        repository.addRelation(tag, userName)
        updateTag(tag)
    }

    /** ユーザーからタグを外す */
    suspend fun deleteRelation(tag: Tag, user: User) {
        repository.deleteRelation(tag, user)
        updateTag(tag)
    }

    /**
     * ユーザータグを作成・編集するダイアログを開く
     */
    fun openUserTagDialog(
        editingUserTag: Tag?,
        fragmentManager: FragmentManager
    ) {
        val dialog = UserTagDialogFragment.createInstance(editingUserTag)

        dialog.setOnCompleteListener listener@ { (tagName, editingUserTag) ->
            val context = SatenaApplication.instance
            if (editingUserTag == null) {
                return@listener if (containsTag(tagName)) {
                    context.showToast(R.string.msg_user_tag_existed)
                    false
                }
                else {
                    addTag(tagName)
                    context.showToast(R.string.msg_user_tag_created, tagName)
                    true
                }
            }
            else {
                val tag = editingUserTag
                if (tag.name != tagName) {
                    if (containsTag(tagName)) {
                        context.showToast(R.string.msg_user_tag_existed)
                        return@listener false
                    }
                    else {
                        val prevName = tag.name
                        val modified = tag.copy(name = tagName)
                        updateTag(modified)
                        context.showToast(R.string.msg_user_tag_updated, prevName, tagName)
                    }
                }
                return@listener true
            }
        }

        val dialogTag =
            if (editingUserTag == null) DIALOG_CREATE_TAG
            else DIALOG_MODIFY_TAG

        dialog.showAllowingStateLoss(fragmentManager, dialogTag)
    }

    /**
     * 現在選択中のタグにユーザーを追加するダイアログを開く
     */
    fun openTagUserDialog(
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {
        TagUserDialogFragment.createInstance().run {
            showAllowingStateLoss(fragmentManager, DIALOG_TAG_USER)

            setOnCompleteListener listener@ { userName ->
                val tag = currentTag.value ?: return@listener true
                val context = SatenaApplication.instance

                return@listener if (tag.users.none { it.name == userName }) {
                    addRelation(tag.userTag, userName)
                    context.showToast(R.string.msg_user_tagged_single, userName)
                    true
                }
                else {
                    context.showToast(
                        R.string.msg_user_has_already_tagged,
                        tag.userTag.name,
                        userName
                    )
                    false
                }
            }
        }
    }

    /** タグに対するメニューダイアログを開く */
    fun openTagMenuDialog(targetTag: Tag, fragmentManager: FragmentManager) {
        val dialog = TagMenuDialog.createInstance(targetTag)

        dialog.setOnEditListener { (tag, f) ->
            openUserTagDialog(tag, f.parentFragmentManager)
        }

        dialog.setOnDeleteListener { (tag) ->
            val context = SatenaApplication.instance
            try {
                val tagAndUsers = tags.value?.firstOrNull { it.userTag.id == tag.id }!!
                deleteTag(tagAndUsers)
                context.showToast(R.string.msg_user_tag_deleted, tag.name)
            }
            catch (e: Throwable) {
                Log.e("DeleteTag", Log.getStackTraceString(e))
            }
            finally {
                loadTags()
            }
        }

        dialog.showAllowingStateLoss(fragmentManager, DIALOG_TAG_MENU)
    }

    /** ユーザーに対するメニューダイアログを開く */
    fun openUserMenuDialog(
        activity: FragmentActivity,
        targetUser: User,
        fragmentManager: FragmentManager
    ) = viewModelScope.launch(Dispatchers.Main) {
        UserMenuDialog.createInstance(targetUser).run {
            showAllowingStateLoss(fragmentManager, DIALOG_USER_MENU)

            setOnShowBookmarksListener { user ->
                val intent = Intent(activity, EntriesActivity::class.java).apply {
                    putExtra(EntriesActivity.EXTRA_USER, user.name)
                }
                activity.startActivity(intent)
            }

            setOnDeleteListener { user ->
                try {
                    val tag = currentTag.value?.userTag ?: return@setOnDeleteListener
                    deleteRelation(tag, user)
                    activity.showToast(R.string.msg_user_tag_relation_deleted, user.name, tag.name)
                }
                catch (e: Throwable) {
                    Log.e("DeleteRelation", Log.getStackTraceString(e))
                }
                finally {
                    updateCurrentTag()
                }
            }
        }
    }
}
