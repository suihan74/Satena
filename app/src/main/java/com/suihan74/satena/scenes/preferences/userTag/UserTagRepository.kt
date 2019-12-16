package com.suihan74.satena.scenes.preferences.userTag

import android.util.Log
import com.suihan74.satena.models.userTag.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserTagRepository(
    private val dao: UserTagDao
) {
    /** タグとそれが付いたユーザーリストを取得 */
    suspend fun loadTags() = withContext(Dispatchers.IO) {
        dao.getAllTags().mapNotNull { dao.getTagAndUsers(it.name) }
    }

    /** タグが付いているユーザーのリストを取得 */
    suspend fun loadUsers() = withContext(Dispatchers.IO) {
        dao.getAllUsers().mapNotNull { dao.getUserAndTags(it.name) }
    }

    /** タグを追加 */
    suspend fun addTag(tagName: String) : Boolean = withContext(Dispatchers.IO) {
        return@withContext if (dao.findTag(tagName) == null) {
            dao.insertTag(tagName)
            true
        }
        else false
    }

    /** タグを削除 */
    suspend fun deleteTag(tag: TagAndUsers) = withContext(Dispatchers.IO) {
        dao.getAllRelations()
            .filter { it.tagId == tag.userTag.id }
            .forEach { dao.deleteRelation(it) }
        dao.deleteTag(tag.userTag)
    }

    /** タグを更新 */
    suspend fun updateTag(tag: Tag) = withContext(Dispatchers.IO) {
        dao.updateTag(tag)
    }

    /** タグが存在するかを確認 */
    suspend fun containsTag(tagName: String) : Boolean = withContext(Dispatchers.IO) {
        dao.findTag(tagName) != null
    }

    /** ユーザーにタグをつける */
    suspend fun addRelation(tag: Tag, user: User) = withContext(Dispatchers.IO) {
        try {
            dao.insertRelation(tag, user)
        }
        catch (e: Exception) {
            Log.d("UserTag", "deprecated relation: tag: ${tag.name}, user: ${user.name}")
        }
    }

    /** ユーザーにタグをつける */
    suspend fun addRelation(tag: Tag, userName: String) = withContext(Dispatchers.IO) {
        try {
            val user = dao.makeUser(userName)
            dao.insertRelation(tag, user)
        }
        catch (e: Exception) {
            Log.d("UserTag", "deprecated relation: tag: ${tag.name}, user: $userName")
        }
    }

    /** ユーザーからタグを外す */
    suspend fun deleteRelation(tag: Tag, user: User) = withContext(Dispatchers.IO) {
        dao.findRelation(tag, user)?.let {
            dao.deleteRelation(it)
        }
    }
}
