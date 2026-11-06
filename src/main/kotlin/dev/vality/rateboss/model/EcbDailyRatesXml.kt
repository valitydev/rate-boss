package dev.vality.rateboss.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.math.BigDecimal

@JacksonXmlRootElement(localName = "Envelope")
@JsonIgnoreProperties(ignoreUnknown = true)
data class EcbDailyRatesXml(
    @field:JacksonXmlProperty(localName = "Cube")
    val cube: EcbCubeXml? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EcbCubeXml(
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Cube")
    val tables: List<EcbRateTableXml>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EcbRateTableXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "time")
    val date: String? = null,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Cube")
    val rates: List<EcbRateItemXml>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EcbRateItemXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "currency")
    val code: String? = null,
    @field:JacksonXmlProperty(isAttribute = true, localName = "rate")
    val rate: BigDecimal? = null,
)
