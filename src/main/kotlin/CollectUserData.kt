package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
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
sealed interface UserDataEvent {
    val timestamp: Instant
}

@Serializable
@SerialName("outboundAccountPosition")
data class AccountUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("u")
    val lastUpdatedAt: Instant,
    @SerialName("B")
    val balances: List<Balance>,
) : UserDataEvent

@Serializable
@SerialName("balanceUpdate")
data class BalanceUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @SerialName("a")
    val asset: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("d")
    val delta: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val clearedAt: Instant,
) : UserDataEvent

@Serializable
@SerialName("executionReport")
data class ExecutionReportEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @SerialName("s")
    val symbol: String,
    @SerialName("c")
    val clientOrderId: String,
    @SerialName("S")
    val side: OrderSide,
    @SerialName("o")
    val type: OrderType,
    @SerialName("f")
    val timeInForce: TimeInForce,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("q")
    val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("p")
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("P")
    val stopPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("F")
    val icebergQuantity: BigDecimal,
    @SerialName("g")
    val orderListId: Long,
    @SerialName("C")
    val originalClientOrderId: String,
    @SerialName("x")
    val executionType: ExecutionType,
    @SerialName("X")
    val status: OrderStatus,
    @SerialName("r")
    val rejectReason: String,
    @SerialName("i")
    val orderId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("l")
    val lastExecutedQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("z")
    val cumulativeFilledQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("L")
    val lastExecutedPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("n")
    val commissionAmount: BigDecimal,
    @SerialName("N")
    val commissionAsset: String?,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("t")
    val tradeId: Long,
    @SerialName("w")
    val isOnTheBook: Boolean,
    @SerialName("m")
    val isMaker: Boolean,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("O")
    val orderCreatedAt: Instant,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Z")
    val cumulativeQuoteQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Y")
    val lastQuoteQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("Q")
    val quoteOrderQuantity: BigDecimal,
    @SerialName("V")
    val selfTradePreventionMode: SelfTradePreventionMode,
) : UserDataEvent

@Serializable
data class OrderId(
    @SerialName("s")
    val symbol: String,
    @SerialName("i")
    val orderId: Long,
    @SerialName("c")
    val clientOrderId: String,
)

@Serializable
@SerialName("listStatus")
data class ListStatusEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @SerialName("s")
    val symbol: String,
    @SerialName("g")
    val orderListId: Long,
    @SerialName("c")
    val contingencyType: String,
    @SerialName("l")
    val status: String,
    @SerialName("L")
    val orderStatus: String,
    @SerialName("r")
    val rejectReason: String,
    @SerialName("C")
    val clientOrderId: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("O")
    val orders: List<OrderId>,
) : UserDataEvent

@Serializable
@SerialName("listenKeyExpired")
data class ListenKeyExpiredEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    val listenKey: String,
) : UserDataEvent

suspend fun Client.collectUserData(
    listenKey: String,
    initialBlock: suspend () -> Unit = {},
    block: suspend (UserDataEvent) -> Unit,
) = client
    .wss("${configuration.websocketUrl}/ws/$listenKey") {
        initialBlock()
        while (true) {
            block(receiveDeserialized<UserDataEvent>())
        }
    }

class CollectUserData : CliktCommand() {
    private val listenKey by argument()

    override fun run() =
        runBlocking {
            val application = currentContext.findObject<Application>()!!

            application.cli.collectUserData(listenKey, { println("ready") }, ::println)
        }
}
