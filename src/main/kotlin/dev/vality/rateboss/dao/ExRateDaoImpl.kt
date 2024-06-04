package dev.vality.rateboss.dao

import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ExRateDaoImpl(
    private val dsl: DSLContext
) : ExRateDao {
    override fun saveBatch(entities: List<ExRate>) {
        val t = EX_RATE
        dsl.batch(
            entities.map { entity ->
                dsl.insertInto(
                    t,
                    t.SOURCE_CURRENCY_SYMBOLIC_CODE,
                    t.SOURCE_CURRENCY_EXPONENT,
                    t.DESTINATION_CURRENCY_SYMBOLIC_CODE,
                    t.DESTINATION_CURRENCY_EXPONENT,
                    t.RATIONAL_P,
                    t.RATIONAL_Q,
                    t.RATE_TIMESTAMP,
                    t.SOURCE
                ).values(
                    entity.sourceCurrencySymbolicCode,
                    entity.sourceCurrencyExponent,
                    entity.destinationCurrencySymbolicCode,
                    entity.destinationCurrencyExponent,
                    entity.rationalP,
                    entity.rationalQ,
                    entity.rateTimestamp,
                    entity.source
                )
            }
        ).execute()
    }

    override fun getRecentBySymbolicCodes(sourceCode: String, destinationCode: String): ExRate? {
        val t = EX_RATE
        return dsl.selectFrom(t)
            .where(
                t.DESTINATION_CURRENCY_SYMBOLIC_CODE.eq(destinationCode)
                    .and(t.SOURCE_CURRENCY_SYMBOLIC_CODE.eq(sourceCode))
            ).orderBy(t.RATE_TIMESTAMP.desc())
            .limit(1)
            .fetchOneInto(ExRate::class.java)
    }

    override fun getByCodesAndTimestamp(request: TimestampExchangeRateRequest): ExRate? {
        val t = EX_RATE
        return dsl.selectFrom(t)
            .where(
                t.DESTINATION_CURRENCY_SYMBOLIC_CODE.eq(request.destinationCurrency)
                    .and(t.SOURCE_CURRENCY_SYMBOLIC_CODE.eq(request.sourceCurrency))
                    .and(t.SOURCE.eq(request.source))
                    .and(t.RATE_TIMESTAMP.le(request.rateTimestamp))
            ).orderBy(t.RATE_TIMESTAMP.desc())
            .limit(1)
            .fetchOneInto(ExRate::class.java)
    }
}
