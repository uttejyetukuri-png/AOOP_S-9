# CO-1 Case Study 1 — Completed (Team Submission)

**Subject:** AOOP (Advanced Object-Oriented Programming, 25CS2205E)
**Case Study 1:** Money and Pricing in an E-Commerce Catalog

> Structure of this submission: Problem Analysis → Why `double` fails (Q1) → Classification (Q2) → The `Percentage` Value Object (Q3) → Currency Conversion (Q4) → Refunds (Q5) → Design Justification. The matching runnable Java project (`case-study-1-demo`) demonstrates every point live.

**Core idea in one line:** Make illegal states unrepresentable — build small, immutable **value objects** (`Money`, `Currency`, `Percentage`, `Quantity`) that validate themselves at construction, so a bad price, a bad discount or a mixed-currency addition *cannot be built at all*, instead of being checked (or forgotten) at every use site.

---

## 1. Problem Analysis — what is wrong with the existing design

The existing system stores price as a `double`, currency (if at all) as a separate `String` or not at all, discount as a raw number, and quantity as a raw `int`. That shape *admits* every bug in the scenario:

| Bug reported | Root cause in the design |
|---|---|
| Cart page and invoice disagree by a cent | Amount is a `double` — a binary floating-point approximation. The same approximate value is rounded in different places (per line vs on the total, cart vs invoice) and the two roundings disagree. There is no single, exact representation and no single rounding rule |
| Negative prices are possible | `double price` accepts any value, including `-5.0`. Nothing in the type says "a price is never negative" — the rule, if it exists at all, lives in scattered validation code that a caller can forget |
| EUR amount and USD amount are silently added together | `double` carries no currency. `eurPrice + usdPrice` is just number + number to the compiler, so a genuine bug (a cart mixing currencies) succeeds and produces a wrong-but-plausible total instead of failing at the point of the mistake |
| Discounts outside 0–100% are accepted (e.g. -10%, 250%) | Discount is a raw `int` / `double` with no range in its type. `250%` off, or a negative discount that *increases* the price, are both storable values |
| Refunds force a negative `Money` / negative price | The only way to represent "money flowing back" in the naive model is a minus sign on the amount. That collides head-on with the "price is never negative" rule, so fixing refunds by relaxing the amount breaks the price guarantee everywhere |

**Design issues, in one sentence:** data is held in types that cannot express the rules (primitive obsession) — `double` for money, a raw number for a percentage, a raw `int` for quantity — so every rule has to be remembered and re-checked at every use site, and inevitably one site forgets.

**The fix:** replace primitives with **self-enforcing value objects** (`Money` in minor units with a `Currency`, `Percentage` in basis points, `Quantity` as a positive int), keep conversion in an explicit domain service, and add *new* types (`Refund`, `SignedMoney`) for the refund concept instead of weakening `Money`.

---

## Q1. (4 Marks) Why `double` is unsuitable for money, and how minor units fix it

**Why `double` fails:**

1. `double` is a *binary* floating-point type. Most decimal fractions have no exact binary representation — `0.1`, `0.2` and `19.99` are all stored as approximations.
2. Arithmetic on approximations accumulates error: in Java, `0.1 + 0.2 != 0.3` (it gives `0.30000000000000004`). Over a cart of many lines, these errors add up to real cents.
3. The visible symptom is exactly the bug in the scenario: the cart page and the invoice round the same approximate value in different places and **disagree by a cent**, and equality tests (`total == expected`) fail unpredictably.
4. `double` also carries no currency and no scale — nothing stops `price = -5`, or adding a EUR amount to a USD amount.

**How minor units fix it:**

- Store the amount as a `long` counting the **smallest unit of the currency** — cents for EUR/USD (2 minor units), yen for JPY (0), fils for KWD (3).
- Example: `10.50 EUR` is stored as the integer `1050` with `Currency.EUR`.
- Integer addition and multiplication by a quantity are **exact** — no representation error, no drift, and equality works. `Money.plus` and `Money.times` are just exact integer operations, plus a currency check.

**One decision that must still be made explicit:**

- **Rounding.** Some operations genuinely produce a fraction of a minor unit — e.g. 15% off `10.50 EUR` is a discount of `1.575 EUR`. The model must document *one* rounding rule and apply it in *one* place: the case study uses `HALF_EVEN` rounding inside `Percentage.applyTo(Money)` (the original combined note placed it in `Money.minus(Percentage)` — same principle: one rule, one place). Equally part of the decision: whether rounding happens **per line** or **once on the total** — the rule must be stated, or cart and invoice will again disagree.

## Q2. (6 Marks) Entity or value object?

| Concept | Kind | Justification (identity & equality) |
|---|---|---|
| **Money** | **Value object** | It has no identity of its own — two `10.00 EUR` amounts are completely interchangeable and equal if amount *and* currency match; nobody asks "which ten euros is this?" |
| **Product** | **Entity** | It is identified by its SKU and persists over time while its name and price *change* — it is still the same product after a price change, so equality is by identity (SKU), not by its current attributes. |
| **Cart** | **Entity (aggregate root)** | It is identified by a cart ID, has a lifecycle, and its contents change over time; two carts with identical items are still two different carts. All changes to its lines go through the cart itself. |
| **Percentage** | **Value object** | It is defined entirely by its number — `15%` is `15%` wherever it appears, it has no lifecycle or identity, and equality is purely by value. |
| **Quantity** | **Value object** | It is defined entirely by its positive integer value; `Quantity(3)` equals any other `Quantity(3)`, and it is interchangeable and immutable. |

**Memory rule:** *Entity = "which one is it?" (identity survives change). Value = "how much / what is it?" (equality by attributes, interchangeable, immutable).*

## Q3. (8 Marks) The `Percentage` value object

Requirements: reject anything outside 0–100%, support `applyTo(Money)`, implement `equals`/`hashCode`, private constructor.

```java
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Percentage {

    // Stored as basis points: 1% = 100 bp, so 100% = 10_000 bp.
    // Basis points keep fractional percentages (e.g. 12.5%) exact in an int.
    private final int basisPoints;              // invariant: 0 .. 10_000

    // Private: the ONLY way in is through a validating factory (below),
    // so the invariant can never be bypassed.
    private Percentage(int basisPoints) {
        if (basisPoints < 0 || basisPoints > 10_000) {
            throw new IllegalArgumentException(
                "Percentage out of range 0..100%: " + basisPoints + " bp");
        }
        this.basisPoints = basisPoints;
    }

    public static Percentage ofBasisPoints(int bp) {
        return new Percentage(bp);              // constructor validates
    }

    public static Percentage ofPercent(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Percent out of range: " + percent);
        }
        return new Percentage(percent * 100);
    }

    public int basisPoints() { return basisPoints; }

    /**
     * Returns the price AFTER this discount is applied.
     * Rounding happens here, in exactly one place, with one documented
     * rule (HALF_EVEN), and the result can never go below zero.
     */
    public Money applyTo(Money price) {
        Objects.requireNonNull(price, "price");
        long discount = BigDecimal.valueOf(price.minorUnits())
                .multiply(BigDecimal.valueOf(basisPoints))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_EVEN)
                .longValueExact();
        long discounted = price.minorUnits() - discount;   // >= 0, since bp <= 10_000
        return Money.ofMinor(discounted, price.currency());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Percentage other && basisPoints == other.basisPoints;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(basisPoints);
    }

    @Override
    public String toString() {
        return (basisPoints / 100.0) + "%";
    }
}
```

*Supporting pieces assumed from the case study's `Money` (same package): `long minorUnits()`, `Currency currency()`, and a factory `Money.ofMinor(long, Currency)` that rejects negatives. In the demo project, domain failures are thrown as `DomainException`, which extends `IllegalArgumentException`, so the rejection contract above holds exactly.*

**Checked examples** (discount arithmetic verified, and re-verified live in the demo project): 15% off 100.00 EUR → **85.00 EUR**; 100% off → **0**; 0% off → price unchanged; `ofPercent(250)` → **rejected** with `IllegalArgumentException`.

**Why the constructor is private:**

1. **One validation gate.** Every `Percentage` that exists has passed the 0–100% check, because there is no other way to create one. Callers never re-check ("did anyone validate this?") — the type itself is the guarantee.
2. **Named factories explain intent.** `ofPercent(15)` and `ofBasisPoints(1250)` say what the number *means* at the call site; a raw `new Percentage(1250)` would not.
3. **Freedom to change the inside.** The internal representation (basis points) can change later without breaking callers, because nobody depends on the constructor.
4. Together with `final class` + `final` field, this also makes the object **immutable and safe to share** — a value object can never change after creation.

## Q4. (6 Marks) Should `Money.plus` silently convert currencies?

**Proposal:** `a.plus(b)` automatically converts `b` into `a`'s currency using "today's rate."

**Verdict: Against.** It hides a business decision inside an arithmetic operator and breaks the case study's central invariant — *money in different currencies cannot be added directly.*

1. **It is not a pure function.** A pure `plus` depends only on its two inputs. A converting `plus` also depends on the *rate*, the *date/time*, and the *rate provider*: the same two amounts give a different answer tomorrow. `a.plus(b)` today ≠ `a.plus(b)` next week.
2. **Testability collapses.** A unit test for a pure `plus` is one line. A converting `plus` needs the test to control time and an external rate source (mock/stub), and tests start failing for reasons unrelated to the code — the rate moved.
3. **It weakens the invariant and hides errors.** The case study makes currency mismatch an explicit exception precisely so a bug surfaces at the point of the mistake. Silent conversion lets a genuine bug (the cart accidentally mixing EUR and USD lines) "succeed" and produce a wrong-but-plausible total.
4. **Information is lost asymmetrically.** Conversion involves rounding, and the chosen target currency is arbitrary: `eur.plus(usd)` and `usd.plus(eur)` can give totals that don't agree after rounding — addition stops behaving like addition.
5. **A conversion is a business event, not arithmetic.** Which rate (buy/sell/mid-market?), as of when, from whom, with what fee — these are policy questions the caller must answer visibly.

**The correct design:** `Money.plus` keeps requiring the same currency and throws on mismatch otherwise. Conversion lives in a **domain service** where rate and date are explicit inputs and the result is auditable:

```java
Money converted = exchangeService.convert(amount, Currency.USD, rate, rateDate);
// ExchangeService.convert(Money from, Currency to, BigDecimal rate, LocalDate on)
//   - rate and date are INPUTS supplied by the caller, never hidden global state
//   - returns NEW Money in the target currency, rounding HALF_EVEN
//   - the original amount is unchanged (Money is immutable)
```

*Note on the earlier combined note:* it sketched `convert(amount, Currency.USD, rateDate)` with the rate looked up inside the service for that date. The demo project makes the rate itself an explicit parameter as well — the strongest form of the same design: nothing about the conversion is hidden.

## Q5. (6 Marks) Refunds — negative amounts without negative prices

**Problem:** "Product price is never negative" must stay enforced, but a refund genuinely is money flowing the other way.

**Wrong fix:** relax `Money` to allow negatives. That deletes the price guarantee everywhere — every price check in the system would have to be re-added by hand at every use site.

**Proposed change — separate the concepts into separate types:**

| New / kept type | What it is | What it guarantees |
|---|---|---|
| `Money` (unchanged) | A non-negative amount in one currency | `minor >= 0`; `Product.price` is of this type, so **a negative price still cannot be constructed** |
| `Price` — optional wrapper, or keep using `Money` directly for prices | The amount charged for a product | Same as `Money`; it can never be a refund, because a `Refund` is a different type |
| `Refund` (new value object) | `Money magnitude` (**positive** amount being returned) + `PaymentId originalPayment` + `Reason` | A refund is represented by a positive magnitude *and its type/direction*, not by a minus sign — it also cannot exist without saying which payment it reverses |
| `SignedMoney` (new, ledger-only) | `long minor` that **may** be negative, plus `Currency` | Used only inside the ledger / account-balance code, where credits and debits genuinely net off; it has no place in the catalog |
| `LedgerEntry` (new) | `SignedMoney amount` + debit/credit kind + date + reference | The audit trail: every movement of money is one entry, and a balance is the sum of entries |

**How the type system keeps the guarantee:**

- `Product` declares `Money price` (or `Price`). A `Refund` or a `SignedMoney` **is not a `Money`**, so `product.setPrice(refund.toSignedMoney())` does not compile — the illegal assignment is a compile error, not a runtime bug.
- Conversion is **one-way and explicit**: `Money.toSigned()` / `SignedMoney.fromMoney(...)` widens a safe amount into the ledger world when accounting needs it; there is **no** silent conversion back — getting back to `Money` requires an explicit check (`SignedMoney.requireNonNegative()` which throws if negative).
- Arithmetic stays honest: `SignedMoney.plus(SignedMoney)` exists for ledger sums (same-currency check still applies); `Money.plus(SignedMoney)` does **not** exist, so price arithmetic can never quietly absorb a negative.

**One-sentence summary for the exam:** *Don't weaken the existing type — add a new type for the new concept. Direction (refund/credit) is carried by the type, magnitude stays positive, and only the ledger gets a signed type, fenced off from prices by the compiler.*

---

## 2. Design Justification — how each reported bug is now prevented

| Reported bug | Prevented by |
|---|---|
| Cart and invoice disagree by a cent | `Money` stores exact `long` minor units; integer arithmetic cannot drift, equality works, and the single HALF_EVEN rounding rule lives in exactly one place (`Percentage.applyTo`) |
| Negative prices possible | `Money`'s private constructor rejects `minor < 0`; `Product.price` is a `Money`, so a negative price cannot be constructed at all |
| EUR + USD silently added | `Money.plus` (and `minus`) check currency and throw on mismatch; conversion is only via `ExchangeService.convert(...)` with explicit rate and date |
| Discounts outside 0–100% | `Percentage` private constructor + factories enforce 0..10,000 basis points; no other construction path exists |
| Refunds forcing negative Money | `Refund` carries direction by type with a positive magnitude; `SignedMoney` exists only in the ledger, with `requireNonNegative()` as the only, explicit way back to `Money` |
| Cart total drifting from its lines (same family as Case 2) | `Cart.total()` is **derived** — recomputed from the lines on every call, never stored; lines are encapsulated and unique by product, re-adding a product merges its quantity |

## 3. Quick revision — Case 1 patterns used again in Cases 2, 4 and 5

| Case 1 pattern | Later instance |
|---|---|
| Primitive that admits illegal values (`double`, raw numbers) → closed, validated value object | `String` status + booleans (Case 2) → guarded state / one type per state; `String` email (Case 5) → `EmailAddress` |
| Private constructor + validating static factories (`Money`, `Percentage`) | `DateRange` (Case 4), `EmailAddress` / `PhoneNumber` (Case 5) — validation in exactly one place |
| One rounding rule, in one place (HALF_EVEN in `applyTo`) | `CancellationPolicy` refunds (Case 4) computed by the policy object, never typed in by staff |
| Derived over stored (`Cart.total()`) | Order `total()` (Case 2), Reservation `total()` = rate × nights (Case 4) |
| "Should this operation silently…?" → No; make inputs explicit, use a domain service | `ExchangeService.convert(amount, to, rate, date)` — rate and date are caller-supplied and auditable |
| New requirement breaks an invariant → add a new type, don't weaken the old one | `Refund` / `SignedMoney` here; state types in Cases 2, 4 and 5 |

*End of CO-1 Case Study 1 — Completed.*
