---
title: 'Story 2.4 — Configurar TicketSectors do rascunho'
type: 'feature'
created: '2026-08-15'
status: 'done'
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

**Problem:** O Organizer precisa definir a configuração de setores de ingressos (`TicketSector`) de um evento em rascunho (`DRAFT`), incluindo nome, descrição opcional, capacidade (maior que zero) e preço em BRL (maior ou igual a zero), além de listar, editar e remover setores de forma segura e acessível antes da publicação do evento, assegurando que as invariantes de capacidade e disponibilidade sejam rigorosamente preservadas sem inventar estados como `LOW_AVAILABILITY`.

**Approach:** 
No backend:
1. Criar a tabela `ticket_sectors` via migration Flyway `V4__create_ticket_sectors.sql` com constraints de integridade (`capacity > 0`, `0 <= available_quantity <= capacity`, `price >= 0`, `event_id REFERENCES events(id) ON DELETE CASCADE`).
2. Implementar a entidade de domínio `TicketSector` com validações das invariantes, mantendo dinheiro estritamente como `BigDecimal`.
3. Criar `TicketSectorRepository` na camada de aplicação e sua implementação JPA em `adapters.persistence`.
4. Implementar casos de uso: `CreateTicketSectorUseCase`, `UpdateTicketSectorUseCase`, `DeleteTicketSectorUseCase`, `ListTicketSectorsUseCase`, validando propriedade do organizador e estado `DRAFT` do evento.
5. Expor endpoints REST em `EventsController`:
   - `GET /api/v1/events/{eventId}/sectors`
   - `POST /api/v1/events/{eventId}/sectors`
   - `PUT /api/v1/events/{eventId}/sectors/{sectorId}`
   - `DELETE /api/v1/events/{eventId}/sectors/{sectorId}`
   protegidos por `@PreAuthorize("hasRole('ORGANIZER')")` e proteção CSRF nas mutações.
6. Mapear endpoints e esquemas no contrato `openapi/elite-dev-ticket-v1.yaml`.

No frontend:
1. Adicionar tipos e funções de cliente em `features/events/api/eventsApi.ts` para setores de ingressos.
2. Atualizar o verificador `frontend/scripts/check-openapi-contract.mjs` para suporte a campos numéricos e novas operações.
3. Criar a Superfície S12 (`SectorManager.tsx`, `SectorEditor.tsx`) e integrá-la no editor de eventos (`DraftEventEditor.tsx`), com estados de loading, lista vazia ("Nenhum setor criado"), formulário acessível com labels persistentes e foco gerenciado, diálogo de confirmação de exclusão e exibição de capacidade/disponibilidade/preço BRL sem `LOW_AVAILABILITY`.

## Boundaries & Constraints

**Always:**
- Setores pertencem a um único Event (`eventId`).
- `capacity > 0`.
- `price >= 0` em BRL com `BigDecimal`.
- `0 <= availableQuantity <= capacity`. Em eventos `DRAFT`, `availableQuantity = capacity`.
- Somente o Organizer proprietário de um evento `DRAFT` pode adicionar, editar ou remover setores.
- Exibir capacidade, preço e disponibilidade numérica real sem inventar `LOW_AVAILABILITY`.
- DTOs próprios, sem expor entidades JPA no HTTP.
- Mutações HTTP (`POST`, `PUT`, `DELETE`) exigem proteção CSRF.
- Foco acessível e retorno de foco após fechamento de diálogo de exclusão.
- Preservação de dados válidos preenchidos após falhas de validação.

**Never:**
- Não permitir que outros usuários ou organizadores não proprietários modifiquem setores de um evento (HTTP 403 `AUTH_FORBIDDEN`).
- Não permitir configuração/mutação de setores em eventos `PUBLISHED` nesta Story (HTTP 409 `EVENT_CANNOT_BE_MODIFIED`).
- Não antecipar lógica de HOLD/reserva/overselling do Epic 4.
- Não implementar publicação de Event (Story 2.5).
- Não criar cache/Redis nem usar tipos float/double para valores monetários.
- Não expor entidades JPA em responses HTTP.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Listar setores de evento DRAFT próprio | `GET /api/v1/events/{eventId}/sectors` com sessão ORGANIZER dona | HTTP 200 OK com `{ sectors: TicketSectorResponse[] }` | Resposta 200 |
| Listar setores de evento sem setores | `GET /api/v1/events/{eventId}/sectors` para evento sem setores | HTTP 200 OK com `{ sectors: [] }` | Frontend exibe "Nenhum setor criado" |
| Criar setor com dados válidos | `POST /api/v1/events/{eventId}/sectors` com `{ name, capacity: 100, price: 150.00 }` e CSRF | HTTP 201 Created com `TicketSectorResponse` (`availableQuantity = 100`) | Resposta 201 |
| Criar setor com capacidade zero ou negativa | `POST /api/v1/events/{eventId}/sectors` com `capacity: 0` | HTTP 400 com `AUTH_INVALID_REQUEST` e `fieldErrors` | Validação de campo |
| Criar setor com preço negativo | `POST /api/v1/events/{eventId}/sectors` com `price: -10.00` | HTTP 400 com `AUTH_INVALID_REQUEST` e `fieldErrors` | Validação de campo |
| Criar setor com nome em branco | `POST /api/v1/events/{eventId}/sectors` com `name: " "` | HTTP 400 com `AUTH_INVALID_REQUEST` e `fieldErrors` | Validação de campo |
| Criar/editar setor em evento de outro Organizer | `POST/PUT /api/v1/events/{eventId}/sectors` por outro usuário | HTTP 403 `AUTH_FORBIDDEN` | Bloqueio de autorização |
| Criar/editar setor em evento PUBLISHED | `POST/PUT /api/v1/events/{eventId}/sectors` para evento publicado | HTTP 409 `EVENT_CANNOT_BE_MODIFIED` | Invariante de estado |
| Atualizar setor em evento DRAFT próprio | `PUT /api/v1/events/{eventId}/sectors/{sectorId}` com novos dados válidos | HTTP 200 OK com `TicketSectorResponse` atualizado | Resposta 200 |
| Excluir setor em evento DRAFT próprio | `DELETE /api/v1/events/{eventId}/sectors/{sectorId}` com CSRF | HTTP 204 No Content | Setor excluído do banco |
| Excluir setor inexistente ou de outro evento | `DELETE /api/v1/events/{eventId}/sectors/{sectorId}` | HTTP 404 `EVENT_NOT_FOUND` | Resposta 404 |

</frozen-after-approval>

## Code Map

- `openapi/elite-dev-ticket-v1.yaml` — Declaração dos endpoints `/api/v1/events/{eventId}/sectors` (`GET`, `POST`, `PUT`, `DELETE`), schemas `TicketSectorResponse`, `TicketSectorListResponse`, `CreateTicketSectorRequest`, `UpdateTicketSectorRequest`.
- `backend/src/main/resources/db/migration/V4__create_ticket_sectors.sql` — Criação da tabela `ticket_sectors` com integridade relacional e constraints numéricas.
- `backend/src/main/java/br/com/elitedevticket/events/` — Módulo `events`:
  - `domain/TicketSector.java` — Entidade de domínio de setor de ingressos.
  - `domain/TicketSectorNotFoundException.java` — Exceção para setor não encontrado.
  - `application/TicketSectorRepository.java` — Interface de repositório de setores.
  - `application/CreateTicketSectorUseCase.java` — Caso de uso de criação de setor.
  - `application/UpdateTicketSectorUseCase.java` — Caso de uso de atualização de setor.
  - `application/DeleteTicketSectorUseCase.java` — Caso de uso de exclusão de setor.
  - `application/ListTicketSectorsUseCase.java` — Caso de uso de listagem de setores de evento.
  - `adapters/persistence/TicketSectorEntity.java` — Entidade JPA de persistência.
  - `adapters/persistence/SpringDataTicketSectorRepository.java` — Interface Spring Data JPA.
  - `adapters/persistence/JpaTicketSectorRepository.java` — Implementação do repositório de setores.
  - `http/CreateTicketSectorRequest.java`, `http/UpdateTicketSectorRequest.java`, `http/TicketSectorResponse.java`, `http/TicketSectorListResponse.java`.
  - `http/EventsController.java` — Adição dos endpoints de setores `/api/v1/events/{eventId}/sectors`.
  - `http/EventsExceptionHandler.java` — Tratamento de exceções de setores.
- `backend/src/test/java/br/com/elitedevticket/events/` — Testes unitários de domínio e use cases, testes de integração Testcontainers e testes de contrato OpenAPI.
- `frontend/src/features/events/` — Módulo frontend:
  - `api/eventsApi.ts` — DTOs, type guards e chamadas HTTP para setores.
  - `components/SectorManager.tsx` — Superfície S12 de gestão de setores (listagem, feedback, ações).
  - `components/SectorEditor.tsx` — Formulário de criação/edição de setor com validação e acessibilidade.
  - `components/DraftEventEditor.tsx` — Integração da gestão de setores ao editor de evento.
  - `components/__tests__/SectorManager.test.tsx` e `SectorEditor.test.tsx` — Testes unitários e de integração frontend.
- `frontend/scripts/check-openapi-contract.mjs` — Validação estendida de tipos OpenAPI (suporte a `number`/`integer`).

## Tasks & Acceptance

**Execution:**

- [x] `openapi/elite-dev-ticket-v1.yaml` — Declarar endpoints de setores, schemas e responses correspondentes.
- [x] `backend/src/main/resources/db/migration/V4__create_ticket_sectors.sql` — Criar migration da tabela `ticket_sectors`.
- [x] `backend/src/main/java/br/com/elitedevticket/events/` — Implementar `TicketSector`, repositórios, use cases, DTOs, controllers e exception handlers.
- [x] `backend/src/test/java/br/com/elitedevticket/events/` — Criar testes unitários, testes de integração com Testcontainers e atualizar verificação OpenAPI.
- [x] `frontend/scripts/check-openapi-contract.mjs` — Atualizar verificador de contrato OpenAPI.
- [x] `frontend/src/features/events/api/eventsApi.ts` — Adicionar tipos, validadores e chamadas HTTP de setores.
- [x] `frontend/src/features/events/components/` — Criar `SectorManager.tsx`, `SectorEditor.tsx`, integrar em `DraftEventEditor.tsx`.
- [x] `frontend/src/features/events/components/__tests__/` — Criar testes unitários e de integração frontend para gestão de setores.
- [x] Validar conformidade: ArchUnit, Testcontainers, Vitest, TypeCheck, Vite Build, OpenAPI check.
- [x] Atualizar status da Story 2.4 no `sprint-status.yaml` para `review`.
