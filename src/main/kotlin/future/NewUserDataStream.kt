package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.alonfalsing.common.NewUserDataStreamResponse
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.newUserDataStream() =
    client
        .post(configuration.baseUrl) {
            url {
                path("/fapi/v1/listenKey")
            }
            headers {
                append("X-MBX-APIKEY", credentials.apiKey)
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
                append("X-MBX-APIKEY", credentials.apiKey)
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
                append("X-MBX-APIKEY", credentials.apiKey)
            }
        }.body<Error>()

class NewUserDataStream :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val listenKey by option()
    private val close by option().flag()

    override fun run() =
        runBlocking {
            client
                .let {
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
