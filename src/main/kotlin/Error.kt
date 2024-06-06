package dev.alonfalsing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val code: Int,
    @SerialName("msg")
    val message: String,
) : AccountResponse,
    OrderResponseAck,
    OrderResponseResult,
    OrderResponseFull,
    CancelOrderResponse,
    OrderResponse,
    OrderArrayResponse
