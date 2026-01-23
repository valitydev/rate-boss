package dev.vality.rateboss.dao

import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.converter.Constants.Companion.DATE_TIME_FORMAT
import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class ExRateDaoImpl(
    private val dsl: DSLContext,
) : ExRateDao {
    override fun saveBatch(entities: List<ExRate>) {
        val t = EX_RATE
        dsl
            .batch(
                entities.map { entity ->
                    dsl
                        .insertInto(
                            t,
                            t.SOURCE_CURRENCY_SYMBOLIC_CODE,
                            t.SOURCE_CURRENCY_EXPONENT,
                            t.DESTINATION_CURRENCY_SYMBOLIC_CODE,
                            t.DESTINATION_CURRENCY_EXPONENT,
                            t.RATIONAL_P,
                            t.RATIONAL_Q,
                            t.RATE_TIMESTAMP,
                            t.SOURCE,
                            t.REQUEST_DATE,
                        ).values(
                            entity.sourceCurrencySymbolicCode,
                            entity.sourceCurrencyExponent,
                            entity.destinationCurrencySymbolicCode,
                            entity.destinationCurrencyExponent,
                            entity.rationalP,
                            entity.rationalQ,
                            entity.rateTimestamp,
                            entity.source,
                            entity.requestDate,
                        )
                },
            ).execute()
    }

    override fun getRecentBySymbolicCodes(request: GetCurrencyExchangeRateRequest): ExRate? {
        val t = EX_RATE

        val targetRateDate =
            request.datetime
                ?.let {
                    LocalDateTime
                        .parse(it, DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
                        .toLocalDate()
                }

        var condition =
            t.DESTINATION_CURRENCY_SYMBOLIC_CODE
                .eq(request.currency_data.destination_currency)
                .and(t.SOURCE_CURRENCY_SYMBOLIC_CODE.eq(request.currency_data.source_currency))

        if (targetRateDate != null) {
            condition =
                condition.and(
                    DSL.cast(t.RATE_TIMESTAMP, SQLDataType.LOCALDATE).eq(targetRateDate)
                )
        }

        return dsl
            .selectFrom(t)
            .where(condition)
            .orderBy(t.RATE_TIMESTAMP.desc())
            .limit(1)
            .fetchOneInto(ExRate::class.java)
    }

    override fun getByCodesAndTimestamp(request: TimestampExchangeRateRequest): ExRate? {
        val t = EX_RATE
        val targetRateDate = request.rateTimestamp.toLocalDate()
        return dsl
            .selectFrom(t)
            .where(
                t.DESTINATION_CURRENCY_SYMBOLIC_CODE
                    .eq(request.destinationCurrency)
                    .and(t.SOURCE_CURRENCY_SYMBOLIC_CODE.eq(request.sourceCurrency))
                    .and(t.SOURCE.eq(request.source))
                    .and(DSL.cast(t.RATE_TIMESTAMP, SQLDataType.LOCALDATE).eq(targetRateDate)),
            ).orderBy(t.RATE_TIMESTAMP.desc())
            .limit(1)
            .fetchOneInto(ExRate::class.java)
    }
}
