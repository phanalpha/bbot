package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.alonfalsing.common.EndpointConfiguration
import io.ktor.client.HttpClient

class Client(
    val client: HttpClient,
    val configuration: EndpointConfiguration,
)

class MainCommand : CliktCommand(name = "future") {
    init {
        subcommands(
            GetExchangeInformation(),
            GetAccount(),
            NewUserDataStream(),
        )
    }

    override fun run() = Unit
}
