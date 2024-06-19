package dev.alonfalsing.future

enum class OrderType {
    LIMIT,
    MARKET,
    STOP,
    STOP_MARKET,
    TAKE_PROFIT,
    TAKE_PROFIT_MARKET,
    TRAILING_STOP_MARKET,
}

enum class PositionSide {
    BOTH,
    LONG,
    SHORT,
}

enum class WorkingType {
    MARK_PRICE,
    CONTRACT_PRICE,
}
