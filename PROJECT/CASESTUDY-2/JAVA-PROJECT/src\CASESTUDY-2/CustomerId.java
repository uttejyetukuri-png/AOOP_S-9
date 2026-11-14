package casestudy2;

import java.util.Objects;
import java.util.UUID;

/**
 * VALUE OBJECT - a customer's identity, in its own file, so it can
 * never be swapped with an OrderId or ProductId (see OrderId.java).
 */
public record CustomerId(UUID value) {

    public CustomerId {
        Objects.requireNonNull(value, "CustomerId");
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
