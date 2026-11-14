package casestudy2;

/**
 * Thrown by Money.requireSame (Word doc skeleton) when two amounts in
 * DIFFERENT currencies are combined directly. Money in different
 * currencies cannot be added or compared - conversion needs an
 * exchange rate and a date, so it belongs in a domain service,
 * never silently inside Money.plus.
 */
public class CurrencyMismatch extends DomainException {

    /** The Word doc form: throw new CurrencyMismatch(); */
    public CurrencyMismatch() {
        super("Currency mismatch: amounts in different currencies "
                + "cannot be added or compared directly");
    }

    public CurrencyMismatch(String message) {
        super(message);
    }
}
