package com.suihan74.satena.models

import com.suihan74.satena.R

enum class Category(
    val int: Int,
    val textId: Int,
    val iconId: Int,
    val code: String? = null,
    val requireSignedIn: Boolean = false,
    val singleColumns : Boolean = false,
    val hasIssues: Boolean = false
) {
    All(0,
        R.string.category_all,
        R.drawable.ic_category_all,
        code = com.suihan74.HatenaLib.Category.All.code),

    General(1,
        R.string.category_general,
        R.drawable.ic_category_general,
        code = com.suihan74.HatenaLib.Category.General.code),

    Social(2,
        R.string.category_social,
        R.drawable.ic_category_social,
        code = com.suihan74.HatenaLib.Category.Social.code,
        hasIssues = true),

    Economics(3,
        R.string.category_economics,
        R.drawable.ic_category_economics,
        code = com.suihan74.HatenaLib.Category.Economics.code,
        hasIssues = true),

    Life(4,
        R.string.category_life,
        R.drawable.ic_category_life,
        code = com.suihan74.HatenaLib.Category.Life.code,
        hasIssues = true),

    Knowledge(5,
        R.string.category_knowledge,
        R.drawable.ic_category_knowledge,
        code = com.suihan74.HatenaLib.Category.Knowledge.code,
        hasIssues = true),

    It(6,
        R.string.category_it,
        R.drawable.ic_category_it,
        code = com.suihan74.HatenaLib.Category.It.code,
        hasIssues = true),

    Entertainment(7,
        R.string.category_entertainment,
        R.drawable.ic_category_entertainment,
        code = com.suihan74.HatenaLib.Category.Entertainment.code,
        hasIssues = true),

    Game(8,
        R.string.category_game,
        R.drawable.ic_category_game,
        code = com.suihan74.HatenaLib.Category.Game.code,
        hasIssues = true),

    Fun(9,
        R.string.category_fun,
        R.drawable.ic_category_fun,
        code = com.suihan74.HatenaLib.Category.Fun.code,
        hasIssues = true),

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
