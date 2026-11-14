package casestudy1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VALUE OBJECT - an exact, non-negative amount of money (Q1/Q2).
 *
 * Matches the Word doc Refined Model skeleton:
 *   final class Money {
 *       private final long minor;            // amount in minor units
 *       private final Currency currency;
 *       static Money of(String decimal, Currency c) { ... }
 *           // rejects negatives and excess precision
 *       Money plus(Money o)  { requireSame(o); ... }
 *       Money times(Quantity q) { ... }
 *       Money minus(Percentage p) { ... rounds HALF_EVEN, result >= 0 }
 *       private void requireSame(Money o) {
 *           if (currency != o.currency) throw new CurrencyMismatch(); }
 *   }
 *
 * Integer arithmetic on minor is exact: 10c + 20c == 30c, always -
 * unlike double, where 0.1 + 0.2 != 0.3.
 */
public final class Money {

    private final long minor;               // amount in minor units
    private final Currency currency;

    private Money(long minor, Currency currency) {
        if (minor < 0) {
            throw new DomainException("Money must not be negative: " + minor
                    + " minor units");
        }
        this.minor = minor;
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    /**
     * Primary factory, exactly as in the doc: from a decimal string,
     * e.g. Money.of("10.50", Currency.EUR). Rejects negatives and
     * excess precision - "10.505" EUR (half a cent) is rejected,
     * not silently rounded.
     */
    public static Money of(String decimal, Currency c) {
        Objects.requireNonNull(decimal, "decimal");
        Objects.requireNonNull(c, "currency");
        final BigDecimal amount;
        try {
            amount = new BigDecimal(decimal.trim());
        } catch (NumberFormatException e) {
            throw new DomainException("Not a decimal amount: \"" + decimal + "\"");
        }
        if (amount.signum() < 0) {
            throw new DomainException("Money must not be negative: " + decimal);
        }
        try {
            long minor = amount.movePointRight(c.minorUnits())
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
            return new Money(minor, c);
        } catch (ArithmeticException e) {
            throw new DomainException("Amount " + decimal + " has excess precision - "
                    + "not exactly representable in " + c + " minor units");
        }
    }

    /**
     * HELPER factory (not in the doc skeleton) from a minor-unit count,
     * e.g. Money.ofMinor(1050, EUR) = 10.50 EUR. Used where a minor
     * count is what is naturally at hand: SignedMoney.requireNonNegative(),
     * ExchangeService results, and Money.zero().
     */
    public static Money ofMinor(long minor, Currency currency) {
        return new Money(minor, currency);
    }

    /** HELPER: zero in a currency, via ofMinor. */
    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public long minor() {
        return minor;
    }

    public Currency currency() {
        return currency;
    }

    private void requireSame(Money o) {
        Objects.requireNonNull(o, "other");
        if (currency != o.currency) {
            throw new CurrencyMismatch("Currency mismatch: cannot combine "
                    + currency + " with " + o.currency
                    + " - convert explicitly via ExchangeService first");
        }
    }

    /** Exact addition. Same currency only (doc signature) - see Q4. */
    public Money plus(Money o) {
        requireSame(o);
        return new Money(Math.addExact(minor, o.minor), currency);
    }

    /** Exact subtraction of another Money. Same currency, result >= 0. */
    public Money minus(Money o) {
        requireSame(o);
        long result = Math.subtractExact(minor, o.minor);
        if (result < 0) {
            throw new DomainException("Money.minus would go negative: "
                    + this + " - " + o);
        }
        return new Money(result, currency);
    }

    /**
     * This amount with a percentage discount taken off (doc signature).
     * The rounding rule lives in ONE place - Percentage.applyTo -
     * so this method simply delegates to it: both always agree.
     * Result is >= 0 because a Percentage is at most 100%.
     */
    public Money minus(Percentage p) {
        Objects.requireNonNull(p, "percentage");
        return p.applyTo(this);
    }

    /** Multiply by a Quantity (doc signature) - exact integer multiplication. */
    public Money times(Quantity q) {
        Objects.requireNonNull(q, "quantity");
        return new Money(Math.multiplyExact(minor, q.value()), currency);
    }

    /** Widen into the ledger world (Q5) - one-way, explicit. */
    public SignedMoney toSigned() {
        return SignedMoney.ofMinor(minor, currency);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money other
                && minor == other.minor
                && currency == other.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minor, currency);
    }

    /**
     * Readable form respecting the currency's minorUnits:
     * 1050 EUR -> "10.50 EUR", 500 JPY -> "500 JPY", 1250 KWD -> "1.250 KWD".
     */
    @Override
    public String toString() {
        if (currency.minorUnits() == 0) {
            return minor + " " + currency;
        }
        long factor = currency.factor();
        long major = minor / factor;
        long rest = minor % factor;
        return major + "." + String.format("%0" + currency.minorUnits() + "d", rest)
                + " " + currency;
    }
}
