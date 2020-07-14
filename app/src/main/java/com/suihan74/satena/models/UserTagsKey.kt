package com.suihan74.satena.models

import android.content.Context
import android.util.Log
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.userTag.UserTagDao
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.typeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

/**************************************
 * version 1
 **************************************/
@Deprecated("DBに移行。後のバージョンで消去する")
@Suppress("DEPRECATION")
@SharedPreferencesKey(fileName = "user_tags", version = 1, latest = true)
enum class UserTagsKey(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    CONTAINER(typeInfo<UserTagsContainer>(), UserTagsContainer())
}

////////////////////////////////////////////////////////////////////////////////
// previous versions
////////////////////////////////////////////////////////////////////////////////

/**************************************
 * version 0
 **************************************/

@Deprecated("DBに移行。後のバージョンで消去する")
@Suppress("DEPRECATION")
@SharedPreferencesKey(fileName = "user_tags", version = 0)
enum class UserTagsKeyVersion0(
    override val valueType: Type,
    override val defaultValue: Any?
) : SafeSharedPreferences.Key {

    CONTAINER(typeInfo<UserTagsContainer>(), UserTagsContainer())
}

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

@Suppress("DEPRECATION")
object UserTagsKeyMigration {
    suspend fun check(context: Context) {
        val dao = SatenaApplication.instance.userTagDao
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)

        when (SafeSharedPreferences.version<UserTagsKey>(context)) {
            0 -> migrateFromVersion0(prefs, dao)
        }
    }

    private suspend fun migrateFromVersion0(
        prefs: SafeSharedPreferences<UserTagsKey>,
        dao: UserTagDao
    ) = withContext(Dispatchers.IO) {
        val container = prefs.get<UserTagsContainer>(UserTagsKey.CONTAINER)

        try {
            container.users.forEach { user ->
                dao.makeUser(user.name)
            }
            container.tags.forEach { tag ->
                val tagEntity = dao.makeTag(tag.name)
                // 全ユーザーのタグ付け処理
                val users = container.getUsersOfTag(tag)
                users.forEach { user ->
                    val userEntity = dao.findUser(user.name)
                    if (userEntity != null) {
                        dao.insertRelation(tagEntity, userEntity)
                    }
                }
            }

            // バージョンを更新する
            prefs.edit {}
        }
        catch (e: Throwable) {
            Log.e("migrationUserTags", e.message ?: "")
            dao.clearAll()
        }
    }
}
