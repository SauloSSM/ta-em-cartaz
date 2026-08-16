---
title: 'Story 4.5 — Exibir checkout e timer autoritativo do hold'
type: 'feature'
created: '2026-08-16'
status: 'done'
baseline_commit: 'HEAD'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-EliteDevTicket-2026-08-12/EXPERIENCE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-EliteDevTicket-2026-08-12/DESIGN.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Clientes autenticados com uma reserva `HOLDING` vigente precisam acompanhar claramente sua reserva na Superfície S04 (Checkout), sabendo exatamente qual evento, setor, quantidade e preços snapshots foram garantidos, sem recalcular valores com base em alterações posteriores do evento ou setor. O cronômetro de contagem regressiva deve ser estritamente derivado da autoridade do backend (`serverNow` e `expiresAt`), avançar localmente de forma monotônica via `performance.now()` sem sofrer drift ou manipulação do relógio civil do sistema operacional, e reconciliar com o backend em pontos críticos (refresh, retomada de visibilidade da aba, expiração para zero e erros). Em termos de acessibilidade, o timer não deve anunciar cada segundo via ARIA, mas somente nos marcos canônicos de 3 minutos, 1 minuto e expiração, e os estados (normal, warning, critical, expired) não podem depender apenas de cor. Além disso, as superfícies Customer devem oferecer acesso consistente "Continuar reserva" para retornar ao hold vigente sem criar novo hold ou duplicar reservas.

**Approach:**
1. **Modelagem de Armazenamento e Ancoragem do Hold (`activeHold.ts`)**:
   - Persistência e restauração de reserva ativa em `sessionStorage` (`edt.active-hold.v1`).
   - Armazena snapshots de `ReservationResponse` juntamente com metadados do evento e setor.
2. **Hook de Timer Autoritativo Monotônico (`useReservationTimer.ts`)**:
   - Ancoragem inicial: `initialRemainingMs = expiresAt - serverNow`, `anchorPerfNow = performance.now()`.
   - Progressão local: `currentRemainingMs = Math.max(0, initialRemainingMs - (performance.now() - anchorPerfNow))`.
   - Estados canônicos: `normal` (> 179s), `warning` (60s..179s), `critical` (1s..59s) e `expired` (<= 0s ou `status === 'EXPIRED'`).
   - Live region ARIA dedicada com emissão única nos marcos de 3 minutos, 1 minuto e expiração.
   - Reconciliação autoritativa via listener de `visibilitychange` (`document.visibilityState === 'visible'`) e quando o timer atinge zero.
3. **Componentes da Superfície S04**:
   - `ReservationTimer.tsx`: renderização acessível do timer com badges textuais e sem dependência exclusiva de cor.
   - `CheckoutSummary.tsx`: exibição estrita dos valores snapshot (`unitPrice`, `totalAmount`, quantidade, status, setor, evento).
   - `DemoEnvironmentNotice.tsx`: aviso persistente e acessível do ambiente simulado.
   - `ActiveReservationBanner.tsx`: banner "Continuar reserva" exibido no catálogo e detalhe de eventos para direcionar à S04 sem criar novo hold.
   - `CheckoutView.tsx`: superfície S04 completa, com suporte ao estado ativo e ao estado expirado (foco acessível na mensagem de expiração, remoção de ações de pagamento e CTA de retorno ao evento).
4. **Integração nas Superfícies Customer**:
   - `AuthenticatedSession.tsx` atualizado para navegar entre `catalog`, `detail` e `checkout`.
   - `PublicEventDetail.tsx` atualizado com o componente `ActiveHoldCard` integrado e link/navegação para o Checkout.
   - `useSession.ts` atualizado para limpar `activeHold` ao encerrar sessão.
5. **Testes Abrangentes**:
   - Testes unitários do hook `useReservationTimer.test.ts`, storage `activeHold.test.ts`, e componentes visuais.
   - Testes de integração de fluxo ponta a ponta `CustomerCheckoutFlow.test.tsx` e `App.test.tsx`.

## Boundaries & Constraints

**Always:**
- `serverNow` e `expiresAt` do backend são a autoridade temporal.
- Frontend utiliza `performance.now()` apenas para progressão monotônica local.
- Preços exibidos no checkout derivam exclusivamente dos snapshots de `ReservationResponse`.
- Live region ARIA anuncia apenas 3 min, 1 min e expiração.
- "Continuar reserva" retorna ao hold existente sem criar nova reserva.
- Hold expirado remove ações de pagamento, foca a mensagem persistente e oferece retorno ao evento.

**Never:**
- Não implementar formulário/gateway de pagamento do Epic 5 (APPROVED/DECLINED).
- Não criar Tickets ou telas da Gate/compartilhamento.
- Não usar `any` ou `@ts-ignore` no TypeScript.

</frozen-after-approval>
