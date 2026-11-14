package casestudy2;

import java.util.Objects;

/**
 * AGGREGATE - a customer's credit account (Q5).
 *
 * It owns the three numbers the credit rule needs - limit,
 * outstanding (already owed) and reserved (held by orders that are
 * placed but not yet settled) - and it enforces ITS OWN invariant:
 *
 *        outstanding + reserved <= limit        at all times
 *
 * reserve(...) is the heart of Q5: the check ("does it fit?") and
 * the reservation ("then hold it") happen in ONE synchronized step
 * on the single object that owns the number. Two simultaneous
 * orders therefore cannot both pass - the second one necessarily
 * sees the first one's reservation already counted. A reservation
 * counts against the limit from the instant it is made, so there
 * is no check-then-act window.
 *
 * (In the database version this same atomicity comes from a
 * versioned UPDATE ... WHERE outstanding + reserved + ? <= limit,
 * or a row lock held for the reservation step - see Q5.)
 */
public final class CreditAccount {

    private final CustomerId customerId;
    private final Money limit;
    private final Money outstanding;
    private Money reserved;

    public CreditAccount(CustomerId customerId, Money limit, Money outstanding) {
        this.customerId = Objects.requireNonNull(customerId, "customer id");
        this.limit = Objects.requireNonNull(limit, "credit limit");
        this.outstanding = Objects.requireNonNull(outstanding, "outstanding");
        if (outstanding.compareTo(limit) > 0) {
            throw new DomainException("Outstanding " + outstanding
                    + " already exceeds the credit limit " + limit);
        }
        this.reserved = Money.zero(limit.currency());
    }

    /**
     * Atomically check-and-reserve: succeeds only if the amount fits
     * WITH all existing reservations counted, and holds the amount
     * immediately. Throws - reserving nothing - otherwise.
     */
    public synchronized void reserve(Money amount) {
        Objects.requireNonNull(amount, "amount to reserve");
        Money wouldBeUsed = outstanding.plus(reserved).plus(amount);
        if (wouldBeUsed.compareTo(limit) > 0) {
            throw new DomainException("Credit limit exceeded: outstanding " + outstanding
                    + " + reserved " + reserved + " + order " + amount
                    + " would exceed the limit " + limit);
        }
        this.reserved = reserved.plus(amount);
    }

    /** Give a reservation back (order cancelled, or placement failed
     *  after reserving - the compensating action in Q5). */
    public synchronized void release(Money amount) {
        Objects.requireNonNull(amount, "amount to release");
        // Money.minus refuses to go negative, so releasing more than
        // was reserved is rejected here too.
        this.reserved = reserved.minus(amount);
    }

    public CustomerId customerId() { return customerId; }
    public Money limit() { return limit; }
    public Money outstanding() { return outstanding; }
    public synchronized Money reserved() { return reserved; }

    /** Derived, never stored: what the customer can still reserve. */
    public synchronized Money available() {
        return limit.minus(outstanding).minus(reserved);
    }
}
