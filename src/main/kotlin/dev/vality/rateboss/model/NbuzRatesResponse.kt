package dev.vality.rateboss.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbuzRatesResponse(
    val data: List<NbuzRatesEntry>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbuzRatesEntry(
    val rates: List<NbuzRateItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbuzRateItem(
    @JsonProperty("rate_code")
    val rateCode: String? = null,
    @JsonProperty("rate_sb")
    val rateSb: String? = null,
    @JsonProperty("rate_equivalent")
    val rateEquivalent: String? = null,
)
