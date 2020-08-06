package com.suihan74.satena.models

import com.suihan74.hatenaLib.Notice
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import org.threeten.bp.LocalDateTime
import java.lang.reflect.Type

data class NoticeTimestamp(
    val created: LocalDateTime,
    val modified: LocalDateTime
) {
    // for Gson
    private constructor() : this(LocalDateTime.MIN, LocalDateTime.MIN)
}

////////////////////////////////////////
// notices
////////////////////////////////////////
@SharedPreferencesKey(fileName = "notices", version = 0, latest = true)
enum class NoticesKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    /** 保存された通知リスト */
    NOTICES(typeInfo<List<Notice>>(), emptyList<Notice>()),
    /** 最大保存数 */
    NOTICES_SIZE(typeInfo<Int>(), 100),
    /**
     * 削除された通知（の作成時間と更新時間のペア）リスト
     * first -> created
     * second -> modified
     */
    REMOVED_NOTICE_TIMESTAMPS(typeInfo<List<NoticeTimestamp>>(), emptyList<NoticeTimestamp>())
}
