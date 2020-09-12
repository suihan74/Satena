package com.suihan74.satena.scenes.preferences.userTag

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.suihan74.satena.R
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.dialogs.TagUserDialogFragment
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.TagAndUsers
import com.suihan74.satena.models.userTag.User
import com.suihan74.utilities.showAllowingStateLoss
import com.suihan74.utilities.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserTagViewModel(
    private val repository: UserTagRepository
) : ViewModel() {

    /** 全てのタグとユーザーのリスト */
    val tags by lazy {
        MutableLiveData<List<TagAndUsers>>().apply {
            // タグリスト更新と同時に、現在表示中のタグ内容も更新する
            viewModelScope.launch(Dispatchers.Main) {
                observeForever {
                    updateCurrentTag()
                }
            }
        }
    }

    /** 現在表示中のタグ */
    val currentTag by lazy { MutableLiveData<TagAndUsers?>() }

    /** リストを取得 */
    suspend fun loadTags() {
        tags.postValue(repository.loadTags())
    }

    /** 選択中の現在表示中のタグ情報を更新 */
    private fun updateCurrentTag() {
        val id = currentTag.value?.userTag?.id
        currentTag.postValue(
            if (id != null) {
                tags.value?.firstOrNull { it.userTag.id == id }
            }
            else null
        )
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
        loadTags()
    }

    /** ユーザーにタグをつける */
    suspend fun addRelation(tag: Tag, userName: String) {
        repository.addRelation(tag, userName)
        loadTags()
    }

    /** ユーザーからタグを外す */
    suspend fun deleteRelation(tag: Tag, user: User) {
        repository.deleteRelation(tag, user)
        loadTags()
    }

    /**
     * 現在選択中のタグにユーザーを追加するダイアログを開く
     */
    fun openTagUserDialog(
        fragmentManager: FragmentManager,
        dialogTag: String?
    ) = viewModelScope.launch(Dispatchers.Main) {
        TagUserDialogFragment.createInstance().run {
            showAllowingStateLoss(fragmentManager, dialogTag)

            setOnCompleteListener listener@ { userName ->
                val tag = currentTag.value ?: return@listener true

                return@listener if (tag.users.none { it.name == userName }) {
                    addRelation(tag.userTag, userName)
                    withContext(Dispatchers.Main) {
                        SatenaApplication.instance.showToast(R.string.msg_user_tagged_single, userName)
                    }
                    true
                }
                else {
                    withContext(Dispatchers.Main) {
                        SatenaApplication.instance.showToast(
                            R.string.msg_user_has_already_tagged,
                            tag.userTag.name,
                            userName
                        )
                    }
                    false
                }
            }
        }
    }

    // ViewModelProvidersを利用する一般的な使用法において、
    // ファクトリを介してインスタンスを作成することでコンストラクタに引数を与える
    class Factory(private val repository: UserTagRepository) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            UserTagViewModel(repository) as T
    }
}
