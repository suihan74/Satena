package com.suihan74.satena.models

import com.suihan74.satena.R

@Suppress("unused")
enum class Category(
    val textId: Int,
    val iconId: Int,
    val categoryInApi: com.suihan74.hatenaLib.Category? = null,
    val requireSignedIn: Boolean = false,
    val singleColumns : Boolean = false,
    val hasIssues: Boolean = false,
    val displayInList: Boolean = true
) {
    All(
        R.string.category_all,
        R.drawable.ic_category_all,
        categoryInApi = com.suihan74.hatenaLib.Category.All),

    General(
        R.string.category_general,
        R.drawable.ic_category_general,
        categoryInApi = com.suihan74.hatenaLib.Category.General),

    Social(
        R.string.category_social,
        R.drawable.ic_category_social,
        categoryInApi = com.suihan74.hatenaLib.Category.Social,
        hasIssues = true),

    Economics(
        R.string.category_economics,
        R.drawable.ic_category_economics,
        categoryInApi = com.suihan74.hatenaLib.Category.Economics,
        hasIssues = true),

    Life(
        R.string.category_life,
        R.drawable.ic_category_life,
        categoryInApi = com.suihan74.hatenaLib.Category.Life,
        hasIssues = true),

    Knowledge(
        R.string.category_knowledge,
        R.drawable.ic_category_knowledge,
        categoryInApi = com.suihan74.hatenaLib.Category.Knowledge,
        hasIssues = true),

    It(
        R.string.category_it,
        R.drawable.ic_category_it,
        categoryInApi = com.suihan74.hatenaLib.Category.It,
        hasIssues = true),

    Entertainment(
        R.string.category_entertainment,
        R.drawable.ic_category_entertainment,
        categoryInApi = com.suihan74.hatenaLib.Category.Entertainment,
        hasIssues = true),

    Game(
        R.string.category_game,
        R.drawable.ic_category_game,
        categoryInApi = com.suihan74.hatenaLib.Category.Game,
        hasIssues = true),

    Fun(
        R.string.category_fun,
        R.drawable.ic_category_fun,
        categoryInApi = com.suihan74.hatenaLib.Category.Fun,
        hasIssues = true),

    MyHotEntries(
        R.string.category_myhotentries,
        R.drawable.ic_category_myhotentries,
        requireSignedIn = true,
        singleColumns = true),

    MyBookmarks(
        R.string.category_mybookmarks,
        R.drawable.ic_category_mybookmarks,
        requireSignedIn = true),

    @Deprecated("`MyTags` is integrated into `MyBookmarks`")
    MyTags(
        R.string.category_mytags,
        R.drawable.ic_category_mytags,
        displayInList = false,
        requireSignedIn = true,
        singleColumns = true),

    Search(
        R.string.category_search,
        R.drawable.ic_category_search,
        singleColumns = true),

    MyStars(
        R.string.category_mystars,
        R.drawable.ic_star,
        requireSignedIn = true,
        singleColumns = true),

    StarsReport(
        R.string.category_stars_report,
        R.drawable.ic_star,
        requireSignedIn = true,
        singleColumns = true),

    Maintenance(
        R.string.category_maintenance,
        R.drawable.ic_category_maintenance,
        requireSignedIn = false,
        singleColumns = true),

    History(
        R.string.category_history,
        R.drawable.ic_category_history,
        requireSignedIn = false,
        singleColumns = true),

    Site(0, 0,
        displayInList = false),

    User(0, 0,
        displayInList = false),

    Notices(
        R.string.notices_desc,
        R.drawable.ic_notifications,
        displayInList = false,
        requireSignedIn = true,
        singleColumns = true)

    ;

    companion object {
        fun fromInt(i: Int) : Category = values().getOrNull(i) ?: All
        fun valuesWithSignedIn() = values().filter { it.displayInList }.toTypedArray()
        fun valuesWithoutSignedIn() = values().filterNot { !it.displayInList || it.requireSignedIn }.toTypedArray()
    }

    val code: String? by lazy { categoryInApi?.code }
}
