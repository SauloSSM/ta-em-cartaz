# Tá em Cartaz — Agent Implementation Guide

**Audience:** Antigravity / Codex / coding agents  
**Purpose:** Implement the frontend without inventing product, domain or design rules. v1.1 adds strict multiplicity/navigation/content guardrails discovered during visual review.

---

# 1. Read before coding

Required context order:

1. Official challenge.
2. Latest approved Project Specification.
3. Relevant Story / Acceptance Criteria.
4. `10_DOMAIN_UI_MAPPING.md` when implementing purchase/ticket/gate flows.
5. This frontend specification pack.
6. Relevant visual reference image(s).

Visual references are supplemental.

---

# 2. Stop rule

If implementation requires a new decision about:

- product;
- domain;
- security;
- API contract;
- persistence;
- authentication;
- payment semantics;
- ticket credential semantics;

STOP.

Report:

1. ambiguity;
2. alternatives;
3. trade-offs;
4. affected files.

Do not silently choose.

---

# 3. Visual references are not literal data contracts

Use images for:

- hierarchy;
- composition;
- visual density;
- brand expression;
- responsive direction.

Do not implement fields/functionality solely because they appear in a generated mockup.

Known examples to reject unless explicitly supported:

- seat / row / gate;
- ticket type such as `INTEIRA`;
- app-store download links;
- save-card;
- fabricated sector benefits;
- public registration (`Cadastrar`) without an approved flow;
- artist/location search when only event-name search exists;
- implicit featured-event ranking;
- device geolocation from `Perto de você`;
- multi-sector cart behavior.

---

# 4. Implement one screen/flow at a time

Preferred sequence:

```text
Home
→ Event Detail / Sectors
→ Checkout
→ My Tickets / Shared Ticket
→ Gate
→ Organizer
```

Stop for visual review after each major screen.

---

# 5. Required behavior per screen

For every screen:

- responsive desktop/mobile behavior;
- loading;
- empty where applicable;
- error;
- dynamic long text;
- missing image;
- keyboard focus;
- interactive component states.

For mutations:

- pending feedback;
- success;
- error;
- unknown result if applicable.

---

# 6. Design-token discipline

Do not scatter arbitrary values.

Use centralized tokens for:

- fonts;
- colors;
- semantic states;
- spacing;
- radius;
- motion;
- breakpoints;
- z-index if needed.

Do not add a new font, color, shadow, or radius because one component “needs something different” without updating the system intentionally.

---

# 7. Domain multiplicity rules

These are mandatory:

```text
one Reservation → exactly one sector
Reservation.quantity = N → N Ticket instances
one Ticket → one validation credential + one manualCode + one shareToken
one share action → one focused Ticket
```

Event Detail may not expose simultaneously active quantity controls for multiple sectors.

My Tickets must support `1 de N` selection when quantity > 1.

---

# 8. Image implementation

Use reusable components such as:

```text
EventArtwork
BrandedImageFallback
TicketArtwork
```

Requirements:

- controlled aspect ratio;
- object-fit cover;
- error fallback;
- no broken-image icon;
- decorative overlays separate from source image;
- center crop by default; do not invent focal-point fields;
- critical text must not rely on empty space inside the image.

---

# 9. Ticket implementation

Ticket must be dynamic.

Use only API-supported fields.

QR area must be visually protected.

Do not place texture over QR.

Dynamic theme may be derived deterministically from `eventId` if approved.

---

# 10. Interaction implementation

Buttons must support:

```text
default
hover
focus-visible
pressed
loading
disabled
```

Prevent repeated UI submission during pending mutation.

Backend remains authority for idempotency and state.

For an unauthenticated purchase intent, authenticate before creating the HOLD. Do not fake reservation success locally.

Before Checkout/Ticket implementation, read `08_LOGIC_GAPS_REVIEW.md` and stop on unresolved backend blockers such as payment reconciliation, active-reservation recovery, demo outcome trigger or ticket DTO credential exposure.

---

# 11. Testing before completion

For the implemented screen:

1. run typecheck/lint;
2. run relevant component tests;
3. run relevant page/E2E tests;
4. test mobile;
5. test long text;
6. test missing image;
7. test combined long-text + difficult-image fixture at 360px when the screen renders dynamic content;
8. test loading/error;
9. inspect keyboard focus;
10. inspect browser console;
11. capture screenshot for review where tool allows.

---

# 12. Browser visual verification

After implementation:

- launch the app;
- open actual route;
- verify target desktop width;
- verify target mobile width;
- compare with visual reference;
- correct spacing/hierarchy deviations;
- confirm no overflow or layout shift.

Do not declare done based only on code compilation.

---

# 13. Report format

At completion report:

- files changed;
- behavior implemented;
- tests executed;
- screenshots/artifacts produced;
- deviations from visual reference;
- unresolved ambiguity;
- known limitations.

