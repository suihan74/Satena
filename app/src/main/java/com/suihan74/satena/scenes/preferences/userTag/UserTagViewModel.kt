package com.suihan74.satena.scenes.preferences.userTag

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.suihan74.satena.models.userTag.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserTagViewModel : ViewModel() {
    /** 全てのタグとユーザーのリスト */
    val tags: MutableLiveData<List<TagAndUsers>> by lazy {
        MutableLiveData<List<TagAndUsers>>()
    }

    /** 現在表示中のタグ */
    val currentTag: MutableLiveData<TagAndUsers?> by lazy {
        MutableLiveData<TagAndUsers?>()
    }

    /** リストを取得 */
    suspend fun loadTags(dao: UserTagDao) = withContext(Dispatchers.IO) {
        tags.postValue(dao.getAllTags().mapNotNull {
            dao.getTagAndUsers(it.name)
        })

        updateCurrentTag()
    }

    /** 選択中の現在表示中のタグ情報を更新 */
    suspend fun updateCurrentTag() = withContext(Dispatchers.IO) {
        val id = currentTag.value?.userTag?.id
        currentTag.postValue(
            if (id != null) {
                tags.value?.firstOrNull { it.userTag.id == id }
            }
            else null
        )
    }

    /** タグを追加 */
    suspend fun addTag(dao: UserTagDao, tagName: String) : Boolean = withContext(Dispatchers.IO) {
        return@withContext if (dao.findTag(tagName) == null) {
            dao.insertTag(tagName)
            loadTags(dao)
            true
        }
        else false
    }

    /** タグを削除 */
    suspend fun deleteTag(dao: UserTagDao, tag: TagAndUsers) = withContext(Dispatchers.IO) {
        dao.getAllRelations()
            .filter { it.tagId == tag.userTag.id }
            .forEach { dao.deleteRelation(it) }
        dao.deleteTag(tag.userTag)
        loadTags(dao)
    }

    /** タグを更新 */
    suspend fun updateTag(dao: UserTagDao, tag: Tag) = withContext(Dispatchers.IO) {
        dao.updateTag(tag)
        loadTags(dao)
    }

    /** タグが存在するかを確認 */
    suspend fun containsTag(dao: UserTagDao, tagName: String) : Boolean = withContext(Dispatchers.IO) {
        tags.value?.any { it.userTag.name == tagName } == true
    }

    /** ユーザーにタグをつける */
    suspend fun addRelation(dao: UserTagDao, tag: Tag, user: User) = withContext(Dispatchers.IO) {
        try {
            dao.insertRelation(tag, user)
            loadTags(dao)
        }
        catch (e: Exception) {
            Log.d("UserTag", "deprecated relation: tag: ${tag.name}, user: ${user.name}")
        }
    }

    /** ユーザーにタグをつける */
    suspend fun addRelation(dao: UserTagDao, tag: Tag, userName: String) = withContext(Dispatchers.IO) {
        try {
            val user = dao.makeUser(userName)
            dao.insertRelation(tag, user)
            loadTags(dao)
        }
        catch (e: Exception) {
            Log.d("UserTag", "deprecated relation: tag: ${tag.name}, user: $userName")
        }
    }

    /** ユーザーからタグを外す */
    suspend fun deleteRelation(dao: UserTagDao, tag: Tag, user: User) = withContext(Dispatchers.IO) {
        dao.findRelation(tag, user)?.let {
            dao.deleteRelation(it)
        }
        loadTags(dao)
    }
}
