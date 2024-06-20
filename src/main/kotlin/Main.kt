package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addResourceSource
import dev.alonfalsing.common.EndpointConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import dev.alonfalsing.future.Client as FutureClient
import dev.alonfalsing.future.MainCommand as FutureCommand
import dev.alonfalsing.spot.Client as SpotClient
import dev.alonfalsing.spot.MainCommand as SpotCommand

data class ApplicationConfiguration(
    val binance: BinanceConfiguration,
)

data class BinanceConfiguration(
    val spot: EndpointConfiguration,
    val future: EndpointConfiguration,
)

class Application : CliktCommand() {
    init {
        subcommands(
            SpotCommand(),
            FutureCommand(),
        )
    }

    @OptIn(ExperimentalHoplite::class)
    override fun run() {
        startKoin {
            modules(
                module {
                    single {
                        ConfigLoaderBuilder
                            .default()
                            .addResourceSource("/application.override.yaml", true)
                            .addResourceSource("/application.yaml")
                            .withExplicitSealedTypes()
                            .strict()
                            .build()
                            .loadConfigOrThrow<ApplicationConfiguration>()
                    }
                    single(named("spot")) { get<ApplicationConfiguration>().binance.spot }
                    single(named("future")) { get<ApplicationConfiguration>().binance.future }
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
                        SpotClient(get(), get<ApplicationConfiguration>().binance.spot)
                    }
                    single {
                        FutureClient(get(), get<ApplicationConfiguration>().binance.future)
                    }
                },
            )
        }
    }
}

fun main(args: Array<String>) = Application().main(args)
