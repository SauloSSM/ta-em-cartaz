# Tá em Cartaz — Visual Acceptance Checklist

Run this checklist for EVERY major frontend screen before approval.

## A. Domain honesty

- [ ] Uses only real fields exposed by current API/Story.
- [ ] No mockup-only seat/row/gate/type/IDs.
- [ ] No fake route/action/control.
- [ ] Search copy matches actual search capability.
- [ ] One Reservation means one selected sector only.
- [ ] Quantity N is represented as N individual tickets where relevant.
- [ ] Share is scoped to one focused ticket.

## B. Visual system

- [ ] Only Anton + Inter families are used.
- [ ] Brand colors come from final tokens.
- [ ] Semantic colors are not replaced by brand meanings.
- [ ] Spacing uses the token scale/layout roles.
- [ ] Grid is 12/8/4 at canonical bands.
- [ ] No arbitrary new radius/shadow/color/font token.
- [ ] Functional icons are visually consistent.
- [ ] Doodles are decorative and non-blocking.

## C. Interaction feedback

- [ ] Important buttons have default/hover/focus/pressed/loading/disabled.
- [ ] Mobile does not depend on hover.
- [ ] Pending actions visibly communicate work.
- [ ] Success appears only after backend confirmation.
- [ ] Known domain errors use specific copy.
- [ ] Unknown async result is not falsely converted into failure/success.
- [ ] Double submission is visually blocked while pending.

## D. Responsive

- [ ] 360px has no horizontal page overflow.
- [ ] 390px / 430px remain readable.
- [ ] 768px transformation is intentional.
- [ ] 1024px transformation is intentional.
- [ ] 1280px / 1440px use the desktop composition.
- [ ] Sticky CTA does not cover content.
- [ ] Ticket transforms horizontal → vertical appropriately.
- [ ] Checkout becomes one column on mobile.
- [ ] Header/menu/search remain obvious.

## E. Dynamic content

- [ ] Portrait image works.
- [ ] Group/band image works.
- [ ] Stage/crowd image works.
- [ ] Poster/artwork image works.
- [ ] Architecture image works.
- [ ] Null image renders branded fallback.
- [ ] Failing URL renders branded fallback.
- [ ] Long event title works.
- [ ] Long venue/address works.
- [ ] Long sector name works.
- [ ] Large price works.
- [ ] Combined-chaos 360px fixture works.

## F. Accessibility

- [ ] Keyboard reaches all essential controls.
- [ ] Focus is always visible.
- [ ] Error messages are text-associated with fields.
- [ ] State is not color-only.
- [ ] Touch targets are at least 44px.
- [ ] Decorative assets are ignored by assistive tech where appropriate.
- [ ] Reduced-motion mode is respected.
- [ ] 200% zoom/reflow remains usable.
- [ ] Gate feedback uses icon + text + semantic state.

## G. Functional surfaces

- [ ] QR has clean quiet zone and no texture overlap.
- [ ] Manual code is readable.
- [ ] Paper texture does not reduce form/small-text contrast.
- [ ] Decorative layers cannot intercept clicks.
- [ ] Decorative layers do not cover price/CTA/error/focus/QR.

## H. Browser/test evidence

- [ ] Typecheck/lint passes.
- [ ] Relevant component/integration tests pass.
- [ ] Relevant E2E test passes or backend blocker is explicitly documented.
- [ ] Browser console has no relevant error.
- [ ] Desktop screenshot captured.
- [ ] Mobile screenshot captured.
- [ ] Screenshot compared to designated reference using `02_REFERENCE_MANIFEST.md`.
- [ ] Any intentional deviation is reported, not silently introduced.
