package casestudy2;

import java.util.Objects;
import java.util.UUID;

/**
 * VALUE OBJECT - an order's identity, in its own file (Word doc
 * Classification: "OrderId, ProductId - Value objects wrapping IDs
 * so they cannot be swapped"). An OrderId can never be passed where
 * a ProductId or CustomerId belongs - the compiler prevents the swap.
 */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "OrderId");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
