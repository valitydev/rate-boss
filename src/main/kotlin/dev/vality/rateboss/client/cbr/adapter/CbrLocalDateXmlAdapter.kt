package dev.vality.rateboss.client.cbr.adapter

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.bind.annotation.adapters.XmlAdapter

class CbrLocalDateXmlAdapter : XmlAdapter<String, LocalDate>() {

    override fun unmarshal(stringValue: String?): LocalDate? {
        return stringValue?.let {
            LocalDate.from(DATE_FORMATTER.parse(it))
        }
    }

    override fun marshal(dateValue: LocalDate?): String? {
        return dateValue?.let {
            DATE_FORMATTER.format(it)
        }
    }

    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
