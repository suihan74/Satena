package com.suihan74.satena.models.userTag

import androidx.room.*
import java.io.Serializable

/**
 * ユーザータグ
 */
@Entity(tableName = "user_tag")
data class Tag (
    var name: String,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) : Serializable

/**
 * ユーザー
 */
@Entity(tableName = "user_tag_user")
data class User (
    var name: String,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) : Serializable

/**
 * ユーザーがタグ付けされていることを示す
 */
@Entity(
    tableName = "user_tag_relation",
    primaryKeys = ["tag_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = Tag::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("tag_id"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = User::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("user_id"),
            onDelete = ForeignKey.CASCADE)
    ])
class TagAndUserRelation : Serializable {
    constructor(tagId: Int, userId: Int) {
        this.tagId = tagId
        this.userId = userId
    }

    constructor(tag: Tag, user: User) {
        this.tagId = tag.id
        this.userId = user.id
    }

    @ColumnInfo(name = "tag_id")
    val tagId: Int

    @ColumnInfo(name = "user_id")
    val userId: Int
}


/**
 * タグとそのタグが付いたユーザーリスト
 */
class TagAndUsers : Serializable {
    @Embedded
    lateinit var userTag: Tag

    @Relation(
        entity = User::class,
        parentColumn = "id",  // Tag#id
        entityColumn = "id",  // User#id
        associateBy = Junction(
            value = TagAndUserRelation::class,
            parentColumn = "tag_id",
            entityColumn = "user_id"
        )
    )
    lateinit var users: List<User>
}

/**
 * ユーザーとそのユーザーについたタグリスト
 */
class UserAndTags : Serializable {
    @Embedded
    lateinit var user: User

    @Relation(
        entity = Tag::class,
        parentColumn = "id",  // User#id
        entityColumn = "id",  // Tag#id
        associateBy = Junction(
            value = TagAndUserRelation::class,
            parentColumn = "user_id",
            entityColumn = "tag_id"
        )
    )
    lateinit var tags: List<Tag>
}
