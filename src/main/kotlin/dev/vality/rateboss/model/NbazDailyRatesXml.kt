package dev.vality.rateboss.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "ValCurs")
@JsonIgnoreProperties(ignoreUnknown = true)
data class NbazDailyRatesXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "Date")
    val date: String? = null,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "ValType")
    val valTypes: List<NbazValTypeXml>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbazValTypeXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "Type")
    val type: String? = null,
    @field:JacksonXmlElementWrapper(useWrapping = false)
    @field:JacksonXmlProperty(localName = "Valute")
    val valutes: List<NbazValuteXml>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NbazValuteXml(
    @field:JacksonXmlProperty(isAttribute = true, localName = "Code")
    val code: String? = null,
    @field:JacksonXmlProperty(localName = "Nominal")
    val nominal: String? = null,
    @field:JacksonXmlProperty(localName = "Value")
    val value: String? = null,
)
