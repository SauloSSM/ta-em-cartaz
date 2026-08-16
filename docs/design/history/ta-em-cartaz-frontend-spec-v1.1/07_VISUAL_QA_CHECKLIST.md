# Tá em Cartaz — Visual & UX QA Checklist

Use this checklist before approving any frontend screen.

---

## A. Source correctness

- [ ] Uses only real approved domain/API fields.
- [ ] No functionality invented from a visual mockup.
- [ ] Reservation UI represents one active sector, not a multi-sector cart.
- [ ] Quantity `N` purchase is represented as `N` individual tickets where applicable.
- [ ] No `Cadastrar`, featured-event rule, geolocation or unsupported search scope was invented.
- [ ] No App Store / native-app promotion.
- [ ] No `Salvar cartão`.
- [ ] No seat/row/gate/type unless real model supports it.
- [ ] No fabricated sector amenities.

---

## B. Typography

- [ ] Uses at most two font families.
- [ ] Display font is used intentionally.
- [ ] UI/body font remains readable at small sizes.
- [ ] Long text does not force unreadably small font.
- [ ] Heading hierarchy is consistent.

---

## C. Spacing and grid

- [ ] Spacing uses approved token scale.
- [ ] Functional elements align to grid.
- [ ] Related elements are visually grouped.
- [ ] Section spacing is consistent.
- [ ] Decorative elements may break grid without breaking usability.
- [ ] Page padding/grid gap/section gap use shared layout tokens.
- [ ] Sticky/menu/modal/toast layering follows z-index tokens.
- [ ] No accidental 1–5px misalignment between repeated components.

---

## D. Components

- [ ] Button style matches system.
- [ ] Input style matches system.
- [ ] Same component does not look unrelated across pages.
- [ ] No random new radius/shadow style.
- [ ] Functional icons come from one coherent family.
- [ ] Doodles remain decorative.

---

## E. Interaction states

- [ ] default
- [ ] hover
- [ ] focus-visible
- [ ] pressed
- [ ] loading
- [ ] disabled
- [ ] selected/error/success where relevant
- [ ] Hover does not cause layout shift.
- [ ] Mobile does not depend on hover.

---

## F. User feedback

- [ ] Every important async action shows pending feedback.
- [ ] Success is visible.
- [ ] Failure is specific.
- [ ] Critical feedback is persistent enough to understand.
- [ ] Toast is not used for critical payment/reservation outcomes.
- [ ] Unknown result is not falsely labelled failure.

---

## G. Content states

- [ ] Loading state exists.
- [ ] Empty state exists where possible.
- [ ] Error state exists.
- [ ] Missing image fallback exists.
- [ ] Sold-out state exists.
- [ ] Sales-closed state exists.
- [ ] Offline/network behavior is understandable.

---

## H. Dynamic content

- [ ] Long event title tested.
- [ ] Long venue tested.
- [ ] Long sector tested.
- [ ] Missing image tested.
- [ ] Portrait image tested.
- [ ] Landscape/group image tested.
- [ ] No text overlaps artwork.
- [ ] QR remains clean.
- [ ] Combined-chaos fixture tested at 360px.
- [ ] Functional text remains readable regardless of image composition.

---

## I. Responsive

- [ ] 360px
- [ ] 390px
- [ ] 430px
- [ ] tablet
- [ ] desktop
- [ ] No horizontal overflow.
- [ ] Sticky CTA does not obscure content.
- [ ] Footer is readable.
- [ ] Navbar transforms correctly.
- [ ] Touch targets are comfortable.

---

## J. Accessibility

- [ ] Visible keyboard focus.
- [ ] Error is not color-only.
- [ ] State is not color-only.
- [ ] Inputs have labels.
- [ ] Icons/buttons have accessible names.
- [ ] Meaningful images have appropriate alt behavior.
- [ ] Reduced-motion preference is respected.
- [ ] Screen reader is not spammed by per-second timer updates.

---

## K. Performance / stability

- [ ] Image dimensions/aspect ratio reserved.
- [ ] No large layout shift on image load.
- [ ] No duplicate network mutation from rapid clicks.
- [ ] No console errors.
- [ ] Skeleton/loading does not jump layout dramatically.

---

## L. Screen-specific

### Home
- [ ] Events appear early enough on mobile.
- [ ] Public list has title/date/location/price.
- [ ] No internal codes shown.
- [ ] Search copy promises only event-name search.
- [ ] Category/locality cards are not dead controls.
- [ ] Hero does not silently invent a featured event.
- [ ] Footer contains only real routes/actions.

### Event
- [ ] Sector availability is clear.
- [ ] Quantity control is clear.
- [ ] Total is visible before reservation.
- [ ] Mobile purchase CTA is reachable.
- [ ] Exactly one sector is the active purchase intent.
- [ ] Switching sector before HOLD does not retain an accidental second quantity.

### Checkout
- [ ] Timer is not over-alarming in normal phase.
- [ ] Demo payment notice is explicit.
- [ ] User always knows total.
- [ ] Decline says hold remains active.

### Ticket
- [ ] Ticket status visible.
- [ ] QR has clear quiet zone.
- [ ] Manual code visible.
- [ ] No invented metadata.
- [ ] Multi-ticket purchase exposes intentional ticket selection (`1 de N`).
- [ ] Share action clearly belongs to the focused individual ticket.
- [ ] USED ticket does not visually imply valid entry.

### Gate
- [ ] Scanner is primary.
- [ ] Manual fallback obvious.
- [ ] Only one validation result dominates at a time.

