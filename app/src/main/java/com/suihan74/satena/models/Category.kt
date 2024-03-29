package com.suihan74.satena.models

import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import com.suihan74.satena.R

@Suppress("unused")
enum class Category(
    @IntRange(from = 1, to = 23) val id: Int,
    @StringRes override val textId: Int,
    @DrawableRes val iconId: Int,
    val categoryInApi: com.suihan74.hatenaLib.Category? = null,
    val requireSignedIn: Boolean = false,
    val singleColumns : Boolean = false,
    val hasIssues: Boolean = false,
    val displayInList: Boolean = true,
    val willBeHome : Boolean = true,
    val canHideReadEntries : Boolean = true
) : TextIdContainer {
    All(id = 0,
        R.string.category_all,
        R.drawable.ic_category_all,
        categoryInApi = com.suihan74.hatenaLib.Category.All
    ),

    General(id = 1,
        R.string.category_general,
        R.drawable.ic_category_general,
        categoryInApi = com.suihan74.hatenaLib.Category.General
    ),

    Social(id = 2,
        R.string.category_social,
        R.drawable.ic_category_social,
        categoryInApi = com.suihan74.hatenaLib.Category.Social,
        hasIssues = true
    ),

    Economics(id = 3,
        R.string.category_economics,
        R.drawable.ic_category_economics,
        categoryInApi = com.suihan74.hatenaLib.Category.Economics,
        hasIssues = true
    ),

    Life(id = 4,
        R.string.category_life,
        R.drawable.ic_category_life,
        categoryInApi = com.suihan74.hatenaLib.Category.Life,
        hasIssues = true
    ),

    Knowledge(id = 5,
        R.string.category_knowledge,
        R.drawable.ic_category_knowledge,
        categoryInApi = com.suihan74.hatenaLib.Category.Knowledge,
        hasIssues = true
    ),

    It(id = 6,
        R.string.category_it,
        R.drawable.ic_category_it,
        categoryInApi = com.suihan74.hatenaLib.Category.It,
        hasIssues = true
    ),

    Entertainment(id = 7,
        R.string.category_entertainment,
        R.drawable.ic_category_entertainment,
        categoryInApi = com.suihan74.hatenaLib.Category.Entertainment,
        hasIssues = true
    ),

    Game(id = 8,
        R.string.category_game,
        R.drawable.ic_category_game,
        categoryInApi = com.suihan74.hatenaLib.Category.Game,
        hasIssues = true
    ),

    Fun(id = 9,
        R.string.category_fun,
        R.drawable.ic_category_fun,
        categoryInApi = com.suihan74.hatenaLib.Category.Fun,
        hasIssues = true
    ),

    MyHotEntries(id = 10,
        R.string.category_myhotentries,
        R.drawable.ic_category_myhotentries,
        requireSignedIn = true,
        singleColumns = true
    ),

    MyBookmarks(id = 11,
        R.string.category_mybookmarks,
        R.drawable.ic_category_mybookmarks,
        requireSignedIn = true
    ),

    Followings(id = 23,
        R.string.category_followings,
        R.drawable.ic_category_followings,
        requireSignedIn = true,
        singleColumns = true
    ),

    FavoriteSites(id = 22,
        R.string.category_favorite_sites,
        R.drawable.ic_category_favorite_sites
    ),

    @Deprecated("`MyTags` is integrated into `MyBookmarks`")
    MyTags(id = 12,
        R.string.category_mytags,
        R.drawable.ic_category_mytags,
        displayInList = false,
        requireSignedIn = true,
        singleColumns = true,
        willBeHome = false,
        canHideReadEntries = false
    ),

    Search(id = 13,
        R.string.category_search,
        R.drawable.ic_category_search,
        singleColumns = false
    ),

    Stars(id = 14,
        R.string.category_mystars,
        R.drawable.ic_star,
        requireSignedIn = true
    ),

    // 消さないで
    @Deprecated("`MyStars` & `StarsReport` is integrated into `Stars`")
    StarsReport(id = 15,
        R.string.category_stars_report,
        R.drawable.ic_star,
        displayInList = false,
        requireSignedIn = true,
        singleColumns = true,
        willBeHome = false,
        canHideReadEntries = false
    ),

    Memorial15th(id = 21,
        R.string.category_memorial15,
        R.drawable.ic_category_memorial,
        requireSignedIn = false,
        willBeHome = false
    ),

    Maintenance(id = 16,
        R.string.category_maintenance,
        R.drawable.ic_category_maintenance,
        requireSignedIn = false,
        singleColumns = true,
        canHideReadEntries = false
    ),

    History(id = 17,
        R.string.category_history,
        R.drawable.ic_category_history,
        requireSignedIn = false,
        singleColumns = true,
        canHideReadEntries = false
    ),

    Site(id = 18,
        R.string.category_site,
        R.drawable.ic_category_site,
        displayInList = false,
        willBeHome = false
    ),

    User(id = 19,
        R.string.category_user,
        0,
        singleColumns = true,
        displayInList = false,
        willBeHome = false
    ),

    Notices(id = 20,
        R.string.notices_desc,
        R.drawable.ic_notifications,
        requireSignedIn = true,
        singleColumns = true,
        displayInList = false,
        willBeHome = false,
        canHideReadEntries = false
    )

    ;

    companion object {
        fun fromId(id: Int) : Category = values().firstOrNull { it.id == id } ?: All
        fun valuesWithSignedIn() = values().filter { it.displayInList }.toTypedArray()
        fun valuesWithoutSignedIn() = values().filterNot { !it.displayInList || it.requireSignedIn }.toTypedArray()
    }

    val code: String? by lazy { categoryInApi?.code }
}
