package com.suihan74.HatenaLib

data class FollowUserResponse (
    val followings: List<FollowUser>
)

data class FollowUser (
    val name: String
)
