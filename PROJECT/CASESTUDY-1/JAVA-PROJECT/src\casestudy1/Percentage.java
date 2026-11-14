package casestudy1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VALUE OBJECT - a percentage in 0..100% (Q3).
 *
 * Matches the Word doc Refined Model skeleton:
 *   final class Percentage {
 *       private final int basisPoints;       // 0..10000
 *       static Percentage ofBasisPoints(int bp) {
 *           if (bp < 0 || bp > 10_000) throw ...; ... }
 *   }
 *
 * Stored as basis points: 1% = 100 bp, so 100% = 10_000 bp. Basis
 * points keep fractional percentages (e.g. 12.5% = 1250 bp) exact
 * in an int.
 *
 * The constructor is private (Q3): the ONLY way in is through a
 * validating factory, so the invariant 0..10_000 bp can never be
 * bypassed, and equal Percentages are interchangeable (equals/hashCode).
 */
public final class Percentage {

    private final int basisPoints;              // invariant: 0 .. 10_000

    private Percentage(int basisPoints) {
        if (basisPoints < 0 || basisPoints > 10_000) {
            throw new DomainException(
                    "Percentage out of range 0..100%: " + basisPoints + " bp");
        }
        this.basisPoints = basisPoints;
    }

    public static Percentage ofBasisPoints(int bp) {
        return new Percentage(bp);              // private constructor validates
    }

    /** Convenience factory: Percentage.ofPercent(15) = 15% = 1500 bp. */
    public static Percentage ofPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new DomainException("Percent out of range: " + percent);
        }
        return new Percentage(percent * 100);
    }

    public int basisPoints() {
        return basisPoints;
    }

    /**
     * Returns the DISCOUNTED price: the price after this percentage
     * is taken off - 15% applied to 100.00 EUR returns 85.00 EUR.
     *
     * This is the ONE place rounding happens, with the one documented
     * rule (HALF_EVEN):
     *   discounted = minor * (10_000 - basisPoints) / 10_000, HALF_EVEN
     * Money.minus(Percentage) delegates here, so the two always agree.
     * The result can never go below zero, since basisPoints <= 10_000.
     */
    public Money applyTo(Money price) {
        Objects.requireNonNull(price, "price");
        long discounted = BigDecimal.valueOf(price.minor())
                .multiply(BigDecimal.valueOf(10_000L - basisPoints))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_EVEN)
                .longValueExact();
        return Money.ofMinor(discounted, price.currency());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Percentage other && basisPoints == other.basisPoints;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(basisPoints);
    }

    @Override
    public String toString() {
        return (basisPoints / 100.0) + "%";
    }
}
