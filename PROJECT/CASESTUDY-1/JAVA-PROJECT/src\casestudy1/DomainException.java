package casestudy1;

/**
 * A business rule was broken. Thrown by the domain objects themselves,
 * so an illegal state can never slip through silently.
 *
 * Extends IllegalArgumentException so factories that the written answer
 * describes as throwing IllegalArgumentException (e.g. Percentage)
 * satisfy that contract exactly, while callers can still catch the
 * more specific DomainException type.
 *
 * Currency mix-ups use the dedicated subclass CurrencyMismatch,
 * exactly as named in the Word doc skeleton.
 */
public class DomainException extends IllegalArgumentException {
    public DomainException(String message) {
        super(message);
    }
}
