package com.suihan74.hatenaLib

import com.google.gson.annotations.SerializedName

data class RelatedEntriesResponse(
    val entries : List<Entry>,
    val metaEntry : Entry?,
    @SerializedName("refered_blog_entries")
    val referredBlogEntries : List<Entry>,
    @SerializedName("refered_entries")
    val referredEntries : List<Entry>,
//    val topics : List<String>
)
