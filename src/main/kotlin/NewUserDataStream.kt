package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
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

object NewUserDataStreamResponseSerializer : JsonContentPolymorphicSerializer<NewUserDataStreamResponse>(NewUserDataStreamResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> UserDataStream.serializer()
        }
}

suspend fun SpotClient.newUserDataStream() =
    client
        .post(configuration.baseUrl) {
            url {
                path("/api/v3/userDataStream")
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<NewUserDataStreamResponse>()

suspend fun SpotClient.keepUserDataStream(listenKey: String) =
    client
        .put(configuration.baseUrl) {
            url {
                path("/api/v3/userDataStream")
                parameters.apply {
                    append("listenKey", listenKey)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<Error>()

suspend fun SpotClient.closeUserDataStream(listenKey: String) =
    client
        .delete(configuration.baseUrl) {
            url {
                path("/api/v3/userDataStream")
                parameters.apply {
                    append("listenKey", listenKey)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<Error>()

class NewUserDataStream : CliktCommand() {
    private val listenKey by option()
    private val close by option().flag()

    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            if (listenKey == null) {
                application.cli
                    .newUserDataStream()
                    .let(::println)
            } else if (!close) {
                application.cli
                    .keepUserDataStream(listenKey!!)
                    .let(::println)
            } else {
                application.cli
                    .closeUserDataStream(listenKey!!)
                    .let(::println)
            }
        }
}
