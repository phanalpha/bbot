package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.alonfalsing.BigDecimalSerializer
import dev.alonfalsing.InstantEpochMillisecondsSerializer
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.wss
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("e")
sealed interface MarketEvent {
    val timestamp: Instant
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
    /*
     * 2024-06-05
     * WebSocket Streams
     * Buyer order ID (b) and Seller order ID (a) have been removed from the Trade streams. (i.e. <symbol>@trade)
     * To monitor if your order was part of a trade, please listen to the User Data Streams.
     */
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val tradedAt: Instant,
    @SerialName("m")
    val isBuyerMaker: Boolean,
) : MarketEvent

fun Client.collectTrades(symbol: String) =
    flow {
        client.wss("${configuration.websocketUrl}/ws/$symbol@trade") {
            while (true) {
                emit(receiveDeserialized<MarketEvent>())
            }
        }
    }

class CollectTrades :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()

    override fun run() =
        runBlocking {
            client.collectTrades(symbol).collect(::println)
        }
}
