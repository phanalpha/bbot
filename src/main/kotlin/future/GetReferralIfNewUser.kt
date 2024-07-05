package dev.alonfalsing.future

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.getReferralIfNewUser(
    code: String,
    coinMargined: Boolean,
) = client
    .get(configuration.baseUrl) {
        url {
            path("/fapi/v1/apiReferral/ifNewUser")
            parameters.apply {
                append("brokerId", code)
                append("type", if (coinMargined) "2" else "1")

                appendTimestamp()
                appendSignature(credentials)
            }
        }
        headers {
            append("X-MBX-APIKEY", credentials.apiKey)
        }
    }.bodyAsText()

class GetReferralIfNewUser :
    CliktCommand(),
    KoinComponent {
    private val client by inject<Client>()
    private val code by argument()
    private val coinMargined by option().flag()

    override fun run() {
        runBlocking {
            client.getReferralIfNewUser(code, coinMargined).let(::println)
        }
    }
}
