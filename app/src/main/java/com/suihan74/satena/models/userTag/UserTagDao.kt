package com.suihan74.satena.models.userTag

import androidx.room.*

@Dao
interface UserTagDao {
    // ===== get all ===== //

    @Query("select * from user_tag order by id asc")
    fun getAllTags(): List<Tag>

    @Query("select * from user_tag_user order by id asc")
    fun getAllUsers(): List<User>

    @Query("select * from user_tag_relation")
    fun getAllRelations(): List<TagAndUserRelation>

    // ===== find ===== //

    @Query("""
        select * from user_tag 
        where name=:name 
        limit 1
    """)
    fun findTag(name: String): Tag?

    @Query("""
        select * from user_tag_user
        where name=:name
        limit 1
    """)
    fun findUser(name: String): User?

    @Query("""
        select * from user_tag_relation
        where tag_id=:tagId and user_id=:userId
        limit 1
    """)
    fun findRelation(tagId: Int, userId: Int): TagAndUserRelation?

    // ===== insert ===== //

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertTag(tag: Tag)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertRelation(relation: TagAndUserRelation)

    // ===== update ===== //

    @Update
    fun updateTag(tag: Tag)

    @Update
    fun updateUser(user: User)

    // ===== delete ===== //

    @Delete
    fun deleteTag(tags: Tag)

    @Delete
    fun deleteUser(user: User)

    @Delete
    fun deleteRelation(relation: TagAndUserRelation)

    // ===== get relation ===== //
    @Transaction
    @Query("""
        select * from user_tag
        where name = :tagName
        limit 1
    """)
    fun getTagAndUsers(tagName: String): TagAndUsers?

    @Transaction
    @Query("""
        select * from user_tag_user
        where name = :userName
        limit 1
    """)
    fun getUserAndTags(userName: String): UserAndTags?
}

fun UserTagDao.insertTag(name: String) =
    insertTag(Tag(name = name))

fun UserTagDao.insertUser(name: String) =
    insertUser(User(name = name))

/**
 * タグを取得する。DBに存在しない場合は新しく登録する
 */
fun UserTagDao.makeTag(name: String) : Tag =
    findTag(name) ?: let {
        insertTag(name)
        findTag(name)!!
    }

/**
 * ユーザーを取得する。DBに存在しない場合は新しく登録する
 */
fun UserTagDao.makeUser(name: String) : User =
    findUser(name) ?: let {
        insertUser(name)
        findUser(name)!!
    }

fun UserTagDao.insertRelation(tag: Tag, user: User) =
    insertRelation(TagAndUserRelation(tag = tag, user = user))

fun UserTagDao.findRelation(tag: Tag, user: User) =
    findRelation(tag.id, user.id)

/**
 * ユーザータグに関する全てのデータを破棄する
 */
fun UserTagDao.clearAll() {
    getAllRelations().forEach { deleteRelation(it) }
    getAllUsers().forEach { deleteUser(it) }
    getAllTags().forEach { deleteTag(it) }
}
