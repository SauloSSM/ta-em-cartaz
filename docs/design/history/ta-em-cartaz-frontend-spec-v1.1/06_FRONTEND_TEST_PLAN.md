# Tá em Cartaz — Frontend Test Plan

**Status:** Reviewed v1.1 baseline  
**Goal:** Test behavior heavily without inflating coverage with meaningless CSS assertions.

---

# 1. Testing philosophy

Prioritize:

- business-impacting behavior;
- user feedback;
- critical flows;
- responsive breakpoints;
- accessibility;
- dynamic content;
- regression-prone visual surfaces.

Avoid tests that merely assert implementation details or CSS class names.

---

# 2. Recommended layers

## A. Component / behavior tests

React Testing Library or equivalent.

Targets:

- Button states;
- QuantityStepper;
- ReservationTimer;
- TicketSectorCard;
- PaymentForm validation;
- StatusAlert;
- TicketCard;
- Image fallback component;
- Search input/results behavior.

Examples:

```text
quantity cannot become invalid
sold-out sector cannot increment
loading button blocks duplicate action
timer changes visual state at thresholds
image error renders branded fallback
payment field shows inline error
only one sector can be active for reservation intent
switching sector clears previous pre-hold quantity/total
multi-ticket selector changes focused QR/manual/share action
```

---

## B. Page integration tests

Examples:

### Event Detail
- select sector;
- choose quantity;
- submit reservation;
- pending UI;
- success;
- insufficient stock;
- sales closed;
- unauthenticated reserve redirects to login without creating a false HOLD;
- restored intent creates reservation only after authentication.

### Checkout
- active timer;
- declined payment keeps hold;
- approved payment moves to tickets;
- expiry prevents payment.

### Gate
- manual-code validation;
- each validation result;
- camera-denied fallback.

---

# 3. E2E critical journeys

Use Playwright or equivalent.

## Customer happy path

```text
Home
→ Search/Browse
→ Event Detail
→ Select sector + quantity
→ Reserve
→ Login if required
→ Checkout
→ Payment APPROVED
→ My Tickets
→ N tickets visible when quantity > 1
→ switch focused ticket
→ each ticket exposes distinct QR/manual/share behavior
```

## Customer decline path

```text
Reserve
→ Checkout
→ Payment DECLINED
→ Hold remains active
→ Retry
```

## Expiry path

```text
Reserve
→ wait/mock expiry
→ checkout expired
→ cannot confirm
```

## Organizer

```text
login
→ search/import Ticketmaster item
→ create draft
→ add sector
→ publish
→ verify published state
```

## Gate

```text
login
→ select event
→ validate valid ticket
→ validate same ticket again
→ ALREADY_USED
```

Also:
- INVALID;
- WRONG_EVENT.

---

# 4. Responsive E2E

Test at least:

```text
360x800
390x844
430x932
768x1024
1024x768
1280x800
1440x900
```

Verify:

- no horizontal overflow;
- sticky CTAs do not cover content;
- navbar transforms correctly;
- ticket transforms horizontal→vertical;
- sector cards reflow;
- checkout becomes one column;
- Gate is usable.

---

# 5. Dynamic imagery tests

For Home/Event/Ticket, run fixtures with:

1. portrait;
2. group;
3. stage/crowd;
4. artwork/poster;
5. architecture;
6. null image;
7. failing image URL.

Expected:
- no broken layout;
- branded fallback;
- QR unaffected;
- text readable.

---

# 6. Dynamic text tests

Fixtures:

- very long title;
- long venue;
- long address;
- long sector name;
- long description;
- high price;
- unusual category.

Expected:
- no overlap;
- no clipped CTA;
- no QR collision;
- intentional wrapping/clamping.

Also run a **combined chaos** fixture at 360px: difficult image + long title + long venue + long sector + large price + status/QR where relevant.

---

# 7. Visual regression

Take stable screenshots for:

- Home desktop;
- Home mobile;
- Event desktop;
- Event mobile;
- Checkout desktop;
- Checkout mobile;
- Ticket desktop;
- Ticket mobile;
- Gate mobile;
- Organizer main views.

Also snapshot dynamic-ticket variants if stable enough.

Do not make visual tests hypersensitive to expected dynamic animation.

---

# 8. Accessibility automation

Use axe or equivalent in key pages.

Check:

- accessible names;
- labels;
- contrast;
- landmark structure;
- focusable controls;
- form errors;
- status messages where practical.

---

# 9. Manual QA

Required manual checks:

- keyboard-only navigation;
- visible focus;
- mobile touch;
- real camera permission flow if possible;
- manual-code fallback;
- browser zoom 200%;
- reduced motion;
- slow network;
- offline transition;
- image failure;
- long content;
- refresh during checkout;
- back/forward navigation;
- session expiration scenario if available.

---

# 10. User-feedback assertions

Critical actions should be tested for visible feedback:

```text
reserve → pending → result
payment → processing → result
save → saving → saved/error
publish → publishing → published/error
share → copied/native share/cancel
gate → validating → one clear result
```

---

# 11. Domain/API mapping tests

Front must not rely on fields absent from API.

Tests or fixtures should ensure components tolerate missing optional fields.

Explicitly avoid assuming:
- seat;
- row;
- gate;
- ticket type;
- sector amenities;
- public registration;
- artist/location search;
- featured-event ranking;
- device geolocation;
- multi-sector cart.

Also assert multiplicity rules:

```text
one Reservation → one sector
Reservation.quantity = N → N Ticket instances
one share action → one Ticket/shareToken
```

---

# 12. Minimum pre-merge front gate

For each implemented screen:

- lint/typecheck;
- relevant unit/component tests;
- relevant E2E or page test;
- visual/manual check at desktop + mobile;
- no console errors;
- no obvious accessibility violations.



---

# 13. Overlay / focus / layering tests

For mobile menu, search overlay, dialogs and toasts:

- opening moves/focuses predictably;
- Escape closes where appropriate;
- closing returns focus to trigger;
- sticky CTA does not sit above modal/overlay;
- decorative layers never intercept pointer events.

---

# 14. Demo-payment testability

Once the backend defines the deterministic APPROVED/DECLINED trigger, add E2E for both using the evaluator-visible mechanism. Do not hardcode a frontend-only fake success/decline that bypasses the backend.
