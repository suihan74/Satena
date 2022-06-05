package com.suihan74.satena.models.favoriteSite

import androidx.room.*
import com.suihan74.satena.models.browser.FaviconInfo

/** お気に入りサイトのエントリを取得するための情報 */
@Entity(
    tableName = "favorite_site",
    indices = [
        Index(value = ["url"], name = "favorite_site_url", unique = true)
    ]
)
data class FavoriteSite (
    /** サイトURL */
    val url : String,

    /** サイトタイトル */
    val title : String,

    /** 有効状態(フィードを取得して画面に表示するか否か) */
    val isEnabled : Boolean,

    /** キャッシュされている場合のfaviconInfo */
    val faviconInfoId : Long,

    /** キャッシュされていない場合のfaviconのURL */
    val faviconUrl : String = "",

    @PrimaryKey(autoGenerate = true)
    val id : Long = 0
) {
    fun same(other: FavoriteSite?) : Boolean {
        if (other == null) return false
        return url == other.url &&
                title == other.title &&
                faviconInfoId == other.faviconInfoId &&
                faviconUrl == other.faviconUrl &&
                isEnabled == other.isEnabled &&
                id == other.id
    }
}

// ------ //

data class FavoriteSiteAndFavicon (
    @Embedded
    val site : FavoriteSite,

    @Relation(
        parentColumn = "faviconInfoId",
        entityColumn = "id"
    )
    val faviconInfo : FaviconInfo?
)
