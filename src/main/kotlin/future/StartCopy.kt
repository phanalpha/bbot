package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.alonfalsing.common.EndpointConfiguration
import dev.alonfalsing.common.ExchangeInformation
import dev.alonfalsing.common.OrderStatus
import dev.alonfalsing.common.UserDataStream
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

class StartCopy :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val httpClient by inject<HttpClient>()
    private val configuration by inject<EndpointConfiguration>(qualifier = named("future"))
    private val multiplier by argument().convert { it.toBigDecimal() }.default(BigDecimal.ONE)
    private val masterKey by option(envvar = "MASTER_KEY").required()
    private val masterSecret by option(envvar = "MASTER_SECRET").required()

    override fun run() =
        runBlocking {
            val master =
                Client(
                    httpClient,
                    configuration.copy(
                        apiKey = masterKey,
                        apiSecret = masterSecret,
                    ),
                )
            val symbols = (client.getExchangeInformation() as ExchangeInformation).symbols
            val (listenKey) = master.newUserDataStream() as UserDataStream

            launch {
                while (true) {
                    delay(30.minutes)
                    client.keepUserDataStream(listenKey)
                }
            }

            master.collectUserData(listenKey) {
                println(it)

                if (it is OrderTradeUpdateEvent &&
                    it.orderUpdate.orderType == OrderType.MARKET &&
                    it.orderUpdate.orderStatus == OrderStatus.NEW
                ) {
                    client
                        .newOrder(
                            it.orderUpdate.symbol,
                            it.orderUpdate.side,
                            symbols.find { si -> si.symbol == it.orderUpdate.symbol }!!.filterQuantity(
                                it.orderUpdate.originalQuantity * multiplier,
                            ),
                            positionSide = it.orderUpdate.positionSide,
                        ).let(::println)
                }
            }
        }
}
