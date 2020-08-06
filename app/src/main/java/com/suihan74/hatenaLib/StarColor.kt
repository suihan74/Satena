package com.suihan74.hatenaLib

import com.google.gson.annotations.JsonAdapter

@JsonAdapter(StarColorDeserializer::class, nullSafe = false)
enum class StarColor {
    Yellow,
    Red,
    Green,
    Blue,
    Purple,
}
