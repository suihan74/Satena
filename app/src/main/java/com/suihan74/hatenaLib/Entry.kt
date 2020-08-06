package com.suihan74.hatenaLib

import android.net.Uri
import com.google.gson.annotations.SerializedName

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
    @SerializedName("myhotentry_comments")
    val myHotEntryComments : List<BookmarkResult>? = null
) {

    // for Gson
    internal constructor() : this(0, "", "", 0, "", null, null, "")

    @SerializedName("title")
    private val mTitle : String = title

    @delegate:Transient
    val title : String by lazy {
        mTitle.indexOfFirst { it == '\n' }.let {
            if (it < 0) {
                mTitle
            }
            else {
                mTitle.substring(0 until it)
            }
        }
    }


    @SerializedName("root_url")
    private var mRootUrl : String? = rootUrl

    @delegate:Transient
    val rootUrl : String by lazy {
        if (mRootUrl.isNullOrBlank()) {
            val uri = Uri.parse(url)
            val scheme = uri.scheme
            val authority = uri.authority

            if (scheme != null && authority != null) "$scheme://$authority/" else ""
        }
        else mRootUrl!!
    }

    @SerializedName("favicon_url")
    private var mFaviconUrl : String? = faviconUrl

    @delegate:Transient
    val faviconUrl : String by lazy {
        mFaviconUrl ?: run {
            "https://www.google.com/s2/favicons?domain=${Uri.parse(url).host}"
        }
    }

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
        myhotentryComments: List<BookmarkResult>? = this.myHotEntryComments
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
        myHotEntryComments = myhotentryComments
    )
}

internal data class EntriesWithIssue(
    val issue: Issue,
    val entries: List<Entry>
) {
    // for Gson
    private constructor() : this(Issue(), emptyList())
}
