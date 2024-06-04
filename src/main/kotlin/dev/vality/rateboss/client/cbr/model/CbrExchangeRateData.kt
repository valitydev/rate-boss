package dev.vality.rateboss.client.cbr.model

import dev.vality.rateboss.client.cbr.adapter.CbrLocalDateXmlAdapter
import java.time.LocalDate
import javax.xml.bind.annotation.*
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlRootElement(name = "ValCurs")
@XmlAccessorType(XmlAccessType.FIELD)
class CbrExchangeRateData {

    @XmlAttribute
    var name: String? = null

    @XmlAttribute(name = "Date")
    @XmlJavaTypeAdapter(CbrLocalDateXmlAdapter::class)
    var date: LocalDate? = null

    @XmlElement(name = "Valute")
    var currencies: List<CbrCurrencyData>? = null
}
