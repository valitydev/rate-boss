package dev.vality.rateboss.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "CurrencyRates")
@JsonIgnoreProperties(ignoreUnknown = true)
data class NbkrDailyRatesXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "Date")
    val date: String? = null,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Currency")
    val currencies: List<NbkrCurrencyXml>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbkrCurrencyXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "ISOCode")
    val isoCode: String? = null,
    @field:JacksonXmlProperty(localName = "Nominal")
    val nominal: String? = null,
    @field:JacksonXmlProperty(localName = "Value")
    val value: String? = null,
)
