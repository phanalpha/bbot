package dev.alonfalsing

import io.ktor.client.HttpClient

class SpotClient(
    val client: HttpClient,
    val configuration: SpotConfiguration,
)
