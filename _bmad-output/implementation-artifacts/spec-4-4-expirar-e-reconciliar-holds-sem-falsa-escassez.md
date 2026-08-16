---
title: 'Story 4.4 — Expirar e reconciliar holds sem falsa escassez'
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

**Problem:** Holds vencidos (`serverNow >= expiresAt`) retêm inventário nos setores até que um processo de limpeza execute. Se a liberação depender exclusivamente de um scheduler periódico, usuários que tentarem comprar ingressos imediatamente após a expiração de holds anteriores sofrerão com falsa escassez (`INSUFFICIENT_AVAILABILITY`). Além disso, concorrência entre o scheduler e operações sob demanda pode levar a devoluções duplicadas de estoque ou deadlocks por inversão de locks.

**Approach:**
1. **Modelo de Expiração e Devolução Atômica (`ExpireReservationUseCase`)**:
   - `serverNow >= expiresAt` torna a reserva `HOLDING` semanticamente vencida.
   - Ordem estrita de locks (AD-5): bloqueia `Reservation` com `PESSIMISTIC_WRITE` (`findByIdWithLock`), verifica se continua `HOLDING` e expirada, bloqueia `TicketSector` com `PESSIMISTIC_WRITE` (`findSectorByIdWithLock`), incrementa `availableQuantity` limitando ao `capacity` (`Math.min(capacity, availableQuantity + quantity)`), e atualiza a `Reservation` para `EXPIRED`.
   - Execução idempotente: se a reserva já estiver `EXPIRED` ou `CONFIRMED`, nenhuma devolução de estoque ocorre.
2. **Lazy Reconciler sob Demanda (`CreateReservationUseCase`)**:
   - Sob lock do Customer (`CustomerLockPort`), reconcilia qualquer hold `HOLDING` vencido anterior do mesmo `(customerId, eventId)` antes de avaliar novo hold.
   - Ao checar disponibilidade de estoque do setor alvo, se o estoque for insuficiente para a quantidade solicitada, busca e expira holds vencidos pendentes naquele setor (`findExpiredHoldingIdsBySector`), restaurando a capacidade antes de tomar a decisão de indisponibilidade (eliminando falsa escassez).
3. **Scheduler de Cleanup em Segundo Plano (`ReservationExpiryScheduler`)**:
   - `@Scheduled(fixedDelay = 30000)` executado a cada 30 segundos com `@EnableScheduling`.
   - Busca lotes pequenos de IDs de reservas `HOLDING` com `expiresAt <= serverNow` ordenadas por UUID crescente (`findExpiredHoldingIds`).
   - Para cada ID, delega a expiração atômica para `ExpireReservationUseCase` em transação individual.

## Boundaries & Constraints

**Always:**
- O relógio do backend via `Clock` injetado é autoritativo.
- Transição autoritativa: `HOLDING → EXPIRED`.
- Ordem canônica de locks: `Customer → Reservation → TicketSector` (AD-5).
- Devolução de estoque exatamente uma vez; `availableQuantity` nunca ultrapassa `capacity`.
- Scheduler e reconciliação lazy concorrentes produzem efeito único sem devolução dupla de estoque.
- Testes de concorrência com PostgreSQL real (Testcontainers).

**Never:**
- Não implementar Superfície S04 (Checkout/timer) da Story 4.5.
- Não implementar pagamento ou emissão de Tickets do Epic 5.
- Não utilizar banco de dados em memória para testes concorrentes.
- Não utilizar `Thread.sleep` em testes nem `Instant.now()` sem injeção de `Clock`.

</frozen-after-approval>
