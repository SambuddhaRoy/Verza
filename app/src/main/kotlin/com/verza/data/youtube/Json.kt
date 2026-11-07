package com.verza.data.youtube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// YouTube's responses are deep and reshuffle their wrappers between versions. Rather than model the
// tree, the parsers find what they want by key wherever it sits, which survives most reshuffles.

internal val JsonElement?.string: String?
    get() = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

internal val JsonElement?.array: JsonArray? get() = this as? JsonArray

internal fun JsonElement?.child(key: String): JsonElement? = (this as? JsonObject)?.get(key)

/** Every object stored under [key], anywhere in the tree. */
internal fun JsonElement.findAll(key: String): List<JsonObject> {
    val out = ArrayList<JsonObject>()
    fun walk(el: JsonElement) {
        when (el) {
            is JsonObject -> { (el[key] as? JsonObject)?.let(out::add); el.values.forEach(::walk) }
            is JsonArray -> el.forEach(::walk)
            else -> Unit
        }
    }
    walk(this)
    return out
}

/** The first value stored under [key], depth first. */
internal fun JsonElement.findFirst(key: String): JsonElement? {
    when (this) {
        is JsonObject -> { this[key]?.let { return it }; for (v in values) v.findFirst(key)?.let { return it } }
        is JsonArray -> for (v in this) v.findFirst(key)?.let { return it }
        else -> Unit
    }
    return null
}

/** The text of the first run under [key]: most titles. */
internal fun JsonElement?.firstRun(key: String): String? =
    child(key)?.findFirst("runs")?.array?.firstOrNull()?.child("text").string

/** The texts of the runs under this element, without the bullets between them. */
internal fun JsonElement?.runTexts(): List<String> =
    this?.findFirst("runs")?.array?.mapNotNull { it.child("text").string }?.filter { it.isNotBlank() && it.trim() != "•" }.orEmpty()

/** The largest thumbnail's URL. */
internal fun JsonElement?.thumbnail(): String? =
    this?.findFirst("thumbnails")?.array?.lastOrNull()?.child("url").string

private val DURATION = Regex("""^\d{1,2}(:\d{2}){1,2}$""")

internal fun String.isDuration() = DURATION.matches(trim())

/** "3:45" or "1:02:03" in milliseconds, or 0. */
internal fun String?.durationMs(): Long {
    val parts = this?.trim()?.split(":")?.mapNotNull { it.toIntOrNull() }.orEmpty()
    return parts.fold(0L) { acc, n -> acc * 60 + n } * 1000
}
