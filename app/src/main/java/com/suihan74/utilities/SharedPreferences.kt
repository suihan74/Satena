package com.suihan74.utilities

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 型情報を取得するコードを統一的に書くためのもの
 * 他の用途で既にGSONのTypeTokenを使っているのでここでも使わせてもらう
 * （単純に T::class.java とかやると，たとえば List<Int> と List<String> の区別ができないため）
 */
inline fun <reified T> typeInfo() : Type = when (T::class) {
    Boolean::class,
    Int::class,
    Long::class,
    Float::class,
    String::class -> T::class.java
    else -> object: TypeToken<T>() {}.type
}

/**
 * キーに対応した設定ファイル名を指定するためのアノテーション
 * 使用されない場合はfileName="default"として扱う
 * ファイル名重複を厳密に避けたいならfileNameにもenumとか使うようにする
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SharedPreferencesKey(
    val fileName: String = "default",
    val version: Int = 0,
    val latest: Boolean = false
)

/**
 * SharedPreferencesで比較的安全に値の読み書きを行うためのラッパー
 * 1.キー名に文字列ではなくenumを使用する（重複・ミスタイプを防止）
 * 2.キーの定義にファイル名・値の型・デフォルト値をまとめて記述する（管理の簡単化）
 * 3.値編集後のapply()忘れ防止
 * 4.json使って任意のオブジェクトの読み書きに対応
 */
class SafeSharedPreferences<KeyT> private constructor (
    private val mFileName: String,
    private val mPrefs: SharedPreferences,
    private val mKeyVersion: Int
) where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> {

    val fileName
        get() = mFileName

    // 外部から秘匿する方法考え中
    /** 生のSharedPreferences */
    val rawPrefs
        get() = mPrefs

    val keyVersion
        get() = mKeyVersion

    companion object {
        private val instancesCache = WeakHashMap<String, SafeSharedPreferences<*>>()

        /** インスタンスを生成 */
        inline fun <reified KeyT> create(context: Context?)
                where KeyT: Key, KeyT: Enum<KeyT> =
            create(context, KeyT::class.java, null)

        /** インスタンスを生成 */
        inline fun <reified KeyT> create(context: Context?, fileName: String)
                where KeyT: Key, KeyT: Enum<KeyT> =
            create(context, KeyT::class.java, fileName)

        fun <KeyT> create(
            context: Context?,
            keyClass: Class<KeyT>,
            explicitFileName: String? = null
        ) : SafeSharedPreferences<KeyT> where KeyT: Key, KeyT: Enum<KeyT> {
            val keyAnnotation = keyClass.getAnnotation(SharedPreferencesKey::class.java)
            val fileName = explicitFileName ?: "preferences_${keyAnnotation?.fileName ?: "default"}"
            val keyVersion = keyAnnotation?.version ?: 0

            val cacheKey = "${fileName},${context!!.hashCode()}"
            val cacheValue = instancesCache[cacheKey]
            if (cacheValue != null) {
                @Suppress("UNCHECKED_CAST")
                return cacheValue as SafeSharedPreferences<KeyT>
            }

            val instance = SafeSharedPreferences<KeyT>(
                fileName,
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE),
                keyVersion
            )

            instancesCache[cacheKey] = instance
            return instance
        }

        /** 設定を消去する */
        inline fun <reified KeyT> delete(context: Context?) where KeyT: Key, KeyT: Enum<KeyT> = delete(context, KeyT::class.java)

        fun <KeyT> delete(
            context: Context?,
            keyClass: Class<KeyT>,
            explicitFileName: String? = null
        ) where KeyT: Key, KeyT: Enum<KeyT> {
            val fileName = explicitFileName ?: let {
                val keyAnnotation = keyClass.getAnnotation(SharedPreferencesKey::class.java)
                "preferences_${keyAnnotation?.fileName ?: "default"}"
            }
            delete(context, fileName, keyClass.name)
        }

        fun delete(
            context: Context?,
            fileName: String,
            keyClassName: String
        ) {
            val cacheKey = "${keyClassName},${context!!.hashCode()}"
            instancesCache.remove(cacheKey)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(fileName)
            }
        }

        /** 設定セットのバージョンを取得 */
        inline fun <reified KeyT> version(context: Context?) : Int where KeyT: Key, KeyT: Enum<KeyT> {
            val prefs = create<KeyT>(context)
            return prefs.version
        }

        /** 設定セットのバージョンを取得 */
        inline fun <reified KeyT> version(context: Context?, fileName: String) : Int where KeyT: Key, KeyT: Enum<KeyT> {
            val prefs = create<KeyT>(context, fileName)
            return prefs.version
        }

        /** 設定セットのバージョン移行 */
        inline fun <reified OldKeyT, reified LatestKeyT> migrate(
            context: Context?,
            action: (old: SafeSharedPreferences<OldKeyT>, latest: SafeSharedPreferences<LatestKeyT>)->Unit
        ) where OldKeyT: Key, OldKeyT: Enum<OldKeyT>,
                LatestKeyT: Key, LatestKeyT: Enum<LatestKeyT> {

            val oldAnnotation = OldKeyT::class.java.getAnnotation(SharedPreferencesKey::class.java)
            val oldKeyVersion = oldAnnotation!!.version

            val latestAnnotation = LatestKeyT::class.java.getAnnotation(SharedPreferencesKey::class.java)
            val latestKeyVersion = latestAnnotation!!.version
            require(latestAnnotation.latest) { "the migration destination PreferencesKey is not latest version" }

            if (oldKeyVersion >= latestKeyVersion) {
                return
            }

            val old = create<OldKeyT>(context)
            val latest = create<LatestKeyT>(context)

            val oldKeys = enumValues<OldKeyT>()
            val latestKeys = enumValues<LatestKeyT>()

            if (old.fileName != latest.fileName) {
                latest.edit {
                    oldKeys.forEach { oldKey ->
                        val oldValue = when(oldKey.valueType) {
                            typeInfo<Boolean>() -> old.rawPrefs.getBoolean(oldKey.name, oldKey.defaultValue as Boolean)
                            typeInfo<Int>() -> old.rawPrefs.getInt(oldKey.name, oldKey.defaultValue as Int)
                            typeInfo<Long>() -> old.rawPrefs.getLong(oldKey.name, oldKey.defaultValue as Long)
                            typeInfo<Float>() -> old.rawPrefs.getFloat(oldKey.name, oldKey.defaultValue as Float)
                            typeInfo<String>() -> old.rawPrefs.getString(oldKey.name, oldKey.defaultValue as String?)
                            else -> old.rawPrefs.getString(oldKey.name, "null") as Any
                        }
                        val latestKey = latestKeys.firstOrNull { it.name == oldKey.name }

                        if (latestKey != null && latestKey.valueType == oldKey.valueType) {
                            try {
                                when(latestKey.valueType) {
                                    typeInfo<Boolean>() -> rawEditor.putBoolean(oldKey.name, oldValue as Boolean)
                                    typeInfo<Int>() -> rawEditor.putInt(oldKey.name, oldValue as Int)
                                    typeInfo<Long>() -> rawEditor.putLong(oldKey.name, oldValue as Long)
                                    typeInfo<Float>() -> rawEditor.putFloat(oldKey.name, oldValue as Float)
                                    typeInfo<String>() -> rawEditor.putString(oldKey.name, oldValue as String?)
                                    else -> rawEditor.putString(oldKey.name, oldValue as String)
                                }
                            }
                            catch (e: Throwable) {
                                Log.e("migration", Log.getStackTraceString(e))
                                put(latestKey, latestKey.defaultValue)
                            }
                        }
                    }
                }
            }

            // ユーザー定義のバージョン移行処理
            action(old, latest)

            if (old.fileName == latest.fileName) {
                old.edit {
                    oldKeys.forEach { oldKey ->
                        val latestContains = latestKeys.firstOrNull { it.name == oldKey.name }
                        if (latestContains == null) {
                            remove(oldKey)
                        }
                    }
                }
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context!!.deleteSharedPreferences(old.fileName)
                }
            }
        }

        // 内部で扱う値のキー名(enumに使えない先頭文字+キー名)
        /** バージョン情報 */
        private const val INTERNAL_KEY_VERSION = "!VERSION"
        const val versionDefault = -1
    }

    // キャッシュしておく必要があるかというと微妙
    private var editorCache = WeakReference<SafeEditor<KeyT>>(null)

    /**
     * 値を編集する（apply()はaction終了後自動で実行される）
     * 今実装ではaction中に例外送出された場合，その時点まででapply()される（するべきか否かはよく考えていない）
     */
    @Suppress("CommitPrefEdits")
    fun edit(action: SafeEditor<KeyT>.()->Unit) {
        synchronized(this) {
            val editor =
                editorCache.get() ?: SafeEditor(rawPrefs.edit(), this).also { editorCache = WeakReference(it) }

            runCatching {
                action.invoke(editor)
            }
            editor.apply()
        }
    }

    /**
     * 値を編集し，変更の反映完了を待機する
     */
    suspend fun editSync(action: SafeEditor<KeyT>.()->Unit) = withContext(Dispatchers.IO) {
        edit(action)
    }

    val all : Map<String, *>
        get() = rawPrefs.all

    val version
        get() = rawPrefs.getInt(INTERNAL_KEY_VERSION, versionDefault)

    fun contains(key: KeyT) = rawPrefs.contains(key.name)

    inline fun <reified T> get(key: KeyT) : T = getNullable<T>(key) ?: throw NullPointerException()

    inline fun <reified T> getNullable(key: KeyT) : T? = when (key.valueType) {
        typeInfo<Boolean>() -> getBoolean(key) as T
        typeInfo<Int>() -> getInt(key) as T
        typeInfo<Long>() -> getLong(key) as T
        typeInfo<Float>() -> getFloat(key) as T
        typeInfo<String>() -> getString(key) as? T
        else -> getObject<T>(key)
    }

    // 見ての通り，get(), getNullable() は以下のメソッドを呼んでいるだけなので，
    // ssp.get<Boolean>(Key.FOO) とかやるよりは ssp.getBoolean(Key.FOO) でいい気がする

    fun getBoolean(key: KeyT) = when(key.valueType) {
        typeInfo<Boolean>() -> synchronized(key) { rawPrefs.getBoolean(key.name, key.defaultValue as Boolean) }
        else -> throw RuntimeException("type mismatch")
    }
    fun getInt(key: KeyT) = when(key.valueType) {
        typeInfo<Int>() -> synchronized(key) { rawPrefs.getInt(key.name, key.defaultValue as Int) }
        else -> throw RuntimeException("type mismatch")
    }
    fun getLong(key: KeyT) = when(key.valueType) {
        typeInfo<Long>() -> synchronized(key) { rawPrefs.getLong(key.name, key.defaultValue as Long) }
        else -> throw RuntimeException("type mismatch")
    }
    fun getFloat(key: KeyT) = when(key.valueType) {
        typeInfo<Float>() -> synchronized(key) { rawPrefs.getFloat(key.name, key.defaultValue as Float) }
        else -> throw RuntimeException("type mismatch")
    }
    fun getString(key: KeyT) : String? = when(key.valueType) {
        typeInfo<String>() -> synchronized(key) { rawPrefs.getString(key.name, key.defaultValue as? String) }
        else -> throw RuntimeException("type mismatch")
    }

    inline fun <reified T> getObject(key: KeyT) = synchronized(key) { rawPrefs.getObject(key.name, key.defaultValue as? T) }


    /**
     * 値を編集する際に使用
     */
    class SafeEditor<KeyT> internal constructor(
        private val mEditor: SharedPreferences.Editor,
        private val safeSharedPreferences: SafeSharedPreferences<KeyT>
    ) where KeyT: Key, KeyT: Enum<KeyT> {

        // 外部から秘匿する方法考え中
        /** 生のSharedPreferences.Editor */
        val rawEditor
            get() = mEditor

        fun put(key: KeyT, value: Any?) = when (key.valueType) {
            typeInfo<Boolean>() -> putBoolean(key, value as Boolean)
            typeInfo<Int>() -> putInt(key, value as Int)
            typeInfo<Long>() -> putLong(key, value as Long)
            typeInfo<Float>() -> putFloat(key, value as Float)
            typeInfo<String>() -> putString(key, value as? String)
            else -> putObject(key, value)
        }

        fun putBoolean(key: KeyT, value: Boolean) {
            if (key.valueType != typeInfo<Boolean>()) throw RuntimeException("type mismatch")
            synchronized(key) {
                rawEditor.putBoolean(key.name, value)
            }
        }
        fun putInt(key: KeyT, value: Int) {
            if (key.valueType != typeInfo<Int>()) throw RuntimeException("type mismatch")
            synchronized(key) {
                rawEditor.putInt(key.name, value)
            }
        }
        fun putLong(key: KeyT, value: Long) {
            if (key.valueType != typeInfo<Long>()) throw RuntimeException("type mismatch")
            synchronized(key) {
                rawEditor.putLong(key.name, value)
            }
        }
        fun putFloat(key: KeyT, value: Float) {
            if (key.valueType != typeInfo<Float>()) throw RuntimeException("type mismatch")
            synchronized(key) {
                rawEditor.putFloat(key.name, value)
            }
        }
        fun putString(key: KeyT, value: String?) {
            if (key.valueType != typeInfo<String>()) throw RuntimeException("type mismatch")
            synchronized(key) {
                rawEditor.putString(key.name, value)
            }
        }
        fun putObject(key: KeyT, value: Any?) {
            synchronized(key) {
                rawEditor.putObject(key.name, value)
            }
        }

        fun remove(key: KeyT) {
            synchronized(key) {
                rawEditor.remove(key.name)
            }
        }

        internal fun apply() {
            synchronized(this) {
                rawEditor.putInt(INTERNAL_KEY_VERSION, safeSharedPreferences.keyVersion)
                rawEditor.apply()
            }
        }

        internal fun commit() {
            synchronized(this) {
                rawEditor.putInt(INTERNAL_KEY_VERSION, safeSharedPreferences.keyVersion)
                rawEditor.commit()
            }
        }
    }

    /** キーを指定するためのenumに必ず実装させる */
    interface Key {
        val valueType: Type
        val defaultValue: Any?

        companion object {
            inline fun <reified KeyT> version() where KeyT: Key, KeyT: Enum<KeyT> = version(KeyT::class.java)

            fun <KeyT> version(keyClass : Class<KeyT>) : Int? where KeyT: Key, KeyT: Enum<KeyT> {
                val keyAnnotation = keyClass.annotations.firstOrNull { it is SharedPreferencesKey } as? SharedPreferencesKey
                return keyAnnotation?.version
            }
        }
    }
}

////////////////////////////////////////////////////////////////////
// オブジェクトをjson化して保存
////////////////////////////////////////////////////////////////////

inline fun <reified T> SharedPreferences.getObject(key: String, defaultValue: T? = null) : T? =
    getObject(key, object: TypeToken<T>() {}.type, defaultValue)

fun <T> SharedPreferences.getObject(key: String, type: Type, defaultValue: T? = null) : T? =
    getString(key, null)?.let { getSharedPreferencesGson().fromJson<T>(it, type) } ?: defaultValue

fun SharedPreferences.Editor.putObject(key: String, src: Any?) : SharedPreferences.Editor {
    val json = getSharedPreferencesGson().toJson(src)
    putString(key, json)
    return this
}

////////////////////////////////////////////////////////////////////

class LocalDateSerializer : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?) =
        JsonPrimitive(formatter.format(src))

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?) : LocalDate? =
        formatter.parse(json!!.asString, LocalDate::from)
}


class LocalDateTimeSerializer : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?) =
        JsonPrimitive(formatter.format(src))

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?) : LocalDateTime? =
        formatter.parse(json!!.asString, LocalDateTime::from)
}

class ZonedDateTimeSerializer : JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    override fun serialize(src: ZonedDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?) =
        JsonPrimitive(formatter.format(src))

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?) : ZonedDateTime? =
        formatter.parse(json!!.asString, ZonedDateTime::from)
}

private fun getSharedPreferencesGson() =
    GsonBuilder()
        .serializeNulls()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(LocalDate::class.java, LocalDateSerializer())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeSerializer())
        .registerTypeAdapter(ZonedDateTime::class.java, ZonedDateTimeSerializer())
        .create()
