---
title: 'Story 4.2 — Criar Reservation atômica com preço capturado'
type: 'feature'
created: '2026-08-16'
status: 'in-progress'
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

**Problem:** Customer autenticado precisa criar uma `Reservation` temporária (status `HOLDING`) garantindo atomicamente a quantidade escolhida de um setor com valor de preço unitário e total capturados e imutáveis durante os 10 minutos de vigência do hold, sem risco de manipulação de preço pelo cliente ou divergência de cálculo monetário.

**Approach:**
1. Tratar a base de infraestrutura e persistência criada na Story 4.1 como base já estabelecida e aprovada (`V6__create_reservations.sql`, `Reservation.java`, `CreateReservationUseCase.java`, `CustomerLockPort`, `EventStockPort`, `ReservationsController`).
2. Consolidar e comprovar todos os critérios de aceitação da Story 4.2:
   - Ordem canônica de locks: `Customer → TicketSector` (`PESSIMISTIC_WRITE`) com decremento atômico de `availableQuantity` e persistência de `HOLDING` na mesma transação.
   - Cálculo e persistência imutável de `unitPrice` (lido do setor no momento da criação) e `totalAmount = unitPrice * quantity` em BRL com decimal exato (`BigDecimal`).
   - Fixação determinística de `expiresAt = serverNow + 10 minutos` via `java.time.Clock` injetado, sem pausas, extensões ou reinício.
   - O payload do cliente (`CreateReservationRequest`) recebe somente `quantity`; qualquer tentativa do cliente de injetar valores de preço é ignorada pelo backend.
   - Imutabilidade do snapshot de preço: mesmo que o organizador altere o preço do `TicketSector` posteriormente, a `Reservation` existente mantém o preço capturado inalterado.
   - Garantia de rollback integral: qualquer falha durante a execução reverte completamente o decremento do estoque e a persistência da reserva.
3. Expandir a suíte de testes TDD com testes dedicados de imutabilidade de snapshot de preço, teste de ignorar preço enviado pelo cliente, e testes determinísticos com `Clock`.

## Boundaries & Constraints

**Always:**
- `CustomerLockPort` bloqueia o cliente primeiro antes de consultar e bloquear o setor (AD-4, AD-5).
- `expiresAt` é estritamente `serverNow + 10 minutos` calculados com `Clock` injetado (AD-6).
- `unitPrice` e `totalAmount` são calculados e persistidos exclusivamente pelo backend em BRL com escala decimal exata (`BigDecimal`).
- `ReservationResponse` retorna `id`, `customerId`, `eventId`, `sectorId`, `quantity`, `unitPrice`, `totalAmount`, `status`, `expiresAt`, `createdAt`, `serverNow`.
- Rollback completo em caso de falha.

**Never:**
- Não implementar `Idempotency-Key` nem recuperação de hold vigente existente (Story 4.3).
- Não implementar scheduler de expiração nem reconciliação lazy de holds vencidos (Story 4.4).
- Não implementar Superfície S04 (Checkout) nem timer com `performance.now()` (Story 4.5).
- Não executar comandos git destrutivos (`git reset`, `git clean`, etc.).

</frozen-after-approval>
