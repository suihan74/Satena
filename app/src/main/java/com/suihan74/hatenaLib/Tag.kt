package com.suihan74.hatenaLib

import java.io.Serializable

data class Tag (
    val text : String,
    val index : Long,
    val count : Long,
    val timestamp : Long
) : Serializable

data class TagsResponse (
    val count : Long,
    val status : Int,
    val tags : Map<String, Tag>
) : Serializable
