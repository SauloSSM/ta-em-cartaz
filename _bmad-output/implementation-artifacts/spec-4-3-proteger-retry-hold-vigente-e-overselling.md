---
title: 'Story 4.3 — Proteger retry, hold vigente e overselling'
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

**Problem:** Retries HTTP, cliques duplos e acessos concorrentes ao endpoint de criação de reserva podem causar baixa duplicada de estoque, múltiplos holds ativos para o mesmo usuário em um evento ou venda acima da capacidade (overselling). O sistema deve garantir idempotência estrita via `Idempotency-Key`, limitar a 1 hold vigente por Customer/Event e proteger a capacidade de ingressos sob concorrência pesada.

**Approach:**
1. Criar migration Flyway `V7__create_reservation_idempotency.sql` com a tabela `reservation_idempotency_keys` tendo constraint `UNIQUE(customer_id, idempotency_key)` e colunas `id`, `customer_id`, `idempotency_key`, `payload_hash` (SHA-256 hex de `v1:{eventId}:{sectorId}:{quantity}`), `reservation_id` e `created_at`.
2. Em `reservations`, criar repositório e entidades de persistência para idempotência.
3. No caso de uso `CreateReservationUseCase`:
   - Bloquear o Customer por `CustomerLockPort` (`PESSIMISTIC_WRITE` em `users`).
   - Se `Idempotency-Key` for fornecida:
     - Buscar registro persistido de idempotência para o par `(customerId, idempotencyKey)`.
     - Se encontrado e o `payload_hash` for idêntico: retornar a `Reservation` existente sem nova baixa de estoque.
     - Se encontrado com `payload_hash` diferente: lançar `IdempotencyConflictException` (HTTP 409 `IDEMPOTENCY_CONFLICT`).
   - Verificar se o Customer já possui `HOLDING` vigente (`!isExpired(serverNow)`) para o mesmo `eventId`:
     - Se existir: recuperar e retornar essa `Reservation` sem criar novo hold e sem decrementar estoque.
   - Adquirir lock pessimista no `TicketSector` via `eventStockPort.findSectorByIdWithLock(sectorId)`.
   - Validar evento publicado, vendas abertas (`serverNow < startsAt`), setor pertencente ao evento e estoque disponível.
   - Decrementar `availableQuantity` atomicamente, criar e salvar a nova `Reservation` `HOLDING`.
   - Salvar o registro em `reservation_idempotency_keys` caso `idempotencyKey` esteja presente.
4. No controlador `ReservationsController`:
   - Aceitar header opcional `@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey`.
5. Em `ReservationsExceptionHandler`:
   - Mapear `IdempotencyConflictException` para status HTTP 409 Conflict com envelope de erro padronizado e código `IDEMPOTENCY_CONFLICT`.
6. Atualizar contrato `openapi/elite-dev-ticket-v1.yaml` e testes de contrato.
7. Escrever testes unitários e testes de concorrência com PostgreSQL real (Testcontainers).

## Boundaries & Constraints

**Always:**
- Idempotência é persistida em banco de dados (`reservation_idempotency_keys`), nunca apenas em memória.
- Lock canônico: `Customer → Reservation → TicketSector` (AD-5).
- Fingerprint canônico versionado: `v1:{eventId}:{sectorId}:{quantity}` com SHA-256 hex em minúsculas (AD-7, AD-23).
- `availableQuantity` nunca pode ser negativa.
- Hold vigente respeita `serverNow < expiresAt` e mesmo `(customerId, eventId)` (AD-4).

**Never:**
- Não implementar scheduler de expiração nem devolução de estoque da Story 4.4.
- Não implementar Superfície S04 (Checkout) da Story 4.5.
- Não usar Redis/cache volátil para idempotência.

</frozen-after-approval>
