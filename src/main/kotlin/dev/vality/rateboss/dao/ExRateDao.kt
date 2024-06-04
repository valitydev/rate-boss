package dev.vality.rateboss.dao

import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest

interface ExRateDao {

    fun saveBatch(entities: List<ExRate>)

    fun getRecentBySymbolicCodes(sourceCode: String, destinationCode: String): ExRate?

    fun getByCodesAndTimestamp(request: TimestampExchangeRateRequest): ExRate?
}
