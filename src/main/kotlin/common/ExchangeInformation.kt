package dev.alonfalsing.common

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.jsonObject
import java.math.BigDecimal

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("filterType")
sealed interface SymbolFilter

@Serializable
@SerialName("PRICE_FILTER")
data class PriceFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val minPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val maxPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val tickSize: BigDecimal,
) : SymbolFilter

@Serializable
@SerialName("PERCENT_PRICE")
data class PercentPriceFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val multiplierUp: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val multiplierDown: BigDecimal,
    val avgPriceMins: Int? = null,
    val multiplierDecimal: Int? = null,
) : SymbolFilter

@Serializable
@SerialName("PERCENT_PRICE_BY_SIDE")
data class PercentPriceBySideFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val bidMultiplierUp: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val bidMultiplierDown: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val askMultiplierUp: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val askMultiplierDown: BigDecimal,
    val avgPriceMins: Int,
) : SymbolFilter

@Serializable
@SerialName("LOT_SIZE")
data class LotSizeFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val minQty: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val maxQty: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val stepSize: BigDecimal,
) : SymbolFilter

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("MIN_NOTIONAL")
data class MinNotionalFilter(
    @Serializable(with = BigDecimalSerializer::class)
    @JsonNames("notional")
    val minNotional: BigDecimal,
    val applyToMarket: Boolean? = null,
    val avgPriceMins: Int? = null,
) : SymbolFilter

@Serializable
@SerialName("NOTIONAL")
data class NotionalFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val minNotional: BigDecimal,
    val applyMinToMarket: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val maxNotional: BigDecimal,
    val applyMaxToMarket: Boolean,
    val avgPriceMins: Int,
) : SymbolFilter

@Serializable
@SerialName("ICEBERG_PARTS")
data class IcebergPartsFilter(
    val limit: Int,
) : SymbolFilter

@Serializable
@SerialName("MARKET_LOT_SIZE")
data class MarketLotSizeFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val minQty: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val maxQty: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val stepSize: BigDecimal,
) : SymbolFilter

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("MAX_NUM_ORDERS")
data class MaxNumOrdersFilter(
    @JsonNames("limit")
    val maxNumOrders: Int,
) : SymbolFilter

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@SerialName("MAX_NUM_ALGO_ORDERS")
data class MaxNumAlgoOrdersFilter(
    @JsonNames("limit")
    val maxNumAlgoOrders: Int,
) : SymbolFilter

@Serializable
@SerialName("MAX_NUM_ICEBERG_ORDERS")
data class MaxNumIcebergOrdersFilter(
    val maxNumIcebergOrders: Int,
) : SymbolFilter

@Serializable
@SerialName("MAX_POSITION")
data class MaxPositionFilter(
    @Serializable(with = BigDecimalSerializer::class)
    val maxPosition: BigDecimal,
) : SymbolFilter

@Serializable
@SerialName("TRAILING_DELTA")
data class TrailingDeltaFilter(
    val minTrailingAboveDelta: Int,
    val maxTrailingAboveDelta: Int,
    val minTrailingBelowDelta: Int,
    val maxTrailingBelowDelta: Int,
) : SymbolFilter

@Serializable
data class Symbol(
    val symbol: String,
    val status: String,
    val baseAsset: String,
    val baseAssetPrecision: Int,
    val quoteAsset: String,
    val quotePrecision: Int,
    val quoteAssetPrecision: Int? = null,
    val orderTypes: List<String>,
    val icebergAllowed: Boolean? = null,
    val ocoAllowed: Boolean? = null,
    val quoteOrderQtyMarketAllowed: Boolean? = null,
    val isSpotTradingAllowed: Boolean? = null,
    val isMarginTradingAllowed: Boolean? = null,
    val filters: List<SymbolFilter>,
) {
    fun filterPrice(price: BigDecimal): BigDecimal =
        (filters.find { it is PriceFilter } as PriceFilter).let { filter ->
            price
                .let { it - it % filter.tickSize }
                .coerceIn(filter.minPrice, filter.maxPrice)
                .setScale(8)
        }

    fun filterQuantity(quantity: BigDecimal): BigDecimal =
        (filters.find { it is LotSizeFilter } as LotSizeFilter).let { filter ->
            quantity
                .let { it - it % filter.stepSize }
                .coerceIn(filter.minQty, filter.maxQty)
                .setScale(8)
        }
}

@Serializable(with = ExchangeInformationSerializer::class)
sealed interface ExchangeInformationResponse

@Serializable
data class ExchangeInformation(
    val timezone: String,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val serverTime: Instant,
    val symbols: List<Symbol>,
) : ExchangeInformationResponse

object ExchangeInformationSerializer :
    JsonContentPolymorphicSerializer<ExchangeInformationResponse>(ExchangeInformationResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> ExchangeInformation.serializer()
        }
}
