package dev.vality.rateboss.service

import dev.vality.rateboss.converter.ExRateConverter
import dev.vality.rateboss.dao.ExRateDao
import dev.vality.rateboss.source.model.ExchangeRateData
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class ExchangeDaoService(
    private val exRateDao: ExRateDao,
    private val exRateConverter: ExRateConverter
) {

    fun saveExchangeRates(
        baseCurrencySymbolCode: String,
        baseCurrencyExponent: Short,
        exchangeRates: ExchangeRates
    ) {
        val exRates = exchangeRates.rates.map { exchangeRatesMap ->
            val exRate = exRateConverter.convert(
                baseCurrencySymbolCode,
                baseCurrencyExponent,
                exchangeRatesMap,
                exchangeRates.timestamp
            )
            exRate
        }
        log.debug("Try to save exRate batch {}", exRates)
        exRateDao.saveBatch(exRates)
        log.info("Successfully save exRate batch with size: {}", exRates.size)
    }

    fun getExRateBySymbolicCodes(sourceCode: String, destinationCode: String): ExchangeRateData? {
        val exRate = exRateDao.getBySymbolicCodes(sourceCode, destinationCode)
        return exRate?.let {
            ExchangeRateData(
                sourceCurrencySymbolicCode = exRate.sourceCurrencySymbolicCode,
                destinationCurrencySymbolicCode = exRate.destinationCurrencySymbolicCode,
                rationalP = exRate.rationalP,
                rationalQ = exRate.rationalQ,
                rateTimestamp = exRate.rateTimestamp
            )
        }
    }
}
