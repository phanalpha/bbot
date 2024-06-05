package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.formUrlEncode
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256
import java.math.BigDecimal

@Serializable(with = AccountResponseSerializer::class)
sealed interface AccountResponse

@Serializable
data class Error(
    val code: Int,
    @SerialName("msg")
    val message: String,
) : AccountResponse

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
    @OptIn(ExperimentalStdlibApi::class)
    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            application.client
                .get(application.configuration.binance.baseUrl) {
                    url {
                        path("/api/v3/account")
                        Clock.System.now().let {
                            parameters.append("omitZeroBalances", "true")
                            parameters.append("timestamp", it.toEpochMilliseconds().toString())
                        }
                        HmacSHA256(
                            application.configuration.binance.apiSecret
                                .toByteArray(),
                        ).let {
                            it.update(parameters.build().formUrlEncode().toByteArray())
                            parameters.append("signature", it.doFinal().toHexString())
                        }
                    }
                    headers {
                        append("X-MBX-APIKEY", application.configuration.binance.apiKey)
                    }
                }.body<AccountResponse>()
                .let(::println)
        }
}
