package dev.alonfalsing.spot

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

class MainCommand : CliktCommand(name = "spot") {
    init {
        subcommands(
            GetExchangeInformation(),
            GetTick(),
            CollectTickers(),
            GetAccount(),
            GetWallet(),
            GetReferralIfNewUser(),
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
