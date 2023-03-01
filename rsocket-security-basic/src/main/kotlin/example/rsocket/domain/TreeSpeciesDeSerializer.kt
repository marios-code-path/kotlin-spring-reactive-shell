package example.rsocket.domain

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import java.util.function.Function

class TreeSpeciesDeSerializer (private val nodeConverter: Function<JsonNode, out Any>) : JsonDeserializer<TreeSpecies>() {
    override fun deserialize(jp: JsonParser?, ctxt: DeserializationContext?): TreeSpecies {
        val oc: ObjectCodec = jp?.codec!!
        val node: JsonNode = oc.readTree(jp)

        val idNode = node.get("id")

        val leafNode = node.get("leaf")


        return TreeSpecies(
                nodeConverter.apply(idNode).toString().toLong(),
                nodeConverter.apply(leafNode).toString()
        )
    }
}