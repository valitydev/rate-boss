package dev.vality.rateboss.service

import dev.vality.exrates.base.Rational
import dev.vality.exrates.service.*
import dev.vality.rateboss.converter.GetCurrencyExchangeRateResultConverter
import dev.vality.rateboss.converter.TimestampExchangeRateRequestConverter
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ExRateServiceHandler(
    private val exRateDaoService: ExchangeDaoService,
    private val getCurrencyExchangeRateResultConverter: GetCurrencyExchangeRateResultConverter,
    private val conversionService: ConversionService,
    private val timestampExchangeRateRequestConverter: TimestampExchangeRateRequestConverter
) : ExchangeRateServiceSrv.Iface {

    override fun getExchangeRateData(request: GetCurrencyExchangeRateRequest): GetCurrencyExchangeRateResult {
        log.info("Get getExchangeRateData request with body: {} ", request)
        val exchangeRateData = exRateDaoService.getRecentExRateBySymbolicCodes(
            request.currencyData.sourceCurrency,
            request.currencyData.destinationCurrency
        )
        val result = exchangeRateData?.let {
            getCurrencyExchangeRateResultConverter.convert(exchangeRateData)
        } ?: throw ExRateNotFound()
        log.info("Result getExchangeRateData with body: {}", result)
        return result
    }

    override fun getConvertedAmount(sourceId: String, conversionRequest: ConversionRequest): Rational {
        log.info("Get getConvertedAmount request for source: {} with body: {} ", sourceId, conversionRequest)
        val exchangeTimestampRequest = timestampExchangeRateRequestConverter.convert(conversionRequest, sourceId)
        val exRateByTimestamp = exRateDaoService.getExRateByTimestamp(exchangeTimestampRequest)
        val result = exRateByTimestamp?.let {
            val convertedAmount = conversionService.convertAmount(conversionRequest.amount, exRateByTimestamp)
            Rational(convertedAmount.numeratorAsLong, convertedAmount.denominatorAsLong)
        } ?: throw ExRateNotFound()
        log.info("Result getConvertedAmount: {} ", result)
        return result
    }
}
