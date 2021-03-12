package com.suihan74.satena.scenes.preferences.favoriteSites

import android.app.Activity
import com.suihan74.satena.models.FavoriteSite
import com.suihan74.satena.scenes.preferences.pages.FavoriteSitesFragment

/**
 *  設定・ブラウザドロワで異なる処理を実行するためのIF
 */
interface FavoriteSitesViewModelInterface {
    /**
     * お気に入りサイト項目をクリックした際の処理
     */
    fun onClickItem(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    )

    /**
     * お気に入りサイト項目をロングクリックした際の処理
     */
    fun onLongClickItem(
        site: FavoriteSite,
        activity: Activity,
        fragment: FavoriteSitesFragment
    )

    /**
     * お気に入りサイト項目を追加するFABをクリックした際の処理
     */
    fun onClickAddButton(
        activity: Activity,
        fragment: FavoriteSitesFragment
    )

    /**
     * サイトを内部ブラウザで開く
     */
    fun openInBrowser(
        site: FavoriteSite,
        activity: Activity
    )
}
