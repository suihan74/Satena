package com.suihan74.satena.models

/**
 * 設定ファイルの外部ファイル入出力データ
 */

data class MigrationData (
    val keyName: String,
    val version: Int,
    val fileName: String,
    val size: Int,
    val data: ByteArray?
) {
    override fun hashCode() =
        (keyName.hashCode() + version + fileName.hashCode() + size) * 31 + (data?.sum() ?: 0)

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

data class MigrationDataFile (
    val version: Int,
    val datas: List<MigrationData>
) {
    override fun hashCode() =
        version * 31 + datas.sumBy { it.hashCode() }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}
