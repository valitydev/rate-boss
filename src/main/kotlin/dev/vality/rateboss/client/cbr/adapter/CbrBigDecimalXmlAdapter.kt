package dev.vality.rateboss.client.cbr.adapter

import java.math.BigDecimal
import java.util.*
import javax.xml.bind.annotation.adapters.XmlAdapter

class CbrBigDecimalXmlAdapter : XmlAdapter<String, BigDecimal>() {

    override fun unmarshal(stringValue: String): BigDecimal {
        return Optional.ofNullable(stringValue)
            .map { value: String ->
                BigDecimal(
                    value.replace(',', '.')
                )
            }
            .orElse(null)
    }

    override fun marshal(bigDecimalValue: BigDecimal): String {
        return Optional.ofNullable(bigDecimalValue)
            .map { value: BigDecimal ->
                value.toString().replace('.', ',')
            }
            .orElse(null)
    }
}
