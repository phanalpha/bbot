package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.wss
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

@Serializable
data class MarkPriceUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    val timestamp: Instant,
    @SerialName("s")
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("p")
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("i")
    val indexPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("P")
    val estimatedSettlePrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("r")
    val fundingRate: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val nextFundingTime: Instant,
)

fun Client.collectMarkPrices(symbol: String) =
    flow {
        client.wss("${configuration.websocketUrl}/ws/${symbol.lowercase()}@markPrice") {
            while (true) {
                emit(receiveDeserialized<MarkPriceUpdateEvent>())
            }
        }
    }

fun Client.collectMarkPrices() =
    flow {
        client.wss("${configuration.websocketUrl}/ws/!markPrice@arr") {
            while (true) {
                emit(receiveDeserialized<List<MarkPriceUpdateEvent>>())
            }
        }
    }

class CollectPrices :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument().optional()

    override fun run() =
        runBlocking {
            when (symbol) {
                null -> client.collectMarkPrices()
                else -> client.collectMarkPrices(symbol!!)
            }.collect(::println)
        }
}
