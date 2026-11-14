package casestudy2;

/**
 * VALUE OBJECT (enumeration) - a currency and its precision, exactly as
 * in the Case Study 1 / Case Study 2 Word doc Refined Model:
 *
 *   enum Currency { EUR(2), USD(2), JPY(0), KWD(3); final int minorUnits; ... }
 *
 * The number is the count of minor-unit digits: EUR/USD have 2 (cents),
 * JPY has 0 (the yen is the minor unit itself), KWD has 3 (fils).
 * A currency is one of a known, closed set - never a free String.
 */
public enum Currency {
    EUR(2),
    USD(2),
    JPY(0),
    KWD(3);

    /** Minor-unit digits, exactly as named in the Word doc skeleton. */
    final int minorUnits;

    Currency(int minorUnits) {
        this.minorUnits = minorUnits;
    }

    /** Minor-unit digits: 2 means 100 minor units = 1 major unit. */
    public int minorUnits() {
        return minorUnits;
    }

    /** 10^minorUnits: e.g. EUR -> 100, JPY -> 1, KWD -> 1000. */
    public long factor() {
        long f = 1;
        for (int i = 0; i < minorUnits; i++) {
            f *= 10L;
        }
        return f;
    }
}
