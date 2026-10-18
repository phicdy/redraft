package com.phicdy.redraft.shared.llm.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Tiny builders for the subset of JSON Schema that strict structured outputs accept.
 * Every object lists all of its properties as required and forbids additional ones.
 */

internal fun stringSchema(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
}

/** A string restricted to the serial names of an enum, read from its [descriptor] so it cannot drift. */
@OptIn(ExperimentalSerializationApi::class)
internal fun enumSchema(descriptor: SerialDescriptor, description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("enum", JsonArray(List(descriptor.elementsCount) { JsonPrimitive(descriptor.getElementName(it)) }))
    put("description", description)
}

internal fun arraySchema(items: JsonObject, description: String): JsonObject = buildJsonObject {
    put("type", "array")
    put("items", items)
    put("description", description)
}

internal fun objectSchema(vararg properties: Pair<String, JsonObject>): JsonObject = buildJsonObject {
    put("type", "object")
    put("properties", JsonObject(properties.toMap()))
    put("required", JsonArray(properties.map { JsonPrimitive(it.first) }))
    put("additionalProperties", false)
}
