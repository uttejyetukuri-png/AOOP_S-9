package casestudy2;

/**
 * VALUE (enum) - the closed set of order states in Model A.
 *
 * The Word doc's Classification table calls this concept "OrderStatus"
 * (Value - closed set); the Model A code names the enum `Status`, so
 * this file is Status.java with exactly the doc's lifecycle values:
 * DRAFT, PLACED, PAID, SHIPPED, DELIVERED, CANCELLED.
 *
 * The naive design used a String, so a typo like "SHIPED" was a
 * perfectly storable "state". An enum admits exactly these six
 * values and nothing else - and DELIVERED / CANCELLED are terminal:
 * no transition method accepts them as a starting state.
 */
public enum Status {
    DRAFT,
    PLACED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
