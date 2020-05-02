package com.suihan74.satena.models

import java.io.Serializable

/** ユーザーをタグ付けするやつ */
@Deprecated("DBに移行")
@Suppress("deprecation")
class UserTagsContainer : Serializable {
    @Suppress("UseSparseArrays")
    private val mTags = HashMap<Int, UserTag>()
    @Suppress("UseSparseArrays")
    private val mUsers = HashMap<Int, TaggedUser>()
    private var mNextTagId = 0
    private var mNextUserId = 0

    /** コンテナに存在する全タグデータ */
    val tags : Collection<UserTag>
        get() = mTags.values

    /** コンテナに存在する全ユーザーデータ */
    val users : Collection<TaggedUser>
        get() = mUsers.values

    /**
     * タグデータを取得する
     * タグが存在しない場合null
     */
    fun getTag(name: String) : UserTag? =
        mTags.values.firstOrNull { it.name == name }

    /** 指定した名前を持つタグが存在するか確認する */
    fun containsTag(name: String) =
        getTag(name) != null

    /**
     * タグデータを作成してインスタンスを返す
     * 既に存在する場合はそのインスタンスを返す
     */
    fun addTag(name: String, color: Int = 0) : UserTag {
        val existed = getTag(name)

        if (existed == null) {
            val id = mNextTagId++
            val newItem = UserTag(id, name, color)
            mTags[id] = newItem
            return newItem
        }
        else {
            return existed
        }
    }

    /** タグを削除する */
    fun removeTag(name: String) {
        val value = getTag(name)
        if (value != null) {
            removeTag(value)
        }
    }

    /** タグを削除する */
    fun removeTag(tag: UserTag) {
        getUsersOfTag(tag).forEach {
            it.removeTag(tag)
        }
        mTags.remove(tag.id)
    }

    /** タグの名前を変更 */
    fun changeTagName(tag: UserTag, name: String) : UserTag {
        if (tag.name == name) return tag

        val modified = tag.newInstance(name = name)
        mTags[tag.id] = modified
        return modified
    }

    /**
     * ユーザーデータを取得
     * 存在しない場合はnullが返る
     */
    fun getUser(name: String) : TaggedUser? =
        mUsers.values.firstOrNull { it.name == name }

    /** ユーザーデータが存在するか確認 */
    fun containsUser(name: String) =
        getUser(name) != null

    /** ユーザーにタグが付いているかを確認 */
    fun checkUserTagged(name: String) =
        getUser(name)?.tags?.isNotEmpty() ?: false

    /**
     * ユーザーデータを作成してインスタンスを返す
     * 既にユーザーが存在する場合はそのインスタンスを返す
     */
    fun addUser(name: String) : TaggedUser {
        val existed = getUser(name)
        if (existed == null) {
            val id = mNextUserId++
            val newItem = TaggedUser(id, name)
            mUsers[id] = newItem
            return newItem
        }
        else {
            return existed
        }
    }

    /** ユーザーを削除する */
    fun removeUser(name: String) {
        val value = getUser(name)
        if (value != null) {
            removeUser(value)
        }
    }

    /** ユーザーを削除する */
    fun removeUser(user: TaggedUser) {
        getTagsOfUser(user).forEach {
            it.removeUser(user)
        }
        mUsers.remove(user.id)
    }

    /** ユーザーにタグをつける */
    fun tagUser(user: TaggedUser, tag: UserTag) {
        user.addTag(tag)
        tag.addUser(user)
    }

    /** ユーザーのタグを外す */
    fun unTagUser(user: TaggedUser, tag: UserTag) {
        user.removeTag(tag)
        tag.removeUser(user)
    }

    /** ユーザーについたタグのリストを取得 */
    fun getTagsOfUser(user: TaggedUser?) =
        user?.tags?.mapNotNull { mTags[it] ?: removeIllegalTagId(user, it) } ?: emptyList()

    /** ユーザーについたタグのリストを取得 */
    fun getTagsOfUser(name: String) =
        getTagsOfUser(getUser(name))

    /** 指定タグがついたユーザーのリストを取得 */
    fun getUsersOfTag(tag: UserTag?) =
        tag?.users?.mapNotNull { mUsers[it] ?: removeIllegalUserId(tag, it) } ?: emptyList()

    /** 指定タグがついたユーザーのリストを取得 */
    fun getUsersOfTag(name: String) =
        getUsersOfTag(getTag(name))

    /**
     * ユーザーデータが何らかの不具合で存在しないタグIDを参照している場合そのIDを除去する
     * getTagsOfUser()で使用する都合上常にnullを返す
     */
    private fun removeIllegalTagId(user: TaggedUser, tagId: Int) : UserTag? {
        user.removeTag(tagId)
        return null
    }

    /**
     * タグデータが何らかの不具合で存在しないユーザーIDを参照している場合そのIDを除去する
     * getUsersOfTag()で使用する都合上常にnullを返す
     */
    private fun removeIllegalUserId(tag: UserTag, userId: Int) : TaggedUser? {
        tag.removeUser(userId)
        return null
    }

    /**
     * コンテナの状態を最適化
     * <<< optimize前に取得したタグ/ユーザーのインスタンスはoptimize後に使用しないよう注意 >>>
     */
    fun optimize() {
        removeEmptyUsers()
        makeTagIdsCompact()
        makeUserIdsCompact()
    }

    /** ひとつもタグがついていないユーザーデータを削除する */
    private fun removeEmptyUsers() {
        users.filter { it.tags.isEmpty() }
            .forEach { mUsers.remove(it.id) }
    }

    /** タグデータのIDを連番に並べ直す */
    private fun makeTagIdsCompact() {
        if (mNextTagId == mTags.size) return

        val modifiedTags = mTags.values.mapIndexed { index, tag ->
            if (index == tag.id) {
                tag
            }
            else {
                tag.newInstance(id = index).apply {
                    getUsersOfTag(tag).forEach { user ->
                        user.removeTag(tag)
                        user.addTag(this)
                    }
                }
            }
        }
        mTags.clear()
        mTags.putAll(modifiedTags.map { Pair(it.id, it) })
        mNextTagId = mTags.size
    }

    /** ユーザーデータのIDを連番に並べ直す */
    private fun makeUserIdsCompact() {
        if (mNextUserId == mUsers.size) return

        val modifiedUsers = mUsers.values.mapIndexed { index, user ->
            if (index == user.id) {
                user
            }
            else {
                user.newInstance(id = index).apply {
                    getTagsOfUser(user).forEach { tag ->
                        tag.removeUser(user)
                        tag.addUser(this)
                    }
                }
            }
        }
        mUsers.clear()
        mUsers.putAll(modifiedUsers.map { Pair(it.id, it) })
        mNextUserId = mUsers.size
    }
}

/** タグ情報 */
@Deprecated("DBに移行")
@Suppress("deprecation")
data class UserTag (
    val id: Int,
    val name: String,
    val color: Int
) : Serializable {

    private val mUsers = HashSet<Int>()
    val users : Set<Int>
        get() = mUsers

    val count
        get() = users.count()

    fun contains(user: TaggedUser) = users.contains(user.id)

    internal fun addUser(user: TaggedUser) = mUsers.add(user.id)

    internal fun removeUser(user: TaggedUser) = removeUser(user.id)
    internal fun removeUser(id: Int) = mUsers.remove(id)

    internal fun newInstance(id: Int = this.id, name: String = this.name, color: Int = this.color) =
        UserTag(id, name, color).also {
            it.mUsers.addAll(this.mUsers)
        }
}

/** ユーザー情報 */
@Deprecated("DBに移行")
@Suppress("deprecation")
data class TaggedUser (
    val id: Int,
    val name: String
) : Serializable {

    private val mTags = HashSet<Int>()
    val tags : Set<Int>
        get() = mTags

    fun containsTag(tag: UserTag) = mTags.contains(tag.id)

    internal fun addTag(tag: UserTag) = mTags.add(tag.id)

    internal fun removeTag(tag: UserTag) = removeTag(tag.id)
    internal fun removeTag(id: Int) = mTags.remove(id)

    internal fun newInstance(id: Int = this.id, name: String = this.name) =
        TaggedUser(id, name).also {
            it.mTags.addAll(this.mTags)
        }
}
