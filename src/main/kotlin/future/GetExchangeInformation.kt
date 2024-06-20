package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import dev.alonfalsing.common.ExchangeInformation
import dev.alonfalsing.common.ExchangeInformationResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.getExchangeInformation() =
    client
        .get(configuration.baseUrl) {
            url {
                path("/fapi/v1/exchangeInfo")
            }
        }.body<ExchangeInformationResponse>()

class GetExchangeInformation :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument().optional()

    override fun run() =
        runBlocking {
            client
                .getExchangeInformation()
                .let {
                    if (it is ExchangeInformation && symbol != null) {
                        it.symbols.find { si -> si.symbol == symbol }
                    } else {
                        it
                    }
                }.let(::println)
        }
}
