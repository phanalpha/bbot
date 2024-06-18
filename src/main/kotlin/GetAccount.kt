package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal

@Serializable(with = AccountResponseSerializer::class)
sealed interface AccountResponse

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Balance(
    @JsonNames("a")
    val asset: String,
    @Serializable(with = BigDecimalSerializer::class)
    @JsonNames("f")
    val free: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @JsonNames("l")
    val locked: BigDecimal,
)

@Serializable
data class Account(
    val uid: Long,
    val balances: List<Balance>,
) : AccountResponse

object AccountResponseSerializer : JsonContentPolymorphicSerializer<AccountResponse>(AccountResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> Account.serializer()
        }
}

suspend fun SpotClient.getAccount() =
    client
        .get(configuration.baseUrl) {
            url {
                path("/api/v3/account")
                parameters.apply {
                    append("omitZeroBalances", "true")

                    appendTimestamp()
                    appendSignature(configuration.apiSecret)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<AccountResponse>()

class GetAccount : CliktCommand() {
    override fun run() =
        runBlocking {
            val cli = currentContext.findObject<Application>()!!.cli

            cli.getAccount().let(::println)
        }
}
