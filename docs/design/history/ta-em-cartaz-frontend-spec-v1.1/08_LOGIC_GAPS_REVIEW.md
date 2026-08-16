# Tá em Cartaz — Logic Gaps, Decisions & Remaining Blockers v1.1

**Status:** Reviewed after visual-design stress tests  
**Purpose:** Separate visual-mockup discoveries into `DECIDED`, `BACKEND/API BLOCKER`, and `OPEN TOKEN/POLISH` so coding agents do not invent behavior.

Severity:
- `P0` — must be resolved before implementing the affected flow.
- `P1` — must be resolved/frozen before final visual implementation of the affected surface.
- `P2` — polish/token detail that can be finalized during the implementation token pass.

Classification:
- `DECIDED` — approved frontend rule; implement.
- `BLOCKED` — depends on current backend/API Story; inspect/resolve before implementing.
- `OPEN` — intentionally not frozen yet.

---

# A. P0 — DECIDED

## 1. One Reservation = one sector purchase intent

**Problem discovered:** visual sector cards with independent steppers can imply `2 Pista + 1 Camarote` in one purchase. Current domain defines one Reservation for one `sectorId` + `quantity`.

**Decision:** the frontend is **not a multi-sector cart**. Exactly one sector is active before hold creation. Unselected sectors expose `Selecionar`; selected sector exposes quantity. Changing sector before the HOLD clears the previous pre-hold quantity/total.

---

## 2. Reservation quantity N = N individual Tickets

**Problem discovered:** ticket mockups were designed around a single beautiful ticket, but payment confirmation issues `N` tickets.

**Decision:** My Tickets needs ticket-level navigation (`Ingresso 1 de N`). Each ticket has its own status, QR, manualCode and share action. Event artwork/theme can be shared visually.

---

## 3. Sharing is individual-ticket scoped

Each Ticket owns a distinct `shareToken`. `Compartilhar este ingresso` acts on the focused ticket. Do not silently share an entire reservation/bundle.

---

## 4. Mockup-only ticket fields are forbidden

Do not implement seat, row, gate/portão, `INTEIRA/MEIA`, or a fake sequential display number unless a real API/domain field exists. Current ticket UI uses event, sector, date/venue, status, QR, manualCode and only identifiers explicitly exposed by DTO.

---

## 5. Fabricated sector amenities are forbidden

Do not create `all inclusive`, `bar exclusivo`, comfort benefits etc. Render actual `TicketSector.description` only.

---

## 6. Save-card UI is forbidden

Fake payment does not provide credential storage/tokenization. Remove `Salvar cartão`.

---

## 7. Native-app promotion is forbidden

App nativo is outside MVP. No App Store / Google Play CTA.

---

## 8. Gate result gallery is documentation only

Operational Gate shows one dominant current result after validation; the four-state board is not a production scanner screen.

---

## 9. No public registration UI unless capability is approved

Current approved IA has Login but no registration flow/API. Do not show `Cadastrar` merely because a mockup did.

---

## 10. Search promises event-name search only

Use conservative copy such as `Buscar eventos...`. Do not promise artists/venues/local search until supported.

---

## 11. Authentication occurs before HOLD creation

A visitor may choose event/sector/quantity, but if unauthenticated the flow preserves non-sensitive intent → Login → restore intent → authenticated reservation request. Do not show `Reserva garantida` before backend confirmation.

---

# B. P1 — DECIDED FRONTEND RULES

## 12. Home hero has no implicit featured-event semantics

No `featuredEvent` or ranking rule exists. Home hero is brand/editorial by default. Never bind it to “first API result” as a hidden product decision.

---

## 13. Category/locality cards must be honest controls

`Shows`, `Festivais`, `Cultura`, `Perto de você` may keep their visual direction, but an arrow/hover/click affordance exists only when a real route/filter exists. Dead clickable cards are forbidden.

`Perto de você` must not silently request device geolocation. If no explicit locality/filter behavior exists, treat the block as non-interactive editorial content or omit it from functional navigation.

---

## 14. Event subtitle/tour name is optional

No guaranteed `subtitle/tourName` exists. Hero/ticket must work with `title` alone. Never parse free-form `description` to manufacture a subtitle.

---

## 15. No UI-only LOW_AVAILABILITY threshold

Until backend/product defines a threshold/state, use actual quantity plus `available` / `sold out` / `sales closed`. Do not invent business urgency from an arbitrary percentage.

---

## 16. Dynamic ticket theme is presentation-only

A deterministic `hash(eventId) → yellow/pink/orange/green` rule is accepted for presentation. It creates no database field and the same event remains visually stable.

---

## 17. Event image updates may update ticket artwork in MVP

`imageUrl` remains editable after publish. Ticket may render current Event artwork; image is presentation, not validation credential. Historical immutable artwork would require a future domain snapshot decision.

---

## 18. Image crop has no invented focal-point control

Use controlled aspect ratio + `object-fit: cover` + center position by default. No Organizer focal-point UI unless the domain explicitly gains that data.

---

## 19. Functional text is independent from photography

Title/date/venue/price/status/CTA cannot rely on negative space inside a particular source photo. Dynamic artwork can change without breaking critical text.

---

## 20. USED ticket remains historical but de-emphasizes entry credential

The ticket can remain visually collectible after use, but `USED` becomes dominant and the QR must not visually communicate that entry is still available.

---

## 21. Generated footer IA is not product IA

Footer composition may remain visually rich, but only routes/actions that actually exist are rendered. No invented careers, press, partner, app-download or support destinations.

---

## 22. Decorative layers have safety boundaries

Doodles/halftone/torn paper are non-interactive (`pointer-events:none` when decorative), should be `aria-hidden` where appropriate, and never cover CTA, focus ring, error, price, QR or manual code. Critical UI uses controlled surfaces.

---

## 23. Shared layout tokens are required

Use centralized page padding, grid gap, section gap, breakpoints and z-index scale. A spacing scale alone does not authorize page-specific arbitrary margins.

---

## 24. Combined chaos test is mandatory

At least one fixture combines difficult image + very long title + long venue + long sector + large price + mobile 360px + status/QR where relevant. Passing isolated stress tests is not enough.

---

# C. P0/P1 — BACKEND/API BLOCKERS

These are not frontend decisions. Resolve against the current Story/API before implementing the affected flow.

## 25. Payment APPROVED/DECLINED demo trigger — P0 Checkout

The challenge requires both outcomes. Confirm the exact deterministic backend contract that lets an evaluator intentionally produce each result. Frontend must make the demo understandable without bypassing the backend.

---

## 26. Payment UNKNOWN_RESULT reconciliation — P0 Checkout

Confirm how a timeout is reconciled: reservation refetch, payment attempt lookup or idempotent result recovery. Never interpret timeout as decline or encourage blind retry.

---

## 27. Active reservation recovery on refresh — P0 Checkout

Confirm which API returns current HOLDING reservation and authoritative `expiresAt`. Never restart the timer from local state.

---

## 28. Login intent restoration mechanism — P1 Purchase flow

UX behavior is approved; storage/transport mechanism must match current auth/reservation architecture. No sensitive payment data in URL/storage.

---

## 29. Private/shared Ticket DTO and credential boundaries — P0 Tickets/Share

Confirm exactly which fields private My Tickets vs shared endpoint expose. `shareToken` and validation credential responsibilities remain separate.

---

## 30. Shared Ticket status after use — P1 Share

Desired UI shows current `VALID/USED`; confirm the shared DTO exposes status safely.

---

## 31. Reservation quantity maximum — P1 Event Detail

Do not invent a client-side purchase limit beyond backend/Story acceptance criteria. The stepper must respect real constraints returned/defined by API.

---

## 32. Public listing ordering / sales-closed visibility — P1 Home

The domain keeps started events `PUBLISHED` while sales close. Confirm endpoint/order behavior. Preferred UX prioritizes upcoming events chronologically and clearly marks any included `SALES_CLOSED` item; do not invent a persisted state or silently hide API data.

---

# D. P1/P2 — OPEN TOKEN / VISUAL FREEZE

## 33. Exact font families — P1 OPEN

Constraint is frozen: maximum two families (`Display`, `UI/Text`). Choose final families before first production visual screen; use centralized `--font-display` / `--font-ui`.

## 34. Canonical brand/semantic hex values — P1 OPEN

Freeze one palette token set before production styling. Generated boards are not canonical color values.

## 35. Type scale values — P1 OPEN

Freeze font-size/line-height/weight/tracking tokens; two families alone are insufficient consistency.

## 36. Motion durations/easing — P2 OPEN

Use 2–3 shared motion tokens; no bespoke component animation timing.

## 37. Radius/elevation exact tokens — P2 OPEN

Use a small fixed set appropriate to the physical/editorial language, not SaaS-card rounding everywhere.

## 38. Visual ticket display identifier — P1 OPEN/BLOCKED

Do not invent sequential `000458`. Prefer `manualCode` or an explicitly exposed display-safe identifier until DTO/domain clarifies.

---

# E. Review gate by screen

```text
HOME
→ resolve 32 + final routes/search support + tokens 33–35

EVENT DETAIL
→ decisions 1, 13, 15 + blocker 31

CHECKOUT
→ blockers 25, 26, 27 + auth mechanism 28

MY TICKETS / SHARE
→ decisions 2, 3, 20 + blockers 29, 30, 38

GATE
→ result rule decided; verify actual validation DTO before final implementation

ORGANIZER
→ no invented analytics; use current Story/API fields only
```

Before an agent declares a screen implementation-ready:

1. inspect current API/Story;
2. resolve all affected P0;
3. ensure P1 frontend rules are represented in specs/tests;
4. implement one flow;
5. run `07_VISUAL_QA_CHECKLIST.md`;
6. run relevant tests;
7. compare browser output to visual reference **without copying mockup-only data**.
