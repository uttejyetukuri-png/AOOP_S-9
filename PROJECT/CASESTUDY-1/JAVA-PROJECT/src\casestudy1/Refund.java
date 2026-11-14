package casestudy1;

import java.util.Objects;

/**
 * VALUE OBJECT - a refund (Q5 extension).
 *
 * Direction is carried by the TYPE, not by a minus sign: the
 * magnitude is an ordinary, positive Money. A Refund also cannot
 * exist without saying WHICH payment it reverses and why - those
 * are part of the value, validated at construction.
 *
 * A Refund is not a Money and not a SignedMoney, so it can never be
 * assigned where a price is expected (compile error, by design).
 */
public final class Refund {

    private final Money magnitude;      // invariant: strictly positive
    private final String paymentId;
    private final String reason;

    public Refund(Money magnitude, String paymentId, String reason) {
        Objects.requireNonNull(magnitude, "magnitude");
        if (magnitude.minor() <= 0) {
            throw new DomainException("Refund magnitude must be positive: " + magnitude);
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new DomainException("Refund must name the payment it reverses");
        }
        if (reason == null || reason.isBlank()) {
            throw new DomainException("Refund reason must not be blank");
        }
        this.magnitude = magnitude;
        this.paymentId = paymentId.trim();
        this.reason = reason.trim();
    }

    public Money magnitude() {
        return magnitude;
    }

    public String paymentId() {
        return paymentId;
    }

    public String reason() {
        return reason;
    }

    /** The ledger view of this refund: a NEGATIVE SignedMoney. Explicit, one-way. */
    public SignedMoney asSignedMoney() {
        return SignedMoney.ofMinor(-magnitude.minor(), magnitude.currency());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Refund other
                && magnitude.equals(other.magnitude)
                && paymentId.equals(other.paymentId)
                && reason.equals(other.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(magnitude, paymentId, reason);
    }

    @Override
    public String toString() {
        return "Refund[" + magnitude + ", payment " + paymentId + ", " + reason + "]";
    }
}
