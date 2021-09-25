package com.suihan74.satena.models

import android.content.Context
import com.suihan74.hatenaLib.Notice
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type
import java.time.LocalDateTime

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
@SharedPreferencesKey(fileName = "notices", version = 1, latest = true)
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
    ;

    companion object {
        fun fileName(user: String) = "notices_$user"
    }
}

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

@Suppress("deprecation")
object NoticesKeyMigration {
    fun check(context: Context) {
        while (true) {
            when (SafeSharedPreferences.version<NoticesKey>(context)) {
                0 -> {
                    migrateFromVersion0(context)
                    break
                }
                else -> break
            }
        }
    }

    private fun migrateFromVersion0(context: Context) {
        val src = SafeSharedPreferences.create<NoticesKey>(context)
        val srcNotices = src.getObject<List<Notice>>(NoticesKey.NOTICES) ?: return
        val srcRemovedTimestamps = src.getObject<List<NoticeTimestamp>>(NoticesKey.REMOVED_NOTICE_TIMESTAMPS).orEmpty()
        srcNotices.groupBy { it.user }
            .forEach { (user, notices) ->
                val removedTimestamps = srcRemovedTimestamps.filter { removed ->
                    notices.any { it.created == removed.created && it.modified == removed.modified }
                }
                val dest = SafeSharedPreferences.create<NoticesKey>(context, NoticesKey.fileName(user))
                dest.edit {
                    putObject(NoticesKey.NOTICES, notices)
                    putObject(NoticesKey.REMOVED_NOTICE_TIMESTAMPS, removedTimestamps)
                }
            }
        SafeSharedPreferences.delete<NoticesKey>(context)
    }
}
