package com.suihan74.HatenaLib

import android.net.Uri
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

    rootUrl : String?,

    faviconUrl : String?,

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


    @SerializedName("root_url")
    private var mRootUrl : String? = rootUrl
    val rootUrl : String
        get() {
            val rootUrl = mRootUrl
            return if (rootUrl.isNullOrBlank()) {
                val uri = Uri.parse(url)
                val scheme = uri.scheme
                val authority = uri.authority

                mRootUrl = if (scheme != null && authority != null) "$scheme://$authority/" else ""
                mRootUrl!!
            }
            else rootUrl
        }

    @SerializedName("favicon_url")
    private var mFaviconUrl : String? = faviconUrl
    val faviconUrl : String
        get() = mFaviconUrl ?: run {
            val uri = Uri.parse(url)
            mFaviconUrl = "https://www.google.com/s2/favicons?domain=${uri.host}"
            return@run mFaviconUrl!!
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
            faviconUrl = null,
            imageUrl = ""
        )
    }
}

internal data class EntriesWithIssue(
    val issue: Issue,
    val entries: List<Entry>
) : Serializable
