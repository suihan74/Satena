package com.suihan74.satena.scenes.browser.bookmarks

import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.models.userTag.UserTagDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface UserTagsRepositoryInterface {
    /** ユーザーに対応するユーザータグ */
    val userTags : List<UserAndTags>

    /** ユーザーにつけられたタグを取得する */
    suspend fun loadUserTags(user: String) : UserAndTags?
}

class UserTagsRepository(
    private val userTagDao: UserTagDao
) : UserTagsRepositoryInterface {

    private val userTagsMutex by lazy { Mutex() }

    /** ユーザーに対応するユーザータグのキャッシュ */
    private val userTagsCache = ArrayList<UserAndTags>()
    override val userTags: List<UserAndTags>
        get() = userTagsCache

    /** ユーザータグを取得する */
    override suspend fun loadUserTags(
        user: String
    ) : UserAndTags? = withContext(Dispatchers.IO) {
        userTagsMutex.withLock {
            val existed = userTagsCache.firstOrNull { it.user.name == user }
            if (existed != null) {
                return@withLock existed
            }
            else {
                val tag = userTagDao.getUserAndTags(user)
                if (tag != null) {
                    userTagsCache.add(tag)
                }
                return@withLock tag
            }
        }
    }
}
