package com.phicdy.redraft.shared.llm.schema

import com.phicdy.redraft.shared.llm.OutputSchema
import com.phicdy.redraft.shared.model.ErrorCategory
import com.phicdy.redraft.shared.model.ProofreadResult
import com.phicdy.redraft.shared.model.TranslationResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Walks each schema alongside the model's serializer descriptor, so a field added,
 * renamed or retyped on one side without the other fails here.
 */
@OptIn(ExperimentalSerializationApi::class)
class OutputSchemasTest {
    @Test
    fun translationSchemaMatchesModel() {
        assertMatches(TranslationResult.serializer().descriptor, OutputSchemas.translation.schema, "$")
    }

    @Test
    fun proofreadSchemaMatchesModel() {
        assertMatches(ProofreadResult.serializer().descriptor, OutputSchemas.proofread.schema, "$")
    }

    @Test
    fun categoryEnumListsEverySerialName() {
        val expected = listOf(
            "article", "number", "tense", "agreement", "preposition", "verb_form", "word_order",
            "sentence_structure", "pronoun", "word_choice", "collocation", "literal_translation",
            "register", "conciseness", "clarity", "naturalness", "spelling_punctuation",
        )
        val issues = OutputSchemas.proofread.schema.property("issues").getValue("items").jsonObject
        assertEquals(expected, issues.property("category").enumValues())
        assertEquals(ErrorCategory.entries.size, expected.size)
    }

    @Test
    fun schemaNamesAreValidForOpenAi() {
        val allowed = Regex("^[a-zA-Z0-9_-]{1,64}$")
        listOf(OutputSchemas.translation, OutputSchemas.proofread).map(OutputSchema::name).forEach { name ->
            assertTrue(allowed.matches(name), name)
        }
    }

    private fun assertMatches(descriptor: SerialDescriptor, schema: JsonObject, path: String) {
        when (val kind = descriptor.kind) {
            StructureKind.CLASS -> {
                assertEquals("object", schema.type(), path)
                val properties = schema.getValue("properties").jsonObject
                val names = descriptor.elementNames.toList()
                assertEquals(names.toSet(), properties.keys, "$path properties")
                assertEquals(names.toSet(), schema.getValue("required").jsonArray.map { it.jsonPrimitive.content }.toSet(), "$path required")
                assertFalse(schema.getValue("additionalProperties").jsonPrimitive.boolean, "$path additionalProperties")
                names.forEachIndexed { index, name ->
                    assertFalse(descriptor.isElementOptional(index), "$path.$name must not have a default value")
                    assertMatches(descriptor.getElementDescriptor(index), properties.getValue(name).jsonObject, "$path.$name")
                }
            }
            StructureKind.LIST -> {
                assertEquals("array", schema.type(), path)
                assertMatches(descriptor.getElementDescriptor(0), schema.getValue("items").jsonObject, "$path[]")
            }
            SerialKind.ENUM -> {
                assertEquals("string", schema.type(), path)
                assertEquals(descriptor.elementNames.toList(), schema.enumValues(), "$path enum")
            }
            PrimitiveKind.STRING -> {
                assertEquals("string", schema.type(), path)
                assertFalse("enum" in schema, "$path must not be an enum")
            }
            else -> fail("$path: kind $kind is not handled by the schema builders")
        }
        assertFalse(descriptor.isNullable, "$path must not be nullable")
    }

    private fun JsonObject.type(): String = getValue("type").jsonPrimitive.content

    private fun JsonObject.property(name: String): JsonObject = getValue("properties").jsonObject.getValue(name).jsonObject

    private fun JsonObject.enumValues(): List<String> = getValue("enum").jsonArray.map(JsonElement::jsonPrimitive).map { it.content }
}
