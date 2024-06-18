package dev.alonfalsing.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

@Serializable(with = NewUserDataStreamResponseSerializer::class)
interface NewUserDataStreamResponse

@Serializable
data class UserDataStream(
    val listenKey: String,
) : NewUserDataStreamResponse

object NewUserDataStreamResponseSerializer :
    JsonContentPolymorphicSerializer<NewUserDataStreamResponse>(NewUserDataStreamResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> UserDataStream.serializer()
        }
}
