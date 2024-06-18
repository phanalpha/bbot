package dev.alonfalsing.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val code: Int = 0,
    @SerialName("msg")
    val message: String = "",
) : ExchangeInformationResponse
