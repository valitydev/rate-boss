package dev.vality.rateboss.converter

import dev.vality.exrates.service.ConversionRequest
import dev.vality.rateboss.converter.Constants.Companion.DATE_TIME_FORMAT
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class TimestampExchangeRateRequestConverter {

    fun convert(
        conversionRequest: ConversionRequest,
        sourceId: String
    ): TimestampExchangeRateRequest {
        return TimestampExchangeRateRequest(
            sourceCurrency = conversionRequest.source,
            destinationCurrency = conversionRequest.destination,
            source = sourceId,
            rateTimestamp = LocalDateTime.parse(
                conversionRequest.datetime,
                DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)
            )
        )
    }
}
