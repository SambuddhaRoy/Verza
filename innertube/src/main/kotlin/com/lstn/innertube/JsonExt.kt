package com.lstn.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// ── Lightweight, null-safe JSON navigation helpers ─────────────────────────────
//
// YouTube Music's InnerTube responses are deeply nested and frequently reshuffle
// their wrapper objects between client versions. Rather than model the entire tree,
// we navigate defensively and lean on recursive finders that survive structural drift.

/** The string content of this element, or null if it's absent/JsonNull. */
internal val JsonElement?.string: String?
    get() = (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

/** This element as an object, or null. */
internal val JsonElement?.obj: JsonObject?
    get() = this as? JsonObject

/** This element as an array, or null. */
internal val JsonElement?.array: JsonArray?
    get() = this as? JsonArray

/** Direct child object by [key]. */
internal fun JsonElement?.child(key: String): JsonElement? = (this as? JsonObject)?.get(key)

/**
 * Recursively collects every object that is stored under [key] anywhere in the tree.
 * e.g. findAll("musicResponsiveListItemRenderer") returns every song/result row.
 */
internal fun JsonElement.findAll(key: String): List<JsonObject> {
    val out = ArrayList<JsonObject>()
    fun walk(el: JsonElement) {
        when (el) {
            is JsonObject -> {
                (el[key] as? JsonObject)?.let { out.add(it) }
                el.values.forEach { walk(it) }
            }
            is JsonArray -> el.forEach { walk(it) }
            else -> {}
        }
    }
    walk(this)
    return out
}

/** Recursively finds the first value stored under [key] anywhere in the tree (depth-first). */
internal fun JsonElement.findFirst(key: String): JsonElement? {
    when (this) {
        is JsonObject -> {
            this[key]?.let { return it }
            for (v in values) v.findFirst(key)?.let { return it }
        }
        is JsonArray -> for (v in this) v.findFirst(key)?.let { return it }
        else -> {}
    }
    return null
}

/** Parses a "m:ss" / "h:mm:ss" duration label into milliseconds, or 0 if unparseable. */
internal fun String?.durationLabelToMillis(): Long {
    if (this == null) return 0L
    val parts = trim().split(":").mapNotNull { it.toIntOrNull() }
    if (parts.isEmpty()) return 0L
    val seconds = parts.fold(0) { acc, n -> acc * 60 + n }
    return seconds * 1000L
}
