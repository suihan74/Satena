package com.suihan74.satena

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Database
import com.suihan74.satena.models.MigrationData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.getMd5Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.experimental.and

class PreferencesMigration {
    companion object {
        @Suppress("SpellCheckingInspection")
        private val SIGNATURE get() = byteArrayOf(0).plus("SATESET".toByteArray())
        private val VERSION get() = byteArrayOf(2)

        private val SIGNATURE_SIZE = SIGNATURE.size
        private const val HASH_SIZE = 16
    }

    // ------ //

    class MigrationFailureException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

    // ------ //

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

        /**
         * @throws IllegalStateException
         */
        suspend inline fun <reified DB_T> addDatabase(fileName: String) =
            addDatabase(DB_T::class.java, fileName)

        /**
         * @throws IllegalStateException
         */
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

        /**
         * {app}/files/以下のディレクトリを追加
         *
         * @throws IllegalStateException
         */
        suspend fun addFiles(dir: File) = withContext(Dispatchers.IO) {
            if (!dir.absolutePath.startsWith(context.filesDir.absolutePath)) {
                throw IllegalStateException("failure adding illegal files")
            }
            val dirname = dir.absolutePath.substring(context.filesDir.absolutePath.length)
            dir.listFiles { file, filename ->
                val bytes = File(file, filename).inputStream().buffered().use {
                    it.readBytes()
                }
                val path = "$dirname/$filename"
                items.add(MigrationData(MigrationData.DataType.FILE, "file__$path", 1, path, bytes.size, bytes))
            }
        }

        /**
         * @throws IOException
         */
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun write(targetUri: Uri) = withContext(Dispatchers.IO) {
            val headerHash = getMd5Bytes(VERSION.plus(items.size.toByteArray()))
            val bodyHash = getMd5Bytes(items.flatMap { data ->
                getMd5Bytes(data.toByteArray()).toList()
            }.toByteArray())

            val contentResolver = context.contentResolver
            contentResolver.openFileDescriptor(targetUri, "w")?.use {
                FileOutputStream(it.fileDescriptor).buffered().use { stream ->
                    stream.write(SIGNATURE)
                    stream.write(headerHash)
                    stream.write(bodyHash)
                    stream.write(VERSION)
                    stream.writeInt(items.size)

                    items.forEach { data ->
                        data.write(stream)
                    }
                }
            }

            Log.d("migration", "completed saving")
        }
    }

    // ------ //

    class Input(private val context: Context) {
        /**
         * @throws MigrationFailureException
         */
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun read(targetUri: Uri, onErrorAction: ((MigrationData) -> Unit)? = null) = withContext(Dispatchers.IO) {
            SatenaApplication.instance.appDatabase.run {
                close()
            }

            val path = targetUri.path!!
            val contentResolver = context.contentResolver
            contentResolver.openFileDescriptor(targetUri, "r")?.use {
                FileInputStream(it.fileDescriptor).buffered().use { stream ->
                    try {
                        val signature = stream.readByteArray(SIGNATURE_SIZE)
                        check(signature.contentEquals(SIGNATURE)) { "the file is not a settings for Satena: $path" }

                        val headerHash = stream.readByteArray(HASH_SIZE)
                        val bodyHash = stream.readByteArray(HASH_SIZE)

                        val version = stream.readByteArray(1)
                        check(version.contentEquals(VERSION)) { "cannot read an old settings file: $path" }

                        val itemsCount = stream.readInt()

                        val actualHeaderHash = getMd5Bytes(
                            version.plus(itemsCount.toByteArray())
                        )
                        check(actualHeaderHash.contentEquals(headerHash)) { "the file is falsified: $path" }

                        val items = ArrayList<MigrationData>(itemsCount)
                        for (i in 0 until itemsCount) {
                            items.add(MigrationData.read(stream))
                        }

                        val actualBodyHash = getMd5Bytes(
                            items.flatMap { getMd5Bytes(it.toByteArray()).toList() }
                                .toByteArray()
                        )
                        check(actualBodyHash.contentEquals(bodyHash)) { "the file is falsified: $path" }

                        for (item in items) {
                            runCatching {
                                apply(item)
                            }
                            .onFailure { e ->
                                Log.e("migration", Log.getStackTraceString(e))
                                onErrorAction?.invoke(item)
                            }
                        }

                        // バージョン移行
                        SatenaApplication.instance.updatePreferencesVersion()
                    }
                    catch (e: Throwable) {
                        throw MigrationFailureException(message = e.message, cause = e)
                    }
                    finally {
                        SatenaApplication.instance.initializeDataBase()
                    }
                }
            }

            Log.d("migration", "completed loading")
        }

        /**
         * @throws MigrationFailureException
         */
        private suspend fun apply(data: MigrationData) {
            when (data.type) {
                MigrationData.DataType.PREFERENCE -> applyPreferences(data)
                MigrationData.DataType.DATABASE -> applyDatabase(data)
                MigrationData.DataType.FILE -> applyFile(data)
            }
        }

        /**
         * @throws MigrationFailureException
         */
        @Suppress("BlockingMethodInNonBlockingContext")
        private suspend fun applyPreferences(data: MigrationData) = withContext(Dispatchers.IO) {
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
                    catch (e: Throwable) {
                        Log.e("migration", "failed to backup the already existed file: $path")
                        false
                    }
                }
                else false

            try {
                // shared_prefsディレクトリが存在しないとファイルが作成できないので予め確認して作成する
                val dir = File(file.parent!!)
                if (!dir.exists()) {
                    dir.mkdir()
                }

                SafeSharedPreferences.delete(context, data.fileName, data.keyName)

                file.outputStream().buffered().use {
                    it.write(data.data)
                }
            }
            catch (e: Throwable) {
                if (backupSuccess) {
                    backup.copyTo(file, true)
                }

                throw MigrationFailureException("failed to read preferences", cause = e)
            }
            finally {
                backup.delete()
            }
        }

        /**
         * @throws MigrationFailureException
         */
        @Suppress("BlockingMethodInNonBlockingContext")
        private suspend fun applyDatabase(data: MigrationData) = withContext(Dispatchers.IO) {
            val file = context.getDatabasePath(data.fileName)
            val backup = context.getDatabasePath(data.keyName + ".bak")

            val backupSuccess =
                if (file.exists()) {
                    try {
                        file.copyTo(backup, true)
                        true
                    }
                    catch (e: Throwable) {
                        Log.e("migration", "failed to backup the already existed database file: ${data.fileName}")
                        false
                    }
                }
                else false

            return@withContext try {
                // 初回起動時にはdatabasesディレクトリが存在しないので作成する必要がある
                val dir = File(file.parent!!)
                if (!dir.exists()) {
                    dir.mkdir()
                }

                file.outputStream().buffered().use {
                    it.write(data.data)
                }
            }
            catch (e: Throwable) {
                if (backupSuccess) {
                    backup.copyTo(file, true)
                }

                throw MigrationFailureException("failed to read databases", cause = e)
            }
            finally {
                backup.delete()
            }
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        private suspend fun applyFile(data: MigrationData) = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(context.filesDir.absolutePath + "/" + data.fileName)
                if (file.parentFile?.exists() != true) {
                    file.parentFile?.mkdirs()
                }
                file.outputStream().buffered().use {
                    it.write(data.data)
                }
            }.onFailure {
                throw MigrationFailureException("failed to read files", cause = it)
            }
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

/**
 * @throws ArrayIndexOutOfBoundsException
 */
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

/**
 * @throws IOException
 */
fun OutputStream.writeInt(value: Int) {
    this.write(value.toByteArray())
}

/**
 * @throws IOException
 */
fun OutputStream.writeString(value: String) {
    value.toByteArray().let {
        writeInt(it.size)
        write(it)
    }
}

/**
 * @exception  IOException
 * @exception  IndexOutOfBoundsException
 */
fun InputStream.readInt() : Int {
    val bytes = ByteArray(Int.SIZE_BYTES)
    this.read(bytes)
    return bytes.toInt()
}

/**
 * @exception  IOException
 * @exception  IndexOutOfBoundsException
 */
fun InputStream.readString() : String {
    val size = readInt()
    val bytes = ByteArray(size)
    this.read(bytes)
    return bytes.toString(Charsets.UTF_8)
}

/**
 * @exception  IOException
 * @exception  IndexOutOfBoundsException
 */
fun InputStream.readByteArray(size: Int) : ByteArray {
    val bytes = ByteArray(size)
    this.read(bytes, 0, size)
    return bytes
}
