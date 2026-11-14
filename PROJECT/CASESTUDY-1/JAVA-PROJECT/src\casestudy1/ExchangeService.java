package casestudy1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DOMAIN SERVICE - explicit currency conversion (Q4 extension).
 *
 * Conversion is a business event, not arithmetic, so it does NOT live
 * in Money.plus. Here the caller must supply, visibly:
 *  - the amount, the target currency,
 *  - the RATE to use (e.g. 1 EUR = 1.10 USD is rate 1.10), and
 *  - the DATE the rate is for (rateDate - kept as an auditable input;
 *    in production it keys the rate lookup / audit record).
 * Same inputs always give the same result - the operation is pure
 * and trivially testable, unlike a plus that secretly reads "today's
 * rate" from somewhere.
 */
public final class ExchangeService {

    public Money convert(Money amount, Currency target,
                         BigDecimal rate, LocalDate rateDate) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(rateDate, "rateDate");
        if (rate.signum() <= 0) {
            throw new DomainException("Exchange rate must be positive: " + rate);
        }
        if (amount.currency() == target) {
            return amount;
        }
        // minor -> major (source minorUnits) -> apply rate -> minor
        // (target minorUnits), rounding HALF_EVEN, the one documented rule.
        BigDecimal major = BigDecimal.valueOf(amount.minor())
                .movePointLeft(amount.currency().minorUnits());
        BigDecimal convertedMajor = major.multiply(rate);
        long targetMinor = convertedMajor.movePointRight(target.minorUnits())
                .setScale(0, RoundingMode.HALF_EVEN)
                .longValueExact();
        return Money.ofMinor(targetMinor, target);
    }
}
