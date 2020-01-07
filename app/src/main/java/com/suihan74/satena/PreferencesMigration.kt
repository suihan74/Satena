package com.suihan74.satena

import android.content.Context
import android.util.Log
import androidx.room.Database
import com.suihan74.satena.models.MigrationData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.getMd5Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

class PreferencesMigration {
    companion object {
        private val SIGNATURE get() = byteArrayOf(0).plus("SATESET".toByteArray())
        private val VERSION get() = byteArrayOf(1)

        private val SIGNATURE_SIZE = SIGNATURE.size
        private const val HASH_SIZE = 16
    }

    class Output(private val context: Context) {
        private val items = ArrayList<MigrationData>()

        suspend inline fun <reified KeyT> addPreference()
                where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> =
            addPreference(KeyT::class.java)

        suspend fun <KeyT> addPreference(keyClass: Class<KeyT>)
                where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> = withContext(Dispatchers.IO) {

            val keyAnnotation = keyClass.annotations.firstOrNull { it is SharedPreferencesKey } as? SharedPreferencesKey
            val keyName = keyClass.name
            val fileName = "preferences_${keyAnnotation?.fileName ?: "default"}"
            val keyVersion = keyAnnotation?.version ?: 0

            val path = context.filesDir.absolutePath.let {
                "${it.substring(0, it.indexOf("/files"))}/shared_prefs/$fileName.xml"
            }

            if (!File(path).exists()) {
                val prefs = SafeSharedPreferences.create(context, keyClass)
                prefs.editSync {
                    /* initialize */
                }
            }

            val bytes = File(path).inputStream().buffered().use {
                it.readBytes()
            }
            val dataSize = bytes.size

            items.add(MigrationData(MigrationData.DataType.PREFERENCE, keyName, keyVersion, fileName, dataSize, bytes))
        }

        suspend inline fun <reified DB_T> addDatabase(fileName: String) =
            addDatabase(DB_T::class.java, fileName)

        suspend fun <DB_T> addDatabase(DBClass: Class<DB_T>, fileName: String) = withContext(Dispatchers.IO) {
            val file = context.getDatabasePath(fileName)
            check(file.exists()) { "the database file does not exist: $fileName" }

            val bytes = file.inputStream().buffered().use {
                it.readBytes()
            }

            val annotations = DBClass.annotations.firstOrNull { it is Database } as? Database
            val version = annotations?.version ?: 0

            items.add(MigrationData(MigrationData.DataType.DATABASE, "database__$fileName", version, fileName, bytes.size, bytes))
        }

        suspend fun write(dest: File) = withContext(Dispatchers.IO) {
            val headerHash = getMd5Bytes(VERSION.plus(items.size.toByteArray()))
            val bodyHash = getMd5Bytes(items.flatMap { data ->
                getMd5Bytes(data.toByteArray()).toList()
            }.toByteArray())

            dest.setReadable(true)
            dest.outputStream().buffered().use { stream ->
                stream.write(SIGNATURE)
                stream.write(headerHash)
                stream.write(bodyHash)
                stream.write(VERSION)
                stream.writeInt(items.size)

                items.forEach { data ->
                    data.write(stream)
                }
            }

            Log.d("migration", "completed saving")
        }
    }

    class Input(private val context: Context) {
        suspend fun read(src: File, onErrorAction: ((MigrationData) -> Unit)? = null) =
            withContext(Dispatchers.IO) {
                SatenaApplication.instance.appDatabase.run {
                    close()
                }

                src.inputStream().buffered().use { stream ->
                    val signature = stream.readByteArray(SIGNATURE_SIZE)
                    check(signature.contentEquals(SIGNATURE)) { "the file is not a settings for Satena: ${src.absolutePath}" }

                    val headerHash = stream.readByteArray(HASH_SIZE)
                    val bodyHash = stream.readByteArray(HASH_SIZE)

                    val version = stream.readByteArray(1)
                    check(version.contentEquals(VERSION)) { "cannot read an old settings file: ${src.absolutePath}" }

                    val itemsCount = stream.readInt()

                    val actualHeaderHash =
                        getMd5Bytes(version.plus(itemsCount.toByteArray()))
                    check(actualHeaderHash.contentEquals(headerHash)) { "the file is falsified: ${src.absolutePath}" }

                    val items = ArrayList<MigrationData>(itemsCount)
                    for (i in 0 until itemsCount) {
                        items.add(MigrationData.read(stream))
                    }

                    val actualBodyHash =
                        getMd5Bytes(items.flatMap { getMd5Bytes(it.toByteArray()).toList() }.toByteArray())
                    check(actualBodyHash.contentEquals(bodyHash)) { "the file is falsified: ${src.absolutePath}" }

                    for (item in items) {
                        val result = apply(item)
                        if (!result) {
                            onErrorAction?.invoke(item)
                        }
                    }

                    // バージョン移行
                    SatenaApplication.instance.updatePreferencesVersion()
                    SatenaApplication.instance.initializeDataBase()
                }

                Log.d("migration", "completed loading")
            }

        private suspend fun apply(data: MigrationData): Boolean = when (data.type) {
            MigrationData.DataType.PREFERENCE ->
                applyPreferences(data)

            MigrationData.DataType.DATABASE ->
                applyDatabase(data)
        }

        private suspend fun applyPreferences(data: MigrationData): Boolean = withContext(Dispatchers.IO) {
            var result = true

            val path = context.filesDir.absolutePath.let {
                "${it.substring(0, it.indexOf("/files"))}/shared_prefs/${data.fileName}.xml"
            }
            val backupPath = "${context.filesDir.absolutePath}/${data.fileName}.xml.old"

            val file = File(path)
            val backup = File(backupPath)

            val backupSuccess =
                if (file.exists()) {
                    try {
                        file.copyTo(backup, true)
                        true
                    }
                    catch (e: Exception) {
                        Log.e("migration", "failed to backup the already existed file: $path")
                        false
                    }
                }
                else false

            try {
                // shared_prefsディレクトリが存在しないとファイルが作成できないので予め確認して作成する
                val dir = File(file.parent)
                if (!dir.exists()) {
                    dir.mkdir()
                }

                SafeSharedPreferences.delete(context, data.fileName, data.keyName)

                file.outputStream().buffered().use {
                    it.write(data.data)
                }
            }
            catch (e: Exception) {
                Log.e("migration", "failed to load: ${data.fileName}")
                result = false

                if (backupSuccess) {
                    backup.copyTo(file, true)
                }
            }
            finally {
                backup.delete()
            }

            return@withContext result
        }

        private suspend fun applyDatabase(data: MigrationData): Boolean = withContext(Dispatchers.IO) {
            val file = context.getDatabasePath(data.fileName)
            val backup = context.getDatabasePath(data.keyName + ".bak")

            val backupSuccess =
                if (file.exists()) {
                    try {
                        file.copyTo(backup, true)
                        true
                    }
                    catch (e: Exception) {
                        Log.e("migration", "failed to backup the already existed database file: ${data.fileName}")
                        false
                    }
                }
                else false

            var result = false
            try {
                // 初回起動時にはdatabasesディレクトリが存在しないので作成する必要がある
                val dir = File(file.parent)
                if (!dir.exists()) {
                    dir.mkdir()
                }

                file.outputStream().buffered().use {
                    it.write(data.data)
                }
                result = true
            }
            catch (e: Exception) {
                Log.e("migration", "failed to load a database: ${data.fileName}")
                if (backupSuccess) {
                    backup.copyTo(file, true)
                }
            }
            finally {
                backup.delete()
            }
            return@withContext result
        }
    }
}

fun Int.toByteArray(): ByteArray {
    var value = this
    val bytes = Int.SIZE_BYTES // 必ず4byteの配列を出力する
    val result = ByteArray(bytes)
    for (i in 0 until bytes) {
        result[i] = (value and 0xFF).toByte()
        value = value shr Byte.SIZE_BITS
    }
    return result
}

fun ByteArray.toInt(): Int {
    var result = 0
    val bytes = Int.SIZE_BYTES
    if (bytes < this.size)
        throw ArrayIndexOutOfBoundsException("ByteArray overflows when treated as Int")

    for (i in 0 until bytes) {
        val value = (this[i] and 0xFF.toByte()).toInt().let {
            if (it < 0) 256 + it else it
        }
        result = result or (value shl (i * Byte.SIZE_BITS))
    }
    return result
}

fun OutputStream.writeInt(value: Int) {
    this.write(value.toByteArray())
}

fun OutputStream.writeString(value: String) {
    value.toByteArray().let {
        writeInt(it.size)
        write(it)
    }
}

fun InputStream.readInt() : Int {
    val bytes = ByteArray(Int.SIZE_BYTES)
    this.read(bytes)
    return bytes.toInt()
}

fun InputStream.readString() : String {
    val size = readInt()
    val bytes = ByteArray(size)
    this.read(bytes)
    return bytes.toString(Charsets.UTF_8)
}

fun InputStream.readByteArray(size: Int) : ByteArray {
    val bytes = ByteArray(size)
    this.read(bytes, 0, size)
    return bytes
}
