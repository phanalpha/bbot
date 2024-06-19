package dev.alonfalsing.spot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val code: Int = 0,
    @SerialName("msg")
    val message: String = "",
) : AccountResponse,
    OrderAckResponse,
    OrderResultResponse,
    OrderFullResponse,
    OrderResponse,
    OrderArrayResponse,
    CancelOrderResponse,
    TradeArrayResponse
