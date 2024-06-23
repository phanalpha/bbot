package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.alonfalsing.common.Credentials
import dev.alonfalsing.common.EndpointConfiguration
import io.ktor.client.HttpClient

class Client(
    val client: HttpClient,
    val configuration: EndpointConfiguration,
    val credentials: Credentials,
)

class MainCommand : CliktCommand(name = "future") {
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
            NewUserDataStream(),
            CollectUserData(),
            StartCopy(),
        )
    }

    override fun run() = Unit
}
