package dev.vality.rateboss.dao

import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import org.jooq.DSLContext
import org.jooq.TableField
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
                    t.RATE_TIMESTAMP
                ).values(
                    entity.sourceCurrencySymbolicCode,
                    entity.sourceCurrencyExponent,
                    entity.destinationCurrencySymbolicCode,
                    entity.destinationCurrencyExponent,
                    entity.rationalP,
                    entity.rationalQ,
                    entity.rateTimestamp
                ).onConflict(t.SOURCE_CURRENCY_SYMBOLIC_CODE, t.DESTINATION_CURRENCY_SYMBOLIC_CODE).doUpdate()
                    .set(mapOnConflict(entity))
            }
        ).execute()
    }

    private fun mapOnConflict(entity: ExRate): MutableMap<TableField<*, *>, Any> {
        val t = EX_RATE
        val onConflictMap = mutableMapOf<TableField<*, *>, Any>().apply {
            put(t.RATIONAL_P, entity.rationalP)
            put(t.RATIONAL_Q, entity.rationalQ)
            put(t.RATE_TIMESTAMP, entity.rateTimestamp)
        }
        return onConflictMap
    }

    override fun getBySymbolicCodes(sourceCode: String, destinationCode: String): ExRate? {
        val t = EX_RATE
        return dsl.selectFrom(t)
            .where(
                t.DESTINATION_CURRENCY_SYMBOLIC_CODE.eq(destinationCode)
                    .and(t.SOURCE_CURRENCY_SYMBOLIC_CODE.eq(sourceCode))
            ).fetchOneInto(ExRate::class.java)
    }
}
