# Antigravity — Frontend Visual Implementation Brief

You are implementing the **Tá em Cartaz** frontend visual system.

## Read order before editing

1. Official challenge.
2. Latest approved Project/Domain Specification.
3. Current Story + acceptance criteria.
4. `00_README.md`.
5. `01_VISUAL_UX_SPEC_FINAL.md`.
6. `02_REFERENCE_MANIFEST.md`.
7. Only the reference image(s) assigned to the current screen.

## Hard rule

**Do not invent product behavior, fields, routes, API data, authentication behavior, payment semantics or ticket credentials.**

If a needed behavior is not defined by Project Spec / current Story / final visual spec:

STOP and report:

1. the ambiguity;
2. alternatives;
3. trade-offs;
4. files that would change.

## Implementation strategy

Do NOT implement the entire frontend in one uncontrolled pass.

Preferred order when scope permits:

```text
Foundation tokens/components
→ App shell / Header / Footer
→ Home
→ Event Detail / Sectors
→ Checkout
→ My Tickets / Shared Ticket
→ Gate
→ Organizer
```

Work one major screen/flow at a time and produce browser screenshots before continuing.

## Foundation task

Before screen styling, centralize:

- the exact typography tokens;
- brand/semantic colors;
- spacing/layout tokens;
- breakpoints;
- radius/elevation;
- z-index;
- motion;
- focus ring.

Create/reuse components instead of page-specific styling for:

```text
Button
Input
SearchInput
Badge
StatusMessage
EventArtwork
BrandedImageFallback
EventCard/EventRow
TicketSectorCard
QuantityStepper
ReservationTimer
CheckoutSummary
TicketCard
QRCodePanel
ShareButton
ScannerFrame
GateResult
Skeleton
EmptyState
ErrorState
```

Do not add a dependency merely for styling convenience without explicit approval.

## Visual references

Treat images as direction, not contracts. For every image read `02_REFERENCE_MANIFEST.md` and actively REMOVE/REPLACE rejected mockup details.

## Real-data rule

Use current API DTOs and existing seeds. Do not create production UI around fields that exist only in a generated image.

## Screen completion protocol

For each screen:

1. inspect current API/Story first;
2. implement real-field layout;
3. implement desktop/mobile transformation;
4. implement loading/empty/error;
5. implement hover/focus/pressed/loading/disabled;
6. test long content;
7. test missing/failing image;
8. run relevant unit/integration/E2E tests;
9. verify 360px and 1440px minimum;
10. inspect browser console;
11. capture desktop and mobile screenshots;
12. compare against designated visual reference and final spec;
13. report deviations and stop for review before a major visual-direction change.

## Forbidden shortcuts

Never:

- hardcode one artist/image into a reusable component;
- depend on face/background removal;
- create fake multi-sector cart behavior;
- add `Cadastrar` without a registration flow;
- promise artist/location search;
- fabricate ticket seat/row/gate/type;
- add `Salvar cartão`;
- create fake analytics;
- create fake newsletter/app-store/footer destinations;
- create frontend-only payment outcome truth;
- use random one-off spacing/color/font values;
- remove focus outlines;
- use a broken image icon instead of the branded fallback.

## Definition of done

Do not call the screen done until `04_VISUAL_ACCEPTANCE_CHECKLIST.md` passes.
