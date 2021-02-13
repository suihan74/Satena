package com.suihan74.satena.scenes.bookmarks.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suihan74.satena.models.userTag.Tag
import com.suihan74.satena.models.userTag.User
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
    /** タグ付けされたユーザー情報を使用する */
    suspend fun <T> withTaggedUsers(action: (Map<String, LiveData<UserAndTags>>)->T) : T

    /** 全てのタグリスト */
    val userTags : List<Tag>

    /** 全てのタグ一覧を取得する */
    suspend fun loadUserTags()

    /** ユーザーにつけられたタグを取得する */
    suspend fun loadUserTags(
        user: String,
        forceRefresh: Boolean = false
    ) : LiveData<UserAndTags>

    /**
     * ユーザータグのキャッシュを取得する
     *
     * ロードは行われない
     */
    suspend fun getUserTags(user : String) : LiveData<UserAndTags>

    /** タグを作成する
     *
     * @throws AlreadyExistedException
     * @throws TaskFailureException
     */
    suspend fun createUserTag(tagName: String) : Tag

    /**
     *  ユーザーにタグをつける
     *
     * @throws TaskFailureException
     */
    suspend fun tagUser(userName: String, tag: Tag)

    /**
     * ユーザーからタグを外す
     *
     * @throws TaskFailureException
     */
    suspend fun unTagUser(userName: String, tag: Tag)
}

// ------ //

class UserTagsRepository(
    private val dao: UserTagDao
) : UserTagsRepositoryInterface {

    private val userTagsMutex by lazy { Mutex() }

    /** ユーザーに対応するユーザータグのキャッシュ */
    private val taggedUsers = HashMap<String, MutableLiveData<UserAndTags>>()

    override suspend fun getUserTags(user: String) : LiveData<UserAndTags> {
        return userTagsMutex.withLock {
            taggedUsers.getOrPut(user) { MutableLiveData<UserAndTags>() }
        }
    }

    override suspend fun <T> withTaggedUsers(action: (Map<String, LiveData<UserAndTags>>)->T) : T {
        return userTagsMutex.withLock {
            action(taggedUsers)
        }
    }

    /** 全てのタグ */
    private var userTagsCache : List<Tag> = emptyList()
    override val userTags: List<Tag>
        get() = userTagsCache

    // ------ //

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
    ) : LiveData<UserAndTags> = withContext(Dispatchers.Default) {
        val liveData = userTagsMutex.withLock {
            taggedUsers.getOrPut(user) { MutableLiveData<UserAndTags>() }
        }

        if (forceRefresh || liveData.value == null) {
            withContext(Dispatchers.Main) {
                liveData.value = dao.getUserAndTags(user) ?: UserAndTags().also {
                    it.user = User(name = user)
                    it.tags = emptyList()
                }
            }
        }

        return@withContext liveData
    }

    /** タグを作成する
     *
     * @throws AlreadyExistedException
     * @throws TaskFailureException
     */
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

    /** ユーザーにタグをつける
     *
     * @throws TaskFailureException
     */
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

            val liveData = userTagsMutex.withLock {
                taggedUsers[userName]
            }

            if (liveData == null) {
                loadUserTags(userName)
            }
            else {
                val cache = liveData.value!!
                liveData.postValue(UserAndTags().also {
                    it.user = cache.user
                    it.tags = cache.tags.updateFirstOrPlus(tag) { t ->
                        t.id == tag.id
                    }
                })
            }
        }
    }

    /** ユーザーからタグを外す
     *
     * @throws TaskFailureException
     */
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

                val liveData = userTagsMutex.withLock {
                    taggedUsers[userName]
                }

                if (liveData == null) {
                    loadUserTags(userName)
                }
                else {
                    val cache = liveData.value!!
                    liveData.postValue(UserAndTags().also {
                        it.user = cache.user
                        it.tags = cache.tags.filterNot { t -> t.id == tag.id }
                    })
                }
            }
        }
    }
}
