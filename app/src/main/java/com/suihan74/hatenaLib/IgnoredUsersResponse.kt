package com.suihan74.hatenaLib

data class IgnoredUsersResponse (
    val users : List<String>,
    val cursor : String?
) {
    // for Gson
    private constructor() : this(emptyList(), null)
}
