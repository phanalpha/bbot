package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import dev.alonfalsing.spot.OrderArrayResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.getOrder(
    symbol: String,
    orderId: Long?,
    clientOrderId: String?,
) = client
    .get(configuration.baseUrl) {
        url {
            path("/fapi/v1/order")
            parameters.apply {
                append("symbol", symbol)
                orderId?.let { append("orderId", it.toString()) }
                clientOrderId?.let { append("origClientOrderId", it) }

                appendTimestamp()
                appendSignature(credentials)
            }
        }
        headers {
            append("X-MBX-APIKEY", credentials.apiKey)
        }
    }.body<OrderResponse>()

suspend fun Client.getOrders(
    symbol: String,
    orderId: Long? = null,
    limit: Int? = null,
) = client
    .get(configuration.baseUrl) {
        url {
            path("/fapi/v1/openOrders")
            parameters.apply {
                append("symbol", symbol)
                orderId?.let { append("orderId", it.toString()) }
                limit?.let { append("limit", it.toString()) }

                appendTimestamp()
                appendSignature(credentials)
            }
        }
        headers {
            append("X-MBX-APIKEY", credentials.apiKey)
        }
    }.body<OrderArrayResponse>()

class GetOrder :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()
    private val orderId by option().long()
    private val clientOrderId by option()
    private val fromId by option().long()
    private val limit by option().int()

    override fun run() =
        runBlocking {
            when {
                orderId != null || clientOrderId != null ->
                    client.getOrder(symbol, orderId, clientOrderId) as Any

                else ->
                    client.getOrders(symbol, fromId, limit)
            }.let(::println)
        }
}
