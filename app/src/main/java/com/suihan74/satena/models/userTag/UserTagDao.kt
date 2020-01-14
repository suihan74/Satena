package com.suihan74.satena.models.userTag

import androidx.room.*

@Dao
abstract class UserTagDao {
    // ===== get all ===== //

    @Query("select * from user_tag order by id asc")
    abstract fun getAllTags(): List<Tag>

    @Query("select * from user_tag_user order by id asc")
    abstract fun getAllUsers(): List<User>

    @Query("select * from user_tag_relation")
    abstract fun getAllRelations(): List<TagAndUserRelation>

    // ===== find ===== //

    @Query("""
        select * from user_tag 
        where name=:name 
        limit 1
    """)
    abstract fun findTag(name: String): Tag?

    @Query("""
        select * from user_tag_user
        where name=:name
        limit 1
    """)
    abstract fun findUser(name: String): User?

    @Query("""
        select * from user_tag_relation
        where tag_id=:tagId and user_id=:userId
        limit 1
    """)
    abstract fun findRelation(tagId: Int, userId: Int): TagAndUserRelation?

    fun findRelation(tag: Tag, user: User) =
        findRelation(tag.id, user.id)

    // ===== insert ===== //

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertTag(tag: Tag)

    fun insertTag(name: String) =
        insertTag(Tag(name = name))

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertUser(user: User)

    fun insertUser(name: String) =
        insertUser(User(name = name))

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun insertRelation(relation: TagAndUserRelation)

    fun insertRelation(tag: Tag, user: User) =
        insertRelation(TagAndUserRelation(tag = tag, user = user))

    // ===== find or insert ===== //

    /**
     * タグを取得する。DBに存在しない場合は新しく登録する
     */
    fun makeTag(name: String) : Tag =
        findTag(name) ?: let {
            insertTag(name)
            findTag(name)!!
        }

    /**
     * ユーザーを取得する。DBに存在しない場合は新しく登録する
     */
    fun makeUser(name: String) : User =
        findUser(name) ?: let {
            insertUser(name)
            findUser(name)!!
        }

    // ===== update ===== //

    @Update
    abstract fun updateTag(tag: Tag)

    @Update
    abstract fun updateUser(user: User)

    // ===== delete ===== //

    @Delete
    abstract fun deleteTag(tags: Tag)

    @Delete
    abstract fun deleteUser(user: User)

    @Delete
    abstract fun deleteRelation(relation: TagAndUserRelation)

    /**
     * ユーザータグに関する全てのデータを破棄する
     */
    fun clearAll() {
        getAllRelations().forEach { deleteRelation(it) }
        getAllUsers().forEach { deleteUser(it) }
        getAllTags().forEach { deleteTag(it) }
    }

    // ===== get relation ===== //
    @Transaction
    @Query("""
        select * from user_tag
        where name = :tagName
        limit 1
    """)
    abstract fun getTagAndUsers(tagName: String): TagAndUsers?

    @Transaction
    @Query("""
        select * from user_tag_user
        where name = :userName
        limit 1
    """)
    abstract fun getUserAndTags(userName: String): UserAndTags?
}
