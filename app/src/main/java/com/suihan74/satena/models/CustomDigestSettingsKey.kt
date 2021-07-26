package com.suihan74.satena.models

import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

@SharedPreferencesKey(fileName = "custom_digest_settings", version = 0, latest = true)
enum class CustomDigestSettingsKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    /** カスタムダイジェストを使用する */
    USE_CUSTOM_DIGEST(typeInfo<Boolean>(), false),

    // ダイジェスト抽出処理の設定

    /** 非表示ユーザーのスターを無視する */
    IGNORE_STARS_BY_IGNORED_USERS(typeInfo<Boolean>(), true),

    /** 同じユーザーが複数つけた同色のスターを1個だけと数える */
    DEDUPLICATE_STARS(typeInfo<Boolean>(), true)
}
