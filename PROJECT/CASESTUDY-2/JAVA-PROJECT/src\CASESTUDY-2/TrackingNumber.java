package casestudy2;

/**
 * VALUE OBJECT - a carrier tracking number (Word doc Classification:
 * TrackingNumber = Value). Validated once, at construction: a blank
 * tracking number can never exist, so ship() never has to re-check it.
 */
public record TrackingNumber(String value) {

    public TrackingNumber {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException("Tracking number must not be blank");
        }
        value = value.trim();
    }

    public static TrackingNumber of(String value) {
        return new TrackingNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
