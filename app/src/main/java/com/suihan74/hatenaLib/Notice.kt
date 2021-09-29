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

    companion object {
        const val VERB_ADD_FAVORITE = "add_favorite"
        const val VERB_BOOKMARK = "bookmark"
        const val VERB_STAR = "star"
    }


    @delegate:Transient
    val eid : Long by lazy {
        if (verb == VERB_STAR) {
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


data class NoticeObject (
    val user : String,

    @JsonAdapter(StarColorDeserializer::class)
    val color : StarColor
) {
    // for Gson
    private constructor() : this("", StarColor.Yellow)
}

data class NoticeMetadata (
    val subjectTitle : String
) {
    // for Gson
    private constructor() : this("")
}
