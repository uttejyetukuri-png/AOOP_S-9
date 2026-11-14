package casestudy2;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * ENTITY and AGGREGATE ROOT - Refined Model A from the Word doc,
 * written in the doc's own form (one Order entity, one Status enum,
 * guarded transitions). Model B (one type per state) is compared in
 * the written answer (Q3) but is NOT implemented - Model A is the
 * recommended model:
 *
 *   final class Order {
 *     private final OrderId id;
 *     private Status status = Status.DRAFT;
 *     private final Map<ProductId, OrderLine> lines = new LinkedHashMap<>();
 *     private Optional<Shipment> shipment = Optional.empty();
 *     ...
 *   }
 *
 * (The doc snippet's total() reduces over Money.zero(currency), so
 * this class also carries a private final Currency currency, set via
 * the constructor, plus the CustomerId the order belongs to.)
 *
 * Lifecycle - every transition is a guarded method; there are no
 * setters, so none of the naive design's illegal states can be built:
 *
 *   DRAFT --place()--> PLACED --pay()--> PAID --ship()--> SHIPPED
 *   SHIPPED --deliver()--> DELIVERED          (terminal)
 *   DRAFT / PLACED / PAID --cancel()--> CANCELLED   (terminal)
 *   Lines may be added/removed ONLY in DRAFT.
 *
 * Invariants held by construction:
 *   1. A placed (or later) order has at least one line - place()
 *      refuses an empty order, and lines lock at placement.
 *   2. total() always equals the sum of the line totals - it is
 *      DERIVED from the lines on every call; no field can drift.
 *   3. Transitions follow the table above - every method guards.
 *   4. Shipment data exists IFF the order has shipped - the
 *      Optional<Shipment> is set inside ship(), together with the
 *      state change, and by no other method.
 *   5. Lines are unique by product - re-adding a product merges
 *      (increases) its quantity instead of adding a second line.
 */
public final class Order {

    private final OrderId id;
    private final CustomerId customerId;
    private final Currency currency;
    private Status status = Status.DRAFT;
    private final Map<ProductId, OrderLine> lines = new LinkedHashMap<>();
    private Optional<Shipment> shipment = Optional.empty();

    public Order(OrderId id, CustomerId customerId, Currency currency) {
        this.id = Objects.requireNonNull(id, "order id");
        this.customerId = Objects.requireNonNull(customerId, "customer id");
        this.currency = Objects.requireNonNull(currency, "currency");
    }

    /** A new order for a customer, in EUR (the demo's currency). */
    public static Order draft(CustomerId customerId) {
        return new Order(OrderId.generate(), customerId, Currency.EUR);
    }

    public static Order draft(CustomerId customerId, Currency currency) {
        return new Order(OrderId.generate(), customerId, currency);
    }

    // ------------------------------------------------------------------
    // Lines - DRAFT only; re-adding a product MERGES its quantity
    // (the Word doc's addLine, verbatim in form)
    // ------------------------------------------------------------------

    public void addLine(ProductId p, Money unitPrice, Quantity q) {
        requireStatus(Status.DRAFT);
        Objects.requireNonNull(p, "product");
        Objects.requireNonNull(unitPrice, "unit price");
        Objects.requireNonNull(q, "quantity");
        if (unitPrice.currency() != currency) {
            throw new CurrencyMismatch("Order is in " + currency
                    + " but the line price is in " + unitPrice.currency());
        }
        lines.merge(p, new OrderLine(p, unitPrice, q), OrderLine::combine);
    }

    public void removeLine(ProductId p) {
        requireStatus(Status.DRAFT);
        Objects.requireNonNull(p, "product");
        if (lines.remove(p) == null) {
            throw new DomainException("Product " + p + " is not on this order");
        }
    }

    // ------------------------------------------------------------------
    // Transitions - the only doors through which state may change
    // ------------------------------------------------------------------

    public void place() {
        requireStatus(Status.DRAFT);
        if (lines.isEmpty()) throw new DomainException("cannot place an empty order");
        status = Status.PLACED;
    }

    public void pay() {
        requireStatus(Status.PLACED);
        status = Status.PAID;
    }

    public void ship(TrackingNumber t, Instant at) {
        requireStatus(Status.PAID);
        Objects.requireNonNull(t, "tracking");
        Objects.requireNonNull(at, "shippedAt");
        shipment = Optional.of(new Shipment(t, at));
        status = Status.SHIPPED;
    }

    public void deliver() {
        requireStatus(Status.SHIPPED);
        status = Status.DELIVERED;
    }

    /** Cancellation is allowed ONLY from DRAFT, PLACED or PAID -
     *  never from SHIPPED, DELIVERED or CANCELLED. */
    public void cancel() {
        if (status != Status.DRAFT && status != Status.PLACED && status != Status.PAID) {
            throw new DomainException("Cannot cancel an order in state " + status
                    + " - only DRAFT, PLACED or PAID orders can be cancelled");
        }
        status = Status.CANCELLED;
    }

    // ------------------------------------------------------------------
    // The guard - one private helper, reused by every transition.
    // This is the whole reason a cancelled order can never be shipped:
    // a cancelled order is in state CANCELLED, so ship()'s
    // requireStatus(Status.PAID) throws before anything is recorded.
    // ------------------------------------------------------------------

    private void requireStatus(Status expected) {
        if (status != expected) {
            throw new DomainException(
                "Cannot perform this action in state " + status
                + " (requires " + expected + ")");
        }
    }

    // ------------------------------------------------------------------
    // Read-only access - NO setters, NO stored total
    // ------------------------------------------------------------------

    /** DERIVED (Word doc, verbatim in form): the sum of the line
     *  totals, computed fresh on every call from the lines. */
    public Money total() {
        return lines.values().stream()
                .map(OrderLine::total)
                .reduce(Money.zero(currency), Money::plus);
    }

    /** An unmodifiable copy - callers can read the lines but can
     *  never add/remove behind the aggregate's back. */
    public List<OrderLine> lines() {
        return List.copyOf(lines.values());
    }

    public OrderId id() { return id; }
    public CustomerId customerId() { return customerId; }
    public Currency currency() { return currency; }
    public Status status() { return status; }
    public Optional<Shipment> shipment() { return shipment; }

    @Override
    public String toString() {
        return "Order[" + status + ", " + lines.size() + " line(s), total " + total() + "]";
    }
}
