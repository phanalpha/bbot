package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.wss
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import java.math.BigDecimal

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("e")
sealed class Event {
    abstract val timestamp: Instant
}

@Serializable
@SerialName("trade")
data class TradeEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @SerialName("s")
    val symbol: String,
    @SerialName("t")
    val tradeId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("p")
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("q")
    val quantity: BigDecimal,
    @SerialName("b")
    val buyerOrderId: Long? = null,
    @SerialName("a")
    val sellerOrderId: Long? = null,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val tradedAt: Instant,
    @SerialName("m")
    val isBuyerMaker: Boolean,
) : Event()

class CollectTrades : CliktCommand() {
    private val baseUrl by option()
    private val symbol by argument()

    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            application.client
                .wss("${baseUrl ?: application.configuration.binance.websocketUrl}/ws/$symbol@trade") {
                    while (true) {
                        println(receiveDeserialized<Event>())
                    }
                }
        }
}
