package com.suihan74.satena.models

import android.content.Context
import android.util.Log
import com.suihan74.satena.SatenaApplication
import com.suihan74.satena.models.userTag.clearAll
import com.suihan74.satena.models.userTag.insertRelation
import com.suihan74.satena.models.userTag.makeTag
import com.suihan74.satena.models.userTag.makeUser
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

////////////////////////////////////////////////////////////////////////////////
// version migration
////////////////////////////////////////////////////////////////////////////////

object UserTagsKeyMigration {
    suspend fun check(context: Context) {
        when (SafeSharedPreferences.version<UserTagsKey>(context)) {
            0 -> migrateFromVersion0(context)
        }
    }

    private suspend fun migrateFromVersion0(context: Context) = withContext(Dispatchers.IO) {
        val dao = SatenaApplication.instance.userTagDao
        val prefs = SafeSharedPreferences.create<UserTagsKey>(context)
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
        catch (e: Exception) {
            Log.e("migrationUserTags", e.message)
            dao.clearAll()
        }
    }
}
