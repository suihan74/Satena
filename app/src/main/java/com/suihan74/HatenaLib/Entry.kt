package com.suihan74.HatenaLib

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Entry (
    @SerializedName("eid")
    val id : Long,
    val title : String,
    val description : String,
    val count : Int,

    val url : String,
    val rootUrl : String,
    val faviconUrl : String?,
    @SerializedName("image")
    val imageUrl : String,

    val ampUrl : String? = null,

    // ユーザーがブクマしている場合のみ取得
    val bookmarkedData : BookmarkResult? = null,

    // ホットエントリにのみ含まれる情報
    val myhotentryComments : List<BookmarkResult>? = null
) : Serializable

fun emptyEntry() = Entry(
    id = 0,
    title = "",
    description = "",
    count = 0,
    url = "",
    rootUrl = "",
    faviconUrl = "",
    imageUrl = ""
)
