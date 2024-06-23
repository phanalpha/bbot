package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
import dev.alonfalsing.common.OrderResponseType
import dev.alonfalsing.common.OrderSide
import dev.alonfalsing.common.OrderStatus
import dev.alonfalsing.common.SelfTradePreventionMode
import dev.alonfalsing.common.TimeInForce
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

@Serializable(with = OrderAckResponseSerializer::class)
sealed interface OrderAckResponse

@Serializable
data class OrderAck(
    val symbol: String,
    val orderId: Long,
    val orderListId: Long,
    val clientOrderId: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val transactTime: Instant,
) : OrderAckResponse

object OrderAckResponseSerializer :
    JsonContentPolymorphicSerializer<OrderAckResponse>(OrderAckResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> OrderAck.serializer()
        }
}

@Serializable(with = OrderResultResponseSerializer::class)
sealed interface OrderResultResponse

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
) : OrderResultResponse

object OrderResultResponseSerializer :
    JsonContentPolymorphicSerializer<OrderResultResponse>(OrderResultResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> OrderResult.serializer()
        }
}

@Serializable(with = OrderFullResponseSerializer::class)
sealed interface OrderFullResponse

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
) : OrderFullResponse

object OrderFullResponseSerializer :
    JsonContentPolymorphicSerializer<OrderFullResponse>(OrderFullResponse::class) {
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
            append("X-MBX-APIKEY", credentials.apiKey)
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
                            OrderAckResponse::class -> OrderResponseType.ACK
                            OrderResultResponse::class -> OrderResponseType.RESULT
                            OrderFullResponse::class -> OrderResponseType.FULL
                            else -> error("Unsupported type: ${T::class}")
                        }.name,
                    )

                    appendTimestamp()
                    appendSignature(credentials)
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
    private val price by argument().convert { it.toBigDecimal() }.optional()
    private val side by option("--sell").flag().convert { if (!it) OrderSide.BUY else OrderSide.SELL }
    private val clientOrderId by option()
    private val responseType by option()
        .choice(choices = OrderResponseType.entries.map { it.name }.toTypedArray())
        .convert { OrderResponseType.valueOf(it) }
        .default(OrderResponseType.ACK)

    override fun run() =
        runBlocking {
            when (responseType) {
                OrderResponseType.ACK ->
                    client
                        .newOrder<OrderAckResponse>(symbol, side, quantity, quote, price, clientOrderId)
                        .let(::println)

                OrderResponseType.RESULT ->
                    client
                        .newOrder<OrderResultResponse>(symbol, side, quantity, quote, price, clientOrderId)
                        .let(::println)

                OrderResponseType.FULL ->
                    client
                        .newOrder<OrderFullResponse>(symbol, side, quantity, quote, price, clientOrderId)
                        .let(::println)
            }
        }
}
