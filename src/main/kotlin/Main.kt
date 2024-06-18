package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import dev.alonfalsing.common.EndpointConfiguration
import dev.alonfalsing.spot.CancelOrder
import dev.alonfalsing.spot.Client
import dev.alonfalsing.spot.CollectTrades
import dev.alonfalsing.spot.CollectUserData
import dev.alonfalsing.spot.GetAccount
import dev.alonfalsing.spot.GetExchangeInformation
import dev.alonfalsing.spot.GetOrder
import dev.alonfalsing.spot.GetTrades
import dev.alonfalsing.spot.NewOrder
import dev.alonfalsing.spot.NewUserDataStream
import dev.alonfalsing.spot.StartGrid
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.dsl.module

data class ApplicationConfiguration(
    val binance: BinanceConfiguration,
)

data class BinanceConfiguration(
    val spot: EndpointConfiguration,
    val future: EndpointConfiguration,
)

class Application : CliktCommand() {
    override fun run() {
        startKoin {
            modules(
                module {
                    single {
                        ConfigLoaderBuilder
                            .default()
                            .addResourceSource("/application.override.yaml", true)
                            .addResourceSource("/application.yaml")
                            .strict()
                            .build()
                            .loadConfigOrThrow<ApplicationConfiguration>()
                    }
                    single {
                        HttpClient(CIO) {
                            install(ContentNegotiation) {
                                json(
                                    Json {
                                        ignoreUnknownKeys = true
                                    },
                                )
                            }
                            install(WebSockets) {
                                contentConverter =
                                    KotlinxWebsocketSerializationConverter(
                                        Json {
                                            ignoreUnknownKeys = true
                                        },
                                    )
                            }
                        }
                    }
                    single {
                        Client(get(), get<ApplicationConfiguration>().binance.spot)
                    }
                },
            )
        }
    }
}

fun main(args: Array<String>) =
    Application()
        .subcommands(GetAccount())
        .subcommands(NewOrder())
        .subcommands(GetOrder())
        .subcommands(CancelOrder())
        .subcommands(CollectTrades())
        .subcommands(NewUserDataStream())
        .subcommands(CollectUserData())
        .subcommands(StartGrid())
        .subcommands(GetTrades())
        .subcommands(GetExchangeInformation())
        .main(args)
