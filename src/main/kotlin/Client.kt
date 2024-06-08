package dev.alonfalsing

import io.ktor.client.HttpClient

class Client(
    val client: HttpClient,
    val configuration: BinanceConfiguration,
)
