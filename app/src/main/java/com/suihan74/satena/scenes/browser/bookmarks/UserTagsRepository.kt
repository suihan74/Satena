package com.suihan74.satena.scenes.browser.bookmarks

import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.UserAndTags
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.utilities.exceptions.AlreadyExistedException
import com.suihan74.utilities.exceptions.TaskFailureException
import com.suihan74.utilities.extensions.updateFirstOrPlus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface UserTagsRepositoryInterface {
    /** ユーザーに対応するユーザータグ */
    val taggedUsers : List<UserAndTags>

    /** 全てのタグリスト */
    val userTags : List<Tag>

    /** 全てのタグ一覧を取得する */
    suspend fun loadUserTags()

    /** ユーザーにつけられたタグを取得する */
    suspend fun loadUserTags(
        user: String,
        forceRefresh: Boolean = false
    ) : UserAndTags?

    /** タグを作成する */
    @Throws(
        AlreadyExistedException::class,
        TaskFailureException::class
    )
    suspend fun createUserTag(tagName: String) : Tag

    /** ユーザーにタグをつける */
    @Throws(TaskFailureException::class)
    suspend fun tagUser(userName: String, tag: Tag)

    /** ユーザーからタグを外す */
    @Throws(TaskFailureException::class)
    suspend fun unTagUser(userName: String, tag: Tag)
}

class UserTagsRepository(
    private val dao: UserTagDao
) : UserTagsRepositoryInterface {

    private val userTagsMutex by lazy { Mutex() }

    /** ユーザーに対応するユーザータグのキャッシュ */
    private val taggedUsersCache = ArrayList<UserAndTags>()
    override val taggedUsers: List<UserAndTags>
        get() = taggedUsersCache

    /** 全てのタグ */
    private var userTagsCache : List<Tag> = emptyList()
    override val userTags: List<Tag>
        get() = userTagsCache

    /** 全てのタグ一覧を取得する */
    override suspend fun loadUserTags() {
        withContext(Dispatchers.Default) {
            userTagsMutex.withLock {
                userTagsCache = dao.getAllTags()
            }
        }
    }

    /** ユーザータグを取得する */
    override suspend fun loadUserTags(
        user: String,
        forceRefresh: Boolean
    ) : UserAndTags? = withContext(Dispatchers.Default) {
        userTagsMutex.withLock {
            val existed = taggedUsersCache.firstOrNull { it.user.name == user }
            if (!forceRefresh && existed != null) {
                return@withLock existed
            }
            else {
                val tag = dao.getUserAndTags(user)
                if (tag != null) {
                    taggedUsersCache.removeAll { it.user.id == tag.user.id }
                    taggedUsersCache.add(tag)
                }
                return@withLock tag
            }
        }
    }

    /** タグを作成する */
    @Throws(
        AlreadyExistedException::class,
        TaskFailureException::class
    )
    override suspend fun createUserTag(
        tagName: String
    ) : Tag = withContext(Dispatchers.Default) {
        if (null != dao.findTag(tagName)) {
            throw AlreadyExistedException(tagName)
        }

        val result = runCatching {
            dao.makeTag(tagName)
        }
        if (result.isFailure) {
            throw TaskFailureException(cause = result.exceptionOrNull())
        }

        val tag = result.getOrElse {
            throw TaskFailureException("the created tag is not found")
        }

        userTagsCache = userTagsCache.plus(tag)

        return@withContext tag
    }

    /** ユーザーにタグをつける */
    @Throws(TaskFailureException::class)
    override suspend fun tagUser(
        userName: String,
        tag: Tag
    ) {
        withContext(Dispatchers.Default) {
            val user = dao.makeUser(userName)
            val result = runCatching {
                dao.insertRelation(tag, user)
            }
            if (result.isFailure) {
                throw TaskFailureException(cause = result.exceptionOrNull())
            }

            val cache = taggedUsersCache.firstOrNull {
                it.user.id == user.id
            }
            if (cache == null) {
                loadUserTags(userName)
            }
            else {
                userTagsMutex.withLock {
                    taggedUsersCache.remove(cache)
                    taggedUsersCache.add(UserAndTags().also {
                        it.user = cache.user
                        it.tags = cache.tags.updateFirstOrPlus(tag) { t ->
                            t.id == tag.id
                        }
                    })
                }
            }
        }
    }

    /** ユーザーからタグを外す */
    @Throws(TaskFailureException::class)
    override suspend fun unTagUser(
        userName: String,
        tag: Tag
    ) {
        withContext(Dispatchers.Default) {
            val user = dao.makeUser(userName)
            val relation = dao.findRelation(tag, user)
            if (relation != null) {
                val result = runCatching {
                    dao.deleteRelation(relation)
                }
                if (result.isFailure) {
                    throw TaskFailureException(cause = result.exceptionOrNull())
                }

                val cache = taggedUsersCache.firstOrNull {
                    it.user.id == user.id
                }
                if (cache == null) {
                    loadUserTags(userName)
                }
                else {
                    userTagsMutex.withLock {
                        taggedUsersCache.remove(cache)
                        taggedUsersCache.add(UserAndTags().also {
                            it.user = cache.user
                            it.tags = cache.tags.filterNot { t -> t.id == tag.id }
                        })
                    }
                }
            }
        }
    }
}
