package com.suihan74.satena.models

import com.suihan74.HatenaLib.Notice
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

////////////////////////////////////////
// notices
////////////////////////////////////////
@SharedPreferencesKey(fileName = "notices", version = 0, latest = true)
enum class NoticesKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {
    NOTICES(typeInfo<List<Notice>>(), emptyList<Notice>()),
    NOTICES_SIZE(typeInfo<Int>(), 100),
}
