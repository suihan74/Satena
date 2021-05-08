package com.suihan74.hatenaLib

data class FollowingsResponse (
    val followings: List<Following>
) {
    // for Gson
    private constructor() : this(emptyList())
}

data class FollowersResponse (
    val followers: List<Follower>
) {
    // for Gson
    private constructor() : this(emptyList())
}

data class Following (
    val name: String
) {
    // for Gson
    private constructor() : this("")
}

data class Follower (
    val name: String,
    val displayName: String,
    val profileImageUrl: String,
    val totalBookmarks: Int,
    val private: Boolean,
    val followedByVisitor: Boolean,
) {
    // for Gson
    private constructor() : this("", "", "", 0, false, false)
}
