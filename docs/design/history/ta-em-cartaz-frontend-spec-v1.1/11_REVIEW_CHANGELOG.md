# Tá em Cartaz — Frontend Review Changelog v1.1

**Review date:** 2026-08-15  
**Scope:** Rigorous re-review after visual mockups, responsive explorations, Dynamic Event Imagery and Dynamic Ticket stress tests.

The review intentionally ignores the identity of people/artists used in mockups. It evaluates the **layout system, data assumptions, multiplicity, interaction semantics, dynamic content, responsive behavior and feedback contracts**.

---

# 1. Why this revision was necessary

The visual direction exposed product/UX assumptions that were not obvious in the earlier wireflows. Generated imagery was useful for composition, but it also introduced plausible-looking data and behaviors that the approved domain never defined.

The v1.1 review therefore follows this rule:

> Mockups can discover UX problems. They cannot create domain truth.

---

# 2. Highest-impact corrections

## Single-sector purchase

Before:
- multiple sector cards visually exposed independent steppers;
- could imply a multi-sector cart.

Now:
- one active sector purchase intent;
- selected sector owns quantity stepper;
- changing sector before HOLD clears previous pre-hold quantity/total.

## Multi-ticket experience

Before:
- My Tickets design focused on one large collectible ticket.

Now:
- quantity `N` means `N` distinct ticket instances;
- customer intentionally selects `Ingresso 1 de N`;
- QR/manual/share/status are individual.

## Authentication branch

Before:
- visual feedback could imply hold was created immediately after Reserve.

Now:
- public selection → Login if needed → restore intent → authenticated HOLD request → confirmation.

## Honest navigation/search

Removed assumptions:
- public `Cadastrar`;
- artist/location search promise;
- implicit featured-event ranking;
- silent device geolocation from `Perto de você`;
- dead clickable category cards.

## Dynamic content resilience

Added:
- critical text independent from image negative space;
- center crop default; no invented focal-point model;
- combined-chaos fixture;
- multi-ticket dynamic rendering.

---

# 3. Mockup details explicitly rejected

Unless a later approved Story/domain adds them:

- seat / row / gate/portão;
- `INTEIRA/MEIA`;
- fake sequential ticket number;
- `Salvar cartão`;
- App Store / Google Play promotion;
- fabricated sector amenities;
- internal/debug identifiers in discovery.

---

# 4. Design-system hardening added

- centralized page padding / grid gap / section gap;
- centralized breakpoints;
- centralized z-index layers;
- decorative safety rules (`pointer-events`, `aria-hidden`, collision boundaries);
- type-scale discipline beyond simply “two font families”;
- functional surfaces protected from paper texture/noisy imagery.

---

# 5. Remaining implementation blockers

These were **not** silently solved because they depend on backend/API Stories:

1. deterministic evaluator-visible trigger for fake payment APPROVED vs DECLINED;
2. payment timeout / UNKNOWN_RESULT reconciliation;
3. active HOLD recovery after refresh;
4. concrete login-intent restoration mechanism;
5. exact private/shared ticket DTO credential boundaries;
6. shared ticket status exposure after use;
7. reservation max quantity if any;
8. public event ordering / treatment of started-but-PUBLISHED events;
9. safe display identifier for a Ticket, if the UI needs one.

These are tracked in `08_LOGIC_GAPS_REVIEW.md` and must be checked against the corresponding backend Story before frontend implementation.

---

# 6. Still-open visual token decisions

The system constraints are stable, but final values remain intentionally open until the production token pass:

- exact two font families;
- canonical brand/semantic hex values;
- exact typography sizes/line-heights/tracking;
- motion timings;
- exact radius/elevation tokens.

Coding agents must centralize temporary tokens rather than scatter guessed values.

---

# 7. Result

The frontend pack is now materially safer for Antigravity/Codex because it distinguishes:

```text
VISUAL INTENT
from
DOMAIN/API TRUTH
from
BACKEND-DEPENDENT UX
```

The main visual identity does **not** need to be redesigned. The important corrections are behavioral and structural: single-sector intent, multi-ticket multiplicity, honest controls, dynamic-content resilience and explicit feedback/recovery contracts.
