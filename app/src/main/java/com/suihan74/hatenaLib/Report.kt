package com.suihan74.hatenaLib

data class Report (
    val entry: Entry,
    val bookmark: Bookmark,
    val category: ReportCategory,
    val comment: String? = null
) {
    // for Gson
    private constructor() : this(Entry(), Bookmark(), ReportCategory.SPAM)
}
