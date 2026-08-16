# Tá em Cartaz — Frontend Specification Pack

**Status:** Reviewed v1.1 — implementation baseline with explicit backend/API blockers  
**Purpose:** Consolidate UX, visual-system, responsive, dynamic-content, feedback, testing, and implementation rules before full frontend implementation.  
**Authority:** This pack does **not** override the official challenge or the approved Project/Domain Specification.

---

## 1. Source hierarchy

When there is conflict, use this order:

1. `Desafio-Elite-Dev-2026.pdf` — external requirement authority.
2. Latest approved Project / Domain Specification — implementation authority.
3. Approved ADRs.
4. UX / Wireflows / Design System / this frontend pack.
5. Historical conversations, visual explorations, generated mockups.

Rules:

- Challenge requirements > internal decisions.
- Newer approved spec/ADR > historical document.
- Visual references **never override domain/API reality**.
- If implementation requires a new product/domain/security/architecture decision: **STOP and flag it**.

---

## 2. Documents in this pack

| File | Purpose |
|---|---|
| `01_DESIGN_SYSTEM.md` | Visual language, typography policy, color roles, spacing, grid, iconography, component consistency |
| `02_UX_INTERACTION_CONTRACT.md` | Interaction states, feedback, async actions, errors, timers, reservation/payment/gate behavior |
| `03_RESPONSIVE_SPEC.md` | Desktop/tablet/mobile layout transformation rules |
| `04_DYNAMIC_CONTENT_SPEC.md` | Dynamic Ticketmaster imagery, missing-image fallback, dynamic text, ticket artwork rules |
| `05_SCREEN_BEHAVIOR_SPEC.md` | Screen-by-screen behavioral contract for Customer, Organizer and Gate |
| `06_FRONTEND_TEST_PLAN.md` | Unit/component, integration, E2E, visual regression, accessibility and manual QA |
| `07_VISUAL_QA_CHECKLIST.md` | Repeatable QA checklist for every implemented screen |
| `08_LOGIC_GAPS_REVIEW.md` | Open questions, contradictions, risks and decisions still needing confirmation |
| `09_AGENT_IMPLEMENTATION_GUIDE.md` | Guardrails for Antigravity/Codex while implementing UI |
| `10_DOMAIN_UI_MAPPING.md` | Canonical mapping from domain/API concepts to UI multiplicity, fields, actions and forbidden assumptions |
| `11_REVIEW_CHANGELOG.md` | What changed in the rigorous visual/UX review, why, and what still blocks implementation |

---

## 3. Current visual direction

Concept:

> **Festival culture organized by product thinking.**

Visual direction:

- Neo-Swiss / Swiss Punk
- Festival Editorial
- Printed-ticket / poster language
- Acid/Rave accents used selectively
- Off-white paper-like surfaces
- Black structure
- Orange / pink / yellow / green expressive accents
- Photography and collage as emotional layer
- Functional UI remains highly legible and grid-disciplined

Expression by experience:

```text
CUSTOMER  → HIGH brand expression / MEDIUM information density
ORGANIZER → LOW-MEDIUM expression / HIGH information density
GATE      → MINIMAL expression / MAXIMUM clarity
```

---

## 4. Visual reference rule

Generated visual references define:

- hierarchy;
- visual density;
- composition;
- intended personality;
- relative color roles;
- graphic language;
- component appearance direction.

They do **not** define or invent:

- domain fields;
- API contracts;
- product rules;
- persistence models;
- payment rules;
- seat/row/gate data not available in the domain;
- mobile app functionality;
- card-storage behavior.

If a mockup contains data/functionality absent from the approved Project Spec, implementation must use the nearest valid domain data instead.

---

## 5. Known visual references

Recommended reference categories to version in `docs/design/references/`:

- Home — desktop
- Home — mobile
- Event Detail / Sector selection — desktop
- Event Detail / Sector selection — mobile
- Checkout — desktop
- Checkout — mobile
- My Ticket — desktop
- My Ticket — mobile
- Gate — mobile
- Dynamic Event Imagery Stress Test
- Dynamic Ticket Stress Test
- Design System boards

The stress-test boards are **documentation**, not literal production screens.

---

## 6. Implementation readiness

Before a screen is considered implementation-ready, it should have:

- real domain fields mapped;
- responsive behavior defined;
- loading/empty/error states defined;
- hover/focus/pressed/loading/disabled states for interactive controls;
- long-text behavior defined;
- missing-image behavior defined;
- critical async feedback defined;
- accessibility considerations defined;
- test cases defined.

---

## 7. Important principle

> **The poster may break the grid. The button may not.**

Functional elements must be consistent.

Decorative elements may be expressive.



---

## 8. v1.1 review decisions

This review treats generated mockups as **visual explorations**, never as product truth. The following rules were promoted to the frontend baseline because they follow the approved domain/UX and remove ambiguity discovered during visual design:

1. A reservation represents **one sector + one quantity**. The UI is not a multi-sector cart.
2. A confirmed reservation with quantity `N` produces **N individual tickets**. My Tickets must support ticket multiplicity.
3. Sharing acts on **one Ticket/shareToken at a time**.
4. Public browsing does not require authentication; a visitor who starts a purchase must authenticate **before the HOLD is created**.
5. No public `Cadastrar` action is shown unless registration becomes an approved capability.
6. Search UI promises only what is approved: **event-name search**. Do not advertise artist/location search until supported.
7. Category/locality cards are interactive only when a real route/filter exists. `Perto de você` must not silently imply device geolocation.
8. The Home hero is brand/editorial by default. It must not silently designate a “featured event” without an explicit product rule.
9. Event artwork is dynamic, but functional text never depends on negative space inside the image.
10. Multiple-ticket, long-text, missing-image, mobile and combined-chaos fixtures are mandatory QA cases.
11. Generated fields such as seat/row/gate/`INTEIRA`, save-card, app-store promotion and fabricated amenities remain forbidden unless the domain changes explicitly.
12. Backend-dependent questions (payment reconciliation, active-reservation recovery, exact share DTO, demo outcome trigger) remain blockers and must be resolved against the relevant Story/API before implementing that affected flow.

### Implementation classification

```text
APPROVED FRONTEND RULE
→ agent may implement directly

BACKEND/API DEPENDENCY
→ agent must inspect current Story/API before implementing

PRODUCT/DOMAIN CHANGE
→ STOP; do not invent
```

---

## 9. Readiness gate

A screen is not ready for visual implementation if any `P0` or backend-dependent item affecting that screen remains unresolved in `08_LOGIC_GAPS_REVIEW.md`.

The coding agent must also read `10_DOMAIN_UI_MAPPING.md` before implementing Event Detail, Checkout, My Tickets, Shared Ticket or Gate.
