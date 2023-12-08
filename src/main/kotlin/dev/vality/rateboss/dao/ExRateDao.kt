package dev.vality.rateboss.dao

import dev.vality.rateboss.dao.domain.tables.pojos.ExRate

interface ExRateDao {

    fun saveBatch(entities: List<ExRate>)

    fun getBySymbolicCodes(sourceCode: String, destinationCode: String): ExRate?
}
