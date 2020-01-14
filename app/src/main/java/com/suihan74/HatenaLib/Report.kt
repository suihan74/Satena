package com.suihan74.HatenaLib

data class Report (
    val entry: Entry,
    val bookmark: Bookmark,
    val category: ReportCategory,
    val comment: String? = null
)
