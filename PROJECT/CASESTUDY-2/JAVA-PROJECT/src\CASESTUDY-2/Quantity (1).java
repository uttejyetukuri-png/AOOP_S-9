package casestudy2;

/**
 * VALUE OBJECT - a positive whole number, as in the Word doc
 * Refined Model:
 *
 *   final class Quantity {
 *     private final int value;             // >= 1
 *     static Quantity of(int v) { if (v < 1) throw ...; ... }
 *   }
 *
 * Zero and negative quantities cannot be constructed.
 */
public final class Quantity {

    private final int value;                // >= 1

    private Quantity(int value) {
        if (value < 1) {
            throw new DomainException("Quantity must be at least 1, got " + value);
        }
        this.value = value;
    }

    public static Quantity of(int v) {
        return new Quantity(v);
    }

    public int value() {
        return value;
    }

    /** Used by OrderLine.combine when the same product is re-added. */
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
