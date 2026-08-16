---
title: 'Story 4.1 — Restaurar intenção após login CUSTOMER e criar hold válido'
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

**Problem:** Visitantes anônimos que selecionam um setor e quantidade em um evento publicado e acionam "Reservar" precisam ser direcionados ao login sem criar hold ou reserva anônima, retendo a intenção no navegador em `sessionStorage` por até 15 minutos (AD-11). Ao autenticar com sucesso como `CUSTOMER`, essa intenção deve ser restaurada e o backend deve revalidar o evento (status `PUBLISHED`), vendas abertas (`startsAt > serverNow` ou nulo), setor pertencente ao evento, quantidade válida (1–6) e disponibilidade autoritativa antes de criar atomicamente uma `Reservation` com status `HOLDING`, decrementando o estoque e fixando `expiresAt = serverNow + 10 minutos` exatos. Caso a revalidação falhe (esgotado, vendas encerradas, evento não publicado), a interface deve retornar ao setor/detalhe do evento, explicar a mudança com clareza, nunca reduzir a quantidade silenciosamente e exigir nova confirmação explícita do usuário.

**Approach:**
No backend:
1. Criar migration Flyway `V6__create_reservations.sql` com schema da tabela `reservations`, constraints e índices.
2. Em `auth`, expor a porta `CustomerLockPort` em `br.com.elitedevticket.auth.application` e sua implementação `JpaCustomerLockAdapter` em `br.com.elitedevticket.auth.adapters.persistence` para adquirir lock pessimista no Customer (AD-4, AD-5).
3. Em `events`, expor a porta `EventStockPort` em `br.com.elitedevticket.events.application` para consulta/validação de evento e lock/decremento de `TicketSector` com `PESSIMISTIC_WRITE` (AD-2, AD-3).
4. Criar o módulo `reservations` com:
   - Entidade de domínio `Reservation` com status (`HOLDING`, `CONFIRMED`, `EXPIRED`), snapshots de preço (`unitPrice`, `totalAmount`), 10 min hold duration determinístico via `Clock`.
   - Caso de uso `CreateReservationUseCase` em `br.com.elitedevticket.reservations.application` aplicando ordem canônica de locks `Customer → TicketSector` (AD-5), revalidações completas e decremento atômico de estoque.
   - Repositório `ReservationRepository` e adapter de persistência JPA `JpaReservationRepository`.
   - Controlador HTTP `ReservationsController` sob `POST /api/v1/events/{eventId}/sectors/{sectorId}/reservations` protegido com `@PreAuthorize("hasRole('CUSTOMER')")` e CSRF.
   - `ReservationsExceptionHandler` mapeando erros de negócio para envelopes padronizados `{code, message, fieldErrors?, traceId, timestamp}` (AD-12).
5. Atualizar contrato `openapi/elite-dev-ticket-v1.yaml` com o novo endpoint, schemas e responses.
6. Adicionar testes unitários, testes de concorrência com PostgreSQL real (Testcontainers), testes de contrato OpenAPI e validações ArchUnit.

No frontend:
1. Criar `features/reservations/api/reservationsApi.ts` com DTOs conformes ao OpenAPI e função `createReservation`.
2. Atualizar script `check-openapi-contract.mjs` para validar os contratos de `reservations`.
3. Integrar restauração de intenção pós-login CUSTOMER em `App.tsx` / `useSession` / `AuthenticatedSession.tsx` / `PublicEventDetail.tsx`:
   - Ao autenticar como CUSTOMER, verificar `getPurchaseIntention()`.
   - Se válida, disparar criação do hold via `createReservation`.
   - Se bem-sucedida, limpar intenção com `clearPurchaseIntention()` e exibir feedback do hold criado com timer e detalhes.
   - Se falhar na revalidação, limpar intenção, retornar à visualização do evento/setor, exibir erro explicativo acessível, não reduzir quantidade silenciosamente e exigir nova confirmação.
4. Adicionar testes unitários e de integração no frontend para todos os fluxos de restauração, sucesso e cenários de erro.

## Boundaries & Constraints

**Always:**
- Visitante não autenticado não cria Reservation nem hold no backend.
- Intenção antes do login guarda somente `eventId`, `ticketSectorId`, `quantity` (1–6), `internalReturnPath` e `createdAt` em `sessionStorage` com TTL de 15 minutos (AD-11).
- Somente login com papel `CUSTOMER` restaura a intenção e tenta criar `HOLDING`.
- Backend é autoridade sobre publicação, vendas abertas, setor e disponibilidade.
- Decremento de estoque e criação de `Reservation` ocorrem na mesma transação com locks `Customer → TicketSector` (AD-3, AD-4, AD-5).
- `expiresAt` é fixado exatamente em `serverNow + 10 minutos` (AD-6).
- Em caso de falha de estoque ou vendas encerradas, UI não reduz quantidade silenciosamente e exige nova confirmação.
- Sem uso de `LOW_AVAILABILITY` (AD-22).
- Sem dependência de bibliotecas não aprovadas.

**Never:**
- Não avançar para Story 4.2 (pagamento, preço complexo checkout fora do hold básico, etc.).
- Não executar comandos git destrutivos (`git reset`, `git clean`, etc.).
- Não usar `any` ou `@ts-ignore` no TypeScript.

</frozen-after-approval>
