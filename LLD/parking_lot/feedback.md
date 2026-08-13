**Interviewer:** Code compiles, the shape is right, main runs. Now the part of the interview where I read your code carefully — and I found **three real correctness bugs**. Your demo runs *and prints fees*, which is exactly what makes them dangerous: the state is silently corrupted. Let me show you, because the way I found them is a habit you need.

**The test that exposes everything: park → unpark → park again.** Your main never re-parks after an unpark. Trace a second bike parking after `bikeTicket` is unparked and watch what happens.

**Bug 1 — `addFloor` rescans ALL floors every call.**

```java
public void addFloor(Floor floor) {
    floors.add(floor);
    for (Floor currFloor : floors) {   // <-- loops every floor, every time
```

Adding floor 2 re-scans floor 1 and increments availability for its spots *again*. In your own main: after both `addFloor` calls, `SMALL = 2` — but you built one small spot. `spotMap` survived only because `computeIfAbsent` is idempotent; the counter wasn't so lucky. Fix: scan only the *incoming* `floor`.

**Bug 2 — `park()`'s spot search is broken twice.**

```java
for (Spot spot : floor.getSpotList()) {
    if (spot.getType() == currType) {   // never checks isOccupied!
        spot.occupy(vehicle);           // and never breaks!
        currSpot = spot;
    }
}
```

- No `!spot.isOccupied()` check → it will happily "park" into an occupied spot, **overwriting the vehicle already there**. That's the Hatchback-with-luxury-pricing class of bug: two cars, one spot, one lost record.
- No `break` (or early return) → it occupies *every* matching spot in the lot and returns a ticket for the *last* one. With 50 medium spots, one car just occupied all 50.

This is the core method of the whole system. The condition should be `spot.getType() == currType && !spot.isOccupied()`, then occupy, then **stop searching**.

**Bug 3 — `unpark` decrements availability.**

```java
availabilityMap.compute(..., (k, v) -> v - 1);   // freeing a spot REDUCES availability??
```

Should be `v + 1`. Trace it: truck parks (LARGE 1→0), truck unparks (0→−1). Next truck is rejected forever with a free spot sitting there. This is the cache-consistency obligation we discussed — three touch points, and one of them pushes the wrong direction.

**Bug 4 (smaller) — tickets are immortal.** `unpark` validates against `ticketMap` but never removes the ticket. Same ticket unparked twice: double-vacate, availability corrupted again. `ticketMap.remove(...)` after success — or mark the ticket used.

**Design notes, non-bug tier:**

- **Your strategies degenerate the pattern.** `SmallTypePayment` returning a hardcoded `10.0` and `LargeTypePayment` returning `40.0` are the *same algorithm* with different constants — that's not two strategies, it's one `HourlyPricing` class taking a rate in its constructor: `new HourlyPricing(10)`, `new HourlyPricing(40)`. Strategy classes should differ in *logic* (hourly vs flat-then-hourly), not in a number. And you skipped the duration math entirely — `Duration.between(entry, exit).toMinutes()` was the one calculation the problem required; "skipping for now" on the core requirement is a flag.
- **`MEDIUM` has no strategy** → if a medium car ever parks, `unpark` NPEs at `ps.calculateFee`. Your availability check accidentally shields you today; it's a landmine.
- Naming drift: we agreed payments were out of scope and it's *pricing* — `PricingStrategy`, `calculateFee`. You wrote `PaymentStrategy`. Cosmetic, but it signals losing track of the agreed contract.
- Still owed from three turns ago: the one-sentence justification for throw-vs-null on a full lot. (Throwing is defensible — "a full lot is an exceptional outcome the caller must handle explicitly, and null invites NPEs" — but *you* have to say it, not me.)

**The habit to take away** — this matters more than the bugs themselves: after writing stateful code, test the **round trip and the invariant**, not just the happy path. One assertion would have caught bugs 1, 3, and 4 instantly: *park, unpark, then availability must equal its starting value, and parking again must succeed.* Interviewers watch for whether you probe your own code like that unprompted; your main only walked forward, never back.

**Verdict if this were real:** design discussion and structure — solid hire signal; the coding block — borderline, because the core flow shipped with an overwrite bug and an inverted counter. The fixes are all small, which is the good news: this is a testing-discipline gap, not an understanding gap.

Your move: send me the corrected `addFloor`, `park`, `unpark`, one parameterized `HourlyPricing` with real duration math, and a main that does the park→unpark→park-again cycle. Then I'll hit you with the extension probe — the "flat rate first 3 hours" scheme plus one more twist — and we close out the interview.
