package com.suihan74.hatenaLib

import android.net.Uri
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

class Entry (
    @SerializedName("eid", alternate = ["entry_id"])
    val id : Long,

    title : String,

    @SerializedName("description", alternate = ["content"])
    val description : String,

    @SerializedName("count", alternate = ["total_bookmarks"])
    val count : Int,

    @SerializedName("url")
    private val _url : String,

    rootUrl : String?,

    faviconUrl : String?,

    @SerializedName("image", alternate = ["image_url"])
    private val _imageUrl : String? = null,

    val ampUrl : String? = null,

    // ユーザーがブクマしている場合のみ取得
    val bookmarkedData : BookmarkResult? = null,

    // ホットエントリにのみ含まれる情報
    @SerializedName("myhotentry_comments")
    val myHotEntryComments : List<BookmarkResult>? = null,

    @JsonAdapter(TimestampDeserializer::class)
    val date: LocalDateTime? = null,
) {

    // for Gson
    internal constructor() : this(0, "", "", 0, "", null, null)

    /**
     * 広告エントリの場合のURL
     *
     * 手動でURL解決し元の広告用URLはここに退避させるため、APIから返された時点では常にnull
     */
    @delegate:Transient
    val adUrl : String? by lazy {
        if (_url.startsWith("https://ad-hatena.com/")) _url
        else null
    }

    /**
     * エントリのURL
     */
    @delegate:Transient
    val url : String by lazy {
        if (adUrl == null) _url
        else Uri.parse(_url).getQueryParameter("url") ?: _url
    }

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

    @delegate:Transient
    val imageUrl : String by lazy {
        _imageUrl ?: "https://b.st-hatena.com/images/v4/public/common/noimage.png"
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
        Uri.parse(url).let { uri ->
            if (uri.host == "anond.hatelabo.jp") "https://anond.hatelabo.jp/images/favicon.ico"
            else mFaviconUrl ?: "https://www.google.com/s2/favicons?domain=${Uri.parse(url).host}"
        }
    }

    fun copy(
        id: Long = this.id,
        title: String = this.title,
        description: String = this.description,
        count: Int = this.count,
        url: String = this._url,
        rootUrl: String = this.rootUrl,
        faviconUrl: String? = this.faviconUrl,
        imageUrl: String = this.imageUrl,
        ampUrl: String? = this.ampUrl,
        bookmarkedData: BookmarkResult? = this.bookmarkedData,
        myhotentryComments: List<BookmarkResult>? = this.myHotEntryComments,
        date: LocalDateTime? = this.date
    ) = Entry(
        id = id,
        title = title,
        description = description,
        count = count,
        _url = url,
        rootUrl = rootUrl,
        faviconUrl = faviconUrl,
        _imageUrl = imageUrl,
        ampUrl = ampUrl,
        bookmarkedData = bookmarkedData,
        myHotEntryComments = myhotentryComments,
        date = date
    )

    /** 同じエントリに対するインスタンスかを確認する */
    fun same(other: Entry?): Boolean =
        id == other?.id || url == other?.url
}

internal data class EntriesWithIssue(
    val issue: Issue,
    val entries: List<Entry>
) {
    // for Gson
    private constructor() : this(Issue(), emptyList())
}

/**
 * お気に入りユーザー情報に含まれるエントリ
 */
data class FollowingEntry(
    @SerializedName("entry_id")
    val id : Long,

    val title : String,

    val content : String,

    @SerializedName("total_bookmarks")
    val count : Int,

    val url : String,

    val ampUrl : String?,

    val faviconUrl: String,

    val imageUrl : String?,

    @SerializedName("created_at")
    @JsonAdapter(TimestampDeserializer::class)
    val timestamp: LocalDateTime

    // val bookmarkByVisitor
) {
    internal constructor() : this(0, "", "", 0, "", null, "", null, LocalDateTime.MIN)
}
