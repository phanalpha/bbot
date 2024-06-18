package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes

class GridOrder(
    val side: OrderSide,
) {
    private var _amount: BigDecimal = BigDecimal.ZERO
    private var _quantity: BigDecimal = BigDecimal.ZERO

    val amount get() = _amount
    val quantity get() = _quantity
    val price get() = _amount / _quantity

    fun fill(x: ExecutionReportEvent): GridOrder {
        _amount += x.lastQuoteQuantity
        _quantity += x.lastExecutedQuantity

        return this
    }
}

interface GridMessage {
    val timestamp: Instant
}

data class BeginMessage(
    override val timestamp: Instant,
) : GridMessage

class StartGrid :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val initial by argument().convert { it.toBigDecimal() }
    private val dropRatio by argument().convert { it.toBigDecimal() }
    private val multiplier by argument().convert { it.toBigDecimal() }
    private val budget by argument().convert { it.toBigDecimal() }

    private val openOrders = mutableMapOf<Long, GridOrder>()
    private var spent = BigDecimal.ZERO
    private var received = BigDecimal.ZERO

    override fun run() =
        runBlocking {
            val ch = Channel<GridMessage>()
            val si = client.getExchangeInformation(symbol) as Symbol
            val (listenKey) = client.newUserDataStream() as UserDataStream

            val stream =
                launch {
                    launch {
                        while (true) {
                            delay(30.minutes)
                            client.keepUserDataStream(listenKey)
                        }
                    }

                    client.collectUserData(listenKey, {
                        ch.send(BeginMessage(Clock.System.now()))
                    }) {
                        println(it)

                        if (it is ExecutionReportEvent) {
                            ch.send(it)
                        }
                    }
                }

            while (true) {
                when (val event = ch.receive()) {
                    is BeginMessage -> {
                        client.newOrder<OrderResponseAck>(symbol, OrderSide.BUY, initial, quote = true).let {
                            println(it)
                            openOrders[(it as OrderAck).orderId] = GridOrder(OrderSide.BUY)
                        }
                    }

                    is ExecutionReportEvent ->
                        if (event.orderId in openOrders) {
                            when (event.status) {
                                OrderStatus.CANCELED, OrderStatus.REJECTED, OrderStatus.EXPIRED -> {
                                    openOrders.remove(event.orderId)
                                    if (openOrders.all { it.value.side != OrderSide.SELL }) break
                                }

                                OrderStatus.PARTIALLY_FILLED ->
                                    openOrders[event.orderId]?.fill(event)

                                OrderStatus.FILLED -> {
                                    fill(openOrders.remove(event.orderId)?.fill(event)!!, si)
                                    if (openOrders.all { it.value.side != OrderSide.SELL }) break
                                }

                                else -> Unit
                            }
                        }
                }
            }

            stream.cancel()
            for (orderId in openOrders.keys) {
                client.cancelOrder(symbol, orderId).let(::println)
            }
        }

    private suspend fun fill(
        order: GridOrder,
        si: Symbol,
    ) {
        val d = order.price * dropRatio
        val p = si.filterPrice(order.price - d)

        when (order.side) {
            OrderSide.SELL -> {
                received += order.amount
                println("$received (+${order.amount}) / $spent")

                if (openOrders.any { it.value.side == OrderSide.SELL }) {
                    client
                        .newOrder<OrderResponseAck>(symbol, OrderSide.BUY, order.quantity, price = p)
                        .let {
                            println(it)
                            openOrders[(it as OrderAck).orderId] = GridOrder(OrderSide.BUY)
                        }
                }
            }

            OrderSide.BUY -> {
                spent += order.amount
                println("$received / $spent (+${order.amount})")

                client
                    .newOrder<OrderResponseAck>(
                        symbol,
                        OrderSide.SELL,
                        order.quantity,
                        price = si.filterPrice(order.price + d),
                    ).let {
                        println(it)
                        openOrders[(it as OrderAck).orderId] = GridOrder(OrderSide.SELL)
                    }

                val q = si.filterQuantity(order.quantity * multiplier)
                if (spent + p * q - received < budget) {
                    client
                        .newOrder<OrderResponseAck>(symbol, OrderSide.BUY, q, price = p)
                        .let {
                            println(it)
                            openOrders[(it as OrderAck).orderId] = GridOrder(OrderSide.BUY)
                        }
                }
            }
        }
    }
}
