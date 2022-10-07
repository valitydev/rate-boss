package dev.vality.rateboss.client.model

import java.math.BigDecimal
import java.time.LocalDate

data class FixerLatestResponse(
    val base: String?,
    val date: LocalDate?,
    val rates: Map<String, BigDecimal>?,
    val success: Boolean,
    val timestamp: Long?
)
