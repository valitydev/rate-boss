package dev.vality.rateboss.source

class ExchangeRateSourceException(
    val currency: String,
    msg: String,
    cause: Throwable? = null
) : RuntimeException(msg, cause)
