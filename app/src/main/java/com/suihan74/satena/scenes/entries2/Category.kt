package com.suihan74.satena.scenes.entries2

import com.suihan74.hatenaLib.SearchType
import com.suihan74.satena.models.Category
import com.suihan74.satena.scenes.entries2.pages.*

/** カテゴリに対応したフラグメントを生成する */
internal fun Category.createFragment() : EntriesFragment =
    when (this) {
        Category.MyBookmarks -> MyBookmarksEntriesFragment.createInstance()

        Category.MyHotEntries -> MyHotEntriesFragment.createInstance()

        Category.Stars -> StarsFragment.createInstance()

        Category.Notices -> NoticesFragment.createInstance()

        Category.History -> HistoryFragment.createInstance()

        Category.Search -> SearchEntriesFragment.createInstance()

        Category.Maintenance -> InformationFragment.createInstance()

        Category.Memorial15th -> Memorial15Fragment.createInstance()

        Category.FavoriteSites -> FavoriteSitesFragment.createInstance()

        else -> HatenaEntriesFragment.createInstance(this)
    }

// 以下別途パラメータが必要なインスタンス生成

/** Category.Site用 */
internal fun Category.createSiteFragment(siteUrl: String) : EntriesFragment {
    check(this == Category.Site) { "Category.createSiteFragment() is not be able to call with $name" }
    return SiteEntriesFragment.createInstance(siteUrl)
}

/** Category.User用 */
internal fun Category.createUserFragment(user: String) : EntriesFragment {
    check(this == Category.User) { "Category.createUserFragment() is not be able to call with $name" }
    return UserEntriesFragment.createInstance(user)
}

/**
 * Category.Search用
 *
 * クエリの初期値を指定して開く場合
 */
internal fun Category.createSearchFragment(query: String, searchType: SearchType) : EntriesFragment {
    check(this == Category.Search) { "Category.createSearchFragment() is not be able to call with $name" }
    return SearchEntriesFragment.createInstance(query, searchType)
}
