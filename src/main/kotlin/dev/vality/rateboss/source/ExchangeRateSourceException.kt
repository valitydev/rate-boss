package dev.vality.rateboss.source

class ExchangeRateSourceException(
    msg: String,
    cause: Throwable? = null
) : RuntimeException(msg, cause)
