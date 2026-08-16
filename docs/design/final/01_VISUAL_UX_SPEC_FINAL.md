# Tá em Cartaz — Visual & UX Implementation Specification v1.0 FINAL

**Status:** FINAL / normative for frontend implementation  
**Project:** Tá em Cartaz  
**Design concept:** **Festival culture organized by product thinking**  
**Visual direction:** Neo-Swiss / Swiss Punk + Festival Editorial + restrained Acid/Rave accents

---

# 0. Non-negotiable implementation contract

The implementation MUST follow these rules:

1. **Structure first. Expression second.**
2. Use **exactly two font families** defined below. No third font.
3. Functional UI follows the shared grid, spacing, type, state and interaction tokens.
4. Decorative layers may break the grid; buttons, inputs, prices, QR, errors and forms may not.
5. Brand colors never replace semantic state meaning.
6. Every important action visibly acknowledges the user.
7. Mobile is a layout transformation, not a compressed desktop.
8. Dynamic Ticketmaster content must not break the composition.
9. Missing image must render a branded fallback, never browser broken-image UI.
10. Mockup-only data/functionality is forbidden unless the current API/Story actually supports it.
11. One Reservation = one sector + quantity. This product is **not a multi-sector cart**.
12. Quantity N creates N individual Tickets. QR/manual/share/status are ticket-scoped.
13. Public discovery is allowed without login; HOLD creation occurs only after Customer authentication.
14. Coding agents do not make new product/domain decisions.
15. A screen is not done until responsive, interaction, dynamic-content and error states have been tested.

> **The poster may break the grid. The button may not.**

---

# 1. Experience intent

The experience must feel:

- young;
- cultural;
- editorial;
- direct;
- energetic;
- tactile;
- organized;
- trustworthy.

It must NOT feel:

- generic SaaS;
- glassmorphism-heavy;
- corporate dashboard template;
- luxury ticketing;
- cyberpunk;
- chaotic;
- gamified;
- like an AI-generated component gallery.

Expression density:

```text
CUSTOMER  → HIGH brand expression / MEDIUM information density
CHECKOUT  → MEDIUM-LOW expression / HIGH certainty
ORGANIZER → LOW-MEDIUM expression / HIGH information density
GATE      → MINIMAL expression / MAXIMUM operational clarity
```

The event supplies emotion. The product supplies structure and confidence.

---

# 2. Frozen visual tokens

This section intentionally closes token gaps that were left open in the reviewed v1.1 pack. Antigravity MUST centralize these values; it may not invent one-off replacements.

## 2.1 Typography

### Display

```text
Family: Anton
Fallback: Impact, Haettenschweiler, "Arial Narrow Bold", sans-serif
Weight: 400
```

Use for:

- hero title;
- event title when used as a display headline;
- page/section display heading;
- large dates/prices;
- reservation timer numerals;
- ticket title;
- highly expressive labels.

### UI / Text

```text
Family: Inter
Fallback: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif
Weights: 400 / 500 / 600 / 700 / 800
```

Use for everything functional: navigation, buttons, forms, metadata, helper text, alerts, organizer UI and gate operational copy.

### Type roles

```css
--type-display-xl-size: clamp(4rem, 8vw, 8rem);   /* 64 → 128 */
--type-display-xl-line: 0.90;
--type-display-xl-track: -0.02em;

--type-display-l-size: clamp(3rem, 6vw, 5.5rem); /* 48 → 88 */
--type-display-l-line: 0.94;
--type-display-l-track: -0.015em;

--type-display-m-size: clamp(2.25rem, 4vw, 3.5rem); /* 36 → 56 */
--type-display-m-line: 0.98;

--type-heading-l-size: 2rem;   /* 32 */
--type-heading-l-line: 1.12;

--type-heading-m-size: 1.5rem; /* 24 */
--type-heading-m-line: 1.25;

--type-body-l-size: 1.125rem;  /* 18 */
--type-body-l-line: 1.55;

--type-body-m-size: 1rem;      /* 16 */
--type-body-m-line: 1.50;

--type-body-s-size: 0.875rem;  /* 14 */
--type-body-s-line: 1.45;

--type-label-size: 0.75rem;    /* 12 */
--type-label-line: 1.35;
--type-label-track: 0.06em;
```

Rules:

- never shrink body/UI text to rescue a broken layout;
- long display titles wrap/reflow according to the Dynamic Content section;
- no page-specific font-size, line-height or letter-spacing hacks outside tokens without explicit revision;
- uppercase is intentional for display labels and primary actions, not for paragraphs.

## 2.2 Brand colors

These values are frozen from the approved visual direction.

```css
--brand-ink:        #10100F;
--brand-paper:      #F3E5D8;
--brand-paper-soft: #FFF8F0;
--brand-paper-deep: #D6C8B7;

--brand-orange: #F25709;
--brand-pink:   #E774A4;
--brand-yellow: #ECA705;
--brand-green:  #4D9A6C;
```

Use black/ink text on orange, pink, yellow and green brand surfaces unless a component-specific contrast test explicitly proves otherwise.

## 2.3 Functional neutrals

```css
--neutral-900: #10100F;
--neutral-700: #3F3D39;
--neutral-500: #6E6A63;
--neutral-300: #A5A098;
--neutral-200: #D6C8B7;
--neutral-100: #EEE3D7;
--neutral-050: #FFF8F0;
```

## 2.4 Semantic states

Semantic colors are separate from brand colors.

```css
--semantic-success:    #176B45;
--semantic-success-bg: #E7F4EC;

--semantic-warning:    #8A5B00;
--semantic-warning-bg: #FFF2CC;

--semantic-danger:     #B42318;
--semantic-danger-bg:  #FDECEA;

--semantic-info:       #175CD3;
--semantic-info-bg:    #EAF2FF;

--semantic-wrong-event: #A970FF;
```

State meaning ALWAYS includes text and/or icon. Never color alone.

## 2.5 Focus

```css
--focus-ring: #175CD3;
--focus-width: 3px;
--focus-offset: 2px;
```

Never remove `focus-visible` without replacing it with this or an equally visible tokenized treatment.

---

# 3. Spacing, grid and layout

## 3.1 Spacing scale

```text
4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 / 96
```

No arbitrary 17px/21px/37px spacing unless a documented geometric reason exists.

## 3.2 Layout tokens

```css
--page-padding-mobile: 16px;
--page-padding-tablet: 24px;
--page-padding-desktop: 32px;

--grid-gap-mobile: 16px;
--grid-gap-tablet: 24px;
--grid-gap-desktop: 24px;

--section-gap-mobile: 48px;
--section-gap-tablet: 64px;
--section-gap-desktop: 96px;

--content-max: 1440px;
```

## 3.3 Grid

```text
Mobile  < 768px       → 4 columns
Tablet  768–1199px    → 8 columns
Desktop >= 1200px     → 12 columns
```

The bands are behavior boundaries; the layout remains fluid inside them.

## 3.4 Radius

```css
--radius-control: 4px;
--radius-container: 8px;
--radius-overlay: 12px;
--radius-pill: 999px;
```

Do not turn the product into rounded-card SaaS UI. Tickets use perforation/serration language; collages/stickers may be irregular.

## 3.5 Elevation

Default cards/sections: **no drop shadow**. Prefer border, contrast, paper layers and spacing.

```css
--shadow-overlay: 0 8px 24px rgba(16,16,15,.12);
```

Use only for floating menus/dialogs/toasts where depth communicates behavior.

## 3.6 Layer scale

```text
base 0
content 10
sticky 20
dropdown 30
overlay 40
modal 50
toast 60
```

Decorative graphics never outrank functional overlays.

---

# 4. Motion

```css
--motion-fast: 120ms;
--motion-base: 180ms;
--motion-slow: 240ms;
--ease-standard: cubic-bezier(.2,0,0,1);
```

Allowed:

- 2–4px arrow movement;
- subtle background/border transition;
- scanner/result transition;
- state changes;
- restrained skeleton shimmer.

Forbidden:

- scroll hijacking;
- long decorative animations;
- parallax-heavy layouts;
- cursor followers;
- layout-changing hover;
- animation that blocks input.

Respect `prefers-reduced-motion`.

---

# 5. Logo, iconography and graphic language

## 5.1 TC seal variants

Only three official variants:

1. **Primary:** black seal + light `TC` — default on light surfaces.
2. **Inverse:** light seal + dark `TC` — Gate/dark contexts.
3. **Accent:** pink seal + dark `TC` — decorative/footer/campaign use only.

Do not invent additional logo variants.

## 5.2 Functional icons

Use one consistent outline icon family. Do not mix filled/outline/system sets arbitrarily. If an icon library already exists, it must visually match this rule. A new dependency requires explicit approval outside this spec.

Functional icons include search, account, calendar, location, share, plus/minus, arrow, camera, QR, warning, info, close and menu.

## 5.3 Decorative marks

Allowed brand marks:

- asterisk/star;
- hand underline;
- short scribble;
- dotted halftone;
- torn-paper edges;
- stamp/seal;
- simple hand-drawn attention lines.

Decorative layers:

- `pointer-events: none` when not interactive;
- `aria-hidden="true"` when semantically empty;
- never cover CTA, focus ring, price, QR, manual code, form error or important text.

---

# 6. Surfaces and texture

Default Customer canvas: warm `brand-paper` / `brand-paper-soft`.

Paper/grain texture is allowed at **low contrast** as atmosphere. Critical UI lives on controlled surfaces:

- input fields;
- payment form;
- status messages;
- QR/manual-code areas;
- Gate scanner/results;
- Organizer forms/tables.

Do not put noisy image/texture behind small functional text.

---

# 7. Component contracts

## 7.1 Buttons

Minimum interactive target: **44 × 44px**. Primary button height preferred: **48px**.

### Primary Customer

- dark/ink background;
- paper/light text;
- 4px radius;
- Inter 700;
- clear arrow when direction matters;
- hover: same dimensions, arrow moves up to 4px and subtle brand accent may appear;
- pressed: translateY(1px) only;
- focus: tokenized focus ring;
- loading: label describes operation; width remains stable;
- disabled: visually unavailable but text remains readable.

Do not randomly switch primary CTA between green/yellow/pink by page.

### Secondary

- transparent/paper surface;
- 1px ink border;
- dark text;
- same state discipline.

### Gate primary

Dedicated high-contrast dark-context variant is permitted. Its meaning must be operational, not decorative.

## 7.2 Inputs

- label outside/above input;
- 48px preferred height;
- 1px neutral border;
- 4px radius;
- error text directly associated with field;
- focus ring visible;
- error cannot rely on red border alone;
- preserve entered values after recoverable errors.

## 7.3 Search

Placeholder: **`Buscar eventos...`**.

Do NOT promise artist, venue or location search until the API supports it.

## 7.4 Status messages

Critical states live in-page and persist long enough to be understood. Toasts are for small non-critical confirmations such as `Link copiado` or `Alterações salvas`.

## 7.5 Skeleton / empty / error

States are surface-specific; do not use one generic empty card everywhere.

Every data area defines:

```text
initial → loading → loaded | empty | error
```

---

# 8. Interaction & feedback contract

Every important async mutation:

```text
IDLE → PENDING → SUCCESS | ERROR | UNKNOWN_RESULT
```

The UI must continuously answer:

- where am I?
- what can I do?
- what is happening now?
- what happened?
- what should I do next?
- is my state safe?

All important controls support:

```text
default / hover / focus-visible / pressed / loading / disabled
```

Mobile never depends on hover.

### Reservation example

```text
GARANTIR INGRESSOS →
↓
GARANTINDO SEUS INGRESSOS...
↓
✓ Reserva garantida — 09:42 restantes
```

Never show success before backend confirmation.

### Domain-specific errors

Prefer specific copy over `Algo deu errado`.

Examples:

- insufficient stock → explain the selected quantity is no longer available and return to quantity selection;
- sales closed → event remains viewable, but reservations are closed;
- expired hold → explain inventory was released and provide return-to-sectors action;
- forbidden → explain access is unavailable; do not expose internal security details.

---

# 9. Reservation and checkout UX

## 9.1 Single-sector rule

One Reservation = one `TicketSector` + quantity.

Event Detail is NOT a basket/cart.

```text
unselected available sector → SELECT / SELECIONAR
selected sector             → selected state + quantity stepper
sold out                    → unavailable
sales closed                → no purchase control
```

Only the active sector exposes an editable quantity. Changing sector before HOLD clears the previous pre-hold quantity/total.

## 9.2 Authentication before HOLD

```text
public event/sector/quantity selection
→ Reserve
→ if unauthenticated: Login
→ restore non-sensitive intent
→ authenticated HOLD request
→ pending/result feedback
```

Never persist payment data as purchase intent.

## 9.3 Timer

Backend `expiresAt` is authoritative.

```text
10:00–03:00 → NORMAL
02:59–01:00 → WARNING
00:59–00:00 → CRITICAL
after 00:00 → EXPIRED
```

NORMAL must not appear urgent. No blinking/panic animation. Screen reader announcements only at meaningful milestones such as 3 min, 1 min and expired.

## 9.4 Checkout

Desktop:

```text
Payment form | Timer + order summary
```

Mobile:

```text
Header
Timer
Compact order summary
Payment form
Demo notice
Primary CTA
```

Always show enough information to answer:

- event;
- sector;
- quantity;
- total;
- remaining hold time.

`Salvar cartão` is forbidden.

Demo notice is mandatory:

> **Ambiente de demonstração. Nenhuma cobrança real será realizada.**

Payment states:

```text
idle
processing
approved
declined
unknown-result
expired
```

Never interpret timeout as decline. The exact APPROVED/DECLINED trigger and unknown-result reconciliation must follow the backend Story/API; the frontend may not invent them.

---

# 10. Dynamic Event Imagery

Core rule:

> **The photography is dynamic. The composition is Tá em Cartaz.**

The Event image comes from the application's saved Event snapshot (`imageUrl`), not a fresh Ticketmaster call on every render.

The same image may be treated differently:

```text
Event list/card → restrained crop
Event Detail    → expressive collage/hero
Checkout        → restrained thumbnail
Ticket          → editorial/halftone treatment
Shared Ticket   → ticket treatment
Gate            → small recognisable thumbnail when useful
```

Never depend on:

- one face;
- centered portrait;
- transparent background;
- manually removed background;
- empty negative space in the source photo.

Implementation:

- controlled aspect ratio;
- `object-fit: cover`;
- center crop by default;
- reserve dimensions before load;
- visual overlays owned by the design system, not baked into source assumptions;
- functional text remains independent from the image.

## 10.1 Missing image

If `imageUrl` is null or load fails, render `BrandedImageFallback`:

- event theme color;
- TC seal;
- `CULTURA QUE CONECTA.` motif;
- simple dots/tear/scribble;
- optional event title where space allows.

Never show browser broken-image UI.

## 10.2 Stress fixtures

Every expressive surface must survive:

1. portrait;
2. band/group;
3. stage/crowd;
4. poster/artwork;
5. architecture/cultural venue;
6. missing image;
7. failing image URL;
8. **combined chaos:** difficult image + long title + long venue + long sector + large price + mobile 360px.

---

# 11. Dynamic text

No guaranteed `tourName/subtitle` exists. Never parse description to manufacture one.

All layouts must work with title only.

Test:

- very long title;
- long venue/address;
- long sector name;
- long sector description;
- large prices;
- unusual categories.

Rules:

- wrap before shrinking;
- use line-clamp only where losing the tail does not hide critical meaning;
- full title is visible on Event Detail and Ticket where practical;
- discovery cards/rows may clamp intentionally;
- no CTA/QR collision;
- no text overlay dependent on photo negative space.

---

# 12. Dynamic Ticket system

The ticket is a reusable brand object, not a one-off artist poster.

## 12.1 Stable visual structure

Desktop visual concept:

```text
┌───────────────────────────────────────┬──────────────┐
│ event / artwork / date / venue        │ QR           │
│ sector / status                       │ manual code  │
│ brand seal / expressive theme         │ clean stub   │
└───────────────────────────────────────┴──────────────┘
```

Mobile transforms vertically; it is not a scaled-down horizontal ticket.

## 12.2 Dynamic fields

Use only fields actually exposed by the API/DTO, typically:

- event title;
- image;
- date;
- venue;
- sector;
- ticket status;
- QR payload/render;
- manual code;
- display-safe identifier only if the DTO defines one.

Forbidden unless a later domain/API explicitly adds them:

- seat;
- row;
- gate/portão;
- `INTEIRA/MEIA`;
- invented sequential ticket number.

## 12.3 Theme

Approved presentation-only rule:

```text
hash(eventId) → yellow | pink | orange | green
```

The same event must keep the same theme between renders. No database field is required.

## 12.4 QR protection

- high contrast;
- clean quiet zone;
- no texture/halftone behind or over QR;
- no decorative overlap;
- no clipping;
- manual code stays visible.

## 12.5 Multiple tickets

Reservation quantity `N` = `N` separate ticket instances.

My Tickets must expose a deliberate selector such as:

```text
Ingresso 1 de 3
[1] [2] [3]
```

or an equivalent accessible pager.

Each focused ticket has its own:

- status;
- QR;
- manualCode;
- share action.

`Compartilhar este ingresso` shares the focused ticket only.

## 12.6 USED

A used ticket remains available as history/collectible, but:

- `UTILIZADO` becomes dominant;
- the QR is de-emphasized;
- the layout must not imply entry remains possible.

---

# 13. Screen specifications

## 13.1 Global header

### Desktop

- primary TC seal + `TÁ EM CARTAZ`;
- real primary route(s), especially Events/discovery;
- search `Buscar eventos...`;
- guest: `Entrar` only;
- authenticated: role-appropriate account/navigation.

Forbidden by default:

- `Cadastrar` without a registration capability;
- `Cultura` / `Perto de você` as navigation if no real route/filter exists;
- fake dropdowns/actions.

### Mobile

```text
[TC] TÁ EM CARTAZ                 SEARCH  MENU
```

Overlay/menu must manage focus: focus enters when opened, Escape closes, focus returns to trigger.

Header baseline heights:

```text
desktop 72px
mobile  64px
```

## 13.2 Home / Discovery

Visual reference priority: `references/home_desktop_direction.png` and `home_mobile_direction.png`.

### Hero

- brand/editorial by default;
- expressive `TÁ EM CARTAZ` display typography;
- collage/festival imagery;
- no hidden `featuredEvent` semantics;
- do not bind to first API event result.

### Category collage strip

The colored `Shows / Festivais / Cultura / Perto de você` composition is a **visual motif**.

If a corresponding real route/filter exists, it may be interactive with arrow/hover/focus.

If not, it must be clearly non-interactive: no arrow, no pointer cursor, no fake hover. It may be omitted if it buries discovery.

`Perto de você` never silently requests device location.

### Em Cartaz

This is the main functional discovery block.

Each row/card prioritizes:

- image/thumbnail where appropriate;
- title;
- date;
- venue/location;
- starting price;
- clear `Ver evento` arrow/action.

Do not show internal/debug IDs. Do not use `+` if it can be interpreted as add-to-cart.

Upcoming events should be visually prioritized; final ordering must follow/confirm API behavior and must not invent persisted state.

### Mobile priority

Hero remains expressive but shorter. `Em Cartaz` must appear early enough that event discovery is not buried.

### Footer

Use the approved paper/editorial footer composition, but render **only real routes/actions**. No careers, press, partner, app-store, newsletter or support claims unless implemented. Accent TC seal may be used here.

## 13.3 Event Detail / Sector Selection

- expressive dynamic Event artwork;
- critical title/date/venue outside image dependency;
- category badge optional if real;
- title must work without subtitle;
- list actual sectors.

### Sector card

Unselected:

```text
Sector name
real description when available
availability
unit price
[ SELECIONAR ]
```

Selected:

```text
SELECTED state
Sector name
availability
unit price
[-] quantity [+]
```

Only one selected sector at a time.

`LOW AVAILABILITY` may not be fabricated. Show actual quantity / sold out / sales closed until a backend rule exists.

### Mobile

Sectors stack. When quantity > 0, use a safe-area-aware sticky summary:

```text
2 ingressos · R$ 298
[ GARANTIR INGRESSOS → ]
```

It must not cover the last sector card.

## 13.4 Checkout

Most restrained Customer screen.

Must show:

- timer;
- event thumbnail/summary;
- sector;
- quantity;
- total;
- payment form/controls defined by backend Story;
- demo notice;
- primary action;
- clear approved/declined/unknown/expired states.

No visual clutter behind form controls.

## 13.5 My Tickets

Page first helps the user locate tickets, then enjoy the collectible object.

- group/list by event as API permits;
- focus one ticket at a time;
- visible `1 de N` when multiple;
- dynamic artwork/theme;
- status prominent;
- QR/manual/share ticket-scoped;
- no invented ticket fields.

Mobile ticket becomes vertical. Desktop may use horizontal physical-ticket language.

## 13.6 Shared Ticket

Render one individual Ticket via share token.

- no owner PII;
- same visual identity as private ticket;
- loading / invalid token / USED / image fallback states;
- exact credential exposure follows backend DTO; do not infer.

## 13.7 Gate

Dark, mobile/tablet-first, operational.

Hierarchy:

```text
Selected Event
Scanner
Manual-code fallback
Validation state/action
ONE dominant result
Next action
```

Result states:

- VALID — success icon + text;
- INVALID — danger icon + text;
- ALREADY_USED — warning icon + text;
- WRONG_EVENT — distinct icon + text.

Never show the four-state gallery on the operational scanner screen. The gallery is documentation only.

Camera denied/unavailable immediately exposes manual-code fallback and a clear retry-camera action.

## 13.8 Organizer

Same brand, much less decorative.

Visual baseline:

- light/paper canvas;
- strong typography but smaller display usage;
- structured sidebar/top nav;
- black/ink primary structure;
- brand accent only for selected/status emphasis;
- forms/tables/cards highly legible;
- no fake analytics.

### My Events

Show title, date, DRAFT/PUBLISHED/SALES CLOSED meaning and useful inventory summary if API supplies it.

### Create/Edit Event

- Ticketmaster import/search step;
- imported snapshot preview;
- local date/venue/content form;
- save feedback;
- structural fields visibly locked after publish with explanation;
- description/image/category remain editable according to domain.

### Sectors

Use real fields only: name, description, capacity, available quantity display and price. Surface backend constraint errors specifically.

---

# 14. Responsive transformation rules

At minimum QA:

```text
360×800
390×844
430×932
768×1024
1024×768
1280×800
1440×900
```

Also drag through intermediate widths.

Transformations:

```text
Desktop nav      → mobile compact header/menu
12 col           → 8 col → 4 col
Hero horizontal  → stacked/recomposed
Category 4-wide  → 2×2 / omitted if nonfunctional
Sector row       → vertical card
Checkout 2-col   → 1-col
Ticket horizontal→ vertical
Footer columns   → stacked groups
Purchase CTA     → sticky on mobile after valid selection
Gate             → remains mobile-first
Organizer table  → stacked/responsive cards where needed
```

No horizontal page overflow. Sticky UI respects safe areas.

---

# 15. Content honesty / forbidden invention list

Unless current approved API/Story explicitly adds it, DO NOT implement:

- multi-sector basket/cart;
- public `Cadastrar`;
- artist/venue/location search promise;
- GPS / silent geolocation;
- implicit featured-event ranking;
- seat / row / gate/portão;
- `INTEIRA/MEIA`;
- fake sequential ticket number;
- `Salvar cartão`;
- fabricated sector benefits;
- App Store / Google Play promo;
- newsletter capture;
- careers/press/partner destinations;
- internal/debug IDs in discovery;
- `LOW_AVAILABILITY` based on an arbitrary frontend threshold;
- fake analytics/KPIs;
- invented payment outcome logic;
- invented registration/recovery flows.

---

# 16. Backend-dependent UX blockers

These are NOT visual decisions. Antigravity must inspect the current Story/API before implementing the affected flow:

1. deterministic fake-payment APPROVED/DECLINED trigger;
2. payment UNKNOWN_RESULT reconciliation;
3. active HOLD recovery after refresh;
4. concrete login-intent restoration mechanism;
5. private/shared Ticket DTO credential boundaries;
6. shared Ticket status exposure after use;
7. reservation maximum quantity if any;
8. public event ordering / started-but-PUBLISHED handling;
9. display-safe Ticket identifier, if needed.

If absent, STOP for that behavior. Do not invent a local frontend truth.

---

# 17. Accessibility baseline

- state never depends only on color;
- keyboard access for essential controls;
- visible focus;
- errors programmatically associated with inputs;
- logical headings;
- semantic buttons/links;
- touch targets >= 44px;
- meaningful image alt; decorative layers ignored by AT;
- timer not announced every second;
- Gate result has text + icon + semantic status;
- reduced-motion supported;
- zoom/reflow must remain usable at 200%.

---

# 18. Required test behavior

Component/behavior tests prioritize:

- Button states;
- sector selection/stepper;
- timer thresholds;
- image fallback;
- payment validation/feedback;
- ticket selector `1 de N`;
- Gate result behavior;
- search states.

E2E critical journeys:

```text
Customer approved purchase
Customer declined purchase + retry
Reservation expiry
Organizer import/create/sector/publish
Gate VALID → second scan ALREADY_USED
Gate INVALID
Gate WRONG_EVENT
```

Visual regression targets:

- Home desktop/mobile;
- Event Detail desktop/mobile;
- Checkout desktop/mobile;
- Ticket desktop/mobile;
- Gate mobile;
- dynamic ticket themes;
- missing image fallback.

Every expressive screen also runs dynamic image/text fixtures and combined-chaos 360px fixture.

---

# 19. Implementation completion rule

A screen is complete only when:

1. it uses real domain/API fields;
2. layout matches this spec and its designated reference;
3. desktop/mobile behavior is verified;
4. hover/focus/pressed/loading/disabled are implemented;
5. loading/empty/error are implemented where applicable;
6. long text and missing images do not break it;
7. no forbidden mockup-only behavior was added;
8. relevant tests pass;
9. browser console has no relevant errors;
10. Antigravity captures desktop + mobile screenshots and reports deviations before moving on.

