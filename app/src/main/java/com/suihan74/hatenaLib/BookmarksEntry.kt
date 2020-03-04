package com.suihan74.hatenaLib

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class BookmarksEntry (
    @SerializedName("eid")
    val id : Long,
    val title : String,
    val count : Int,
    val url : String,
    val entryUrl : String,
    val screenshot : String,
    val bookmarks : List<Bookmark>
) : Serializable {
    /**
     * ブクマした全ユーザーが付けたタグをその数と共に集計して返す
     */
    @Expose(serialize = false, deserialize = false)
    private var mTags : List<Pair<String, Int>>? = null
    val tags : List<Pair<String, Int>>
        get() {
            if (mTags == null) {
                mTags = bookmarks.flatMap { it.tags }
                    .groupBy { it }
                    .map { it.key to it.value.count() }
                    .sortedByDescending { it.second }
            }
            return mTags!!
        }
}
