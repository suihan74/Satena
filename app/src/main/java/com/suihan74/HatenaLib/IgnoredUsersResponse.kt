package com.suihan74.HatenaLib

import java.io.Serializable

data class IgnoredUsersResponse (
    val users : List<String>,
    val cursor : String?
) : Serializable
