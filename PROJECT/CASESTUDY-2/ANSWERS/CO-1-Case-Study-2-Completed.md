# CO-1 Case Study 2 — Completed (Team Submission)

**Subject:** AOOP (Advanced Object-Oriented Programming, 25CS2205E)
**Case Study 2:** Order Lifecycle and State
**Common base:** Case Study 1 (Money & Pricing — value objects, invariants) — Case 2 applies the same discipline to *state*: a closed set of states, guarded transitions, and a total that is derived, never stored.

> Structure of this submission, as required: Problem Analysis → Illegal States and Invariants (Q1) → Transition Table and Guarded `ship()` (Q2) → Model A vs Model B (Q3) → Derived vs Stored Total (Q4) → Cross-Aggregate Credit Rule (Q5) → Design Justification. The matching runnable Java project (`case-study-2-demo`) demonstrates every point live and implements **Model A** — the model recommended in Q3.

**Core idea in one line:** The naive `Order` stores state as a free string + three booleans + nullable dates + a stored total, so *most combinations of its own fields describe orders that should not exist*. The refined models fix this by (A) guarding every transition in one entity, or (B) making each state its own type so illegal transitions don't even compile — and by **deriving** the total instead of storing it.

---

## 1. Problem Analysis — what is wrong with the naive design

The naive `Order` holds its lifecycle in **independent, publicly settable fields**: a `status` String, three booleans (`paid`, `shipped`, `cancelled`), nullable dates (`shippedAt`, `cancelledAt`), a **public `lines` list** that any code can mutate, and a **stored `total` field**. No field knows about any other field, so the class admits every illegal state in Q1:

| Reachable illegal state / reported problem | Root cause in the design |
|---|---|
| An order marked SHIPPED with zero lines | `status` is a public String — it can be set directly on a new, empty order; nothing connects status to the lines |
| An order both cancelled *and* shipped, with both `cancelledAt` and `shippedAt` set | `cancelled` and `shipped` are independent booleans — nothing checks the flags against each other |
| `status = "DRAFT"` but `shippedAt` is set | The date is a separate public, nullable field, settable in any state — state and its evidence are stored twice and can disagree |
| `status = "SHIPPED"` but `shippedAt` is null and there is no tracking number | The reverse contradiction: the string was set and the date forgotten — again, two independent fields for one fact |
| Shipped without payment (`paid = false, shipped = true`), or `status = "PAID"` while `paid = false` | The status String and the `paid` flag are two independent sources of truth for the same fact |
| The order total no longer equals the sum of its lines | `lines` is a public list — add or remove a line from outside and the separately stored `total` field is never updated |
| Two lines for the same product; lines edited after the order was placed | The public list allows duplicates and edits in any state — no method on `Order` controls what goes into it |
| A status that is not a state at all — `status = "SHIPED"` (typo) | A String is an *open* set where the lifecycle is a *closed* set — any typo is a perfectly legal String |

**Design issues, in the language of Case 1:** (1) state is held in types that cannot express the rules (a String for a closed lifecycle, booleans for mutually exclusive stages — exactly like `double` for money in Case 1); (2) one fact is stored in several independent fields that can contradict each other; (3) the total is *stored* (duplicated) instead of *derived* from its single source of truth, the lines.

**The fix follows two moves:** hold the lifecycle in **one private field of a closed type** (a `Status` enum in Model A, or one type per state in Model B), changeable only through **guarded transition methods** on the aggregate — and **compute `total()` from the lines** instead of storing it.

---

## Q1. (6 Marks) Illegal / contradictory states in the naive `Order`

The naive class has 2³ boolean combinations × any status string × nullable dates — almost all of them meaningless. Any four (six given) earn the marks:

| # | Reachable illegal state | How it is reached | Invariant violated |
|---|---|---|---|
| 1 | `status = "SHIPPED"` with **zero lines** | Status field is public; set it directly on a new, empty order | **Inv. 1** — a placed/shipped order must have at least one line |
| 2 | `cancelled = true` **and later** `shipped = true`, with both `cancelledAt` and `shippedAt` set | Nothing checks the flags against each other; any code can set both | **Inv. 3** — a cancelled order can never be shipped; Shipped/Delivered cannot be cancelled |
| 3 | `status = "DRAFT"` but `shippedAt != null` | The date is a separate public field, settable in any state | **Inv. 4** — `shippedAt`/tracking exist **iff** the order has shipped |
| 4 | `status = "SHIPPED"` but `shippedAt == null` and no tracking number | Set the string, forget the date — the reverse contradiction of #3 | **Inv. 4** (the "only if" direction) |
| 5 | `paid = false, shipped = true` (shipped without payment), or `status = "PAID"` while `paid = false` | Status string and the `paid` flag are two independent sources of truth that can disagree | **Inv. 3** — transitions must follow Draft → Placed → **Paid** → Shipped |
| 6 | `total` no longer equals the sum of the lines | `lines` is a public list: add/remove a line from outside and the stored `total` field is never updated | **Inv. 2** — total must equal the sum of line totals, always |
| 7 | Two lines for the same product, or lines edited after placement | Public list allows duplicates and edits in any state | **Inv. 5** (unique by product; re-adding a product increases quantity) and **Inv. 2/3** (lines editable only in Draft) |

*Also worth one line:* the status can be a **typo** — `status = "SHIPED"` is a perfectly legal `String` but not a legal state, because a string is an open set where the lifecycle is a closed set.

**The invariants, collected:** **Inv. 1** — a placed (or later) order has at least one line. **Inv. 2** — total always equals the sum of line totals. **Inv. 3** — transitions follow Draft → Placed → Paid → Shipped → Delivered, with cancellation only from Draft/Placed/Paid; Delivered and Cancelled are terminal. **Inv. 4** — shipment data (tracking + `shippedAt`) exists if and only if the order has shipped. **Inv. 5** — lines are unique by product (re-adding a product increases its quantity) and are editable only in Draft.

## Q2. (8 Marks) Transition table + `ship()` in Model A

### State transition table

| From ↓ / Action → | addLine / removeLine | place() | pay() | ship() | deliver() | cancel() |
|---|---|---|---|---|---|---|
| **DRAFT** | ✔ (stays Draft) | → **PLACED** *(needs ≥ 1 line)* | ✘ | ✘ | ✘ | → **CANCELLED** |
| **PLACED** | ✘ | ✘ | → **PAID** | ✘ | ✘ | → **CANCELLED** |
| **PAID** | ✘ | ✘ | ✘ | → **SHIPPED** | ✘ | → **CANCELLED** |
| **SHIPPED** | ✘ | ✘ | ✘ | ✘ | → **DELIVERED** | ✘ |
| **DELIVERED** | ✘ | ✘ | ✘ | ✘ | ✘ | ✘ *(terminal)* |
| **CANCELLED** | ✘ | ✘ | ✘ | ✘ | ✘ | ✘ *(terminal)* |

### The `ship` method (Model A — guarded state machine)

```java
void ship(TrackingNumber tracking, Instant at) {
    requireStatus(Status.PAID);                 // THE guard
    Objects.requireNonNull(tracking, "tracking");
    Objects.requireNonNull(at, "shippedAt");
    this.shipment = Optional.of(new Shipment(tracking, at));
    this.status = Status.SHIPPED;
}

private void requireStatus(Status expected) {
    if (status != expected) {
        throw new DomainException(
            "Cannot perform this action in state " + status
            + " (requires " + expected + ")");
    }
}
```

**How this prevents a cancelled order from being shipped:**

1. `ship()` only proceeds when the current state is exactly `PAID`. A cancelled order's state is `CANCELLED`, so `requireStatus(Status.PAID)` **throws before anything is changed** — no shipment is recorded and the state does not move.
2. There is **no other path** to "shipped": `status` and `shipment` are `private`, there are no setters, and `ship()` is the only method that sets either of them. In the naive class, any code anywhere could write `order.status = "SHIPPED"`; here, state can only change through methods that each check their guard first.
3. The guard and the data move together: `shipment` (tracking + `shippedAt`) is set in the *same* method, at the *same* moment the state becomes `SHIPPED` — so Invariant 4 (`shippedAt` exists iff shipped) holds by construction, in both directions.

*The demo project (`case-study-2-demo`) implements exactly this method in `Order.java` and proves point 1 live: an order cancelled from PAID refuses `ship()` and records no shipment.*

## Q3. (8 Marks) Model A vs Model B, and a recommendation

| Criterion | Model A — single entity, guarded transitions | Model B — one type per state |
|---|---|---|
| **Type safety** | Illegal transitions are caught at **runtime** — the guard throws `DomainException`. Safe, but only when the code path is actually executed/tested. State-specific data must be held as `Optional<Shipment>` etc. | Illegal transitions are **compile errors** — `ShippedOrder` has no `cancel()` method and `DraftOrder` has no `shippedAt` field, so the wrong call cannot be written. State-specific data is simply present as normal fields on the type that needs it. |
| **Code volume** | **Low.** One class, one `Status` enum, one guard helper reused by every method. Adding a state = add an enum value + adjust the affected guards. | **High.** One class/interface per state (`DraftOrder`, `PlacedOrder`, `PaidOrder`, `ShippedOrder`, `DeliveredOrder`, `CancelledOrder`…), shared data duplicated or factored into a common base, and every transition is a method that *builds a new object* and copies the state across. |
| **Persistence** | **Simple.** One table / one document: a `status` column (discriminator) plus nullable state-specific columns. Any ORM maps it directly. | **Awkward.** The database still stores one row with a status column — so on load you must *reconstruct* the correct subtype from the discriminator, and on save map each subtype back. The type-level safety largely evaporates at the persistence boundary and must be re-checked when rehydrating. |

**Recommendation for a system with eight states that changes often: Model A.**

- With eight states, Model B already means eight-plus classes; every new state or changed transition ripples through several type-to-type conversion methods. In a lifecycle that *changes often*, that friction is paid on every change.
- Model A's change cost is local and small: add the enum value, update the guards in the few methods affected, add tests for the new transitions.
- The safety Model A gives up (runtime instead of compile-time) can be recovered *where it matters* with a thorough transition test — a test that walks the full transition table (Q2) and asserts every ✘ throws. Tests for a state machine are cheap and exhaustive, unlike most runtime checks.
- Persistence stays trivial, which matters because *every* state change is also a save.
- Model B remains the better choice in the opposite situation: *few, stable* states whose data and behaviour differ sharply (the case study's own comparison table) — there, paying the extra classes once buys permanent compile-time safety.

*That is why the demo project implements Model A: one `Order` entity, one `Status` enum, one `requireStatus(...)` guard reused by every transition.*

## Q4. (4 Marks) Why compute `total()` instead of storing it?

**Why derived is better:**

- A stored `total` is **duplicated information** — the same fact held in two places (the lines, and the field). Two copies can disagree, and in the naive class they *do*: mutate the public `lines` list and the stored total is silently wrong (Invariant 2 broken).
- A computed `total()` has a **single source of truth**. It is correct by construction after *every* possible edit, with no discipline required from callers — there is simply no code path that can make it drift, exactly like the cart/invoice mismatch in Case Study 1.

**When storing it *would* be justified:**

- **Performance:** an order with thousands of lines whose total is read constantly (lists, reports, sorting/filtering by total in the database — you cannot `ORDER BY` a computed value without computing it for every row).
- **Historical snapshot:** once an order is *placed*, the charged total may need to be frozen as a legal/financial record even if prices or lines are later corrected — that is a deliberate snapshot, not a cache.

**The extra invariant you must then protect:** `storedTotal == sum of line totals` **at all times**. Concretely: make the field private with no setter, recompute it **inside the same method/transaction** that adds, removes or reprices a line (never in a separate step a caller could forget), and ideally assert/verify it on save. The moment total is stored, *you* become responsible for an invariant the derived version gave you for free.

## Q5. (6 Marks) "An order must not be placed if the customer's credit limit would be exceeded"

**Is this an Order invariant? No.**

- An invariant of `Order` must be checkable from data the `Order` aggregate itself owns. The credit rule needs the customer's **limit**, their **current outstanding balance**, and the totals of their **other orders** (including ones being placed right now) — none of which lives inside this `Order`.
- It is a **cross-aggregate rule** spanning `Order` and the `Customer` / `CreditAccount` aggregate — the same pattern as the case study's stock rule ("insufficient stock" belongs to Inventory, not Order).

**Where the rule should live:**

- In a **domain service / application-layer workflow** — e.g. `OrderPlacementService.placeOrder(orderId, customerId)` — which coordinates the aggregates:
  1. Ask the credit side to **reserve** the order amount against the customer's credit (`CreditAccount.reserve(amount)` — the *CreditAccount* aggregate enforces "reserved + outstanding ≤ limit" as *its own* invariant).
  2. Only if the reservation succeeds, call `order.place()`.
  3. If placement (or a later step) fails, **release the reservation** as a compensating action; if the order is later cancelled, release it then too.
- The `Order` keeps only the rules it can own (has ≥ 1 line, is in Draft, …).

**Avoiding the race between two simultaneous orders:**

- The danger is the classic **check-then-act race**: two orders for the same customer both read "₹8,000 of ₹10,000 limit used, my amount is ₹1,500 — fits!", and both place, totalling ₹11,000.
- The fix is to make the check-and-reserve **one atomic step on the single aggregate that owns the number** — the `CreditAccount`:
  - Load-and-update it under **optimistic concurrency** (a `@Version` column): the reservation update is `UPDATE credit_account SET reserved = reserved + ?, version = version + 1 WHERE id = ? AND version = ? AND outstanding + reserved + ? <= limit`. If a concurrent reservation already committed, the version/condition fails, the transaction rolls back, and the service retries or rejects — the second order now sees the first one's reservation.
  - (Equivalent alternatives: a pessimistic row lock / serializable transaction on the credit row, held only for the short reservation step.)
- Because *reservations*, not just final orders, count against the limit from the instant they are made, there is no window in which two in-flight orders can both pass. Consistency here is **strong, inside one bounded context and one database transaction** — unlike the stock rule across aggregates, this rule guards money the business is about to commit, so eventual consistency ("place now, discover the breach later") is not acceptable.

*The demo project proves this live: with a ₹10,000 limit and ₹8,000 outstanding, one ₹1,500 order reserves and places, a second ₹1,500 order is rejected and stays in Draft — and when two threads race for the same credit account, exactly one succeeds, because `CreditAccount.reserve(...)` checks and reserves in one synchronized step.*

---

## 2. Design Justification — how each illegal state is now prevented

| Illegal state / reported problem | Prevented by |
|---|---|
| Shipped order with zero lines | `place()` requires ≥ 1 line and `ship()` requires PAID — there is no path from an empty Draft to Shipped; `status` is private with no setter |
| Cancelled order later shipped; both `cancelledAt` and `shippedAt` set | One `status` field, not independent flags — an order is in exactly one state; `ship()`'s `requireStatus(PAID)` throws for a CANCELLED order before anything changes (Q2) |
| Draft order carrying a `shippedAt` | Shipment data lives in one `Optional<Shipment>` set only inside `ship()`, at the same moment the state becomes SHIPPED (Invariant 4, "if" direction) |
| Shipped order with no tracking / `shippedAt` | `ship()` cannot complete without a non-null `TrackingNumber` and instant — the state and its evidence are created together (Invariant 4, "only if" direction) |
| Shipped without payment; status/flag disagreement | There is no `paid` flag — PAID is a state, reachable only via `pay()` from PLACED, and `ship()` requires exactly that state |
| Total drifting from the sum of the lines | No stored total field exists — `total()` is derived from the lines on every call (Q4), and the lines are private |
| Duplicate line for one product; lines edited after placement | `addLine(...)` merges into the existing line for that product (Invariant 5), and `addLine`/`removeLine` are guarded to DRAFT only |
| Typo status (`"SHIPED"`) | `Status` is a closed enum — a value outside the six states cannot be stored at all |
| Two simultaneous orders breaching the credit limit | Atomic check-and-reserve on the `CreditAccount` aggregate (synchronized in the demo; versioned update / row lock in the database), coordinated by `OrderPlacementService` (Q5) |

## 3. Quick revision — Case 2 in one table

| Question pattern | Answer pattern |
|---|---|
| "Why not a `String` / booleans for state?" | The type admits illegal values (typos, contradictory flags). Replace it with a closed type — one enum field (Model A) or one type per state (Model B) |
| Illegal states in a naive class | Count the field combinations; each meaningless combination is a violated invariant — name the invariant it breaks |
| Guarded transition | One private state field, no setters; every transition method checks its guard (`requireStatus`) *before* changing anything, and sets state + its data together |
| Model A vs Model B | Runtime guards, low code, simple persistence vs compile-time safety, high code, awkward persistence. Many/changing states → Model A; few/stable states → Model B |
| Stored vs computed? | Compute from the single source of truth, unless performance/snapshot justifies storing — then *you* own the equality invariant |
| "Rule needs data from another aggregate" | Not this aggregate's invariant. A domain/application service coordinates; reserve first, compensate on failure; make check-and-reserve atomic against races |

*End of CO-1 Case Study 2 — Completed.*
