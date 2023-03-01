package example.rsocket.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import java.util.*
import java.util.function.Function


object JsonNodeToAnyConverter : Function<JsonNode, Any> {
    override fun apply(record: JsonNode): Any {
        return when (record.nodeType) {
            JsonNodeType.MISSING -> throw Exception("Missing field")
            JsonNodeType.BINARY -> {
                val cborFactory = CBORFactory()
                val mapper = ObjectMapper(cborFactory)
                val cborData = mapper.writeValueAsBytes(record.binaryValue())

                try {
                    val data = mapper.readValue(cborData, UUID::class.java)
                    data
                } catch (e: Exception) {
                    cborData.toString()
                }
            }
            JsonNodeType.NUMBER -> {
                val variable = record.asDouble()
                if (variable % 1 == 0.0)
                    record.asLong()
                else
                    variable
            }
            JsonNodeType.STRING -> {
                try {
                    UUID.fromString(record.asText())
                } catch (e: Exception) {
                    record.asText()
                }
            }
            JsonNodeType.BOOLEAN -> record.asBoolean()
            JsonNodeType.OBJECT -> record.fields().asSequence().associate { it.key to apply(it.value) }
            JsonNodeType.ARRAY -> record.elements().asSequence().map { apply(it) }.toList()
            else -> record.asText()
        }
    }
}