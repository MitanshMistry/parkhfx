# ADR 0001: The database enforces "no double-booking"

**Status:** accepted

## Context
Two drivers can try to book the same spot at the same moment. Checking
availability in application code ("SELECT, then INSERT") has a race window:
both requests can pass the check before either one writes.

## Decision
- `bookings.during` is a `tstzrange` with half-open bounds `[start, end)`, so
  back-to-back bookings (11:00 end, 11:00 start) do not collide.
- An **exclusion constraint** (`btree_gist`) forbids overlapping ranges for the
  same listing while the booking is `PENDING` or `CONFIRMED`.
- A booking starts as a `PENDING` hold with a 10-minute `expires_at`. The
  constraint blocks other drivers immediately. Payment confirmation flips it to
  `CONFIRMED`.
- A constraint cannot reference `now()`, so stale holds are marked `EXPIRED` by
  `expire_stale_holds(listing, as_of)`, called inside the same transaction right
  before inserting a new hold. A scheduled cleanup job can be added later.
- The service maps SQLSTATE `23P01` (exclusion_violation) to
  `BookingConflictException`, which the API returns as `409 Conflict`.

## Consequences
- Correct under concurrency without application-level locks.
- A concurrent test (20 threads, one slot, exactly one winner) proves it.
- Slightly more SQL to maintain, which is a good trade for correctness.
