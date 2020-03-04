package com.suihan74.hatenaLib

data class FollowUserResponse (
    val followings: List<FollowUser>
)

data class FollowUser (
    val name: String
)
