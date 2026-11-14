package casestudy2;

import java.util.Objects;

/**
 * VALUE OBJECT - one line of an order (Word doc Classification:
 * OrderLine = Value (product ID, unit price, quantity)).
 *
 * Its total is DERIVED (unit price x quantity), never stored, and
 * combine(...) merges two lines for the SAME product - it is used
 * exactly as the doc writes it, as the merge function in
 * Order.addLine:  lines.merge(p, new OrderLine(...), OrderLine::combine)
 */
public record OrderLine(ProductId product, Money unitPrice, Quantity quantity) {

    public OrderLine {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(unitPrice, "unit price");
        Objects.requireNonNull(quantity, "quantity");
    }

    /** Derived, never stored: unit price x quantity. (Doc name: total()) */
    public Money total() {
        return unitPrice.times(quantity);
    }

    /**
     * Merge this line with another line for the same product at the
     * same unit price: quantities add, still ONE line (Invariant 5 -
     * lines are unique by product). A different product, or the same
     * product at a different price, is not a merge - it is an error.
     */
    public OrderLine combine(OrderLine other) {
        Objects.requireNonNull(other, "other line");
        if (!product.equals(other.product)) {
            throw new DomainException("Cannot combine lines for different products: "
                    + product + " and " + other.product);
        }
        if (!unitPrice.equals(other.unitPrice)) {
            throw new DomainException("Product " + product
                    + " is already on this order at a different unit price: "
                    + unitPrice + " vs " + other.unitPrice);
        }
        return new OrderLine(product, unitPrice, quantity.plus(other.quantity));
    }
}
