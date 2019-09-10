package com.suihan74.satena

import android.content.Context
import android.util.Log
import com.suihan74.satena.models.MigrationData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import com.suihan74.utilities.getMd5Bytes
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.experimental.and

class PreferencesMigrator {
    companion object {
        private val SIGNATURE get() = byteArrayOf(0).plus("SATESET".toByteArray())
        private val VERSION get() = byteArrayOf(0)
    }

    class Output(private val context: Context) {
        val items = ArrayList<MigrationData>()

        inline fun <reified KeyT> addPreference() where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> =
            addPreference(KeyT::class.java)

        fun <KeyT> addPreference(keyClass: Class<KeyT>) where KeyT: SafeSharedPreferences.Key, KeyT: Enum<KeyT> {
            val keyAnnotation = keyClass.annotations.firstOrNull { it is SharedPreferencesKey } as? SharedPreferencesKey
            val keyName = keyClass.name
            val fileName = "preferences_${keyAnnotation?.fileName ?: "default"}"
            val keyVersion = keyAnnotation?.version ?: 0

            val path = context.filesDir.absolutePath.let {
                "${it.substring(0, it.indexOf("/files"))}/shared_prefs/$fileName.xml"
            }

            val bytes = File(path).inputStream().buffered().use {
                it.readBytes()
            }
            val dataSize = bytes.size

            items.add(MigrationData(keyName, keyVersion, fileName, dataSize, bytes))
        }

        fun write(dest: File) {
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
        fun read(src: File, onErrorAction: ((MigrationData)->Unit)? = null) {
            src.inputStream().buffered().use { stream ->
                val signature = stream.readByteArray(8)
                check(signature.contentEquals(SIGNATURE)) { "the file is not a settings for Satena: ${src.absolutePath}" }

                val headerHash = stream.readByteArray(16)
                val bodyHash = stream.readByteArray(16)

                val version = stream.readByteArray(1)
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

                Log.d("migration", "completed loading")
            }
        }

        fun apply(data: MigrationData) : Boolean {
            var result = true

            val path = context.filesDir.absolutePath.let {
                "${it.substring(0, it.indexOf("/files"))}/shared_prefs/${data.fileName}.xml"
            }
            val backupPath = "${context.filesDir.absolutePath}/${data.fileName}.xml.old"

            val file = File(path)
            val backup = File(backupPath)

            file.copyTo(backup, true)
            try {
                SafeSharedPreferences.delete(context, data.fileName, data.keyName)

                file.outputStream().buffered().use {
                    it.write(data.data)
                }
            }
            catch (e: Exception) {
                Log.e("migration", "failed to load: ${data.fileName}")
                result = false
                backup.copyTo(file, true)
            }
            finally {
                backup.delete()
            }

            return result
        }
    }
}

fun Int.toByteArray(): ByteArray {
    var value = this
    val result = ByteArray(4)
    for (i in 0..3) {
        result[i] = (value and 0xFF).toByte()
        value = value shr 8
    }
    return result
}

fun ByteArray.toInt(): Int {
    var result = 0
    val size = minOf(3, this.size)
    for (i in 0..size) {
        val value = (this[i] and 0xFF.toByte()).toInt().let {
            if (it < 0) 256 + it else it
        }
        result = result or (value shl (i * 8))
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
    val bytes = ByteArray(4)
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
