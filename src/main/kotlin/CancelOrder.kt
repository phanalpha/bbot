package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.http.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
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

object CancelOrderResponseSerializer : JsonContentPolymorphicSerializer<CancelOrderResponse>(CancelOrderResponse::class) {
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
                appendSignature(configuration.apiSecret)
            }
        }
        headers {
            append("X-MBX-APIKEY", configuration.apiKey)
        }
    }.body<CancelOrderResponse>()

class CancelOrder : CliktCommand() {
    private val symbol by argument()
    private val orderId by option().long()
    private val clientOrderId by option()

    override fun run() =
        runBlocking {
            val cli = currentContext.findObject<Application>()?.cli!!

            cli.cancelOrder(symbol, orderId, clientOrderId).let(::println)
        }
}
