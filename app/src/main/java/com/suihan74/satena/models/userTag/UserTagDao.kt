package com.suihan74.satena.models.userTag

import androidx.room.*

@Dao
interface UserTagDao {
    // ===== get all ===== //

    @Query("select * from user_tag order by id asc")
    suspend fun getAllTags(): List<Tag>

    @Query("select * from user_tag_user order by id asc")
    suspend fun getAllUsers(): List<User>

    @Query("select * from user_tag_relation")
    suspend fun getAllRelations(): List<TagAndUserRelation>

    // ===== find ===== //

    @Query("""
        select * from user_tag 
        where name=:name 
        limit 1
    """)
    suspend fun findTag(name: String): Tag?

    @Query("""
        select * from user_tag_user
        where name=:name
        limit 1
    """)
    suspend fun findUser(name: String): User?

    @Query("""
        select * from user_tag_relation
        where tag_id=:tagId and user_id=:userId
        limit 1
    """)
    suspend fun findRelation(tagId: Int, userId: Int): TagAndUserRelation?

    suspend fun findRelation(tag: Tag, user: User) =
        findRelation(tag.id, user.id)

    // ===== insert ===== //

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: Tag)

    suspend fun insertTag(name: String) =
        insertTag(Tag(name = name))

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    suspend fun insertUser(name: String) =
        insertUser(User(name = name))

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRelation(relation: TagAndUserRelation)

    suspend fun insertRelation(tag: Tag, user: User) =
        insertRelation(TagAndUserRelation(tag = tag, user = user))

    // ===== find or insert ===== //

    /**
     * タグを取得する。DBに存在しない場合は新しく登録する
     */
    @Transaction
    suspend fun makeTag(name: String) : Tag =
        findTag(name) ?: let {
            insertTag(name)
            findTag(name)!!
        }

    /**
     * ユーザーを取得する。DBに存在しない場合は新しく登録する
     */
    @Transaction
    suspend fun makeUser(name: String) : User =
        findUser(name) ?: let {
            insertUser(name)
            findUser(name)!!
        }

    // ===== update ===== //

    @Update
    suspend fun updateTag(tag: Tag)

    @Update
    suspend fun updateUser(user: User)

    // ===== delete ===== //

    @Delete
    suspend fun deleteTag(tags: Tag)

    @Delete
    suspend fun deleteUser(user: User)

    @Delete
    suspend fun deleteRelation(relation: TagAndUserRelation)

    /**
     * ユーザータグに関する全てのデータを破棄する
     */
    @Transaction
    suspend fun clearAll() {
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
    suspend fun getTagAndUsers(tagName: String): TagAndUsers?

    @Transaction
    @Query("""
        select * from user_tag_user
        where name = :userName
        limit 1
    """)
    suspend fun getUserAndTags(userName: String): UserAndTags?
}
