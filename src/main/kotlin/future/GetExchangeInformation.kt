package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.alonfalsing.common.ExchangeInformation
import dev.alonfalsing.common.ExchangeInformationResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import dev.alonfalsing.common.Error as CommonError

suspend fun Client.getExchangeInformation(symbol: String) =
    client
        .get(configuration.baseUrl) {
            url {
                path("/fapi/v3/exchangeInfo")
                parameters.apply {
                    append("symbol", symbol)
                }
            }
        }.body<ExchangeInformationResponse>()
        .let {
            when (it) {
                is CommonError -> it
                is ExchangeInformation -> it.symbols.find { s -> s.symbol == symbol }!!
            }
        }

class GetExchangeInformation :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val symbol by argument()

    override fun run() =
        runBlocking {
            client.getExchangeInformation(symbol).let(::println)
        }
}
