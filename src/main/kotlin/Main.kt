package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ParametersBuilder
import io.ktor.http.formUrlEncode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256

data class BinanceConfiguration(
    val apiKey: String,
    val apiSecret: String,
    val baseUrl: String,
    val websocketUrl: String,
)

fun ParametersBuilder.appendTimestamp() =
    append(
        "timestamp",
        Clock.System
            .now()
            .toEpochMilliseconds()
            .toString(),
    )

@OptIn(ExperimentalStdlibApi::class)
fun ParametersBuilder.appendSignature(apiSecret: String) =
    HmacSHA256(apiSecret.toByteArray()).let {
        it.update(build().formUrlEncode().toByteArray())
        append("signature", it.doFinal().toHexString())
    }

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
        .subcommands(NewOrder())
        .subcommands(GetOrder())
        .subcommands(CancelOrder())
        .subcommands(CollectTrades())
        .subcommands(NewUserDataStream())
        .main(args)
