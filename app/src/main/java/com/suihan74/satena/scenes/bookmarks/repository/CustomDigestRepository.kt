package com.suihan74.satena.scenes.bookmarks.repository

import androidx.lifecycle.LiveData
import com.suihan74.satena.models.CustomDigestSettingsKey
import com.suihan74.utilities.PreferenceLiveData
import com.suihan74.utilities.SafeSharedPreferences

fun CustomDigestRepository(prefs : SafeSharedPreferences<CustomDigestSettingsKey>) : CustomDigestRepository =
    CustomDigestRepositoryImpl(prefs)

fun mutableCustomDigestRepository(prefs : SafeSharedPreferences<CustomDigestSettingsKey>) =
    CustomDigestRepositoryImpl(prefs)

// ------ //

interface CustomDigestRepository {
    /** 最大要素数 */
    val maxNumOfElements : LiveData<Int>

    /** 抽出対象になるスター数の閾値 */
    val starsCountThreshold : LiveData<Int>

    /** カスタムダイジェストを使用する */
    val useCustomDigest : LiveData<Boolean>

    /** 非表示ユーザーのスターを無視する */
    val ignoreStarsByIgnoredUsers : LiveData<Boolean>

    /** 同じユーザーが複数つけた同色のスターを1個だけと数える */
    val deduplicateStars : LiveData<Boolean>
}

// ------ //

class CustomDigestRepositoryImpl(
    private val customDigestSettings : SafeSharedPreferences<CustomDigestSettingsKey>,
) : CustomDigestRepository {
    /** 最大要素数 */
    override val maxNumOfElements by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.MAX_NUM_OF_ELEMENTS) { p, key ->
            p.getInt(key)
        }
    }

    /** 抽出対象になるスター数の閾値 */
    override val starsCountThreshold by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.STARS_COUNT_THRESHOLD) { p, key ->
            p.getInt(key)
        }
    }

    /** カスタムダイジェストを使用する */
    override val useCustomDigest by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.USE_CUSTOM_DIGEST) { p, key ->
            p.getBoolean(key)
        }
    }

    /** 非表示ユーザーのスターを無視する */
    override val ignoreStarsByIgnoredUsers by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.IGNORE_STARS_BY_IGNORED_USERS) { p, key ->
            p.getBoolean(key)
        }
    }

    /** 同じユーザーが複数つけた同色のスターを1個だけと数える */
    override val deduplicateStars by lazy {
        PreferenceLiveData(customDigestSettings, CustomDigestSettingsKey.DEDUPLICATE_STARS) { p, key ->
            p.getBoolean(key)
        }
    }
}
