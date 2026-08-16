# Tá em Cartaz — Screen Behavior Specification

**Status:** Reviewed v1.1 baseline; API-dependent blockers referenced in Logic Gaps Review  
**Scope:** Intended UI behavior per major screen. Visual references supplement this document.

---

# 1. Home / Discovery

## Purpose
Help the user quickly discover published events.

## Must show
- brand/navigation;
- search;
- event discovery;
- date;
- location;
- starting price.

## Visual
- expressive **brand/editorial hero by default**; do not silently treat the first returned event as a featured event;
- colored category/editorial blocks;
- `Em Cartaz` as programming-board/list language;
- category/editorial shortcuts are clickable only when backed by a real route/filter;
- `Perto de você` must not imply device geolocation unless a later approved feature adds it;
- full brand footer desktop, stacked footer mobile, using only real links/actions.

## States
- loading;
- loaded;
- empty search;
- API/load error;
- image fallback.

## Avoid
- login wall for browsing;
- public `Cadastrar` action unless registration is an approved capability;
- search copy that promises artist/location search when only event-name search exists;
- internal IDs;
- app-download promotion;
- fake navigation items.

---

# 2. Event Detail / Sector Selection

## Purpose
Understand the event and choose sector + quantity.

## Must show
- event title;
- date/time;
- venue;
- event artwork;
- sectors;
- availability;
- price;
- quantity;
- purchase total before reservation.

## Sector selection model

The UI supports **one active sector purchase intent at a time** because one Reservation belongs to one sector. It is not a multi-sector cart.

- unselected available sector: explicit `Selecionar`;
- selected sector: selected state + quantity stepper;
- changing sector before hold creation clears the previous quantity/total;
- sold-out sector cannot become active.

## Sector content
Use real model:

- `name`;
- `description`;
- `availableQuantity`;
- `price`.

Do not fabricate amenities.

## States
- available;
- low availability if intentionally defined;
- sold out;
- sales closed;
- reservation pending;
- reservation success;
- insufficient availability.

## Mobile
Sticky total/CTA is preferred after selection.

---

# 3. Login in purchase flow

## Purpose
Authenticate only when required.

If customer began purchase intent before login:

- preserve event;
- preserve selected sector/quantity where safe;
- after login, restore intent;
- only then create/recover HOLD according to the approved API flow;
- never communicate reservation success before the authenticated backend confirms it.

Do not force login for discovery.

---

# 4. Checkout

## Purpose
Complete simulated payment confidently.

## Must show
- event summary;
- sector;
- quantity;
- total;
- reservation timer;
- demo-payment notice;
- payment form.

## Must not show
- card-storage promise;
- real-charge language.

## States
- payment idle;
- processing;
- approved;
- declined;
- unknown result;
- reservation expired.

Declined payment keeps reservation HOLDING until expiry.

---

# 5. Payment success

## Purpose
Clearly transition from purchase to ticket access.

Show:

```text
Pagamento aprovado
Ingressos emitidos
[ VER MEUS INGRESSOS ]
```

Do not show success before backend confirmation.

---

# 6. My Tickets

## Purpose
Allow customer to find and use issued tickets.

List/ticket content:

- event;
- date;
- venue;
- sector;
- ticket status;
- QR;
- manual code;
- share action.

Ticket multiplicity:

- a reservation with quantity `N` yields `N` individual tickets;
- the page must expose `Ingresso 1 de N` (or equivalent) and let the customer intentionally switch the focused ticket;
- QR, manualCode, status and share action belong to the focused ticket;
- the event artwork/theme may be shared across ticket instances.

Ticket states:

```text
VALID
USED
```

A used ticket should clearly communicate that status. The historical ticket may remain visible, but `USED` becomes the dominant state and the QR must not visually imply that entry is still available.

Do not show invented seat/row/gate/type fields.

---

# 7. Shared Ticket

## Purpose
Allow bearer-like access to **one individual Ticket** via that ticket's share token without exposing private account data.

Must:
- render ticket/event essentials;
- preserve QR/manual-code security boundaries per backend contract;
- avoid leaking owner PII.

Needs:
- loading;
- invalid share token;
- used ticket;
- event image fallback.

---

# 8. Gate

## Purpose
Validate one ticket as quickly and clearly as possible.

Primary flow:

```text
select event
scan QR
or manual code
validate
show one result
next scan
```

States:
- camera requesting;
- camera ready;
- camera denied/unavailable;
- validating;
- VALID;
- INVALID;
- ALREADY_USED;
- WRONG_EVENT;
- API/network failure.

Scanner pauses after a read while result is being handled.

---

# 9. Organizer — My Events

## Purpose
See own events and status quickly.

Show:
- event title;
- date;
- status;
- useful inventory summary if available.

States:
- loading;
- no events;
- draft;
- published;
- sales closed.

Avoid fake analytics.

---

# 10. Organizer — Create/Edit Event

## Purpose
Create internal Event from external Ticketmaster selection and configure local sales data.

Need:
- Ticketmaster search/import step;
- title/reference snapshot display;
- date;
- venue;
- editable content;
- clear save feedback.

After `PUBLISHED`, structural fields must be visibly locked with explanation.

Editable after publish:
- description;
- imageUrl;
- category.

---

# 11. Organizer — Manage Sectors

## Purpose
Create/edit valid sectors.

Use real fields:
- name;
- description;
- capacity;
- available quantity display;
- price.

Rules reflected in UX:
- cannot remove committed sector if backend rejects;
- capacity cannot drop below committed amount;
- existing reservations preserve old unit-price snapshots.

Surface backend error specifically.

---

# 12. Search

Must support simple search by **event name**.

Public placeholder/copy should therefore be conservative, e.g.:

```text
Buscar eventos...
```

Do not promise artist/location search until the backend contract supports it.

States:
- initial;
- searching;
- results;
- no results;
- error.

Search feedback should not block page unnecessarily.

Search result rows/cards must not expose internal identifiers.



---

# 13. Public event ordering / sales state

The domain keeps an Event `PUBLISHED` even when `startsAt <= now`, while new reservations become `SALES_CLOSED`. The frontend must not accidentally let raw database order become UX.

Before final Home implementation, confirm endpoint ordering. Preferred presentation is to prioritize upcoming events chronologically and clearly mark any included started/sales-closed event. Do not silently hide valid API data or invent a new persisted event state.

---

# 14. Demo payment discoverability

The challenge requires evaluators to exercise both approval and decline. The exact deterministic trigger is a backend/API contract, not a visual invention.

Before implementing final Checkout controls, verify the Story/API and make the demo mechanism understandable enough that an evaluator can intentionally test both outcomes without implying real card processing.
