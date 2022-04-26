package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class Notice (
    @JsonAdapter(EpochTimeDeserializer::class)
    val created : LocalDateTime,

    @JsonAdapter(EpochTimeDeserializer::class)
    val modified : LocalDateTime,

    @SerializedName("object")
    val objects : List<NoticeObject>,

    val verb : String,  // "star" など

    @SerializedName("subject")
    val link : String,

    val metadata : NoticeMetadata?,

    @SerializedName("user_name")
    val user : String

) {

    // for Gson
    private constructor() : this(LocalDateTime.MIN, LocalDateTime.MIN, emptyList(), "", "", null, "")

    @delegate:Transient
    val eid : Long by lazy {
        if (verb == NoticeVerb.STAR.str) {
            runCatching {
                val idx = link.lastIndexOf('-') + 1
                link.substring(idx).toLong()
            }.getOrElse {
                throw IllegalArgumentException("notice's verb: $verb", it)
            }
        }
        else {
            throw IllegalArgumentException("notice's verb: $verb")
        }
    }
}


data class NoticeResponse (
    val status : String,

    @JsonAdapter(EpochTimeDeserializer::class)
    val lastSeen : LocalDateTime,

    val notices : List<Notice>

) {
    // for Gson
    private constructor() : this("", LocalDateTime.MIN, emptyList())
}

enum class NoticeVerb(
    val code : Int,
    val str : String,
) {
    OTHERS(0b0000_0001, ""),

    ADD_FAVORITE(0b0000_0010, "add_favorite"),
    BOOKMARK(0b0000_0100, "bookmark"),
    STAR(0b0000_1000, "star"),
    FIRST_BOOKMARK(0b0001_0000, "first_bookmark"),
    ;

    companion object {
        val all : Int = values().sumOf { it.code }
        fun fromInt(int: Int) : List<NoticeVerb> = values().filter { it.code and int > 0 }
        fun isOthers(verb: String) = verb.isBlank() || values().all { it.str != verb }
    }
}

data class NoticeObject (
    val user : String,

    @JsonAdapter(StarColorDeserializer::class)
    val color : StarColor
) {
    // for Gson
    private constructor() : this("", StarColor.Yellow)
}

data class NoticeMetadata (
    val subjectTitle : String?,

    // for VERB_FIRST_BOOKMARK
    private val totalBookmarksAchievement : Int? = null,
    private val entryCanonicalUrl : String? = null,
    private val entryTitle : String? = null
) {
    // for Gson
    private constructor() : this(null, null, null, null)

    @delegate:Transient
    val firstBookmarkMetadata : FirstBookmarkMetadata? by lazy {
        val count = totalBookmarksAchievement ?: return@lazy null
        val url = entryCanonicalUrl ?: return@lazy null
        val title = entryTitle ?: return@lazy null
        FirstBookmarkMetadata(count, url, title)
    }
}

data class FirstBookmarkMetadata (
    val totalBookmarksAchievement : Int,
    val entryCanonicalUrl : String,
    val entryTitle : String
)
