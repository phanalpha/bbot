package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

data class BinanceConfiguration(
    val apiKey: String,
    val apiSecret: String,
    val baseUrl: String,
    val websocketUrl: String,
)

data class ApplicationConfiguration(
    val binance: BinanceConfiguration,
)

class Application : CliktCommand() {
    private lateinit var _configuration: ApplicationConfiguration
    private lateinit var _client: HttpClient

    override fun run() {
        _configuration =
            ConfigLoaderBuilder
                .default()
                .addResourceSource("/application.override.yaml", true)
                .addResourceSource("/application.yaml")
                .strict()
                .build()
                .loadConfigOrThrow<ApplicationConfiguration>()

        _client =
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

        currentContext.obj = this
    }

    val configuration get() = _configuration
    val client get() = _client
}

fun main(args: Array<String>) =
    Application()
        .subcommands(GetAccount())
        .subcommands(CollectTrades())
        .main(args)
