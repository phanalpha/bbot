package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import dev.alonfalsing.common.BigDecimalSerializer
import dev.alonfalsing.common.InstantEpochMillisecondsSerializer
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.call.body
import io.ktor.client.request.get
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

@Serializable(with = AccountResponseSerializer::class)
sealed interface AccountResponse

@Serializable
data class Balance(
    val asset: String,
    @Serializable(with = BigDecimalSerializer::class)
    val walletBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val unrealizedProfit: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val marginBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("maintMargin")
    val maintenanceMarginBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val initialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val positionInitialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val openOrderInitialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val crossWalletBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val crossUnPnl: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val availableBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val maxWithdrawAmount: BigDecimal,
    val marginAvailable: Boolean,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val updateTime: Instant,
)

@Serializable
data class Position(
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    val initialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("maintMargin")
    val maintenanceMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val unrealizedProfit: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val positionInitialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val openOrderInitialMargin: BigDecimal,
    val leverage: Int,
    val isolated: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val entryPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val maxNotional: BigDecimal,
    val positionSide: PositionSide,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("positionAmt")
    val positionAmount: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val updateTime: Instant,
)

@Serializable
data class Account(
    val assets: List<Balance>,
    val positions: List<Position>,
    val feeTier: Int,
    val feeBurn: Boolean,
    val canTrade: Boolean,
    val canDeposit: Boolean,
    val canWithdraw: Boolean,
    val multiAssetsMargin: Boolean,
    val tradeGroupId: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val totalInitialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("totalMaintMargin")
    val totalMaintenanceMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalWalletBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalUnrealizedProfit: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalMarginBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalPositionInitialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalOpenOrderInitialMargin: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalCrossWalletBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val totalCrossUnPnl: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val availableBalance: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val maxWithdrawAmount: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val updateTime: Instant,
) : AccountResponse

object AccountResponseSerializer :
    JsonContentPolymorphicSerializer<AccountResponse>(AccountResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> Account.serializer()
        }
}

suspend fun Client.getAccount() =
    client
        .get(configuration.baseUrl) {
            url {
                path("/fapi/v2/account")
                parameters.apply {
                    append("omitZeroBalances", "true")

                    appendTimestamp()
                    appendSignature(configuration)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<AccountResponse>()

class GetAccount :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument().optional()

    override fun run() =
        runBlocking {
            client
                .getAccount()
                .let {
                    if (it is Account && symbol != null) {
                        it.assets.find { balance -> balance.asset == symbol }
                            ?: it.positions.find { position -> position.symbol == symbol }
                    } else {
                        it
                    }
                }.let(::println)
        }
}
