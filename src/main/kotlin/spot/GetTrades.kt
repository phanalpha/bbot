package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
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
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

@Serializable(with = TradeArrayResponseSerializer::class)
sealed interface TradeArrayResponse

@Serializable
data class Trade(
    val id: Long,
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
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
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("time")
    val tradedAt: Instant,
    val isBuyer: Boolean,
    val isMaker: Boolean,
    val isBestMatch: Boolean,
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
    ) = encoder.encodeSerializableValue(delegateSerializer, value.trades)

    override fun deserialize(decoder: Decoder) = TradeArray(decoder.decodeSerializableValue(delegateSerializer))
}

object TradeArrayResponseSerializer :
    JsonContentPolymorphicSerializer<TradeArrayResponse>(TradeArrayResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            element is JsonObject -> Error.serializer()
            else -> TradeArray.serializer()
        }
}

suspend fun Client.getTrades(
    symbol: String,
    orderId: Long? = null,
    fromId: Long? = null,
    limit: Int? = null,
) = client
    .get(configuration.baseUrl) {
        url {
            path("/api/v3/myTrades")
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
    }.body<TradeArrayResponse>()

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
