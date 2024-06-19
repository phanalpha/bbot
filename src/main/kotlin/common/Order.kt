package dev.alonfalsing.common

enum class OrderSide {
    BUY,
    SELL,
}

enum class TimeInForce {
    GTC, // Good Till Cancel
    IOC, // Immediate Or Cancel
    FOK, // Fill or Kill
    GTX, // Good Till Crossing (Post Only)
    GTD, // Good Till Date
}

enum class OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    PENDING_CANCEL,
    REJECTED,
    EXPIRED,
    EXPIRED_IN_MATCH,
}

enum class SelfTradePreventionMode {
    NONE,
    EXPIRE_TAKER,
    EXPIRE_MAKER,
    EXPIRE_BOTH,
}

enum class OrderResponseType {
    ACK,
    RESULT,
    FULL,
}
