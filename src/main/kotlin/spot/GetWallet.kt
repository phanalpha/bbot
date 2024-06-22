package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

enum class WalletName(
    val value: String,
) {
    SPOT("Spot"),
    FUNDING("Funding"),
    CROSS_MARGIN("Cross Margin"),
    ISOLATED_MARGIN("Isolated Margin"),
    USD_M_FUTURES("USDⓈ-M Futures"),
    COIN_M_FUTURES("COIN-M Futures"),
    EARN("Earn"),
    OPTIONS("Options"),
}

@Serializable(with = WalletResponseSerializer::class)
sealed interface WalletResponse

@Serializable
data class WalletBalance(
    @Serializable(with = BigDecimalSerializer::class)
    val balance: BigDecimal,
    val walletName: String,
)

@Serializable(with = WalletSerializer::class)
data class Wallet(
    val balances: List<WalletBalance>,
) : WalletResponse

object WalletSerializer : KSerializer<Wallet> {
    private val delegateSerializer = serializer<List<WalletBalance>>()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("Wallet", delegateSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: Wallet,
    ) = encoder.encodeSerializableValue(delegateSerializer, value.balances)

    override fun deserialize(decoder: Decoder) = Wallet(decoder.decodeSerializableValue(delegateSerializer))
}

object WalletResponseSerializer :
    JsonContentPolymorphicSerializer<WalletResponse>(WalletResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            element is JsonObject -> Error.serializer()
            else -> Wallet.serializer()
        }
}

suspend fun Client.getWallet() =
    client
        .get(configuration.baseUrl) {
            url {
                path("/sapi/v1/asset/wallet/balance")
                parameters.apply {
                    appendTimestamp()
                    appendSignature(configuration)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<WalletResponse>()

class GetWallet :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()

    override fun run() =
        runBlocking {
            client.getWallet().let(::println)
        }
}
