package dev.vality.rateboss.service

import dev.vality.exrates.service.ExRateNotFound
import dev.vality.exrates.service.ExchangeRateServiceSrv
import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.exrates.service.GetCurrencyExchangeRateResult
import dev.vality.rateboss.converter.GetCurrencyExchangeRateResultConverter
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ExRateServiceHandler(
    private val exRateDaoService: ExchangeDaoService,
    private val getCurrencyExchangeRateResultConverter: GetCurrencyExchangeRateResultConverter
) : ExchangeRateServiceSrv.Iface {

    override fun getExchangeRateData(request: GetCurrencyExchangeRateRequest): GetCurrencyExchangeRateResult {
        log.info("Get getExchangeRateData request with body: {} ", request)
        val exchangeRateData = exRateDaoService.getExRateBySymbolicCodes(
            request.currencyData.sourceCurrency,
            request.currencyData.destinationCurrency
        )
        val result = exchangeRateData?.let {
            getCurrencyExchangeRateResultConverter.convert(exchangeRateData)
        } ?: throw ExRateNotFound()
        log.info("Result getExchangeRateData with body: {}", result)
        return result
    }
}
