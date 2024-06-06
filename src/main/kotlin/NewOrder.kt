package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.parameters
import io.ktor.http.path
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal

@Serializable(with = OrderResponseAckSerializer::class)
sealed interface OrderResponseAck

@Serializable
data class OrderAck(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val transactTime: Instant,
) : OrderResponseAck

object OrderResponseAckSerializer : JsonContentPolymorphicSerializer<OrderResponseAck>(OrderResponseAck::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> OrderAck.serializer()
        }
}

@Serializable(with = OrderResponseResultSerializer::class)
sealed interface OrderResponseResult

@Serializable
data class OrderResult(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val transactTime: Instant,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val origQty: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val executedQty: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cummulativeQuoteQty")
    val cumulativeQuoteQty: BigDecimal,
    val status: OrderStatus,
    val timeInForce: TimeInForce,
    val type: OrderType,
    val side: OrderSide,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val workingTime: Instant,
    val selfTradePreventionMode: SelfTradePreventionMode,
) : OrderResponseResult

object OrderResponseResultSerializer : JsonContentPolymorphicSerializer<OrderResponseResult>(OrderResponseResult::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> OrderResult.serializer()
        }
}

@Serializable(with = OrderResponseFullSerializer::class)
sealed interface OrderResponseFull

@Serializable
data class Fill(
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("qty")
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val commission: BigDecimal,
    val commissionAsset: String,
    val tradeId: Long,
)

@Serializable
data class OrderFull(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("transactTime")
    val transactedAt: Instant,
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
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val workingTime: Instant,
    val selfTradePreventionMode: SelfTradePreventionMode,
    val fills: List<Fill>,
) : OrderResponseFull

object OrderResponseFullSerializer : JsonContentPolymorphicSerializer<OrderResponseFull>(OrderResponseFull::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> OrderFull.serializer()
        }
}

class NewOrder : CliktCommand() {
    private val symbol by argument()
    private val quantity by argument().convert { it.toBigDecimal() }
    private val clientOrderId by option()
    private val price by option().convert { it.toBigDecimal() }
    private val buy by option().flag()
    private val rt by option().convert { OrderResponseType.valueOf(it) }.default(OrderResponseType.ACK)

    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            application.client
                .post(application.configuration.binance.baseUrl) {
                    url {
                        path("/api/v3/order")
                    }
                    headers {
                        append("X-MBX-APIKEY", application.configuration.binance.apiKey)
                    }

                    setBody(
                        FormDataContent(
                            parameters {
                                append("symbol", symbol)
                                append("side", (if (buy) OrderSide.BUY else OrderSide.SELL).name)
                                if (price == null) {
                                    append("type", OrderType.MARKET.name)
                                } else {
                                    append("type", OrderType.LIMIT.name)
                                    append("price", price!!.toPlainString())
                                    append("timeInForce", TimeInForce.GTC.name)
                                }
                                append("quantity", quantity.toPlainString())
                                append("newClientOrderId", clientOrderId ?: NanoId.generate())
                                append("newOrderRespType", rt.name)

                                appendTimestamp()
                                appendSignature(application.configuration.binance.apiSecret)
                            },
                        ),
                    )
                }.let {
                    when (rt) {
                        OrderResponseType.ACK -> it.body<OrderResponseAck>()
                        OrderResponseType.RESULT -> it.body<OrderResponseResult>()
                        OrderResponseType.FULL -> it.body<OrderResponseFull>()
                    }
                }.let(::println)
        }
}
