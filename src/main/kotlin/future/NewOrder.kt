package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
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

@Serializable(with = OrderResultResponseSerializer::class)
sealed interface OrderResponse

@Serializable
data class Order(
    val symbol: String,
    val orderId: Long,
    val clientOrderId: String,
    val side: OrderSide,
    val type: OrderType,
    val timeInForce: TimeInForce,
    @SerialName("origType")
    val originalType: OrderType,
    val positionSide: PositionSide,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("origQty")
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("executedQty")
    val executedQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cumQty")
    val cumulateQuantity: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("price")
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("avgPrice")
    val averagePrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val stopPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("activatePrice")
    val activationPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cumQuote")
    val cumulateQuote: BigDecimal,
    val reduceOnly: Boolean,
    val closePosition: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val priceRate: BigDecimal? = null,
    val priceProtect: Boolean,
    val priceMatch: String,
    val selfTradePreventionMode: SelfTradePreventionMode,
    val status: OrderStatus,
    val workingType: WorkingType,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val goodTillDate: Instant? = null,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("updateTime")
    val updatedAt: Instant,
) : OrderResponse

object OrderResultResponseSerializer :
    JsonContentPolymorphicSerializer<OrderResponse>(OrderResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> Order.serializer()
        }
}

suspend fun Client.newOrder(
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    price: BigDecimal? = null,
    positionSide: PositionSide? = null,
    newClientOrderId: String? = null,
) = client
    .post(configuration.baseUrl) {
        url {
            path("/fapi/v1/order")
        }
        headers {
            append("X-MBX-APIKEY", configuration.apiKey)
        }

        setBody(
            FormDataContent(
                parameters {
                    append("symbol", symbol)
                    append("side", side.name)
                    if (positionSide != null) {
                        append("positionSide", positionSide.name)
                    }
                    if (price == null) {
                        append("type", OrderType.MARKET.name)
                    } else {
                        append("type", OrderType.LIMIT.name)
                        append("price", price.toPlainString())
                        append("timeInForce", TimeInForce.GTC.name)
                    }
                    append("quantity", quantity.toPlainString())
                    append("newClientOrderId", newClientOrderId ?: NanoId.generate())

                    appendTimestamp()
                    appendSignature(configuration)
                },
            ),
        )
    }.body<OrderResponse>()

class NewOrder :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val quantity by argument().convert { it.toBigDecimal() }
    private val price by argument().convert { it.toBigDecimal() }.optional()
    private val side by option("--sell").flag().convert { if (!it) OrderSide.BUY else OrderSide.SELL }
    private val positionSide by option()
        .choice(choices = PositionSide.entries.map { it.name }.toTypedArray())
        .convert { PositionSide.valueOf(it) }
    private val newClientOrderId by option()

    override fun run() =
        runBlocking {
            client
                .newOrder(
                    symbol = symbol,
                    side = side,
                    quantity = quantity,
                    price = price,
                    positionSide = positionSide,
                    newClientOrderId = newClientOrderId,
                ).let(::println)
        }
}
