package com.suihan74.satena.models

import com.suihan74.satena.*
import java.io.InputStream
import java.io.OutputStream

/**
 * 設定ファイルの外部ファイル入出力データ
 */
data class MigrationData(
    val type: DataType,
    val keyName: String,
    val version: Int,
    val fileName: String,
    val size: Int,
    val data: ByteArray?
) {
    enum class DataType {
        PREFERENCE,
        DATABASE,
        FILE;

        companion object {
            /**
             * @throws IndexOutOfBoundsException
             */
            fun fromOrdinal(idx: Int) = values()[idx]
        }
    }

    override fun hashCode() =
        (keyName.hashCode() + version + fileName.hashCode() + size) * 31 + (data?.sum() ?: 0)

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    fun toByteArray() =
        ArrayList<Byte>().apply {
            addAll(keyName.toByteArray().toList())
            addAll(version.toByteArray().toList())
            addAll(fileName.toByteArray().toList())
            addAll(size.toByteArray().toList())
            addAll(data?.toList() ?: emptyList())
        }.toByteArray()

    /**
     * @throws java.io.IOException
     */
    fun write(stream: OutputStream) = stream.run {
        writeInt(type.ordinal)
        writeString(keyName)
        writeInt(version)
        writeString(fileName)
        writeInt(size)
        write(data)
    }

    companion object {
        fun read(stream: InputStream) : MigrationData = stream.run {
            val type = DataType.fromOrdinal(readInt())
            val keyName = readString()
            val dataVersion = readInt()
            val fileName = readString()
            val dataSize = readInt()
            val data = readByteArray(dataSize)

            MigrationData(
                type = type,
                keyName = keyName,
                fileName = fileName,
                version = dataVersion,
                size = dataSize,
                data = data
            )
        }
    }
}
