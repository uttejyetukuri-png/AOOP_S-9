package casestudy1;

import java.util.Objects;

/**
 * A line inside the Cart aggregate (doc Classification table: CartLine).
 *
 * The naive model called this CartItem { Product product; int quantity; } -
 * here the quantity is a Quantity value object (never zero/negative)
 * and the product is the refined Product entity, so a line total is
 * always an exact Money. Immutable: merging in Cart replaces the line
 * with a new CartLine rather than mutating this one.
 */
public final class CartLine {

    private final Product product;
    private final Quantity quantity;

    public CartLine(Product product, Quantity quantity) {
        this.product = Objects.requireNonNull(product, "product");
        this.quantity = Objects.requireNonNull(quantity, "quantity");
    }

    public Product product() {
        return product;
    }

    public Quantity quantity() {
        return quantity;
    }

    /** Derived: unit price x quantity, exact minor-unit arithmetic. */
    public Money lineTotal() {
        return product.price().times(quantity);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CartLine other
                && product.equals(other.product)
                && quantity.equals(other.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, quantity);
    }

    @Override
    public String toString() {
        return "CartLine[" + product.sku() + " x " + quantity + " = " + lineTotal() + "]";
    }
}
