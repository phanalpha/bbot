package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.cancelOrder(
    symbol: String,
    orderId: Long? = null,
    clientOrderId: String? = null,
) = client
    .delete(configuration.baseUrl) {
        url {
            path("/fapi/v1/order")
            parameters.apply {
                append("symbol", symbol)
                orderId?.let { append("orderId", it.toString()) }
                clientOrderId?.let { append("origClientOrderId", it) }

                appendTimestamp()
                appendSignature(configuration)
            }
        }
        headers {
            append("X-MBX-APIKEY", configuration.apiKey)
        }
    }.body<OrderResponse>()

class CancelOrder :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val orderId by option().long()
    private val clientOrderId by option()

    override fun run() =
        runBlocking {
            client.cancelOrder(symbol, orderId, clientOrderId).let(::println)
        }
}
