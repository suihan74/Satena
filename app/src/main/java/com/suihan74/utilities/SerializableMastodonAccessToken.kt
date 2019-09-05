package com.suihan74.utilities

import java.io.Serializable

data class SerializableMastodonAccessToken(
    val instanceName: String,
    val accessToken: String
) : Serializable
