package com.suihan74.hatenaLib

enum class ReportCategory(
    val type: String,
    val description: String
) {
    SPAM(
        "spam",
        "スパム行為"
    ),

    CRIME_NOTICE(
        "crime_notice",
        "犯罪予告"
    ),

    INSULT(
        "insult",
        "差別、侮辱、嫌がらせ"
    ),

    OTHERS(
        "others",
        "その他"
    );

    // for Gson
    private constructor() : this("", "")

    companion object {
        fun fromOrdinal(int: Int) = values().getOrElse(int) { SPAM }
    }
}
