package casestudy1;

/**
 * Thrown when Money values in different currencies are combined,
 * exactly as the Word doc Refined Model shows:
 *   private void requireSame(Money o) {
 *       if (currency != o.currency) throw new CurrencyMismatch();
 *   }
 *
 * A dedicated type (not a generic error), so a currency mix-up can be
 * caught and recognised specifically. It extends DomainException,
 * which extends IllegalArgumentException.
 */
public class CurrencyMismatch extends DomainException {

    public CurrencyMismatch() {
        super("Currency mismatch");
    }

    public CurrencyMismatch(String message) {
        super(message);
    }
}
