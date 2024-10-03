package dev.alonfalsing

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addFileSource
import com.sksamuel.hoplite.addResourceSource
import dev.alonfalsing.common.Credentials
import dev.alonfalsing.common.EndpointConfiguration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import dev.alonfalsing.future.Client as FutureClient
import dev.alonfalsing.future.MainCommand as FutureCommand
import dev.alonfalsing.spot.Client as SpotClient
import dev.alonfalsing.spot.MainCommand as SpotCommand

enum class Endpoint {
    SPOT,
    FUTURE,
}

data class ApplicationConfiguration(
    val production: BinanceConfiguration,
    val test: BinanceConfiguration,
    val credentials: Map<String, EnvironmentCredentials>,
)

data class BinanceConfiguration(
    val spot: EndpointConfiguration,
    val future: EndpointConfiguration,
)

data class EnvironmentCredentials(
    val production: Credentials?,
    val test: EndpointCredentials?,
) {
    val credentials: List<Pair<String, Credentials>>
        get() =
            listOfNotNull(production?.let { "production" to it }) +
                (test?.credentials?.map { (k, v) -> "test.$k" to v } ?: emptyList())

    fun getCredentials(
        environment: String,
        endpoint: Endpoint,
    ): Credentials? =
        when (environment) {
            "production" -> production
            else ->
                test?.let {
                    when (endpoint) {
                        Endpoint.SPOT -> it.spot
                        Endpoint.FUTURE -> it.future
                    }
                }
        }
}

data class EndpointCredentials(
    val spot: Credentials?,
    val future: Credentials?,
) {
    val credentials: List<Pair<String, Credentials>>
        get() =
            listOfNotNull(
                spot?.let { "spot" to it },
                future?.let { "future" to it },
            )
}

class ApiKeyOptions : OptionGroup() {
    val apiKey by option().required()
    val apiSecret by option().required()
}

class Application : CliktCommand() {
    private val environment by option("--test", "-t").flag().convert {
        if (!it) "production" else "test"
    }
    private val credentials by option("--credentials", "-c").default("default")
    private val apiKey by ApiKeyOptions().cooccurring()

    init {
        subcommands(
            SpotCommand(),
            FutureCommand(),
            CredentialsCommand(),
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
                            .addFileSource("application.yaml", true)
                            .addResourceSource("/application.yaml")
                            .withExplicitSealedTypes()
                            .strict()
                            .build()
                            .loadConfigOrThrow<ApplicationConfiguration>()
                    }
                    single {
                        get<ApplicationConfiguration>().let {
                            if (environment == "production") it.production else it.test
                        }
                    }
                    single(named("spot")) { get<BinanceConfiguration>().spot }
                    single(named("future")) { get<BinanceConfiguration>().future }
                    single { get<ApplicationConfiguration>().credentials[credentials]!! }
                    single(named("spot")) {
                        apiKey?.let { Credentials(it.apiKey, it.apiSecret) }
                            ?: get<EnvironmentCredentials>().getCredentials(environment, Endpoint.SPOT)
                    }
                    single(named("future")) {
                        apiKey?.let { Credentials(it.apiKey, it.apiSecret) }
                            ?: get<EnvironmentCredentials>().getCredentials(environment, Endpoint.FUTURE)
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
                        SpotClient(get(), get<EndpointConfiguration>(named("spot")), get(named("spot")))
                    }
                    single {
                        FutureClient(get(), get<EndpointConfiguration>(named("future")), get(named("future")))
                    }
                },
            )
        }
    }
}

class CredentialsCommand :
    CliktCommand(),
    KoinComponent {
    private val configuration by inject<ApplicationConfiguration>()

    override fun run() {
        configuration.credentials.forEach { (name, credentials) ->
            credentials.credentials.forEach { (k, v) ->
                println("$name.$k: ${v.apiKey}")
            }
        }
    }
}

fun main(args: Array<String>) = Application().main(args)
