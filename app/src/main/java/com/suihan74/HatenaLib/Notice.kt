package com.suihan74.HatenaLib

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import java.io.Serializable

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

) : Serializable {

    companion object {
        const val VERB_ADD_FAVORITE = "add_favorite"
        const val VERB_BOOKMARK = "bookmark"
        const val VERB_STAR = "star"
    }

    val eid : Long
        get() {
            if (verb == VERB_STAR) {
                val idx = link.lastIndexOf('-') + 1
                return link.substring(idx).toLong()
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

) : Serializable


data class NoticeObject (
    val user : String,

    @JsonAdapter(StarColorDeserializer::class)
    val color : StarColor
) : Serializable

data class NoticeMetadata (
    val subjectTitle : String
) : Serializable
