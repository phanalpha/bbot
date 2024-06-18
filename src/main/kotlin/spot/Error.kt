package dev.alonfalsing.spot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val code: Int = 0,
    @SerialName("msg")
    val message: String = "",
) : AccountResponse,
    OrderResponseAck,
    OrderResponseResult,
    OrderResponseFull,
    CancelOrderResponse,
    OrderResponse,
    OrderArrayResponse,
    TradeArrayResponse,
    NewUserDataStreamResponse,
    ExchangeInformationResponse
