package com.suihan74.hatenaLib

data class Profile (
    val id : String,
    val name : String,
    val iconUrl : String,
    val description: String,
    val profiles : List<Pair<String, String>>,
    val addresses: List<Pair<String, Address>>,
    val services : List<Service>,

    val bookmark: Bookmark
) {
    data class Address (
        val text: String,
        val url: String
    )

    data class Service (
        val name : String,
        val url : String,
        val imageUrl : String
    )

    data class Bookmark (
        val count: Int,
        val followingCount: Int,
        val followerCount: Int,
        val tags: List<Tag>,
        val entries: List<Entry>
    ) {
        data class Tag (
            val name: String,
            val count: Int
        )

        companion object {
            fun createEmpty() = Bookmark(
                count = 0,
                followingCount = 0,
                followerCount = 0,
                tags = emptyList(),
                entries = emptyList()
            )
        }
    }
}

