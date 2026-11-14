package casestudy1;

/**
 * VALUE OBJECT (enumeration) - a currency and its precision.
 *
 * Matches the Word doc Refined Model skeleton exactly:
 *   enum Currency { EUR(2), USD(2), JPY(0), KWD(3); final int minorUnits; ... }
 *
 * The field is called minorUnits, as in the doc: it is the number of
 * decimal digits of minor units in one major unit - EUR/USD have 2
 * (cents), JPY has 0 (the yen is its own minor unit), KWD has 3 (fils).
 * Money formatting and decimal conversion both read it from here only.
 */
public enum Currency {
    EUR(2),
    USD(2),
    JPY(0),
    KWD(3);

    final int minorUnits;

    Currency(int minorUnits) {
        this.minorUnits = minorUnits;
    }

    /** Decimal digits of minor units per major unit: EUR 2, JPY 0, KWD 3. */
    public int minorUnits() {
        return minorUnits;
    }

    /** Helper: 10^minorUnits - e.g. EUR -> 100, JPY -> 1, KWD -> 1000. */
    public long factor() {
        long f = 1;
        for (int i = 0; i < minorUnits; i++) {
            f *= 10L;
        }
        return f;
    }
}
