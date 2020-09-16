package com.suihan74.satena.models

import com.suihan74.satena.scenes.browser.BlockUrlSetting
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

    /** UserAgent設定(null = デフォルト) */
    USER_AGENT(typeInfo<String?>(), null),

    /** 検索エンジン */
    SEARCH_ENGINE(typeInfo<String>(), "https://www.google.com/search?q="),

    /** スタートページ */
    START_PAGE_URL(typeInfo<String>(), "https://www.hatena.ne.jp/"),

    /** JavaScriptを有効にする */
    JAVASCRIPT_ENABLED(typeInfo<Boolean>(), true),

    /** ブロックURL設定を使用する */
    USE_URL_BLOCKING(typeInfo<Boolean>(), true),

    /** ブロックURLリスト */
    BLOCK_URLS(typeInfo<List<BlockUrlSetting>>(), listOf(
        "amazon-adsystem",  // テスト用のデータ
        ".adnxs.com",
        ".logly.co.jp",
        "pb.ladsp.com",
        "impact-ad.jp",
        ".adtdp.com",
        "socdm.com/adsv",
        ".doubleclick.net",
        ".criteo.net",
        "ads.nicovideo.jp"
    ).map { BlockUrlSetting(it, false) })
}

