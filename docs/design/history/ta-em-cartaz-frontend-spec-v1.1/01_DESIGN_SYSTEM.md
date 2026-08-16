# Tá em Cartaz — Design System v1.1 Reviewed Baseline

**Status:** Reviewed baseline; exact font families and canonical hex tokens remain intentionally open  
**Scope:** Frontend visual-system rules.  
**Important:** Exact font families and exact hex values remain open until explicitly approved. The **roles and constraints** below are considered the intended system.

---

# 1. Core principles

1. **Structure first. Expression second.**
2. Maximum **2 font families**.
3. Functional UI follows a consistent grid and spacing scale.
4. Decorative graphics may intentionally break the grid.
5. Brand colors do not replace semantic state colors.
6. The same component should look and behave the same wherever it appears.
7. Customer can be expressive; Checkout, Organizer and Gate become progressively more restrained.
8. No visual decision may reduce clarity, accessibility, or conversion.

---

# 2. Typography

## 2.1 Maximum two families

### A. Display family

Use for:

- hero titles;
- event names;
- section headings;
- large dates;
- large prices;
- ticket title;
- timer numerals;
- expressive labels.

Characteristics:

- condensed / bold / editorial;
- strong hierarchy;
- suitable for very large type.

### B. UI / Text family

Use for:

- navigation;
- forms;
- body copy;
- status messages;
- helper text;
- addresses;
- buttons;
- metadata;
- tables / organizer UI.

Characteristics:

- highly readable;
- clear at small sizes;
- multiple useful weights.

### Rule

Do not introduce a third family to solve a one-off styling problem.

---

## 2.2 Suggested type-role scale

Exact font sizes may be tuned in implementation, but keep roles stable.

```text
Display XL  → hero / brand statement
Display L   → event title / major section
Display M   → ticket title / page title
Heading L   → section heading
Heading M   → card / panel heading
Body L      → important explanatory text
Body M      → default body
Body S      → metadata
Label       → small uppercase tags / field labels
```

Use responsive `clamp()` where appropriate for large display type.

---

# 3. Spacing

Approved base scale:

```text
space-1 = 4px
space-2 = 8px
space-3 = 12px
space-4 = 16px
space-5 = 24px
space-6 = 32px
space-7 = 48px
space-8 = 64px
space-9 = 96px
```

Rules:

```text
within a tiny control             → 4 / 8
related content                   → 8 / 12 / 16
component internal grouping       → 16 / 24
component-to-component            → 24 / 32
major section separation          → 48 / 64 / 96
```

Avoid arbitrary spacing such as 17px, 21px, 37px unless there is a documented geometric reason.


## 3.1 Layout spacing tokens

The base spacing scale is not enough by itself. All pages must also use centralized layout roles:

```text
layout.page-padding.mobile  = 16px
layout.page-padding.tablet  = 24px
layout.page-padding.desktop = 32px

layout.grid-gap.mobile      = 16px
layout.grid-gap.tablet      = 24px
layout.grid-gap.desktop     = 24px

layout.section-gap.mobile   = 48px
layout.section-gap.tablet   = 64px
layout.section-gap.desktop  = 96px
```

These are the baseline values. A component may not invent its own page gutter or section rhythm.

---

# 4. Grid

Target system:

```text
Desktop → 12-column grid
Tablet  → 8-column grid
Mobile  → 4-column grid
```

Container widths should be fluid with reasonable max widths.


Canonical responsive bands for implementation:

```text
mobile  < 768px
tablet  768px–1199px
desktop >= 1200px
```

Use one centralized breakpoint source. Do not let components define unrelated breakpoints locally. Fluid behavior inside each band is required; these are behavior boundaries, not fixed canvases.

Functional alignment must respect grid:

- form fields;
- CTAs;
- ticket metadata;
- card content;
- prices;
- QR region;
- organizer tables/forms.

Decorative elements may break grid:

- torn-paper overlays;
- doodles;
- halftone;
- hand-drawn marks;
- collage shapes;
- decorative stamps.

---

# 5. Color system

## 5.1 Brand palette roles

Current expressive palette family:

- Black / near-black
- Warm off-white / paper
- Orange
- Pink
- Yellow
- Green

Exact hex values: **OPEN — must be approved from final references / implementation token pass.**

Suggested role model:

```text
brand.ink
brand.paper
brand.orange
brand.pink
brand.yellow
brand.green
```

Use expressive colors for:

- category surfaces;
- posters/collage;
- ticket themes;
- campaign blocks;
- selected decorative accents;
- branded fallback imagery.

---

## 5.2 Semantic colors

Semantic states are separate tokens:

```text
semantic.success
semantic.warning
semantic.danger
semantic.info
semantic.neutral
```

Never assume:

- green decorative surface = success;
- yellow category = warning;
- pink identity = danger.

State must always include text and/or icon, not color alone.

---

# 6. Logo / TC seal variants

Freeze only these conceptual variants:

### Primary
- black seal;
- white/light TC;
- default on light surfaces.

### Inverse
- light seal;
- dark TC;
- Gate / dark contexts.

### Accent
- pink or brand accent seal;
- decorative/footer/campaign use.

Accent variant is not the default product logo.

---

# 7. Borders, corners and physical language

Do not make every container a rounded SaaS card.

Suggested hierarchy:

```text
Inputs/buttons             → small radius
Functional containers      → small-medium radius
Modal/panel                → medium radius
Ticket                     → perforation / serration language
Collage                    → irregular / torn-paper masks
Decorative sticker         → irregular
```

Irregularity is deliberate and decorative, never random on functional controls.

---

# 8. Iconography

## Functional icons

Use one coherent icon family with consistent:

- stroke weight;
- optical size;
- line caps;
- visual density.

Examples:

- search;
- user;
- calendar;
- location;
- share;
- plus/minus;
- arrow;
- camera;
- QR;
- warning/info.

## Decorative marks

Doodles are separate from functional iconography:

- stars;
- scribbles;
- hand-drawn arrows;
- underlines;
- dots;
- torn paper.

Never use a doodle as the sole representation of a system action.

---

# 9. Primary actions

Customer default primary action:

- stable high-contrast button;
- black/dark base is preferred;
- brand accent can appear on hover/indicator.

Examples:

- `GARANTIR INGRESSOS`
- `FINALIZAR PAGAMENTO`
- `COMPARTILHAR INGRESSO`

Gate may use a dedicated high-contrast variant suitable for dark mode.

Do not randomly switch primary button color by page.

---

# 10. Component visual states

Every interactive component must define:

```text
default
hover
focus-visible
pressed
loading
disabled
```

Additional states where relevant:

```text
selected
success
warning
error
```

Hover must not change component dimensions or cause layout shift.

Focus-visible must be obvious and must not be removed.

---

# 11. Motion

Motion exists to orient, not impress.

Allowed:

- subtle arrow movement;
- button feedback;
- state transitions;
- skeleton shimmer only if subtle;
- scanner feedback;
- success/error state transitions.

Avoid:

- scroll hijacking;
- excessive parallax;
- long decorative animations;
- elements following cursor;
- animation that blocks input.

Support `prefers-reduced-motion`.

---

# 12. Expression density

## Customer

Allowed:

- collage;
- event imagery;
- expressive typography;
- color blocks;
- ticket language.

## Checkout

Reduce expression.

Prioritize:

- form clarity;
- timer clarity;
- payment certainty;
- total visibility.

## Organizer

Low-medium expression.

Prioritize:

- task completion;
- state;
- inventory;
- forms;
- legibility.

## Gate

Minimal expression.

Prioritize:

- camera;
- selected event;
- validation result;
- next action.

---

# 13. Decorative safety and functional surfaces

Decorative layers must never become interactive or block interaction:

- use `pointer-events: none` for purely decorative overlays;
- decorative SVG/images should be hidden from assistive technology when they add no meaning;
- doodles, torn paper and halftone must not cover focus rings, form errors, prices, CTAs, QR codes or manual codes;
- critical text, forms, alerts and QR surfaces use controlled high-legibility backgrounds;
- paper texture is a background treatment, not a license to reduce text contrast.

Functional content may overlap decorative artwork only when readability remains invariant across dynamic images.

---

# 14. Typography token discipline

Two font families alone do not guarantee consistency. Before the first production visual screen, freeze a centralized type scale with:

```text
font-family
font-size
line-height
font-weight
letter-spacing
text-transform (only when intentional)
```

Rules:

- large display text may use responsive `clamp()`;
- body/UI text may not shrink to rescue a broken layout;
- long titles reflow/clamp according to component rules in `04_DYNAMIC_CONTENT_SPEC.md`;
- do not create one-off tracking/line-height values inside page CSS.

---

# 15. Layer / z-index contract

Use a small centralized layer scale to avoid sticky CTA, menu, toast and modal collisions:

```text
z.base     = 0
z.content  = 10
z.sticky   = 20
z.dropdown = 30
z.overlay  = 40
z.modal    = 50
z.toast    = 60
```

Decorative graphics normally remain at `base/content`; they must not outrank functional overlays.

---

# 16. Visual consistency boundary

The more functionally important an element is, the more visually consistent it must be.

```text
Inputs / Buttons / Timer / QR / Errors → maximum consistency
Event cards / Ticket                   → high consistency
Hero / Collage                         → moderate consistency
Doodles / decorative marks             → high expressive freedom
```

