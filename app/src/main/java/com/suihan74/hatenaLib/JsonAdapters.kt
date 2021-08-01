package com.suihan74.hatenaLib

import com.google.gson.*
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import java.lang.reflect.Type

internal val Boolean.int
    get() = if (this) 1 else 0

internal class BooleanDeserializer : JsonSerializer<Boolean>, JsonDeserializer<Boolean> {
    override fun serialize(src: Boolean?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(if (src == true) "true" else "false")
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?) =
        try {
            json?.asString?.let {
                it == "1" || it == "true"
            } ?: false
        }
        catch (e: Throwable) {
            false
        }
}

internal class StarDeserializer : JsonDeserializer<Star> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Star {
        val obj = json!!.asJsonObject
        val user = obj.get("name").asString
        val quote = obj.get("quote").asString

        val color = if (obj.has("color")) context!!.deserialize(obj.get("color"), StarColor::class.java) else StarColor.Yellow
        val count = if (obj.has("count")) context!!.deserialize(obj.get("count"), Int::class.java) else 1

        return Star(
            user = user,
            quote = quote,
            count = count,
            color = color)
    }
}

internal class StarColorDeserializer : JsonSerializer<StarColor>, JsonDeserializer<StarColor> {
    override fun serialize(
        src: StarColor?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.name?.lowercase() ?: "yellow")
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): StarColor {
        return if (json == null || json.isJsonNull) {
            StarColor.Yellow
        }
        else {
            when (json.asString) {
                "normal", "yellow" -> StarColor.Yellow
                "red" -> StarColor.Red
                "green" -> StarColor.Green
                "blue" -> StarColor.Blue
                "purple" -> StarColor.Purple
                else -> StarColor.Yellow
            }
        }
    }
}

internal class TimestampDeserializer(
    private val format : String? = null
) : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern(
        format ?: "uuuu-MM-dd'T'HH:mm:ssXXX"
    )

    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        if (src == null) return JsonNull.INSTANCE
        val offset = src.atOffset(ZoneOffset.ofHours(9))
        return JsonPrimitive(offset.format(dateTimeFormatter))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime =
        LocalDateTime.parse(json!!.asString, dateTimeFormatter)
}

internal open class EpochTimeDeserializer(
    private val zoneId: ZoneId = ZoneId.of("Asia/Tokyo")
) : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    override fun serialize(
        src: LocalDateTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val instant = Instant.now()
        val zoneOffset = zoneId.rules.getOffset(instant)
        return JsonPrimitive(src?.toEpochSecond(zoneOffset) ?: 0)
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDateTime {
        val epoch = json!!.asLong
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), zoneId)
    }
}
