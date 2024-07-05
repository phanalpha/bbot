package dev.alonfalsing.spot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.alonfalsing.common.appendSignature
import dev.alonfalsing.common.appendTimestamp
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.path
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun Client.getReferralIfNewUser(code: String) =
    client
        .get(configuration.baseUrl) {
            url {
                path("/sapi/v1/apiReferral/ifNewUser")
                parameters.apply {
                    append("apiAgentCode", code)

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

    override fun run() {
        runBlocking {
            client.getReferralIfNewUser(code).let(::println)
        }
    }
}
