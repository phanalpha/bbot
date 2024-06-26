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
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.http.path
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

@Serializable(with = CancelOrderResponseSerializer::class)
sealed interface CancelOrderResponse

@Serializable
data class CancelOrderResult(
    val symbol: String,
    @SerialName("origClientOrderId")
    val originalClientOrderId: String,
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
    val selfTradePreventionMode: SelfTradePreventionMode,
) : CancelOrderResponse

object CancelOrderResponseSerializer :
    JsonContentPolymorphicSerializer<CancelOrderResponse>(CancelOrderResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> CancelOrderResult.serializer()
        }
}

suspend fun Client.cancelOrder(
    symbol: String,
    orderId: Long? = null,
    clientOrderId: String? = null,
) = client
    .delete(configuration.baseUrl) {
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
    }.body<CancelOrderResponse>()

class CancelOrder :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val orderId by option().long()
    private val clientOrderId by option()

    override fun run() =
        runBlocking {
            client.cancelOrder(symbol, orderId, clientOrderId).let(::println)
        }
}
