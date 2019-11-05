package com.suihan74.satena.models

import com.suihan74.satena.R

enum class Category(
    val int: Int,
    val textId: Int,
    val iconId: Int,
    val requireSignedIn: Boolean = false,
    val singleColumns : Boolean = false
) {
    All(0,
        R.string.category_all,
        R.drawable.ic_category_all),

    General(1,
        R.string.category_general,
        R.drawable.ic_category_general),

    Social(2,
        R.string.category_social,
        R.drawable.ic_category_social),

    Economics(3,
        R.string.category_economics,
        R.drawable.ic_category_economics),

    Life(4,
        R.string.category_life,
        R.drawable.ic_category_life),

    Knowledge(5,
        R.string.category_knowledge,
        R.drawable.ic_category_knowledge),

    It(6,
        R.string.category_it,
        R.drawable.ic_category_it),

    Entertainment(7,
        R.string.category_entertainment,
        R.drawable.ic_category_entertainment),

    Game(8,
        R.string.category_game,
        R.drawable.ic_category_game),

    Fun(9,
        R.string.category_fun,
        R.drawable.ic_category_fun),

    MyHotEntries(10,
        R.string.category_myhotentries,
        R.drawable.ic_category_myhotentries,
        requireSignedIn = true,
        singleColumns = true),

    MyBookmarks(11,
        R.string.category_mybookmarks,
        R.drawable.ic_category_mybookmarks,
        requireSignedIn = true),

    MyTags(12,
        R.string.category_mytags,
        R.drawable.ic_category_mytags,
        requireSignedIn = true,
        singleColumns = true),

    Search(13,
        R.string.category_search,
        R.drawable.ic_category_search,
        singleColumns = true),

    MyStars(14,
        R.string.category_mystars,
        R.drawable.ic_star,
        requireSignedIn = true,
        singleColumns = true),

    StarsReport(15,
        R.string.category_stars_report,
        R.drawable.ic_star,
        requireSignedIn = true,
        singleColumns = true),

    Maintenance(16,
        R.string.category_maintenance,
        R.drawable.ic_category_maintenance,
        requireSignedIn = false,
        singleColumns = true);

    companion object {
        fun fromInt(i: Int) : Category = values().getOrNull(i) ?: All
        fun valuesWithSignedIn() = values()
        fun valuesWithoutSignedIn() = values().filterNot { it.requireSignedIn }.toTypedArray()
    }

    fun toApiCategory() : com.suihan74.HatenaLib.Category =
        com.suihan74.HatenaLib.Category.fromInt(this.int)
}
