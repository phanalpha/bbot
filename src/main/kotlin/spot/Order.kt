package dev.alonfalsing.spot

enum class OrderType {
    LIMIT,
    MARKET,
    STOP_LOSS,
    STOP_LOSS_LIMIT,
    TAKE_PROFIT,
    TAKE_PROFIT_LIMIT,
    LIMIT_MAKER,
}

enum class ExecutionType {
    NEW,
    CANCELED,
    REPLACED,
    REJECTED,
    TRADE,
    EXPIRED,
    TRADE_PREVENTION,
}
