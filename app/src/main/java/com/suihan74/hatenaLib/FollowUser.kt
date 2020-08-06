package com.suihan74.hatenaLib

data class FollowUserResponse (
    val followings: List<FollowUser>
) {
    // for Gson
    private constructor() : this(emptyList())
}

data class FollowUser (
    val name: String
) {
    // for Gson
    private constructor() : this("")
}
