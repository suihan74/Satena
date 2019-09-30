package com.suihan74.HatenaLib

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Entry (
    @SerializedName("eid", alternate = ["entry_id"])
    val id : Long,

    title : String,

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

    @SerializedName("title")
    private val mTitle : String = title

    val title : String
        get() = mTitle.indexOfFirst { it == '\n' }.let {
            if (it < 0) {
                mTitle
            }
            else {
                mTitle.substring(0 until it)
            }
        }

    fun plusBookmarkedData(bookmark: BookmarkResult) = copy(
        count = if (bookmarkedData == null) count + 1 else count,
        bookmarkedData = bookmark
    )

    fun copy(
        id: Long = this.id,
        title: String = this.title,
        description: String = this.description,
        count: Int = this.count,
        url: String = this.url,
        rootUrl: String = this.rootUrl,
        faviconUrl: String? = this.faviconUrl,
        imageUrl: String = this.imageUrl,
        ampUrl: String? = this.ampUrl,
        bookmarkedData: BookmarkResult? = this.bookmarkedData,
        myhotentryComments: List<BookmarkResult>? = this.myhotentryComments
    ) = Entry(
        id = id,
        title = title,
        description = description,
        count = count,
        url = url,
        rootUrl = rootUrl,
        faviconUrl = faviconUrl,
        imageUrl = imageUrl,
        ampUrl = ampUrl,
        bookmarkedData = bookmarkedData,
        myhotentryComments = myhotentryComments
    )

    companion object {
        fun createEmpty() = Entry(
            id = 0,
            title = "",
            description = "",
            count = 0,
            url = "",
            rootUrl = "",
            faviconUrl = "",
            imageUrl = ""
        )
    }
}

