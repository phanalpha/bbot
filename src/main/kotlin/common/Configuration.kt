package dev.alonfalsing.common

data class EndpointConfiguration(
    val baseUrl: String,
    val websocketUrl: String,
)

data class Credentials(
    val apiKey: String,
    val apiSecret: String,
)
