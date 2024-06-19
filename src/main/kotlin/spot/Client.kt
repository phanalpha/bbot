package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.alonfalsing.common.EndpointConfiguration
import io.ktor.client.HttpClient

class Client(
    val client: HttpClient,
    val configuration: EndpointConfiguration,
)

class MainCommand : CliktCommand(name = "spot") {
    init {
        subcommands(
            GetExchangeInformation(),
            GetTick(),
            CollectTickers(),
            GetAccount(),
            NewOrder(),
            GetOrder(),
            CancelOrder(),
            GetTrades(),
            CollectTrades(),
            NewUserDataStream(),
            CollectUserData(),
            StartGrid(),
        )
    }

    override fun run() = Unit
}
