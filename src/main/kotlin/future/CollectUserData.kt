package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
import dev.alonfalsing.common.OrderSide
import dev.alonfalsing.common.OrderStatus
import dev.alonfalsing.common.SelfTradePreventionMode
import dev.alonfalsing.common.TimeInForce
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.wss
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal

@Serializable(with = AbstractUserDataEventSerializer::class)
sealed interface AbstractUserDataEvent

@Serializable
data class UserDataStreamExpired(
    @SerialName("E")
    val timestamp: Instant,
    @SerialName("listenKey")
    val listenKey: String,
)

@Serializable
data class UserDataStreamExpiredEvent(
    val stream: String,
    val data: UserDataStreamExpired,
) : AbstractUserDataEvent

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("e")
sealed interface UserDataEvent : AbstractUserDataEvent {
    val timestamp: Instant
}

@Serializable
data class PositionOfMarginCall(
    @SerialName("s")
    val symbol: String,
    @SerialName("ps")
    val positionSide: PositionSide,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("pa")
    val positionAmount: BigDecimal,
    @SerialName("mt")
    val marginType: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("iw")
    val isolatedWallet: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mp")
    val markPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("up")
    val unrealizedPnL: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mm")
    val maintenanceMarginRequired: BigDecimal,
)

@Serializable
@SerialName("MARGIN_CALL")
data class MarginCallEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cw")
    val crossWalletBalance: BigDecimal,
    @SerialName("p")
    val positions: List<PositionOfMarginCall>,
) : UserDataEvent

@Serializable
data class BalanceUpdate(
    @SerialName("a")
    val asset: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("wb")
    val walletBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cw")
    val crossWalletBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("bc")
    val balanceChange: BigDecimal,
)

@Serializable
data class PositionUpdate(
    @SerialName("s")
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("pa")
    val positionAmount: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("ep")
    val entryPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("bep")
    val breakevenPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cr")
    val accumulatedRealized: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("up")
    val unrealizedPnL: BigDecimal,
    @SerialName("mt")
    val marginType: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("iw")
    val isolatedWallet: BigDecimal? = null,
    @SerialName("ps")
    val positionSide: PositionSide,
)

@Serializable
data class UpdateData(
    @SerialName("m")
    val eventReasonType: String,
    @SerialName("B")
    val balances: List<BalanceUpdate>,
    @SerialName("P")
    val positions: List<PositionUpdate>,
)

@Serializable
@SerialName("ACCOUNT_UPDATE")
data class AccountUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("a")
    val updateData: UpdateData,
) : UserDataEvent

@Serializable
data class OrderTradeUpdate(
    @SerialName("s")
    val symbol: String,
    @SerialName("c")
    val clientOrderId: String,
    @SerialName("S")
    val side: OrderSide,
    @SerialName("o")
    val orderType: OrderType,
    @SerialName("f")
    val timeInForce: TimeInForce,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("q")
    val originalQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("p")
    val originalPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("ap")
    val averagePrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("sp")
    val stopPrice: BigDecimal,
    @SerialName("x")
    val executionType: ExecutionType,
    @SerialName("X")
    val orderStatus: OrderStatus,
    @SerialName("i")
    val orderId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("l")
    val lastFilledQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("z")
    val filledAccumulatedQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("L")
    val lastFilledPrice: BigDecimal,
    @SerialName("N")
    val commissionAsset: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("n")
    val commission: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val tradedAt: Instant,
    @SerialName("t")
    val tradeId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("b")
    val bidsNotional: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("a")
    val askNotional: BigDecimal,
    @SerialName("m")
    val isMaker: Boolean,
    @SerialName("R")
    val isReduceOnly: Boolean,
    @SerialName("wt")
    val stopPriceWorkingType: WorkingType,
    @SerialName("ot")
    val originalOrderType: OrderType,
    @SerialName("ps")
    val positionSide: PositionSide,
    @SerialName("cp")
    val isCloseAll: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("AP")
    val activationPrice: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("cr")
    val callbackRate: BigDecimal? = null,
    @SerialName("pP")
    val isPriceProtectionOn: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("rp")
    val realizedProfit: BigDecimal,
    @SerialName("V")
    val selfTradePreventionMode: SelfTradePreventionMode,
    @SerialName("pm")
    val priceMatchMode: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("gtd")
    val goodTillDate: Instant,
)

@Serializable
@SerialName("ORDER_TRADE_UPDATE")
data class OrderTradeUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("o")
    val orderUpdate: OrderTradeUpdate,
) : UserDataEvent

@Serializable
data class AccountConfig(
    @SerialName("s")
    val symbol: String,
    @SerialName("l")
    val leverage: Int,
)

@Serializable
data class AccountInfo(
    @SerialName("m")
    val multiAssetMode: Boolean,
)

@Serializable
@SerialName("ACCOUNT_CONFIG_UPDATE")
data class AccountConfigUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("ac")
    val accountConfig: AccountConfig? = null,
    @SerialName("ai")
    val accountInfo: AccountInfo? = null,
) : UserDataEvent

enum class StrategyStatus {
    NEW,
    WORKING,
    CANCELLED,
    EXPIRED,
}

@Serializable
data class StrategyUpdate(
    @SerialName("si")
    val strategyId: Long,
    @SerialName("st")
    val strategyType: String,
    @SerialName("ss")
    val strategyStatus: StrategyStatus,
    @SerialName("s")
    val symbol: String,
    @SerialName("ut")
    val updatedAt: Instant,
    @SerialName("c")
    val opCode: Int,
)

@Serializable
@SerialName("STRATEGY_UPDATE")
data class StrategyUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("su")
    val strategyUpdate: StrategyUpdate,
) : UserDataEvent

@Serializable
data class GridUpdate(
    @SerialName("si")
    val strategyId: Long,
    @SerialName("st")
    val strategyType: String,
    @SerialName("ss")
    val strategyStatus: StrategyStatus,
    @SerialName("s")
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("r")
    val realizedPnL: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("up")
    val unmatchedAveragePrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("uq")
    val unmatchedQuantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("uf")
    val unmatchedFee: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("mp")
    val matchedPnL: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("ut")
    val updatedAt: Instant,
)

@Serializable
@SerialName("GRID_UPDATE")
data class GridUpdateEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("gu")
    val gridUpdate: GridUpdate,
) : UserDataEvent

@Serializable
data class ConditionalOrderTriggerReject(
    @SerialName("s")
    val symbol: String,
    @SerialName("i")
    val orderId: Long,
    @SerialName("r")
    val rejectReason: String,
)

@Serializable
@SerialName("CONDITIONAL_ORDER_TRIGGER_REJECT")
data class ConditionalOrderTriggerRejectEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    override val timestamp: Instant,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("T")
    val transactedAt: Instant,
    @SerialName("or")
    val conditionalOrderTriggerReject: ConditionalOrderTriggerReject,
) : UserDataEvent

object AbstractUserDataEventSerializer :
    JsonContentPolymorphicSerializer<AbstractUserDataEvent>(AbstractUserDataEvent::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "stream" in element.jsonObject -> UserDataStreamExpiredEvent.serializer()
            else -> UserDataEvent.serializer()
        }
}

suspend fun Client.collectUserData(
    listenKey: String,
    onMessage: suspend (AbstractUserDataEvent) -> Unit,
) = client
    .wss("${configuration.websocketUrl}/ws/$listenKey") {
        while (true) {
            onMessage(receiveDeserialized<AbstractUserDataEvent>())
        }
    }

class CollectUserData :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val listenKey by argument()

    override fun run() =
        runBlocking {
            client.collectUserData(listenKey, ::println)
        }
}
