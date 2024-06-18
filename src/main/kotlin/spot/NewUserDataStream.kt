package dev.alonfalsing.spot

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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

suspend fun Client.newUserDataStream() =
    client
        .post(configuration.baseUrl) {
            url {
                path("/api/v3/userDataStream")
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<NewUserDataStreamResponse>()

suspend fun Client.keepUserDataStream(listenKey: String) =
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

suspend fun Client.closeUserDataStream(listenKey: String) =
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

class NewUserDataStream :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val listenKey by option()
    private val close by option().flag()

    override fun run() =
        runBlocking {
            when {
                listenKey == null ->
                    client.newUserDataStream().let(::println)

                !close ->
                    client.keepUserDataStream(listenKey!!).let(::println)

                else ->
                    client.closeUserDataStream(listenKey!!).let(::println)
            }
        }
}
