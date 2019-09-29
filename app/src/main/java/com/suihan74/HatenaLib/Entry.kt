package com.suihan74.HatenaLib

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Entry (
    @SerializedName("eid", alternate = ["entry_id"])
    val id : Long,
    @SerializedName("title")
    private val mTitle : String,

    @SerializedName("description", alternate = ["content"])
    val description : String,

    @SerializedName("count", alternate = ["total_bookmarks"])
    val count : Int,

    val url : String,
    val rootUrl : String,
    val faviconUrl : String?,
    @SerializedName("image", alternate = ["image_url"])
    val imageUrl : String,

    val ampUrl : String? = null,

    // ユーザーがブクマしている場合のみ取得
    val bookmarkedData : BookmarkResult? = null,

    // ホットエントリにのみ含まれる情報
    val myhotentryComments : List<BookmarkResult>? = null
) : Serializable {

    val title : String
        get() {
            val wrapPosition = mTitle.indexOfFirst { it == '\n' }
            return if (wrapPosition < 0) {
                mTitle
            }
            else {
                mTitle.substring(0 until wrapPosition)
            }
        }

    fun plusBookmarkedData(bookmark: BookmarkResult) = Entry(
        id = id,
        mTitle = mTitle,
        description = description,
        count = if (bookmarkedData == null) count + 1 else count,
        url = url,
        rootUrl = rootUrl,
        faviconUrl = faviconUrl,
        imageUrl = imageUrl,
        ampUrl = ampUrl,
        bookmarkedData = bookmark,
        myhotentryComments = myhotentryComments
    )

    companion object {
        fun createEmpty() = Entry(
            id = 0,
            mTitle = "",
            description = "",
            count = 0,
            url = "",
            rootUrl = "",
            faviconUrl = "",
            imageUrl = ""
        )
    }
}

