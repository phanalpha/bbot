package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal

@Serializable(with = AccountResponseSerializer::class)
sealed interface AccountResponse

@Serializable
data class Balance(
    val asset: String,
    @Serializable(with = BigDecimalSerializer::class)
    val free: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
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

class GetAccount : CliktCommand() {
    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            application.client
                .get(application.configuration.binance.baseUrl) {
                    url {
                        path("/api/v3/account")
                        parameters.apply {
                            append("omitZeroBalances", "true")

                            appendTimestamp()
                            appendSignature(application.configuration.binance.apiSecret)
                        }
                    }
                    headers {
                        append("X-MBX-APIKEY", application.configuration.binance.apiKey)
                    }
                }.body<AccountResponse>()
                .let(::println)
        }
}
