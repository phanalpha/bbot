package dev.alonfalsing.common

import io.ktor.http.ParametersBuilder
import io.ktor.http.formUrlEncode
import kotlinx.datetime.Clock
import org.kotlincrypto.macs.hmac.sha2.HmacSHA256

fun ParametersBuilder.appendTimestamp() =
    append(
        "timestamp",
        Clock.System
            .now()
            .toEpochMilliseconds()
            .toString(),
    )

@OptIn(ExperimentalStdlibApi::class)
fun ParametersBuilder.appendSignature(credentials: Credentials) =
    HmacSHA256(credentials.apiSecret.toByteArray()).let {
        it.update(build().formUrlEncode().toByteArray())
        append("signature", it.doFinal().toHexString())
    }
