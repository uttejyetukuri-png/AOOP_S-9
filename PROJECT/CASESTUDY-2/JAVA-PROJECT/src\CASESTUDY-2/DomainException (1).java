package casestudy2;

/** A business rule was broken. Thrown by the domain objects themselves,
 *  so an illegal state or transition can never slip through silently. */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
