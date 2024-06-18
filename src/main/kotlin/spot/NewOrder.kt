package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.alonfalsing.BigDecimalSerializer
import dev.alonfalsing.InstantEpochMillisecondsSerializer
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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

object OrderResponseAckSerializer :
    JsonContentPolymorphicSerializer<OrderResponseAck>(OrderResponseAck::class) {
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

object OrderResponseResultSerializer :
    JsonContentPolymorphicSerializer<OrderResponseResult>(OrderResponseResult::class) {
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

object OrderResponseFullSerializer :
    JsonContentPolymorphicSerializer<OrderResponseFull>(OrderResponseFull::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> OrderFull.serializer()
        }
}

suspend inline fun <reified T> Client.newOrder(
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    quote: Boolean = false,
    price: BigDecimal? = null,
    clientOrderId: String? = null,
) = client
    .post(configuration.baseUrl) {
        url {
            path("/api/v3/order")
        }
        headers {
            append("X-MBX-APIKEY", configuration.apiKey)
        }

        setBody(
            FormDataContent(
                parameters {
                    append("symbol", symbol)
                    append("side", side.name)
                    if (price == null) {
                        append("type", OrderType.MARKET.name)
                        if (!quote) {
                            append("quantity", quantity.toPlainString())
                        } else {
                            append("quoteOrderQty", quantity.toPlainString())
                        }
                    } else {
                        append("type", OrderType.LIMIT.name)
                        append("quantity", quantity.toPlainString())
                        append("price", price.toPlainString())
                        append("timeInForce", TimeInForce.GTC.name)
                    }
                    append("newClientOrderId", clientOrderId ?: NanoId.generate())
                    append(
                        "newOrderRespType",
                        when (T::class) {
                            OrderResponseAck::class -> OrderResponseType.ACK
                            OrderResponseResult::class -> OrderResponseType.RESULT
                            OrderResponseFull::class -> OrderResponseType.FULL
                            else -> error("Unsupported type: ${T::class}")
                        }.name,
                    )

                    appendTimestamp()
                    appendSignature(configuration)
                },
            ),
        )
    }.body<T>()

class NewOrder :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val quantity by argument().convert { it.toBigDecimal() }
    private val quote by option().flag()
    private val clientOrderId by option()
    private val price by option().convert { it.toBigDecimal() }
    private val buy by option().flag()
    private val rt by option().convert { OrderResponseType.valueOf(it) }.default(OrderResponseType.ACK)

    override fun run() =
        runBlocking {
            val side = if (buy) OrderSide.BUY else OrderSide.SELL

            when (rt) {
                OrderResponseType.ACK ->
                    client
                        .newOrder<OrderResponseAck>(symbol, side, quantity, quote, price, clientOrderId)
                        .let(::println)

                OrderResponseType.RESULT ->
                    client
                        .newOrder<OrderResponseResult>(symbol, side, quantity, quote, price, clientOrderId)
                        .let(::println)

                OrderResponseType.FULL ->
                    client
                        .newOrder<OrderResponseFull>(symbol, side, quantity, quote, price, clientOrderId)
                        .let(::println)
            }
        }
}
