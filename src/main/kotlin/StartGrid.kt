package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

class StartGrid : CliktCommand() {
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
            val cli = currentContext.findObject<Application>()?.cli!!
            val si = cli.getExchangeInformation(symbol) as Symbol
            val (listenKey) = cli.newUserDataStream() as UserDataStream

            launch {
                while (true) {
                    delay(30.minutes)
                    cli.keepUserDataStream(listenKey)
                }
            }

            try {
                cli.collectUserData(listenKey, {
                    cli.newOrder<OrderResponseAck>(symbol, OrderSide.BUY, initial, quote = true).let {
                        println(it)
                        openOrders[(it as OrderAck).orderId] = GridOrder(OrderSide.BUY)
                    }
                }) { event ->
                    println(event)

                    if (event is ExecutionReportEvent && event.orderId in openOrders) {
                        when (event.status) {
                            OrderStatus.CANCELED, OrderStatus.REJECTED, OrderStatus.EXPIRED ->
                                openOrders.remove(event.orderId)

                            OrderStatus.PARTIALLY_FILLED ->
                                openOrders[event.orderId]?.fill(event)

                            OrderStatus.FILLED ->
                                fill(openOrders.remove(event.orderId)?.fill(event)!!, cli, si)

                            else -> Unit
                        }
                    }
                }
            } finally {
                for (orderId in openOrders.keys) {
                    cli.cancelOrder(symbol, orderId).let(::println)
                }
            }
        }

    private suspend fun fill(
        order: GridOrder,
        cli: Client,
        si: Symbol,
    ) {
        val d = order.price * dropRatio
        val p = si.filterPrice(order.price - d)

        when (order.side) {
            OrderSide.SELL -> {
                received += order.amount
                println("$received (+${order.amount}) / $spent")
                check(openOrders.count { it.value.side == OrderSide.SELL } > 0)

                cli
                    .newOrder<OrderResponseAck>(symbol, OrderSide.BUY, order.quantity, price = p)
                    .let {
                        println(it)
                        openOrders[(it as OrderAck).orderId] = GridOrder(OrderSide.BUY)
                    }
            }

            OrderSide.BUY -> {
                spent += order.amount
                println("$received / $spent (+${order.amount})")

                cli
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
                    cli
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
