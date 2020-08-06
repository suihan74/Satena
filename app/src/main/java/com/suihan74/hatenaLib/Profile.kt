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
    // for Gson
    private constructor() : this("", "", "", "", emptyList(), emptyList(), emptyList(), Bookmark())

    data class Address (
        val text: String,
        val url: String
    ) {
        // for Gson
        private constructor() : this("", "")
    }

    data class Service (
        val name : String,
        val url : String,
        val imageUrl : String
    ) {
        // for Gson
        private constructor() : this("", "", "")
    }

    data class Bookmark (
        val count: Int,
        val followingCount: Int,
        val followerCount: Int,
        val tags: List<Tag>,
        val entries: List<Entry>
    ) {
        // for Gson
        internal constructor() : this(0, 0, 0, emptyList(), emptyList())

        data class Tag (
            val name: String,
            val count: Int
        ) {
            // for Gson
            private constructor() : this("", 0)
        }
    }
}

