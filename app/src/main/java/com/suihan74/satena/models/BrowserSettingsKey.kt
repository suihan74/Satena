package com.suihan74.satena.models

import com.suihan74.satena.scenes.browser.BlockUrlSetting
import com.suihan74.satena.scenes.browser.SearchEngineSetting
import com.suihan74.satena.scenes.browser.WebViewTheme
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

/**
 * WebView版内部ブラウザの設定
 */
@SharedPreferencesKey(fileName = "browser_settings", version = 0, latest = true)
enum class BrowserSettingsKey (
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    /** アプリバーを下部に配置する */
    USE_BOTTOM_APP_BAR(typeInfo<Boolean>(), true),

    /** ダークテーマを使用するかどうかの設定 */
    THEME(typeInfo<WebViewTheme>(), WebViewTheme.AUTO),

    /** UserAgent設定(null = デフォルト) */
    USER_AGENT(typeInfo<String?>(), null),

    /** アドレスバーで使用する検索エンジン */
    SEARCH_ENGINE(typeInfo<SearchEngineSetting>(), SearchEngineSetting.Presets.Google.setting),

    /** スタートページ */
    START_PAGE_URL(typeInfo<String>(), "https://www.hatena.ne.jp/"),

    /** ドロワが開かれるまでブクマ情報を取得しない */
    BOOKMARKS_LAZY_LOADING(typeInfo<Boolean>(), true),

    /** セーフブラウジングを有効にする */
    PRIVATE_BROWSING_ENABLED(typeInfo<Boolean>(), false),

    /** JavaScriptを有効にする */
    JAVASCRIPT_ENABLED(typeInfo<Boolean>(), true),

    /** ブロックURL設定を使用する */
    USE_URL_BLOCKING(typeInfo<Boolean>(), true),

    /** ブロックURLリスト */
    @Suppress("SpellCheckingInspection")
    BLOCK_URLS(typeInfo<List<BlockUrlSetting>>(), listOf(
        // プリセット
        "sp.gmossp-sp.jp/ads",
        "in.treasuredata.com",
        "pixon.ads-pixiv.net",
        "bidder.criteo.com",
        ".openx.net",
        "torimochi.line-apps.com",
        "aladdin.genieesspv.jp",
        ".ad-nex.com",
        ".nend.net",
        ".bidswitch.net",
        ".teads.tv",
        ".adsrvr.org",
        ".taboola.com",
        "www.google.co.jp/ads/",
        "static.ads-twitter.com",
        "pagead2.googlesyndication.com",
        ".microad.jp",
        "ads.pubmatic.com",
        ".adingo.jp",
        "fam-8.net",
        ".ad-stir.com",
        "adserver-as.adtech.advertising.com",
        "www.googleadservices.com/pagead/",
        "ads.nicovideo.jp",
        ".criteo.net",
        ".doubleclick.net",
        "socdm.com/adsv",
        ".adtdp.com",
        "impact-ad.jp",
        "pb.ladsp.com",
        ".logly.co.jp",
        ".adnxs.com",
        "amazon-adsystem"
    ).map { BlockUrlSetting(it, false) })
}

