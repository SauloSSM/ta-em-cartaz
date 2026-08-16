# Tá em Cartaz — Responsive Behavior Specification

**Status:** Reviewed v1.1 baseline  
**Goal:** Responsive behavior is a layout transformation, not a desktop shrink.

---

# 1. Target grids

```text
Desktop → 12 columns
Tablet  → 8 columns
Mobile  → 4 columns
```

Canonical behavior bands: mobile `<768px`, tablet `768–1199px`, desktop `>=1200px`. Use one centralized breakpoint source; behaviors below remain stable.

---

# 2. Global rules

- No horizontal page overflow.
- Touch targets must be comfortably tappable.
- Mobile must not depend on hover.
- Text must reflow without clipping critical content.
- Images live inside controlled aspect-ratio containers.
- Sticky CTAs must not cover content or system browser UI.
- Respect safe-area insets where relevant.
- Navigation must remain obvious at all widths.

---

# 3. Navigation

## Desktop

Show:

- TC / Tá em Cartaz;
- main nav;
- search;
- auth/account controls.

## Tablet

Compact spacing and possibly reduce secondary text.

## Mobile

Use:

```text
[ TC ] TÁ EM CARTAZ      SEARCH   MENU
```

Menu contains only **approved real routes/actions**:

- Events / discovery;
- authentication/account items;
- Organizer/Gate destinations only when appropriate to the authenticated role.

Category/locality shortcuts (`Shows`, `Festivais`, `Cultura`, `Perto de você`) appear in navigation only if their route/filter behavior actually exists. `Perto de você` must not silently trigger device geolocation.

Search may open a dedicated row/overlay. Search focus must be trapped/restored appropriately when implemented as an overlay.

Do not squeeze full desktop navigation into one line.

---

# 4. Home

## Desktop

- expressive split/composed hero;
- 4 category cards in one row;
- editorial `Em Cartaz` program/list;
- full footer.

## Tablet

- hero composition becomes less horizontally extreme;
- category cards may become 2x2;
- event list retains clear date/title/location/price structure.

## Mobile

Order:

```text
Header
Hero
Category 2x2
Em Cartaz
Footer
```

Requirements:

- hero remains expressive but shorter than desktop;
- `Em Cartaz` should appear early enough that discovery is not buried;
- category cards use 2x2 rather than tiny 4-column row;
- event rows become taller and reflow metadata;
- internal/debug codes must not be shown in public event discovery;
- footer stacks into readable groups.

No App Store / Google Play block unless an app is actually part of the product (currently it is not).

---

# 5. Event Detail / Sector Selection

## Desktop

- event info + artwork hero;
- sectors as horizontal product rows;
- price/availability/quantity visible at once;
- total/CTA region.

## Mobile

- artwork and event metadata stack vertically;
- title can wrap;
- sectors become vertical cards;
- each card shows sector name, real description when available, availability and unit price;
- **one sector is active at a time**;
- the active sector exposes the quantity stepper; unselected available sectors expose an explicit `Selecionar` affordance rather than behaving like a multi-sector cart.

Use sticky purchase summary after the active sector quantity > 0:

```text
2 ingressos · R$ 298
[ GARANTIR INGRESSOS → ]
```

Sticky region must account for safe area and not hide the final card.

Do not invent sector amenities. Use real `TicketSector.description` if present.

---

# 6. Checkout

## Desktop

Two-column layout:

```text
Payment form | Timer + order summary
```

## Mobile

Single-column preferred order:

```text
Header
Timer
Compact order summary
Payment form
Demo notice
Primary CTA
```

Order summary may be collapsible only if key facts remain visible:

- event;
- sector;
- quantity;
- total.

Timer remains visible but does not dominate unnecessarily in NORMAL phase.

---

# 7. My Tickets / Ticket Detail

## Desktop

Ticket may use horizontal physical-ticket format:

```text
Event artwork/info | QR/manual-code stub
```

## Mobile

Ticket becomes vertical while preserving brand identity:

```text
Event title
Artwork
Date / venue
Sector / ticket number / status
Perforation
QR
Manual code
Share CTA
```

QR must remain large, high contrast and unobstructed.


For reservations that issued multiple tickets:

- expose an explicit ticket selector/pager (`Ingresso 1 de N`);
- desktop may use tabs/thumbnails/list + one focused ticket;
- mobile may use pager/swipe only if the current index is always visible and keyboard/touch alternatives exist;
- each ticket owns its own QR, manual code, status and share action;
- sharing acts on the currently selected ticket.

Do not depend on seat/row/gate/type fields not present in actual domain data.

---

# 8. Gate

Gate is mobile/tablet first.

Operational hierarchy:

```text
Selected Event
Scanner
Manual code fallback
Validation action / scanner state
Result
```

When a result appears, it should dominate the screen.

Do not permanently show a 4-card gallery of possible results on the scanner screen.

Desktop can center a mobile-like operational panel or adapt for tablet, but must preserve speed and clarity.

---

# 9. Organizer

## Desktop

Primary productivity experience:

- sidebar/top navigation;
- event lists;
- forms;
- sector management.

## Tablet

Sidebar may collapse.

## Mobile

Must remain usable if accessed, but can be denser/less expressive than Customer.

Forms stack to one column.

Tables become:

- responsive cards;
- stacked rows;
- horizontally scrollable only when necessary and intentional.

Critical status must remain visible:

```text
DRAFT
PUBLISHED
SALES CLOSED
```

---

# 10. Footer

Desktop:

- multi-column.

Tablet:

- reduced column count.

Mobile:

- stacked groups or accordions;
- only routes/actions that actually exist in the product may appear;
- legal/support links remain readable when implemented;
- no App Store / Google Play promotion in the MVP;
- no micro-sized multi-column footer.

---

# 11. Responsive QA widths

At minimum test:

```text
360
390
430
768
1024
1280
1440
```

Also test intermediate fluid resizing rather than only fixed presets.



---

# 12. Shared responsive layout tokens

Use the Design System baseline instead of page-specific margins:

```text
page padding: 16 mobile / 24 tablet / 32 desktop
grid gap:     16 mobile / 24 tablet / 24 desktop
section gap:  48 mobile / 64 tablet / 96 desktop
```

Sticky purchase/footer regions must include device safe-area inset where relevant.
