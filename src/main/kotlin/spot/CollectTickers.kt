package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import dev.alonfalsing.common.MiniTickerEvent
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.wss
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

fun Client.collectMiniTickers(symbol: String) =
    flow {
        client.wss("${configuration.websocketUrl}/ws/${symbol.lowercase()}@miniTicker") {
            while (true) {
                emit(receiveDeserialized<MiniTickerEvent>())
            }
        }
    }

fun Client.collectMiniTickers() =
    flow {
        client.wss("${configuration.websocketUrl}/ws/!miniTicker@arr") {
            while (true) {
                emit(receiveDeserialized<List<MiniTickerEvent>>())
            }
        }
    }

class CollectTickers :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument().optional()

    override fun run() =
        runBlocking {
            when (symbol) {
                null -> client.collectMiniTickers()
                else -> client.collectMiniTickers(symbol!!)
            }.collect(::println)
        }
}
