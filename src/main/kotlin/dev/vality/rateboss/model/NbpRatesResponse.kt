package dev.vality.rateboss.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbpRatesResponse(
    val effectiveDate: String? = null,
    val rates: List<NbpRateItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbpRateItem(
    val mid: BigDecimal? = null,
    val code: String? = null,
)
