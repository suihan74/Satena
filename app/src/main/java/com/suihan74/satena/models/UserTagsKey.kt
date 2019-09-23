package com.suihan74.satena.models

import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import java.lang.reflect.Type

/**************************************
 * version 0
 **************************************/
@SharedPreferencesKey(fileName = "user_tags", version = 0, latest = true)
enum class UserTagsKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    CONTAINER(typeInfo<UserTagsContainer>(), UserTagsContainer())
}

