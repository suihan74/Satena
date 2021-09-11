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

    /** 最大要素数 */
    MAX_NUM_OF_ELEMENTS(typeInfo<Int>(), 10),

    /** 抽出対象になるスター数の閾値 */
    STARS_COUNT_THRESHOLD(typeInfo<Int>(), 1),

    /** 非表示ユーザーのスターを無視する */
    IGNORE_STARS_BY_IGNORED_USERS(typeInfo<Boolean>(), true),

    /** 同じユーザーが複数つけた同色のスターを1個だけと数える */
    DEDUPLICATE_STARS(typeInfo<Boolean>(), true),

    ;
    companion object {
        const val MAX_NUM_OF_ELEMENTS_LOWER_BOUND = 1
        const val MAX_NUM_OF_ELEMENTS_UPPER_BOUND = 30

        const val STARS_COUNT_THRESHOLD_LOWER_BOUND = 0
        const val STARS_COUNT_THRESHOLD_UPPER_BOUND = 100
    }
}
