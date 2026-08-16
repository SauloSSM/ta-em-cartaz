# Tá em Cartaz — Visual Reference Manifest

**Rule:** images define composition, density and personality. `01_VISUAL_UX_SPEC_FINAL.md` defines actual behavior/content.

If an image conflicts with the final spec, **the spec wins**.

## 1. `home_desktop_direction.png`

Use for:
- off-white/paper canvas;
- expressive brand-led hero;
- large condensed `TÁ EM CARTAZ` title;
- collage language;
- four-color category-strip visual motif;
- editorial `Em Cartaz` list rhythm;
- strong black CTA language.

IGNORE/REPLACE:
- `Cadastrar`;
- search copy promising artists/locations;
- `Cultura` / `Perto de você` nav unless real;
- internal TC debug/event codes;
- plus symbols that imply add-to-cart;
- newsletter CTA unless implemented.

## 2. `home_mobile_direction.png`

Use for:
- mobile vertical recomposition;
- logo/search/menu hierarchy;
- 2×2 colored category composition;
- `Em Cartaz` list reflow;
- stacked branded footer energy.

IGNORE/REPLACE:
- app-store download CTA;
- fake footer routes;
- any interactive category behavior not supported by product.

## 3. `five_screen_board_direction.png`

Use as a **composition board**, not literal data contract.

Use for:
- relative expression density between Home, Event, Checkout, Ticket and Gate;
- physical-ticket language;
- checkout restraint;
- Gate dark mode/result hierarchy.

IGNORE/REPLACE:
- multiple sector steppers active simultaneously;
- `Salvar cartão`;
- ticket seat/row/gate/`INTEIRA`;
- any artist-specific content;
- all mock IDs.

## 4. `event_mobile_direction.png`

Use for:
- title + dynamic image hierarchy;
- expressive image framing;
- stacked sector cards;
- selected sector emphasis;
- sticky total/CTA composition.

REQUIRED CORRECTION:
- only ONE sector may expose active quantity selection at a time; other available sectors use `Selecionar`.

## 5. `checkout_mobile_direction.png`

Use for:
- large but controlled timer;
- order summary before form;
- restrained paper UI;
- clear demo notice;
- single strong CTA.

IGNORE/REPLACE:
- any card-storage behavior;
- any payment outcome behavior not provided by backend.

## 6. `ticket_desktop_direction.png`

Use for:
- horizontal physical ticket silhouette;
- expressive color field + clean QR stub;
- image halftone/editorial treatment;
- strong event title;
- perforation language;
- QR quiet functional area.

IGNORE/REPLACE:
- seat;
- row;
- gate;
- `INTEIRA`;
- fake sequential ticket number.

## 7. `ticket_mobile_direction.png`

Use for:
- vertical ticket transformation;
- serration/perforation;
- status/event/artwork hierarchy;
- large protected QR;
- manual code + share CTA.

Same forbidden fields as desktop.

## 8. `gate_mobile_direction.png`

Use for:
- dark mobile-first operational surface;
- selected-event context;
- large scanner frame;
- manual-code fallback;
- high-contrast validation CTA.

Do NOT reproduce a permanent gallery of four results on the scanner screen. After validation show ONE dominant result.

## 9. `dynamic_ticket_stress_test.png`

Documentation/stress-test reference only.

Use to verify:
- portrait;
- group;
- festival/crowd;
- artwork;
- culture/architecture;
- missing-image fallback;
- stable ticket identity across different content.

Do not copy fabricated event fields/content.

## 10. `design_system_board_direction.png`

Use for overall visual rhythm only. Exact values come from the final spec, not text rendered inside this image.

## 11. `footer_direction.png`

Use for:
- horizontal editorial footer composition on desktop;
- accent TC seal;
- separators;
- ticket/stamp motif.

Only real project routes/actions may be rendered. Fake Help/Careers/Press/Partner/App QR destinations are forbidden.
