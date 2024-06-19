package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import dev.alonfalsing.common.TickArrayResponse
import dev.alonfalsing.common.TickResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.getTick(symbol: String) =
    client
        .get(configuration.baseUrl) {
            url {
                path("/api/v3/ticker/price")
                parameters.apply {
                    append("symbol", symbol)
                }
            }
        }.body<TickResponse>()

suspend fun Client.getTick() =
    client
        .get(configuration.baseUrl) {
            url {
                path("/api/v3/ticker/price")
            }
        }.body<TickArrayResponse>()

class GetTick :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument().optional()

    override fun run() =
        runBlocking {
            when (symbol) {
                null -> client.getTick() as Any
                else -> client.getTick(symbol!!)
            }.let(::println)
        }
}
