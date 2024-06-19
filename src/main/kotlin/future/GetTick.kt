package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.alonfalsing.common.TickArrayResponse
import dev.alonfalsing.common.TickResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.getTick(
    symbol: String,
    realtime: Boolean = false,
) = client
    .get(configuration.baseUrl) {
        url {
            path(
                if (realtime) "/fapi/v2/ticker/price" else "/fapi/v1/ticker/price",
            )
            parameters.apply {
                append("symbol", symbol)
            }
        }
    }.body<TickResponse>()

suspend fun Client.getTick(realtime: Boolean = false) =
    client
        .get(configuration.baseUrl) {
            url {
                path(
                    if (realtime) "/fapi/v2/ticker/price" else "/fapi/v1/ticker/price",
                )
            }
        }.body<TickArrayResponse>()

class GetTick :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument().optional()
    private val realtime by option().flag()

    override fun run() =
        runBlocking {
            when (symbol) {
                null -> client.getTick(realtime) as Any
                else -> client.getTick(symbol!!, realtime)
            }.let(::println)
        }
}
