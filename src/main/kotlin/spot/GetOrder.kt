package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
import dev.alonfalsing.common.OrderSide
import dev.alonfalsing.common.OrderStatus
import dev.alonfalsing.common.SelfTradePreventionMode
import dev.alonfalsing.common.TimeInForce
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

@Serializable(with = OrderResponseSerializer::class)
sealed interface OrderResponse

@Serializable
data class Order(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("origQty")
    val originalQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("executedQty")
    val executedQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cummulativeQuoteQty")
    val cumulativeQuoteQuantity: BigDecimal,
    val status: OrderStatus,
    val timeInForce: TimeInForce,
    val type: OrderType,
    val side: OrderSide,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("stopPrice")
    val stopPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("icebergQty")
    val icebergQuantity: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("time")
    val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("updateTime")
    val updatedAt: Instant,
    val isWorking: Boolean,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val workingTime: Instant,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("origQuoteOrderQty")
    val originalQuoteOrderQuantity: BigDecimal,
    val selfTradePreventionMode: SelfTradePreventionMode,
) : OrderResponse

object OrderResponseSerializer :
    JsonContentPolymorphicSerializer<OrderResponse>(OrderResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> Order.serializer()
        }
}

@Serializable(with = OrderArrayResponseSerializer::class)
sealed interface OrderArrayResponse

@Serializable(with = OrderArraySerializer::class)
data class OrderArray(
    val orders: List<Order>,
) : OrderArrayResponse

class OrderArraySerializer : KSerializer<OrderArray> {
    private val delegateSerializer = serializer<List<Order>>()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("OrderArray", delegateSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: OrderArray,
    ) = encoder.encodeSerializableValue(delegateSerializer, value.orders)

    override fun deserialize(decoder: Decoder) = OrderArray(decoder.decodeSerializableValue(delegateSerializer))
}

object OrderArrayResponseSerializer :
    JsonContentPolymorphicSerializer<OrderArrayResponse>(OrderArrayResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            element is JsonObject -> Error.serializer()
            else -> OrderArray.serializer()
        }
}

suspend fun Client.getOrder(
    symbol: String,
    orderId: Long?,
    clientOrderId: String?,
) = client
    .get(configuration.baseUrl) {
        url {
            path("/api/v3/order")
            parameters.apply {
                append("symbol", symbol)
                orderId?.let { append("orderId", it.toString()) }
                clientOrderId?.let { append("origClientOrderId", it) }

                appendTimestamp()
                appendSignature(credentials)
            }
        }
        headers {
            append("X-MBX-APIKEY", credentials.apiKey)
        }
    }.body<OrderResponse>()

suspend fun Client.getOrders(symbol: String) =
    client
        .get(configuration.baseUrl) {
            url {
                path("/api/v3/openOrders")
                parameters.apply {
                    append("symbol", symbol)

                    appendTimestamp()
                    appendSignature(credentials)
                }
            }
            headers {
                append("X-MBX-APIKEY", credentials.apiKey)
            }
        }.body<OrderArrayResponse>()

class GetOrder :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val orderId by option().long()
    private val clientOrderId by option()

    override fun run() =
        runBlocking {
            when {
                orderId != null || clientOrderId != null ->
                    client.getOrder(symbol, orderId, clientOrderId).let(::println)

                else ->
                    client.getOrders(symbol).let(::println)
            }
        }
}
