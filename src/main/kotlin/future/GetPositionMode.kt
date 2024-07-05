package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable(with = PositionModeResponseSerializer::class)
sealed interface PositionModeResponse

@Serializable
data class PositionMode(
    val dualSidePosition: Boolean,
) : PositionModeResponse

object PositionModeResponseSerializer :
    JsonContentPolymorphicSerializer<PositionModeResponse>(PositionModeResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> PositionMode.serializer()
        }
}

suspend fun Client.getPositionMode() =
    client
        .get(configuration.baseUrl) {
            url {
                path("/fapi/v1/positionSide/dual")
                parameters.apply {
                    appendTimestamp()
                    appendSignature(credentials)
                }
            }
            headers {
                append("X-MBX-APIKEY", credentials.apiKey)
            }
        }.body<PositionModeResponse>()

class GetPositionMode :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()

    override fun run() =
        runBlocking {
            client.getPositionMode().let(::println)
        }
}
