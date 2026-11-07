package com.animeav.tv.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevalueTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `resuelve referencias anidadas`() {
        val payload = """
            {"type":"data","nodes":[null,{"type":"data","data":[
              {"media":1,"episode":3},
              {"title":2,"score":4},
              "One Piece",
              {"id":5,"number":6},
              8.73,
              3433,
              1
            ]}]}
        """.trimIndent()

        val decoded = Devalue.decode(json.parseToJsonElement(payload).jsonObject).jsonObject
        val media = decoded["media"]!!.jsonObject
        val episode = decoded["episode"]!!.jsonObject

        assertEquals("One Piece", media["title"]!!.jsonPrimitive.content)
        assertEquals(8.73, media["score"]!!.jsonPrimitive.content.toDouble(), 0.001)
        assertEquals(3433, episode["id"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, episode["number"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `resuelve listas de referencias`() {
        val payload = """
            {"type":"data","nodes":[null,{"type":"data","data":[
              {"items":1},
              [2,3,4],
              {"id":5},
              {"id":6},
              {"id":7},
              10,11,12
            ]}]}
        """.trimIndent()

        val decoded = Devalue.decode(json.parseToJsonElement(payload).jsonObject).jsonObject
        val items = decoded["items"]!!.let { it as kotlinx.serialization.json.JsonArray }

        assertEquals(3, items.size)
        assertEquals(10, items[0].jsonObject["id"]!!.jsonPrimitive.content.toInt())
        assertEquals(12, items[2].jsonObject["id"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `devuelve null para payload vacio`() {
        val payload = """{"type":"data","nodes":[null]}"""
        val decoded = Devalue.decode(json.parseToJsonElement(payload).jsonObject)
        assertTrue(decoded.toString() == "null")
    }
}
