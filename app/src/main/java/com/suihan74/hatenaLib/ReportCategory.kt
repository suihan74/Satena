package com.suihan74.hatenaLib

enum class ReportCategory(
    val type: String,
    val description: String
) {
    SPAM(
        "report.type.spam",
        "スパム行為"
    ),

    DEFAMATION(
        "report.type.fud",
        "ご自身に対する誹謗中傷・嫌がらせ"
    ),

    CRIME_NOTICE(
        "report.type.crime",
        "犯罪予告"
    ),

    INSULT(
        "report.type.discrimination",
        "差別、侮辱を目的とした利用"
    ),

    OTHERS(
        "report.type.misc",
        "その他"
    );

    // for Gson
    private constructor() : this("", "")

    companion object {
        fun fromOrdinal(int: Int) = values().getOrElse(int) { SPAM }
    }
}
