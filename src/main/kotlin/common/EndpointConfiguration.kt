package dev.alonfalsing.common

data class EndpointConfiguration(
    val apiKey: String,
    val apiSecret: String,
    val baseUrl: String,
    val websocketUrl: String,
)
