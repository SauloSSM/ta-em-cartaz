# Tá em Cartaz — Domain ↔ UI Mapping v1.1

**Status:** Normative frontend mapping derived from the approved Project Specification.  
**Purpose:** Prevent generated visual references or coding agents from creating UI semantics that the domain/API does not support.

This document does **not** add backend fields. If a Story/API differs, update this mapping explicitly.

---

# 1. Multiplicity rules

```text
Event 1 ── N TicketSector
Reservation 1 ── 1 TicketSector
Reservation.quantity = N
Confirmed Reservation 1 ── N Ticket
Ticket 1 ── 1 validationToken
Ticket 1 ── 1 manualCode
Ticket 1 ── 1 shareToken
```

UI consequences:

- Event Detail is a **single-sector purchase intent**, not a cart.
- A quantity-3 purchase creates three distinct ticket instances.
- `Compartilhar este ingresso` shares only the focused Ticket.
- Gate validates one ticket credential at a time.

---

# 2. Event fields safe for UI

Current model guarantees conceptually:

```text
id
title
description
imageUrl
category
venueName
venueAddress
startsAt
status
```

Derived/public list values may include:

```text
startingPrice = MIN(sector.price)
totalCapacity = SUM(sector.capacity)
salesOpen = status == PUBLISHED && startsAt > serverNow
```

Do not assume:

- subtitle/tourName;
- featured ranking;
- artist entity;
- geolocation distance;
- image focal point.

---

# 3. TicketSector fields safe for UI

```text
id
eventId
name
description
capacity
availableQuantity
price
```

Do not fabricate structured amenities from free text.

Availability UI:

- `availableQuantity == 0` → SOLD OUT;
- event not sales-open → SALES CLOSED;
- `LOW_AVAILABILITY` has no approved threshold yet and must not be fabricated.

---

# 4. Reservation fields / UI meaning

```text
id
customerId
eventId
sectorId
quantity
unitPrice snapshot
totalAmount snapshot
status
expiresAt
createdAt
confirmedAt
```

UI rules:

- frontend never recomputes authoritative payment total from current sector price after HOLD;
- timer derives from authoritative `expiresAt`;
- `DECLINED` payment does not itself release HOLD;
- one reservation cannot contain several sectors.

---

# 5. Ticket fields / UI meaning

```text
id
reservationId
eventId
sectorId
ownerUserId
status
validationToken
manualCode
shareToken
createdAt
usedAt
usedByGateUserId
```

UI may render only fields exposed by the relevant DTO. Never expose secrets/PII merely because the persistence model contains them.

Do not assume persisted:

- seat;
- row;
- gate/portão;
- `INTEIRA/MEIA`;
- sequential ticket number.

---

# 6. Ticket display modes

## Private My Tickets

Needs enough approved data for customer use:

- event/date/venue;
- sector;
- status;
- QR validation payload according to API contract;
- manual code;
- share action.

## Shared Ticket

Loaded by `shareToken`. Must not leak owner PII. The exact validation credential/status exposure is an API blocker to verify before implementation.

## USED

Historical/collectible presentation may remain, but status dominates and UI must not imply entry remains valid.

---

# 7. Search / navigation truth

Approved MUST search scope: **event name**.

Therefore default copy is conservative:

```text
Buscar eventos...
```

Do not promise:

- artist search;
- venue search;
- “near me” geolocation;
- category filters that are not implemented.

Visual category blocks may exist editorially, but interactive affordance requires real behavior.

---

# 8. Auth truth

Approved roles:

```text
CUSTOMER
ORGANIZER
GATE
```

Public discovery is allowed. Checkout/My Tickets require Customer authentication. No public registration flow is assumed by current contracts.

Purchase branch:

```text
public selection
→ Login if needed
→ restore intent
→ authenticated HOLD creation
```

---

# 9. Payment truth

Payment is simulated and supports:

```text
APPROVED
DECLINED
```

A Reservation may have multiple payment attempts.

Frontend must not:

- store card credentials;
- claim real charging;
- invent APPROVED/DECLINED locally;
- treat a network timeout as a known decline.

The exact demo trigger and unknown-result reconciliation must match backend Story/API.

---

# 10. Gate truth

Before scanning, Gate selects an event. Validation accepts QR/manual credential and returns one of:

```text
VALID
INVALID
ALREADY_USED
WRONG_EVENT
```

Operational UI shows one current result at a time. `WRONG_EVENT` does not consume the ticket.

---

# 11. Dynamic imagery truth

`Event.imageUrl` is presentation input. One source image may be treated differently across Home/Event/Checkout/Ticket/Gate.

Rules:

- fallback exists for null/error;
- image is never required to contain a face;
- no manual cutout assumption;
- no focal-point field is assumed;
- functional text remains independent from image negative space;
- QR remains on a protected functional surface.

---

# 12. Mockup rejection checklist

If a visual reference contains any of these, treat them as composition placeholders unless current API proves otherwise:

```text
seat / row / gate
INTEIRA / MEIA
sequential ticket number
all inclusive / special amenities
Salvar cartão
Cadastrar
App Store / Google Play
artist/location search copy
featured-event ranking
GPS / near-me permission
multi-sector basket
internal/debug IDs in discovery
```
