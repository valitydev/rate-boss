package dev.vality.rateboss.dao

import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest

interface ExRateDao {
    fun saveBatch(entities: List<ExRate>)

    fun getRecentBySymbolicCodes(request: GetCurrencyExchangeRateRequest): ExRate?

    fun getByCodesAndTimestamp(request: TimestampExchangeRateRequest): ExRate?
}
