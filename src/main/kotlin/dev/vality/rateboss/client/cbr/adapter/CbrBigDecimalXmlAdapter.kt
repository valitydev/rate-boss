package dev.vality.rateboss.client.cbr.adapter

import java.math.BigDecimal
import javax.xml.bind.annotation.adapters.XmlAdapter

class CbrBigDecimalXmlAdapter : XmlAdapter<String, BigDecimal>() {
    override fun unmarshal(stringValue: String?): BigDecimal? =
        stringValue?.let {
            BigDecimal(it.replace(',', '.'))
        }

    override fun marshal(bigDecimalValue: BigDecimal?): String? = bigDecimalValue?.toString()?.replace('.', ',')
}
