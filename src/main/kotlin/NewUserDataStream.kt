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

class NewUserDataStream : CliktCommand() {
    private val listenKey by option()
    private val close by option().flag()

    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            if (listenKey == null) {
                application.client
                    .post(application.configuration.binance.baseUrl) {
                        url {
                            path("/api/v3/userDataStream")
                        }
                        headers {
                            append("X-MBX-APIKEY", application.configuration.binance.apiKey)
                        }
                    }.body<NewUserDataStreamResponse>()
                    .let(::println)
            } else if (!close) {
                application.client
                    .put(application.configuration.binance.baseUrl) {
                        url {
                            path("/api/v3/userDataStream")
                            parameters.apply {
                                append("listenKey", listenKey!!)
                            }
                        }
                        headers {
                            append("X-MBX-APIKEY", application.configuration.binance.apiKey)
                        }
                    }.body<Error>()
                    .let(::println)
            } else {
                application.client
                    .delete(application.configuration.binance.baseUrl) {
                        url {
                            path("/api/v3/userDataStream")
                            parameters.apply {
                                append("listenKey", listenKey!!)
                            }
                        }
                        headers {
                            append("X-MBX-APIKEY", application.configuration.binance.apiKey)
                        }
                    }.body<Error>()
                    .let(::println)
            }
        }
}
