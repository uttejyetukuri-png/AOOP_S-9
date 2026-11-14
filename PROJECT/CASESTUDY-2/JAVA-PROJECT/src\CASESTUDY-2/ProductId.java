package casestudy2;

/**
 * VALUE OBJECT - a product's identity (its SKU / catalogue code,
 * e.g. "P-100"), in its own file. Wrapping the String means a
 * ProductId can never be swapped with an OrderId or CustomerId,
 * and a blank product ID can never exist.
 */
public record ProductId(String value) {

    public ProductId {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainException("Product id must not be blank");
        }
        value = value.trim();
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
