package casestudy1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DEMO / PRESENTATION DRIVER for CO-1 Case Study 1.
 *
 * One scenario per bug / question in the problem statement,
 * PASS/FAIL per check. All amounts and the exchange rate/date are
 * fixed, so the demo is deterministic. Amounts are built with the
 * Word doc's primary factory, Money.of(String, Currency).
 */
public class Main {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==============================================================");
        System.out.println(" CO-1 CASE STUDY 1 - Money and Pricing in an E-Commerce Catalog");
        System.out.println(" (code matching the Word doc Refined Model skeleton)");
        System.out.println("==============================================================");

        // ------------------------------------------------ Scenario 1
        section("1. Bug: cart and invoice disagree by a cent - why double fails (Q1)");
        check("double: 0.1 + 0.2 != 0.3 in Java (it is " + (0.1 + 0.2) + ")",
                (0.1 + 0.2) != 0.3);
        check("Money: 10 cents + 20 cents = 30 cents, exactly",
                Money.ofMinor(10, Currency.EUR)
                        .plus(Money.ofMinor(20, Currency.EUR))
                        .equals(Money.ofMinor(30, Currency.EUR)));
        check("Money: 19.99 EUR + 0.01 EUR = 20.00 EUR, exactly",
                Money.of("19.99", Currency.EUR)
                        .plus(Money.ofMinor(1, Currency.EUR))
                        .equals(Money.of("20.00", Currency.EUR)));

        // ------------------------------------------------ Scenario 2
        section("2. Minor units: each currency keeps its own precision (Q1)");
        check("1050 minor EUR prints as 10.50 EUR",
                Money.ofMinor(1050, Currency.EUR).toString().equals("10.50 EUR"));
        check("500 minor JPY prints as 500 JPY (0 minor units)",
                Money.ofMinor(500, Currency.JPY).toString().equals("500 JPY"));
        check("1250 minor KWD prints as 1.250 KWD (3 minor units)",
                Money.ofMinor(1250, Currency.KWD).toString().equals("1.250 KWD"));
        check("Money.of(\"1.250\", KWD) = 1250 fils",
                Money.of("1.250", Currency.KWD)
                        .equals(Money.ofMinor(1250, Currency.KWD)));
        check("Money.of(\"500\", JPY) = 500 minor units",
                Money.of("500", Currency.JPY)
                        .equals(Money.ofMinor(500, Currency.JPY)));

        // ------------------------------------------------ Scenario 3
        section("3. Bug: negative prices are possible");
        expectReject("Money of -1 cent cannot be created",
                () -> Money.ofMinor(-1, Currency.EUR));
        expectReject("Money.of(\"-5.00\", EUR) cannot be created",
                () -> Money.of("-5.00", Currency.EUR));
        expectReject("Money.of(\"10.505\", EUR) - excess precision (half a cent) is rejected, not rounded",
                () -> Money.of("10.505", Currency.EUR));
        expectReject("A Product with a negative price cannot be constructed",
                () -> new Product("SKU-NEG", "Bad product",
                        Money.ofMinor(-500, Currency.EUR)));

        // ------------------------------------------------ Scenario 4
        section("4. Bug: EUR and USD are silently added - no silent conversion (Q4)");
        Money eur100 = Money.of("100.00", Currency.EUR);
        Money usd50 = Money.of("50.00", Currency.USD);
        expectReject("100.00 EUR + 50.00 USD throws CurrencyMismatch (no silent conversion)",
                () -> eur100.plus(usd50));
        expectReject("100.00 EUR - 50.00 USD throws CurrencyMismatch too",
                () -> eur100.minus(usd50));
        Cart eurCartForCurrencyCheck = new Cart("CART-FX", Currency.EUR);
        Product usdProduct = new Product("SKU-USD", "Imported item", usd50);
        expectReject("An EUR cart rejects a USD-priced product (CurrencyMismatch)",
                () -> eurCartForCurrencyCheck.add(usdProduct, Quantity.of(1)));

        // ------------------------------------------------ Scenario 5
        section("5. Bug: discounts outside 0-100% are accepted (Q3)");
        expectReject("Percentage.ofPercent(-1) is rejected",
                () -> Percentage.ofPercent(-1));
        expectReject("Percentage.ofPercent(101) is rejected",
                () -> Percentage.ofPercent(101));
        expectReject("Percentage.ofPercent(250) is rejected",
                () -> Percentage.ofPercent(250));
        expectReject("Percentage.ofBasisPoints(-100) is rejected",
                () -> Percentage.ofBasisPoints(-100));
        expectReject("Percentage.ofBasisPoints(10001) is rejected",
                () -> Percentage.ofBasisPoints(10_001));

        // ------------------------------------------------ Scenario 6
        section("6. Percentage.applyTo / Money.minus(Percentage) - one HALF_EVEN rule, in one place (Q3)");
        check("15% off 100.00 EUR = 85.00 EUR, exactly - and Money.minus(Percentage) agrees",
                Percentage.ofPercent(15).applyTo(eur100)
                        .equals(Money.of("85.00", Currency.EUR))
                        && eur100.minus(Percentage.ofPercent(15))
                                .equals(Money.of("85.00", Currency.EUR)));
        check("0% off leaves the price unchanged",
                Percentage.ofPercent(0).applyTo(eur100).equals(eur100));
        check("100% off gives 0.00 EUR (never negative)",
                Percentage.ofPercent(100).applyTo(eur100)
                        .equals(Money.zero(Currency.EUR)));
        // HALF_EVEN case 1: discounted = 1050 x 8500 / 10000 = 892.5 cents.
        // HALF_EVEN rounds 892.5 to 892 (the even neighbour), so 8.92 EUR.
        check("HALF_EVEN: 15% off 10.50 EUR = 8.92 EUR (892.5c rounds to 892c)",
                Percentage.ofPercent(15)
                        .applyTo(Money.of("10.50", Currency.EUR))
                        .equals(Money.ofMinor(892, Currency.EUR)));
        // HALF_EVEN case 2: discounted = 250 x 9500 / 10000 = 237.5 cents.
        // HALF_EVEN rounds 237.5 UP to 238 (the even neighbour), so 2.38 EUR.
        check("HALF_EVEN: 5% off 2.50 EUR = 2.38 EUR (237.5c rounds to 238c)",
                Percentage.ofPercent(5)
                        .applyTo(Money.of("2.50", Currency.EUR))
                        .equals(Money.ofMinor(238, Currency.EUR)));
        check("ofPercent(15) equals ofBasisPoints(1500) - equality is by value",
                Percentage.ofPercent(15).equals(Percentage.ofBasisPoints(1500)));

        // ------------------------------------------------ Scenario 7
        section("7. Quantity is a positive-int value object (Q2)");
        expectReject("Quantity.of(0) is rejected",
                () -> Quantity.of(0));
        expectReject("Quantity.of(-3) is rejected",
                () -> Quantity.of(-3));
        check("10.00 EUR x Quantity(3) = 30.00 EUR",
                Money.of("10.00", Currency.EUR)
                        .times(Quantity.of(3))
                        .equals(Money.of("30.00", Currency.EUR)));

        // ------------------------------------------------ Scenario 8
        section("8. Cart: CartLines unique by product, total() DERIVED - cart and invoice agree (Q1/Q2)");
        Product book = new Product("SKU-BOOK", "AOOP Textbook",
                Money.of("10.50", Currency.EUR));
        Product pen = new Product("SKU-PEN", "Pen",
                Money.of("5.00", Currency.EUR));
        Cart cart = new Cart("CART-1", Currency.EUR);
        cart.add(book, Quantity.of(2));
        cart.add(book, Quantity.of(1));   // same product again - must MERGE
        check("Re-adding the same product merges into ONE CartLine (not two lines)",
                cart.lineCount() == 1);
        check("Merged line quantity is 2 + 1 = 3",
                cart.quantityOf(book).equals(Quantity.of(3)));
        cart.add(pen, Quantity.of(2));
        check("Adding a different product gives 2 lines",
                cart.lineCount() == 2);
        check("Cart total = 10.50 x 3 + 5.00 x 2 = 41.50 EUR",
                cart.total().equals(Money.of("41.50", Currency.EUR)));
        check("Invoice total computed a second, independent way from lines() agrees with cart total()",
                cart.total().equals(independentTotal(cart)));
        cart.remove(pen);
        check("After removing the pen, total() is 31.50 EUR and still equals the sum of lines",
                cart.total().equals(Money.of("31.50", Currency.EUR))
                        && cart.total().equals(independentTotal(cart)));
        expectReject("lines() is encapsulated - the returned list cannot be mutated",
                () -> cart.lines().add(new CartLine(pen, Quantity.of(1))));

        // ------------------------------------------------ Scenario 9
        section("9. ExchangeService: conversion is explicit - rate and date are inputs (Q4)");
        ExchangeService exchange = new ExchangeService();
        LocalDate rateDate = LocalDate.of(2026, 11, 13);
        Money converted = exchange.convert(eur100, Currency.USD,
                new BigDecimal("1.10"), rateDate);
        check("Explicit: 100.00 EUR -> USD at rate 1.10 on 2026-11-13 = 110.00 USD",
                converted.equals(Money.of("110.00", Currency.USD)));
        check("A different stated rate gives a different, stated result: rate 1.20 -> 120.00 USD",
                exchange.convert(eur100, Currency.USD, new BigDecimal("1.20"), rateDate)
                        .equals(Money.of("120.00", Currency.USD)));
        check("Same inputs give the same result every time (pure, testable)",
                exchange.convert(eur100, Currency.USD, new BigDecimal("1.10"), rateDate)
                        .equals(converted));

        // ------------------------------------------------ Scenario 10
        section("10. Refunds without negative prices - direction carried by type (Q5)");
        Refund refund = new Refund(
                Money.of("30.00", Currency.EUR),
                "PAY-123", "Damaged item");
        check("Refund magnitude is a positive 30.00 EUR and it carries its payment id PAY-123",
                refund.magnitude().equals(Money.of("30.00", Currency.EUR))
                        && refund.paymentId().equals("PAY-123"));
        expectReject("A Refund with zero magnitude is rejected",
                () -> new Refund(Money.zero(Currency.EUR), "PAY-124", "Nothing to refund"));
        expectReject("A Refund with no payment id is rejected",
                () -> new Refund(Money.of("5.00", Currency.EUR),
                        "  ", "Missing payment reference"));
        SignedMoney ledgerBalance = SignedMoney.fromMoney(eur100)
                .plus(refund.asSignedMoney());
        check("Ledger: +100.00 EUR and the -30.00 EUR refund entry net to 70.00 EUR",
                ledgerBalance.equals(SignedMoney.ofMinor(7000, Currency.EUR))
                        && ledgerBalance.requireNonNegative()
                                .equals(Money.of("70.00", Currency.EUR)));
        expectReject("A negative SignedMoney cannot become a Money - requireNonNegative() throws",
                () -> refund.asSignedMoney().requireNonNegative());
        expectReject("SignedMoney still checks currency: EUR ledger + USD entry is rejected",
                () -> ledgerBalance.plus(SignedMoney.ofMinor(100, Currency.USD)));
        expectReject("Product price still cannot be built from a negative amount after refunds exist",
                () -> new Product("SKU-BAD2", "Bad product",
                        refund.asSignedMoney().requireNonNegative()));

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

    /** A second, independent way to total a cart: sum lines() by hand. */
    private static Money independentTotal(Cart cart) {
        Money sum = Money.zero(cart.currency());
        List<CartLine> lines = new ArrayList<>(cart.lines());
        for (CartLine line : lines) {
            sum = sum.plus(line.lineTotal());
        }
        return sum;
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
        } catch (RuntimeException e) {
            passed++;
            String detail = e.getMessage() != null
                    ? e.getMessage() : e.getClass().getSimpleName();
            System.out.println("  PASS  " + label + "  ->  " + detail);
        }
    }
}
