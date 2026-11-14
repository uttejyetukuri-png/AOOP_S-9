package casestudy1;

/**
 * VALUE OBJECT - a positive count of items (Q2).
 *
 * Matches the Word doc Refined Model skeleton:
 *   final class Quantity {
 *       private final int value;             // >= 1
 *       static Quantity of(int v) { if (v < 1) throw ...; ... }
 *   }
 *
 * The doc's Classification table also gives Quantity behaviour -
 * add (and subtract) - so plus() is provided; it can only ever
 * produce another valid, positive Quantity.
 */
public final class Quantity {

    private final int value;                    // invariant: value >= 1

    private Quantity(int value) {
        if (value < 1) {
            throw new DomainException("Quantity must be positive: " + value);
        }
        this.value = value;
    }

    public static Quantity of(int v) {
        return new Quantity(v);                 // constructor validates v >= 1
    }

    public int value() {
        return value;
    }

    /** Add behaviour from the doc's Classification table. */
    public Quantity plus(Quantity other) {
        return new Quantity(Math.addExact(value, other.value));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Quantity other && value == other.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
