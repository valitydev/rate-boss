package dev.vality.rateboss.client.cbr.adapter

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.bind.annotation.adapters.XmlAdapter

class CbrLocalDateXmlAdapter : XmlAdapter<String, LocalDate>() {

    override fun unmarshal(stringValue: String): LocalDate {
        return Optional.ofNullable(stringValue)
            .map { value: String? ->
                LocalDate.from(
                    DATE_FORMATTER.parse(value)
                )
            }
            .orElse(null)
    }

    override fun marshal(dateValue: LocalDate): String {
        return Optional.ofNullable(dateValue)
            .map { value: LocalDate? ->
                DATE_FORMATTER.format(
                    value
                )
            }
            .orElse(null)
    }

    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
