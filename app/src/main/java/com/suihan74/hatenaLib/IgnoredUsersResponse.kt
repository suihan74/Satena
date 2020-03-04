package com.suihan74.hatenaLib

import java.io.Serializable

data class IgnoredUsersResponse (
    val users : List<String>,
    val cursor : String?
) : Serializable
