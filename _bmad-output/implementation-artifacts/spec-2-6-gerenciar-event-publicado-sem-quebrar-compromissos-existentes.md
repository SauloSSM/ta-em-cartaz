---
title: 'Story 2.6 — Gerenciar Event publicado sem quebrar compromissos existentes'
type: 'feature'
created: '2026-08-15'
status: 'in-progress'
baseline_commit: '018cfc683bc0fdf27a74c345ea9d3780593912db'
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

**Problem:** Após um evento ser publicado (`PUBLISHED`), o Organizer precisa continuar gerenciando informações complementares (`description`, `imageUrl`, `category`) e os setores de ingressos (`TicketSector`), podendo aumentar capacidade, diminuir capacidade até o limite já comprometido (`newCapacity >= committedQuantity`), alterar preços e adicionar novos setores ou remover setores sem compromissos. Campos estruturais essenciais (`title`, `venueName`, `venueAddress`, `startsAt`, `externalSource`, `externalId`) e exclusão de eventos publicados devem ser rigorosamente bloqueados pelo backend autoritativo e exibidos na UI com justificativa clara e acessível, protegendo os compromissos com compradores.

**Approach:**
No backend:
1. Na entidade `Event`, adicionar método `withUpdatedPublishedDetails(...)` para mutação segura de `description`, `imageUrl`, `category` e `updatedAt`.
2. Em `UpdateDraftEventUseCase`, permitir atualização de eventos `PUBLISHED` estritamente para campos não-estruturais, rejeitando alterações em `title`, `venueName`, `venueAddress`, `startsAt` com HTTP 409 `EVENT_CANNOT_BE_MODIFIED`.
3. Na entidade `TicketSector`, implementar `committedQuantity()` (`capacity - availableQuantity`) e `withUpdatedDetails(...)` com fórmula `newAvailableQuantity = newCapacity - committedQuantity`.
4. Em `TicketSectorRepository` e `SpringDataTicketSectorRepository`, implementar `findByIdWithLock(UUID id)` com `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
5. Em `UpdateTicketSectorUseCase`, aplicar lock pessimista no setor, validar que `newCapacity >= committedQuantity`, calcular `newAvailableQuantity = newCapacity - committedQuantity`, atualizar preço/descrição/nome e salvar.
6. Em `CreateTicketSectorUseCase`, permitir criação de novos setores tanto em `DRAFT` quanto em `PUBLISHED` pelo proprietário.
7. Em `DeleteTicketSectorUseCase`, aplicar lock pessimista no setor e impedir exclusão se `committedQuantity > 0` com HTTP 409 `EVENT_CANNOT_BE_MODIFIED`.

No frontend:
1. No `DraftEventEditor.tsx`:
   - Para eventos `PUBLISHED`, manter campos estruturais (`title`, `venueName`, `venueAddress`, `startsAt`, `externalId`, `externalSource`) visíveis, legíveis e desabilitados/bloqueados com explicação acessível contextual.
   - Manter campos complementares (`description`, `imageUrl`, `category`) editáveis com botão "Salvar alterações".
   - Bloquear exclusão de evento publicado informando que eventos publicados não podem ser excluídos.
2. No `SectorManager.tsx`:
   - Permitir que o Organizer adicione e edite setores em eventos `PUBLISHED`.
   - Exibir quantidade disponível e quantidade comprometida.
   - Desabilitar ou sinalizar remoção de setores com ingressos comprometidos (`committedQuantity > 0`).
3. No `SectorEditor.tsx`:
   - Indicar capacidade mínima permitida (`committedQuantity`) e validar client-side antes do envio.

## Boundaries & Constraints

**Always:**
- Somente o Organizer proprietário do evento pode alterá-lo ou gerenciar seus setores.
- Em eventos `PUBLISHED`, campos estruturais (`title`, `venueName`, `venueAddress`, `startsAt`, `externalSource`, `externalId`) são imutáveis.
- Em eventos `PUBLISHED`, campos complementares (`description`, `imageUrl`, `category`) permanecem editáveis.
- Eventos `PUBLISHED` nunca podem ser excluídos (HTTP 409 `EVENT_CANNOT_BE_DELETED`).
- Em alteração de capacidade de setor: `committedQuantity = capacity - availableQuantity`.
- Exigir `newCapacity >= committedQuantity` e definir `newAvailableQuantity = newCapacity - committedQuantity`.
- Preço pode ser alterado no setor sem afetar snapshots de reservas existentes.
- Setor com compromisso (`committedQuantity > 0`) não pode ser removido (HTTP 409 `EVENT_CANNOT_BE_MODIFIED`).
- Toda mutação de setor com concorrência utiliza `PESSIMISTIC_WRITE`.
- Invariantes no banco e no domínio: `capacity > 0`, `0 <= availableQuantity <= capacity`, `price >= 0`.
- Manipulação temporal exclusivamente via `Clock` injetado.
- Valores monetários exclusivamente com `BigDecimal`.

**Never:**
- Não permitir que outros organizadores ou papéis modifiquem eventos ou setores (HTTP 403 `AUTH_FORBIDDEN`).
- Não permitir alteração de campos estruturais em evento `PUBLISHED` (HTTP 409 `EVENT_CANNOT_BE_MODIFIED`).
- Não permitir redução de capacidade abaixo da quantidade comprometida (HTTP 409 `EVENT_CANNOT_BE_MODIFIED`).
- Não permitir exclusão de setor com `committedQuantity > 0` (HTTP 409 `EVENT_CANNOT_BE_MODIFIED`).
- Não antecipar lógica de Reservation/HOLD ou pagamentos (Epic 4/5).
- Não usar `double`/`float` para dinheiro nem `Instant.now()` sem `Clock`.
- Não esconder os campos estruturais na UI quando bloqueados; eles devem permanecer legíveis com explicação clara.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Editar descrição/imagem/categoria de Event PUBLISHED | `PUT /api/v1/events/{id}` com mesmos dados estruturais e nova descrição/imagem/categoria | HTTP 200 OK com `EventResponse` atualizado | Resposta 200 |
| Tentar alterar título/local/data de Event PUBLISHED | `PUT /api/v1/events/{id}` com novo `title` ou `startsAt` ou `venueName` | HTTP 409 `EVENT_CANNOT_BE_MODIFIED` ("Campos estruturais de eventos publicados são imutáveis.") | Rejeição com 409 |
| Aumentar capacidade de TicketSector publicado | `PUT /api/v1/events/{id}/sectors/{sectorId}` com `newCapacity > oldCapacity` | HTTP 200 OK com `availableQuantity` incrementada pela diferença, preservando `committed` | Resposta 200 |
| Diminuir capacidade de TicketSector até o comprometido | `PUT /api/v1/events/{id}/sectors/{sectorId}` com `newCapacity >= committed` | HTTP 200 OK com `newAvailableQuantity = newCapacity - committed` | Resposta 200 |
| Diminuir capacidade de TicketSector abaixo do comprometido | `PUT /api/v1/events/{id}/sectors/{sectorId}` com `newCapacity < committed` | HTTP 409 `EVENT_CANNOT_BE_MODIFIED` informando quantidade já comprometida | Rejeição com 409 |
| Alterar preço de TicketSector publicado | `PUT /api/v1/events/{id}/sectors/{sectorId}` com novo `price` | HTTP 200 OK com preço atualizado | Resposta 200 |
| Excluir setor sem compromissos em Event PUBLISHED | `DELETE /api/v1/events/{id}/sectors/{sectorId}` com `committed == 0` | HTTP 204 No Content | Setor excluído |
| Excluir setor com compromissos em Event PUBLISHED | `DELETE /api/v1/events/{id}/sectors/{sectorId}` com `committed > 0` | HTTP 409 `EVENT_CANNOT_BE_MODIFIED` ("Não é possível remover setor com ingressos ou reservas associadas.") | Rejeição com 409 |
| Excluir Event PUBLISHED | `DELETE /api/v1/events/{id}` | HTTP 409 `EVENT_CANNOT_BE_DELETED` | Rejeição com 409 |
| Criar novo setor em Event PUBLISHED | `POST /api/v1/events/{id}/sectors` com dados válidos | HTTP 201 Created com `TicketSectorResponse` | Resposta 201 |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/br/com/elitedevticket/events/domain/`:
  - `Event.java` — Adicionar `withUpdatedPublishedDetails(...)`.
  - `TicketSector.java` — Adicionar `committedQuantity()` e `withUpdatedDetails(...)`.
- `backend/src/main/java/br/com/elitedevticket/events/application/`:
  - `TicketSectorRepository.java` — Adicionar `findByIdWithLock(UUID id)`.
  - `UpdateDraftEventUseCase.java` — Suporte a atualização controlada de eventos `PUBLISHED` e validação de imutabilidade estrutural.
  - `UpdateTicketSectorUseCase.java` — Lock pessimista, validação `newCapacity >= committed` e cálculo de `newAvailableQuantity`.
  - `CreateTicketSectorUseCase.java` — Permitir criação de setores para `PUBLISHED` e `DRAFT`.
  - `DeleteTicketSectorUseCase.java` — Lock pessimista e bloqueio de exclusão se `committedQuantity > 0`.
- `backend/src/main/java/br/com/elitedevticket/events/adapters/persistence/`:
  - `SpringDataTicketSectorRepository.java` — Query com `@Lock(LockModeType.PESSIMISTIC_WRITE)` para `findByIdForUpdate`.
  - `JpaTicketSectorRepository.java` — Implementação de `findByIdWithLock(UUID id)`.
- `backend/src/test/java/br/com/elitedevticket/events/`:
  - Testes unitários de domínio e use cases para mutações em eventos e setores publicados.
  - Testes de integração com Testcontainers PostgreSQL cobrindo lock pessimista, constraints de banco, alteração de capacidade e bloqueio de exclusão com compromisso.
- `frontend/src/features/events/components/`:
  - `DraftEventEditor.tsx` — Campos estruturais bloqueados com explicação de motivo; campos opcionais editáveis; bloqueio de exclusão de publicado.
  - `SectorManager.tsx` — Exibição de compromissos/disponibilidade, permissão de edição/criação/exclusão em publicado.
  - `SectorEditor.tsx` — Validação de capacidade mínima comprometida.
  - `DraftEventEditor.test.tsx`, `SectorManager.test.tsx`, `SectorEditor.test.tsx` — Testes de frontend.

## Tasks & Acceptance

**Execution:**

- [ ] Atualizar `Event.java` e `TicketSector.java` com métodos de mutação de publicados e cálculo de compromissos.
- [ ] Atualizar `SpringDataTicketSectorRepository.java`, `TicketSectorRepository.java` e `JpaTicketSectorRepository.java` com `findByIdWithLock`.
- [ ] Atualizar `UpdateDraftEventUseCase.java`, `UpdateTicketSectorUseCase.java`, `CreateTicketSectorUseCase.java` e `DeleteTicketSectorUseCase.java`.
- [ ] Criar testes unitários e de integração no backend com PostgreSQL real Testcontainers.
- [ ] Atualizar componentes frontend `DraftEventEditor.tsx`, `SectorManager.tsx`, `SectorEditor.tsx`.
- [ ] Atualizar/adicionar testes frontend para gestão pós-publicação.
- [ ] Executar suíte completa: backend tests, ArchUnit, frontend tests, contract check, TypeScript build.
- [ ] Atualizar status da Story 2.6 no `sprint-status.yaml` para `review`.
