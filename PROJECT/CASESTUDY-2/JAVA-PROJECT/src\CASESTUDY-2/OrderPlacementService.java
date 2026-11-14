package casestudy2;

/**
 * DOMAIN SERVICE - coordinates the Order and CreditAccount
 * aggregates for the cross-aggregate credit rule (Q5).
 *
 * The rule "an order must not be placed if the customer's credit
 * limit would be exceeded" is NOT an Order invariant - an Order
 * cannot see the customer's limit, outstanding balance or other
 * orders. So it lives here, in the workflow that touches both:
 *
 *   placeOrder:  1. RESERVE the order total on the credit account
 *                   (CreditAccount enforces limit + atomicity)
 *                2. only then place() the order
 *                3. if placement fails, RELEASE the reservation
 *                   (compensating action) and rethrow
 *   cancelOrder: cancel the order and RELEASE its reservation,
 *                so the freed credit can be used again.
 *
 * Note: cancelOrder must be used for orders placed through
 * placeOrder - only those hold a reservation to release.
 */
public final class OrderPlacementService {

    public void placeOrder(Order order, CreditAccount account) {
        Money amount = order.total();
        account.reserve(amount);            // throws -> order stays DRAFT, nothing reserved
        try {
            order.place();
        } catch (RuntimeException e) {
            account.release(amount);        // compensating action
            throw e;
        }
    }

    public void cancelOrder(Order order, CreditAccount account) {
        Status before = order.status();
        boolean holdsReservation = before == Status.PLACED || before == Status.PAID;
        Money amount = order.total();
        order.cancel();                     // throws if illegal -> reservation untouched
        if (holdsReservation) {
            account.release(amount);
        }
    }
}
