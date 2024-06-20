package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.alonfalsing.common.EndpointConfiguration
import dev.alonfalsing.common.NewUserDataStreamResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

suspend fun Client.newUserDataStream() =
    client
        .post(configuration.baseUrl) {
            url {
                path("/fapi/v1/listenKey")
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<NewUserDataStreamResponse>()

suspend fun Client.keepUserDataStream(listenKey: String) =
    client
        .put(configuration.baseUrl) {
            url {
                path("/fapi/v1/listenKey")
                parameters.apply {
                    append("listenKey", listenKey)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<Error>()

suspend fun Client.closeUserDataStream(listenKey: String) =
    client
        .delete(configuration.baseUrl) {
            url {
                path("/fapi/v1/listenKey")
                parameters.apply {
                    append("listenKey", listenKey)
                }
            }
            headers {
                append("X-MBX-APIKEY", configuration.apiKey)
            }
        }.body<Error>()

class NewUserDataStream :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val httpClient by inject<HttpClient>()
    private val configuration by inject<EndpointConfiguration>(qualifier = named("future"))
    private val listenKey by option()
    private val close by option().flag()
    private val apiKey by option()
    private val apiSecret by option()

    override fun run() =
        runBlocking {
            (
                if (apiKey == null || apiSecret == null) {
                    client
                } else {
                    Client(
                        httpClient,
                        configuration.copy(
                            apiKey = apiKey!!,
                            apiSecret = apiSecret!!,
                        ),
                    )
                }
            ).let {
                when {
                    listenKey == null ->
                        it.newUserDataStream()

                    !close ->
                        it.keepUserDataStream(listenKey!!)

                    else ->
                        it.closeUserDataStream(listenKey!!)
                }
            }.let(::println)
        }
}
