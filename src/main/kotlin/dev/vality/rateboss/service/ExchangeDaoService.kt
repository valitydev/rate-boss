package dev.vality.rateboss.service

import dev.vality.rateboss.converter.ExRateConverter
import dev.vality.rateboss.dao.ExRateDao
import dev.vality.rateboss.service.model.ExchangeRateData
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest
import dev.vality.rateboss.source.model.ExchangeRatesData
import mu.KotlinLogging
import org.apache.commons.math3.fraction.BigFraction
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ExchangeDaoService(
    private val exRateDao: ExRateDao,
    private val exRateConverter: ExRateConverter
) {

    fun saveExchangeRates(
        exchangeRatesData: ExchangeRatesData
    ) {
        val exRates = exchangeRatesData.exchangeRates.rates.map { exchangeRatesMap ->
            val exRate = exRateConverter.convert(
                exchangeRatesData.destinationCurrencySymbolicCode,
                exchangeRatesData.destinationCurrencyExponent,
                exchangeRatesMap,
                exchangeRatesData.exchangeRates.timestamp,
                exchangeRatesData.sourceId
            )
            exRate
        }
        log.debug("Try to save exRate batch {}", exRates)
        exRateDao.saveBatch(exRates)
        log.info("Successfully save exRate batch with size: {}", exRates.size)
    }

    fun getRecentExRateBySymbolicCodes(sourceCode: String, destinationCode: String): ExchangeRateData? {
        val exRate = exRateDao.getRecentBySymbolicCodes(sourceCode, destinationCode)
        return exRate?.let {
            ExchangeRateData(
                sourceCurrencySymbolicCode = it.sourceCurrencySymbolicCode,
                destinationCurrencySymbolicCode = it.destinationCurrencySymbolicCode,
                rationalP = it.rationalP,
                rationalQ = it.rationalQ,
                rateTimestamp = it.rateTimestamp,
                source = it.source
            )
        }
    }

    fun getExRateByTimestamp(request: TimestampExchangeRateRequest): BigFraction? {
        val exRate = exRateDao.getExRateByTimestamp(request)
        return exRate?.let {
            BigFraction(it.rationalP, it.rationalQ)
        }
    }
}
