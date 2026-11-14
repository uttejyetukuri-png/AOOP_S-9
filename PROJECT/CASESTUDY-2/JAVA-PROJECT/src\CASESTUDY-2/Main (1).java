package casestudy2;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DEMO / PRESENTATION DRIVER for CO-1 Case Study 2.
 *
 * One scenario per illegal state / question in the written submission,
 * PASS/FAIL per check. Amounts are EUR, built with the Word doc's
 * Money.of("...", Currency.EUR) factory and Quantity.of(...).
 * Timestamps are fixed so the demo is deterministic (the credit race
 * in Scenario 10 is repeated-safe: CreditAccount.reserve is atomic,
 * so exactly one thread always wins).
 */
public class Main {

    private static int passed = 0;
    private static int failed = 0;

    private static final Instant NOW = Instant.parse("2027-01-15T10:00:00Z");
    private static final ProductId WIDGET = ProductId.of("P-100");
    private static final ProductId GADGET = ProductId.of("P-200");

    /** Shorthand for a EUR amount, in the doc's Money.of form. */
    private static Money eur(String decimal) {
        return Money.of(decimal, Currency.EUR);
    }

    public static void main(String[] args) throws Exception {
        OrderPlacementService service = new OrderPlacementService();

        System.out.println("==============================================================");
        System.out.println(" CO-1 CASE STUDY 2 - Order Lifecycle and State Demo (Model A)");
        System.out.println("==============================================================");

        // ------------------------------------------------ Scenario 1
        section("1. Illegal states are unreachable from a new order");
        Order empty = Order.draft(CustomerId.generate());
        expectReject("An EMPTY order cannot be placed (Invariant 1)",
                () -> empty.place());
        Order fresh = Order.draft(CustomerId.generate());
        expectReject("A new (DRAFT) order cannot be paid",
                () -> fresh.pay());
        expectReject("A new (DRAFT) order cannot be shipped",
                () -> fresh.ship(TrackingNumber.of("TRK-1"), NOW));
        expectReject("A new (DRAFT) order cannot be delivered",
                () -> fresh.deliver());

        // ------------------------------------------------ Scenario 2
        section("2. Lines: duplicate product merges, total follows the lines");
        Order draft = Order.draft(CustomerId.generate());
        draft.addLine(WIDGET, eur("500.00"), Quantity.of(2));
        draft.addLine(GADGET, eur("250.00"), Quantity.of(1));
        draft.addLine(WIDGET, eur("500.00"), Quantity.of(1));   // same product again
        check("Re-adding P-100 MERGES into one line - still 2 lines, not 3 (Invariant 5)",
                draft.lines().size() == 2);
        check("The merged P-100 line has quantity 3",
                draft.lines().stream()
                        .filter(l -> l.product().equals(WIDGET))
                        .findFirst().orElseThrow().quantity().value() == 3);
        check("total() = 500.00 x 3 + 250.00 x 1 = 1750.00 EUR",
                draft.total().equals(eur("1750.00")));
        draft.removeLine(GADGET);
        check("After removing P-200 in Draft, total() = 1500.00 EUR",
                draft.total().equals(eur("1500.00")));

        // ------------------------------------------------ Scenario 3
        section("3. Happy path: DRAFT -> PLACED -> PAID -> SHIPPED -> DELIVERED");
        Order happy = orderWith(CustomerId.generate(), eur("750.00"), 2);
        check("A new order starts in DRAFT", happy.status() == Status.DRAFT);
        check("No shipment exists in DRAFT (Invariant 4, 'if' direction)",
                happy.shipment().isEmpty());
        happy.place();
        check("place() moves DRAFT -> PLACED", happy.status() == Status.PLACED);
        happy.pay();
        check("pay() moves PLACED -> PAID", happy.status() == Status.PAID);
        check("Still no shipment in PAID - it exists only after ship()",
                happy.shipment().isEmpty());
        happy.ship(TrackingNumber.of("TRK-123"), NOW);
        check("ship() moves PAID -> SHIPPED", happy.status() == Status.SHIPPED);
        check("Shipment is present exactly after ship(), with its tracking number",
                happy.shipment().isPresent()
                        && happy.shipment().get().tracking().value().equals("TRK-123"));
        happy.deliver();
        check("deliver() moves SHIPPED -> DELIVERED", happy.status() == Status.DELIVERED);

        // ------------------------------------------------ Scenario 4
        section("4. THE Q2 PROOF: a cancelled order can NEVER be shipped");
        Order cancelled = orderWith(CustomerId.generate(), eur("400.00"), 1);
        cancelled.place();
        cancelled.pay();
        cancelled.cancel();
        check("Cancelling from PAID works -> CANCELLED",
                cancelled.status() == Status.CANCELLED);
        expectReject("BUG FIXED: ship() on the cancelled order is refused by the guard",
                () -> cancelled.ship(TrackingNumber.of("TRK-999"), NOW));
        check("No shipment was recorded by the refused ship()",
                cancelled.shipment().isEmpty());

        // ------------------------------------------------ Scenario 5
        section("5. Shipped cannot be cancelled; DELIVERED and CANCELLED are terminal");
        Order shipped = orderWith(CustomerId.generate(), eur("600.00"), 1);
        shipped.place();
        shipped.pay();
        shipped.ship(TrackingNumber.of("TRK-555"), NOW);
        expectReject("A SHIPPED order cannot be cancelled",
                () -> shipped.cancel());
        shipped.deliver();
        expectReject("A DELIVERED order cannot be cancelled",
                () -> shipped.cancel());
        expectReject("A DELIVERED order cannot be paid (again)",
                () -> shipped.pay());
        expectReject("A DELIVERED order cannot be shipped (again)",
                () -> shipped.ship(TrackingNumber.of("TRK-556"), NOW));
        expectReject("A CANCELLED order cannot be paid - it is terminal too",
                () -> cancelled.pay());

        // ------------------------------------------------ Scenario 6
        section("6. Lines are locked once the order is placed");
        Order placed = orderWith(CustomerId.generate(), eur("300.00"), 1);
        placed.place();
        expectReject("addLine after placement is refused",
                () -> placed.addLine(GADGET, eur("250.00"), Quantity.of(1)));
        expectReject("removeLine after placement is refused",
                () -> placed.removeLine(WIDGET));

        // ------------------------------------------------ Scenario 7
        section("7. ship() needs payment AND a real tracking number");
        Order paidOrder = orderWith(CustomerId.generate(), eur("450.00"), 1);
        paidOrder.place();
        paidOrder.pay();
        expectReject("ship() with a NULL tracking number is refused",
                () -> paidOrder.ship(null, NOW));
        expectReject("A blank tracking number cannot even be constructed",
                () -> TrackingNumber.of("   "));
        Order unpaid = orderWith(CustomerId.generate(), eur("450.00"), 1);
        unpaid.place();
        expectReject("ship() from PLACED (unpaid) is refused - no shipping without payment",
                () -> unpaid.ship(TrackingNumber.of("TRK-777"), NOW));

        // ------------------------------------------------ Scenario 8
        section("8. Derived total: correct after EVERY Draft edit, frozen by the lock");
        Order evolving = Order.draft(CustomerId.generate());
        evolving.addLine(WIDGET, eur("500.00"), Quantity.of(1));
        check("After adding 500.00 x 1, total() = 500.00 EUR",
                evolving.total().equals(eur("500.00")));
        evolving.addLine(GADGET, eur("250.00"), Quantity.of(2));
        check("After adding 250.00 x 2, total() = 1000.00 EUR",
                evolving.total().equals(eur("1000.00")));
        evolving.removeLine(WIDGET);
        check("After removing P-100, total() = 500.00 EUR - recomputed, never stale",
                evolving.total().equals(eur("500.00")));
        evolving.place();
        expectReject("Editing lines after placement is refused (so the total cannot drift)",
                () -> evolving.addLine(WIDGET, eur("500.00"), Quantity.of(1)));
        check("Total is still 500.00 EUR after placement - it cannot drift (Q4)",
                evolving.total().equals(eur("500.00")));

        // ------------------------------------------------ Scenario 9
        section("9. Credit rule (Q5): limit 2000.00 EUR, outstanding 500.00 EUR");
        CustomerId creditCustomer = CustomerId.generate();
        CreditAccount account = new CreditAccount(
                creditCustomer, eur("2000.00"), eur("500.00"));
        Order first = orderWith(creditCustomer, eur("500.00"), 3);   // 1500.00 EUR
        service.placeOrder(first, account);
        check("First 1500.00 EUR order PLACES (500.00 + 1500.00 = 2000.00 <= 2000.00)",
                first.status() == Status.PLACED);
        check("Its 1500.00 EUR is RESERVED immediately - it counts against the limit",
                account.reserved().equals(eur("1500.00")));
        Order second = orderWith(creditCustomer, eur("500.00"), 3);  // 1500.00 EUR
        expectReject("Second 1500.00 EUR order is REJECTED (would total 3500.00 EUR)",
                () -> service.placeOrder(second, account));
        check("The rejected order stays in DRAFT - nothing was placed",
                second.status() == Status.DRAFT);
        service.cancelOrder(first, account);
        check("Cancelling the first order through the service works",
                first.status() == Status.CANCELLED);
        check("Its reservation is RELEASED - reserved is back to 0.00 EUR",
                account.reserved().equals(Money.zero(Currency.EUR)));
        service.placeOrder(second, account);
        check("With the credit freed, the second order now PLACES",
                second.status() == Status.PLACED);

        // ------------------------------------------------ Scenario 10
        section("10. The race: two 1500.00 EUR orders, one 2000.00 EUR limit, same instant");
        CustomerId raceCustomer = CustomerId.generate();
        CreditAccount raceAccount = new CreditAccount(
                raceCustomer, eur("2000.00"), Money.zero(Currency.EUR));
        Order raceA = orderWith(raceCustomer, eur("500.00"), 3);     // 1500.00 EUR
        Order raceB = orderWith(raceCustomer, eur("500.00"), 3);     // 1500.00 EUR
        CountDownLatch startTogether = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.submit(() -> racePlace(service, raceA, raceAccount, startTogether, successes, rejections));
        pool.submit(() -> racePlace(service, raceB, raceAccount, startTogether, successes, rejections));
        startTogether.countDown();                       // both fire at once
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
        check("Exactly ONE of the two simultaneous orders was placed", successes.get() == 1);
        check("The other was rejected - the limit was never breached", rejections.get() == 1);

        // ------------------------------------------------ Summary
        System.out.println();
        System.out.println("==============================================================");
        System.out.printf(" RESULT: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0
                ? " ALL CHECKS PASSED - every reported bug is prevented by the design."
                : " SOME CHECKS FAILED - see above.");
        System.out.println("==============================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** An order in Draft holding one line: WIDGET at the given price x qty. */
    private static Order orderWith(CustomerId customer, Money unitPrice, int quantity) {
        Order order = Order.draft(customer);
        order.addLine(WIDGET, unitPrice, Quantity.of(quantity));
        return order;
    }

    private static void racePlace(OrderPlacementService service, Order order,
                                  CreditAccount account, CountDownLatch start,
                                  AtomicInteger ok, AtomicInteger rejected) {
        try {
            start.await();
            service.placeOrder(order, account);
            ok.incrementAndGet();
        } catch (DomainException e) {
            rejected.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------
    // Small assertion helpers (no test framework needed for the demo)
    // ------------------------------------------------------------------

    private static void section(String title) {
        System.out.println();
        System.out.println("--- " + title);
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label);
        }
    }

    private static void expectReject(String label, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL  " + label + "  (nothing was thrown!)");
        } catch (DomainException e) {
            passed++;
            System.out.println("  PASS  " + label + "  ->  " + e.getMessage());
        } catch (NullPointerException e) {
            // ship() validates its arguments with Objects.requireNonNull
            // - a null is a rejection too.
            passed++;
            System.out.println("  PASS  " + label + "  ->  " + e.getMessage());
        }
    }
}
