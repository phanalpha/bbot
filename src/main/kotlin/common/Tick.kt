package dev.alonfalsing.common

import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.math.BigDecimal

@Serializable(with = TickSerializer::class)
sealed interface TickResponse

@Serializable
data class Tick(
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("time")
    val timestamp: Instant? = null,
) : TickResponse

object TickSerializer :
    JsonContentPolymorphicSerializer<TickResponse>(TickResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            "code" in element.jsonObject -> Error.serializer()
            else -> Tick.serializer()
        }
}

@Serializable(with = TickArrayResponseSerializer::class)
sealed interface TickArrayResponse

@Serializable(with = TickArraySerializer::class)
data class TickArray(
    val ticks: List<Tick>,
) : TickArrayResponse

object TickArraySerializer : KSerializer<TickArray> {
    private val delegateSerializer = serializer<List<Tick>>()

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("TickArray", delegateSerializer.descriptor)

    override fun serialize(
        encoder: Encoder,
        value: TickArray,
    ) = encoder.encodeSerializableValue(delegateSerializer, value.ticks)

    override fun deserialize(decoder: Decoder) = TickArray(decoder.decodeSerializableValue(delegateSerializer))
}

object TickArrayResponseSerializer :
    JsonContentPolymorphicSerializer<TickArrayResponse>(TickArrayResponse::class) {
    override fun selectDeserializer(element: JsonElement) =
        when {
            element is JsonObject -> Error.serializer()
            else -> TickArray.serializer()
        }
}

@Serializable
data class MiniTickerEvent(
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    @SerialName("E")
    val timestamp: Instant,
    @SerialName("s")
    val symbol: String,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("c")
    val closePrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("o")
    val openPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("h")
    val highPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("l")
    val lowPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("v")
    val baseAssetVolume: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    @SerialName("q")
    val quoteAssetVolume: BigDecimal,
)
