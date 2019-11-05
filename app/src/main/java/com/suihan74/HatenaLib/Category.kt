package com.suihan74.HatenaLib

import com.google.gson.annotations.SerializedName
import java.io.Serializable

enum class Category (
    val int: Int,
    val code: String
) {
    All(0, "315767106563433873"),
    General(1, "315756341902288872"),
    Social(2, "301816409282464093"),
    Economics(3, "300989576564947867"),
    Life(4, "244148959988020477"),
    Knowledge(5, "315890158150969179"),
    It(6, "261248828312298389"),
    Entertainment(7, "302115476501939948"),
    Game(8, "297347994088281699"),
    Fun(9, "302115476506048236");
//    CurrentEvents(10, "83497569613451046"),

    companion object {
        fun fromInt(i: Int) : Category = values().firstOrNull { it.int == i } ?: All
    }
}

data class Issue (
    val name: String,

    @SerializedName("issue_id")
    val code: String,

    val imageUrl: String? = null,
    val entry: Entry? = null
) : Serializable

data class CategoryEntry (
    val name: String,
    @SerializedName("category_id")
    val code: String,
    val imageUrl: String,
    val pickupEntry: Entry,
    val issues: List<Issue>
) : Serializable

data class IssueEntry (
    val issues: List<Issue>
) : Serializable
