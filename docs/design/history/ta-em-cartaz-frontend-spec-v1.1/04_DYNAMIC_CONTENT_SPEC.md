# Tá em Cartaz — Dynamic Content Specification

**Status:** Reviewed v1.1 baseline  
**Scope:** Ticketmaster imagery, event artwork, missing images, dynamic text and dynamic ticket rendering.

---

# 1. Core rule

> **The photography is dynamic. The composition is Tá em Cartaz.**

The UI must not depend on a specific artist photo or manual cutout.

---

# 2. Event image source

The application stores event data in its own Event snapshot, including `imageUrl`.

The frontend should render event imagery from the application Event model.

Ticketmaster is discovery/import input; public rendering should not require a fresh Ticketmaster fetch on every page view.

---

# 3. Dynamic Event Imagery

The same event image may be used differently across contexts:

```text
Home Hero      → expressive crop/collage
Event Detail   → expressive hero
Checkout       → restrained thumbnail
Ticket         → controlled editorial treatment
Shared Ticket  → ticket treatment
Gate           → small recognisable thumbnail, optional
```

---

# 4. Controlled image containers

Never rely on:

- a single face;
- a centered portrait;
- transparent background;
- manually removed background;
- empty negative space inside the source image.

Use controlled media frames:

- fixed/defined aspect ratio;
- `object-fit: cover`;
- `object-position: center` by default; do not invent per-event focal-point controls unless such data becomes an approved field;
- overflow hidden;
- overlays owned by the design system.

Reserve image dimensions before loading to avoid layout shift.


## 4.1 Functional text independence

Critical text must not depend on a lucky empty area inside a source image. Event title, date, venue, price, availability, CTA and errors must remain readable when the image contains faces/text/detail across the entire frame.

Allowed: artwork behind/adjacent to decorative title treatment when contrast is structurally guaranteed.

Not allowed: placing critical text over arbitrary Ticketmaster photography because the current mockup happens to have negative space.

---

# 5. Brand treatment

Possible treatment layers:

- grayscale;
- duotone;
- contrast;
- halftone;
- torn-paper mask;
- color blocks;
- dots;
- sticker/doodle overlays.

The source photo must remain recognisable.

Do not apply treatment that reduces usability or makes essential event imagery unreadable.

---

# 6. Stress-test categories

The system must survive:

1. portrait;
2. band/group;
3. stage/crowd;
4. poster/artwork;
5. architecture/cultural venue;
6. missing image.

Primary surfaces to test:

- Home hero;
- Event Detail;
- Ticket.

---

# 7. Missing image fallback

If:

```text
imageUrl == null
OR image fails to load
```

render a branded fallback — never broken-image browser UI.

Fallback can include:

- TC seal;
- brand color theme;
- `CULTURA QUE CONECTA`;
- dots / torn-paper / simple doodles;
- event title where appropriate.

Fallback must work in:

- event card;
- hero;
- checkout thumbnail;
- ticket;
- shared ticket;
- gate thumbnail.

Implementation must avoid infinite error loops when fallback assets fail.

---

# 8. Ticket dynamic imagery

The ticket is a reusable template, not a one-off Liniker poster.


A reservation may issue multiple tickets. The dynamic ticket template renders **one Ticket at a time**; My Tickets controls which ticket instance is focused. Image/theme may be shared by the event, but QR, manual code, status and share token are ticket-specific.

Stable structure:

```text
Brand / event title
Event artwork zone
Date / venue
Sector
Ticket status
Ticket number or appropriate identifier
Perforation language
QR area
Manual code
Share action outside/adjacent as appropriate
```

Dynamic fields:

- event title;
- event subtitle/edition if available from real model;
- image;
- date;
- venue;
- sector;
- ticket status;
- manual code;
- QR;
- ticket identifier;
- theme.

Do not invent:

- seat;
- row;
- gate;
- ticket type such as `INTEIRA`;

unless those fields become real approved domain fields.

---

# 9. Ticket themes

A small set of official themes can create variety while preserving identity:

```text
yellow
pink
orange
green
```

Theme should be stable per event.

Possible MVP rule:

```text
hash(eventId) → theme index
```

No database field is required unless product explicitly needs manual theme control.

Same event should not randomly change ticket color between renders.

---

# 10. Ticket image evolution

Current MVP recommendation:

- ticket reads the current Event image at render time;
- do not duplicate `imageUrl` into Ticket just for presentation;
- if organizer changes editable `imageUrl`, public/ticket presentation may update.

If the product later needs immutable historical ticket artwork, that requires a new explicit snapshot decision.

---

# 11. QR protection

QR region is functional and visually protected.

Rules:

- high contrast;
- clear quiet zone;
- no halftone over QR;
- no textures behind QR;
- no clipping;
- no decorative overlap;
- manual code remains visible.

---

# 12. Dynamic text stress test

All components must be tested with:

- short event title;
- very long event title;
- long venue name;
- long address;
- long sector name;
- long sector description;
- large prices;
- multi-day date labels;
- unusual category labels.

Examples:

```text
LINIKER
```

vs.

```text
FESTIVAL INTERNACIONAL DE CULTURA
E MÚSICA CONTEMPORÂNEA 2026
```

Rules to define per component:

- wrapping;
- line clamp;
- ellipsis;
- minimum type size;
- max lines;
- layout reflow.

Never solve long text by shrinking below usable readability.


## 12.1 Combined chaos fixture

Do not test dynamic risks only in isolation. At least one fixture must combine:

- difficult landscape/group/poster image;
- very long event title;
- long venue/address;
- long sector name;
- large formatted price;
- mobile 360px width;
- ticket/status badge where relevant.

The layout passes only if no critical action, price, status, QR or manual code overlaps/clips.

---

# 13. Public-list content

Event discovery should prioritize:

- image;
- title;
- date;
- location;
- starting price.

Do not display internal/debug identifiers just because they exist.

---

# 14. Accessibility

All meaningful images need appropriate alt behavior:

- meaningful event artwork: descriptive alt;
- purely decorative overlays: empty alt / presentation;
- fallback is still meaningful visually but should not duplicate excessive screen-reader content.



---

# 15. Home hero dynamic-content boundary

The current MVP has no `featuredEvent` domain field or ranking rule. Therefore the Home hero is **brand/editorial by default**, even if it uses dynamic event-style imagery.

Do not silently bind the hero to “the first event returned” or invent featured ranking. If a future Story defines a featured event, add that behavior explicitly and test its fallback.
