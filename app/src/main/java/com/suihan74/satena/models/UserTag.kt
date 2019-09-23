package com.suihan74.satena.models

import java.io.Serializable

/** ユーザーをタグ付けするやつ */
class UserTagsContainer : Serializable {
    @Suppress("UseSparseArrays")
    private val mTags = HashMap<Long, UserTag>()
    @Suppress("UseSparseArrays")
    private val mUsers = HashMap<Long, TaggedUser>()
    private var mNextTagId = 0L
    private var mNextUserId = 0L

    val tags : Collection<UserTag>
        get() = mTags.values

    val users : Collection<TaggedUser>
        get() = mUsers.values

    fun getTag(name: String) : UserTag? =
        mTags.values.firstOrNull { it.name == name }

    fun containsTag(name: String) =
        getTag(name) != null

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

    fun removeTag(name: String) {
        val value = getTag(name)
        if (value != null) {
            removeTag(value)
        }
    }

    fun removeTag(tag: UserTag) {
        getUsersOfTag(tag).forEach {
            it.removeTag(tag)
        }
        mTags.remove(tag.id)
    }

    fun changeTagName(tag: UserTag, name: String) {
        tag.name = name
    }

    fun getUser(name: String) : TaggedUser? =
        mUsers.values.firstOrNull { it.name == name }

    fun containsUser(name: String) =
        getUser(name) != null

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

    fun removeUser(name: String) {
        val value = getUser(name)
        if (value != null) {
            removeUser(value)
        }
    }

    fun removeUser(user: TaggedUser) {
        getTagsOfUser(user).forEach {
            it.remove(user)
        }
        mUsers.remove(user.id)
    }

    fun tagUser(user: TaggedUser, tag: UserTag) {
        user.addTag(tag)
        tag.add(user)
    }

    fun unTagUser(user: TaggedUser, tag: UserTag) {
        user.removeTag(tag)
        tag.remove(user)
    }

    fun getTagsOfUser(user: TaggedUser?) =
        user?.tags?.map { mTags[it]!! } ?: emptyList()

    fun getTagsOfUser(name: String) =
        getTagsOfUser(getUser(name))

    fun getUsersOfTag(tag: UserTag?) =
        tag?.users?.map { mUsers[it]!! } ?: emptyList()

    fun getUsersOfTag(name: String) =
        getUsersOfTag(getTag(name))
}

/** タグ情報 */
data class UserTag (
    val id: Long,
    var name: String,
    var color: Int
) : Serializable {

    private val mUsers = HashSet<Long>()
    val users : Set<Long>
        get() = mUsers

    val count
        get() = users.count()

    fun contains(user: TaggedUser) = users.contains(user.id)

    internal fun add(user: TaggedUser) = mUsers.add(user.id)

    internal fun remove(user: TaggedUser) = mUsers.remove(user.id)
}

/** ユーザー情報 */
data class TaggedUser (
    val id: Long,
    val name: String
) : Serializable {

    private val mTags = HashSet<Long>()
    val tags : Set<Long>
        get() = mTags

    fun containsTag(tag: UserTag) = mTags.contains(tag.id)

    internal fun addTag(tag: UserTag) = mTags.add(tag.id)

    internal fun removeTag(tag: UserTag) = mTags.remove(tag.id)
}
