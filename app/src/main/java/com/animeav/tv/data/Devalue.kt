package com.animeav.tv.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Decodificador del payload "devalue" que SvelteKit incrusta en los ficheros
 * __data.json. El formato es un array plano donde los enteros son referencias
 * a posiciones del propio array.
 */
object Devalue {

    fun decode(root: JsonObject): JsonElement {
        val nodes = root["nodes"] as? JsonArray ?: return JsonNull
        val dataNode = nodes.lastOrNull { node ->
            node is JsonObject &&
                (node["type"] as? JsonPrimitive)?.contentOrNull == "data" &&
                node.containsKey("data")
        } as? JsonObject ?: return JsonNull
        val data = dataNode["data"] as? JsonArray ?: return JsonNull
        return resolve(data, 0, 0)
    }

    private fun resolve(data: JsonArray, index: Int, depth: Int): JsonElement {
        if (depth > 80 || index < 0 || index >= data.size) return JsonNull
        return when (val raw = data[index]) {
            is JsonObject -> {
                val type = (raw["type"] as? JsonPrimitive)?.contentOrNull
                if (type != null && raw.size == 2 && raw.containsKey("value")) {
                    when (type) {
                        "Date" -> JsonPrimitive((raw["value"] as? JsonPrimitive)?.contentOrNull ?: "")
                        "BigInt" -> JsonPrimitive((raw["value"] as? JsonPrimitive)?.contentOrNull ?: "0")
                        "undefined" -> JsonNull
                        "Set", "Map" -> raw["value"] ?: JsonNull
                        else -> raw
                    }
                } else {
                    buildJsonObject {
                        raw.forEach { (key, value) ->
                            put(key, resolveValue(data, value, depth + 1))
                        }
                    }
                }
            }
            is JsonArray -> buildJsonArray {
                raw.forEach { value -> add(resolveValue(data, value, depth + 1)) }
            }
            else -> raw
        }
    }

    private fun resolveValue(data: JsonArray, value: JsonElement, depth: Int): JsonElement {
        val index = (value as? JsonPrimitive)?.intOrNull
        return if (index != null) resolve(data, index, depth) else value
    }
}
