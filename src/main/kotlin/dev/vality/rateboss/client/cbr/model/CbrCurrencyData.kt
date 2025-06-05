package dev.vality.rateboss.client.cbr.model

import dev.vality.rateboss.client.cbr.adapter.CbrBigDecimalXmlAdapter
import java.math.BigDecimal
import javax.xml.bind.annotation.*
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlRootElement(name = "Valute")
@XmlAccessorType(XmlAccessType.FIELD)
class CbrCurrencyData {
    @XmlAttribute(name = "ID")
    var id: String? = null

    @XmlElement(name = "NumCode")
    var numCode: Int? = null

    @XmlElement(name = "CharCode")
    var charCode: String? = null

    @XmlElement(name = "Nominal")
    var nominal: Int? = null

    @XmlElement(name = "Name")
    var name: String? = null

    @XmlJavaTypeAdapter(CbrBigDecimalXmlAdapter::class)
    @XmlElement(name = "Value")
    var value: BigDecimal? = null
}
