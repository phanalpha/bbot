package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
import dev.alonfalsing.common.OrderSide
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

@Serializable
sealed interface TradeArrayResponse

@Serializable
data class Trade(
    val id: Long,
    val orderId: Long,
    val symbol: String,
    val side: OrderSide,
    val positionSide: PositionSide,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("qty")
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("quoteQty")
    val quoteQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val commission: BigDecimal,
    val commissionAsset: String,
    @Serializable(with = BigDecimalSerializer::class)
    val realizedPnl: BigDecimal,
    val buyer: Boolean,
    val maker: Boolean,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("time")
    val tradedAt: Instant,
)

@Serializable(with = TradeArraySerializer::class)
data class TradeArray(
    val trades: List<Trade>,
) : TradeArrayResponse

class TradeArraySerializer : KSerializer<TradeArray> {
    private val delegateSerializer = serializer<List<Trade>>()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("TradeArray", delegateSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: TradeArray,
    ) = delegateSerializer.serialize(encoder, value.trades)

    override fun deserialize(decoder: Decoder) = TradeArray(delegateSerializer.deserialize(decoder))
}

suspend fun Client.getTrades(
    symbol: String,
    orderId: Long? = null,
    fromId: Long? = null,
    limit: Int? = null,
) = client
    .get(configuration.baseUrl) {
        url {
            path("/fapi/v1/userTrades")
            parameters.apply {
                append("symbol", symbol)
                orderId?.let { append("orderId", it.toString()) }
                fromId?.let { append("fromId", it.toString()) }
                limit?.let { append("limit", it.toString()) }

                appendTimestamp()
                appendSignature(configuration)
            }
        }
        headers {
            append("X-MBX-APIKEY", configuration.apiKey)
        }
    }.body<TradeArray>()

class GetTrades :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val orderId by option().long()
    private val fromId by option().long()
    private val limit by option().int()

    override fun run() =
        runBlocking {
            client.getTrades(symbol, orderId, fromId, limit).let(::println)
        }
}
