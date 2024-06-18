package dev.alonfalsing.spot

import dev.alonfalsing.common.EndpointConfiguration
import io.ktor.client.HttpClient

class Client(
    val client: HttpClient,
    val configuration: EndpointConfiguration,
)
