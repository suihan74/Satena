package com.suihan74.satena

import android.content.Context
import android.util.Log
import com.suihan74.satena.models.MigrationData
import com.suihan74.utilities.SafeSharedPreferences
import com.suihan74.utilities.SharedPreferencesKey
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.lang.RuntimeException
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
            dest.setReadable(true)
            dest.outputStream().buffered().use { stream ->
                stream.write(SIGNATURE)
                stream.write(VERSION)
                stream.writeInt(items.size)

                items.forEach { data ->
                    stream.writeString(data.keyName)
                    stream.writeString(data.fileName)
                    stream.writeInt(data.version)
                    stream.writeInt(data.size)
                    stream.write(data.data!!)
                }
            }

            Log.d("migration", "completed saving")
        }
    }

    class Input(private val context: Context) {
        fun read(src: File, onErrorAction: ((MigrationData)->Unit)? = null) {
            src.inputStream().buffered().use { stream ->
                val signature = ByteArray(8)
                stream.read(signature)
                if (!signature.contentEquals(SIGNATURE))
                    throw RuntimeException("the file is not a settings for Satena")

                val version = ByteArray(1)
                stream.read(version)

                val itemsCount = stream.readInt()

                for (i in 0 until itemsCount) {
                    val keyName = stream.readString()
                    val fileName = stream.readString()
                    val dataVersion = stream.readInt()
                    val dataSize = stream.readInt()
                    val data = ByteArray(dataSize)
                    stream.read(data, 0, dataSize)

                    val md = MigrationData(
                        keyName = keyName,
                        fileName = fileName,
                        version = dataVersion,
                        size = dataSize,
                        data = data
                    )

                    val result = apply(md)
                    if (!result) {
                        onErrorAction?.invoke(md)
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
    var result: Int = 0
    val size = minOf(3, this.size)
    for (i in 0..size) {
        val value = (this[i] and 0xFF.toByte()).toInt().let {
            if (it < 0) 256 + it else it
        }
        result = result or (value shl (i * 8))
    }
    return result
}

fun BufferedOutputStream.writeInt(value: Int) {
    this.write(value.toByteArray())
}

fun BufferedOutputStream.writeString(value: String) {
    value.toByteArray().let {
        writeInt(it.size)
        write(it)
    }
}

fun BufferedInputStream.readInt(): Int {
    val bytes = ByteArray(4)
    this.read(bytes)
    return bytes.toInt()
}

fun BufferedInputStream.readString(): String {
    val size = readInt()
    val bytes = ByteArray(size)
    this.read(bytes)
    return bytes.toString(Charsets.UTF_8)
}
