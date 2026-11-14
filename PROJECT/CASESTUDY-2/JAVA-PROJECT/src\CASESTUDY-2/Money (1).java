package casestudy2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VALUE OBJECT - an exact amount of money, as in the Word doc
 * Refined Model (reused from Case Study 1):
 *
 *   final class Money {
 *     private final long minor;            // amount in minor units
 *     private final Currency currency;
 *     static Money of(String decimal, Currency c) { ... }
 *     Money plus(Money o)  { requireSame(o); ... }
 *     Money times(Quantity q) { ... }
 *     private void requireSame(Money o) { if (currency != o.currency)
 *                                         throw new CurrencyMismatch(); }
 *   }
 *
 * Stored as a long counting MINOR units (cents / yen / fils) plus a
 * Currency. Integer arithmetic is exact - unlike double, where
 * 0.1 + 0.2 != 0.3. A Money is never negative: the one private
 * constructor rejects negatives, so a negative price cannot be built.
 */
public final class Money implements Comparable<Money> {

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
     * The Word doc factory: Money.of("10.50", Currency.EUR).
     * The amount must be exactly representable in the currency's minor
     * units - "10.505" EUR (half a cent) is rejected, not rounded -
     * and negatives are rejected by the private constructor.
     */
    public static Money of(String decimal, Currency c) {
        Objects.requireNonNull(decimal, "decimal amount");
        Objects.requireNonNull(c, "currency");
        try {
            long minorUnits = new BigDecimal(decimal.trim())
                    .movePointRight(c.minorUnits())
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
            return new Money(minorUnits, c);
        } catch (ArithmeticException | NumberFormatException e) {
            throw new DomainException("Amount \"" + decimal + "\" is not exactly "
                    + "representable in " + c + " minor units");
        }
    }

    /** Zero in the given currency - the identity for Order.total(). */
    public static Money zero(Currency currency) {
        return new Money(0, currency);
    }

    public long minor() {
        return minor;
    }

    public Currency currency() {
        return currency;
    }

    /** Word doc guard: different currencies can never be combined. */
    private void requireSame(Money o) {
        Objects.requireNonNull(o, "other amount");
        if (currency != o.currency) {
            throw new CurrencyMismatch();
        }
    }

    /** Exact addition. Same currency only - see requireSame. */
    public Money plus(Money o) {
        requireSame(o);
        return new Money(Math.addExact(minor, o.minor), currency);
    }

    /** Exact subtraction. The result may not go negative. */
    public Money minus(Money o) {
        requireSame(o);
        long result = Math.subtractExact(minor, o.minor);
        if (result < 0) {
            throw new DomainException("Money.minus would go negative: "
                    + this + " - " + o);
        }
        return new Money(result, currency);
    }

    /** Multiply by a Quantity - exact integer multiplication. */
    public Money times(Quantity q) {
        Objects.requireNonNull(q, "quantity");
        return new Money(Math.multiplyExact(minor, q.value()), currency);
    }

    @Override
    public int compareTo(Money other) {
        requireSame(other);
        return Long.compare(minor, other.minor);
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
     * Readable form respecting the currency scale:
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
