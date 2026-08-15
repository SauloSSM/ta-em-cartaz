---
title: 'Story 2.3 — Editar, listar e excluir Events em rascunho'
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
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O Organizer precisa listar todos os seus eventos próprios (`DRAFT` e `PUBLISHED`), editar livremente os campos de eventos que ainda estão em rascunho (`title`, `description`, `imageUrl`, `category`, `venue`, `startsAt`) e excluir rascunhos desnecessários através de um fluxo seguro e acessível, garantindo que eventos `PUBLISHED` nunca sejam excluídos ou alterados indevidamente e que outros usuários não acessem dados de rascunhos alheios.

**Approach:** No backend, estender a porta `EventRepository` com `findByOrganizerId(UUID organizerId)` e `deleteById(UUID id)`. Implementar os casos de uso `ListMyEventsUseCase`, `UpdateDraftEventUseCase` e `DeleteDraftEventUseCase` no pacote `events.application`, validando posse do organizador e invariantes de estado (`PUBLISHED` não pode ser excluído nem editado livremente). Expor os endpoints REST `GET /api/v1/events/mine`, `PUT /api/v1/events/{id}` e `DELETE /api/v1/events/{id}` no `EventsController` protegidos por `@PreAuthorize("hasRole('ORGANIZER')")` e verificação CSRF nas mutações. No frontend, criar a Superfície S09 (`MyEventsList.tsx`) e aprimorar a Superfície S11 (`DraftEventEditor.tsx`) com formulário completo de edição, diálogo acessível de confirmação de exclusão, estados de loading, vazio, sucesso e erro, mantendo TypeScript estrito e conformidade OpenAPI sem drift.

## Boundaries & Constraints

**Always:**
- O Organizer lista e gerencia estritamente os eventos dos quais é proprietário (`organizerId == authenticatedUser.id`).
- Eventos em estado `DRAFT` podem ter seus campos editados (`title`, `description`, `imageUrl`, `category`, `venue`, `startsAt`) e podem ser excluídos.
- Eventos em estado `PUBLISHED` **nunca** podem ser excluídos (retorna HTTP 409 `EVENT_CANNOT_BE_DELETED`).
- Manipulação de datas e instantes de atualização utiliza exclusivamente o `Clock` injetado no backend.
- Mutações HTTP (`PUT`, `DELETE`) exigem proteção CSRF (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header).
- Interface acessível com confirmação de exclusão, `aria-live` / `role="status"` para confirmações, `role="alert"` para erros, labels persistentes e foco gerenciado.
- Exclusão com sucesso retorna HTTP 204 No Content.
- Entidades JPA (`EventEntity`) residem exclusivamente em `adapters.persistence` e nunca são expostas no HTTP.

**Never:**
- Não permitir que `CUSTOMER` ou `GATE` acessem ou listem eventos de organizador (403 `AUTH_FORBIDDEN`).
- Não permitir que um Organizer acesse, edite ou exclua eventos de outro Organizer (403 `AUTH_FORBIDDEN`).
- Não excluir evento `PUBLISHED`.
- Não antecipar publicação de eventos (Story 2.5) nem regras avançadas de pós-publicação (Story 2.6).
- Não usar `Instant.now()` ou tempo não injetado.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Listagem de eventos próprios com sucesso | `GET /api/v1/events/mine` com sessão `ORGANIZER` válida | HTTP 200 OK com `{ events: EventResponse[] }` contendo rascunhos e publicados do organizador | Resposta 200 |
| Listagem sem eventos criados | `GET /api/v1/events/mine` com organizador sem eventos | HTTP 200 OK com `{ events: [] }` | Frontend exibe estado vazio amigável |
| Listagem por Customer ou Gate | `GET /api/v1/events/mine` com sessão `CUSTOMER` ou `GATE` | HTTP 403 com envelope `AUTH_FORBIDDEN` | Handler RBAC |
| Edição de DRAFT próprio com sucesso | `PUT /api/v1/events/{id}` com dados válidos e CSRF | HTTP 200 OK com `EventResponse` atualizado | Resposta 200 |
| Edição de DRAFT com título em branco | `PUT /api/v1/events/{id}` com `{ title: "  " }` | HTTP 400 com envelope padrão `AUTH_INVALID_REQUEST` e `fieldErrors` | Validação de campo |
| Edição de DRAFT alheio | `PUT /api/v1/events/{id}` por outro Organizer | HTTP 403 com envelope `AUTH_FORBIDDEN` | Proteção de posse |
| Edição de Event PUBLISHED | `PUT /api/v1/events/{id}` para evento `PUBLISHED` | HTTP 409 com envelope `EVENT_CANNOT_BE_MODIFIED` | Invariante de publicação |
| Exclusão de DRAFT próprio com sucesso | `DELETE /api/v1/events/{id}` com CSRF válido | HTTP 204 No Content | Evento removido do banco |
| Exclusão de DRAFT alheio | `DELETE /api/v1/events/{id}` por outro Organizer | HTTP 403 com envelope `AUTH_FORBIDDEN` | Proteção de posse |
| Exclusão de Event PUBLISHED | `DELETE /api/v1/events/{id}` para evento `PUBLISHED` | HTTP 409 com envelope `EVENT_CANNOT_BE_DELETED` | Bloqueio de exclusão publicado |
| Exclusão/Edição de Event inexistente | `DELETE` / `PUT /api/v1/events/{id}` para UUID inexistente | HTTP 404 com envelope `EVENT_NOT_FOUND` | Handler de erro 404 |

</frozen-after-approval>

## Code Map

- `openapi/elite-dev-ticket-v1.yaml` — Definição dos endpoints `GET /api/v1/events/mine`, `PUT /api/v1/events/{id}`, `DELETE /api/v1/events/{id}`, schemas `EventListResponse`, `UpdateDraftEventRequest`, `EventErrorCode` atualizado com conflitos e response `EventConflict`.
- `backend/src/main/java/br/com/elitedevticket/events/` — Módulo `events`:
  - `domain/Event.java` — Adição de métodos de mutação de domínio imutável `withUpdatedDetails(...)`.
  - `domain/EventConflictException.java` — Exceção para conflito de estado de evento (`409`).
  - `application/EventRepository.java` — Métodos `findByOrganizerId(UUID organizerId)` e `deleteById(UUID id)`.
  - `application/ListMyEventsUseCase.java` — Caso de uso de listagem de eventos próprios.
  - `application/UpdateDraftEventUseCase.java` — Caso de uso de atualização de rascunho.
  - `application/DeleteDraftEventUseCase.java` — Caso de uso de exclusão de rascunho com bloqueio de publicado.
  - `adapters/persistence/SpringDataEventRepository.java` e `JpaEventRepository.java` — Implementação das consultas e deleções.
  - `http/EventsController.java` — Novos endpoints `GET /mine`, `PUT /{id}`, `DELETE /{id}`.
  - `http/UpdateDraftEventRequest.java`, `http/EventListResponse.java`, `http/EventsExceptionHandler.java`.
- `backend/src/test/java/br/com/elitedevticket/events/` — Testes unitários, de integração com Testcontainers e conformidade OpenAPI.
- `frontend/src/features/events/` — Módulo frontend:
  - `api/eventsApi.ts` — Métodos `listMyEvents`, `updateDraftEvent`, `deleteDraftEvent`.
  - `components/MyEventsList.tsx` — Superfície S09 (Meus Eventos): listagem, badges de status, estados de loading, vazio, erro e ações.
  - `components/DraftEventEditor.tsx` — Superfície S11: formulário de edição de todos os campos e confirmação de exclusão.
  - `components/DeleteConfirmDialog.tsx` — Diálogo acessível de confirmação de exclusão com foco gerenciado.
  - `index.ts` — Exportações públicas da feature.
- `frontend/src/features/auth/AuthenticatedSession.tsx` — Integração entre Meus Eventos (S09), Busca de Catálogo (S10) e Editor de Evento (S11).
- `frontend/scripts/check-openapi-contract.mjs` — Validação de contrato OpenAPI estendida para novas operações.

## Tasks & Acceptance

**Execution:**

- [x] `openapi/elite-dev-ticket-v1.yaml` — Declarar `GET /api/v1/events/mine`, `PUT /api/v1/events/{id}`, `DELETE /api/v1/events/{id}`, schemas e responses correspondentes.
- [x] `backend/src/main/java/br/com/elitedevticket/events/` — Implementar casos de uso, métodos de repositório, controller REST e exception handler.
- [x] `backend/src/test/java/br/com/elitedevticket/events/` — Testes unitários de use cases, testes de integração de endpoints (Testcontainers) e teste de conformidade OpenAPI.
- [x] `frontend/src/features/events/` — Implementar `eventsApi.ts`, `MyEventsList.tsx`, `DraftEventEditor.tsx` com formulário de edição e exclusão com diálogo acessível.
- [x] `frontend/src/features/auth/AuthenticatedSession.tsx` — Conectar fluxo S09 Meus Eventos ↔ S10 Busca Catálogo ↔ S11 Editor de Evento.
- [x] `frontend/scripts/check-openapi-contract.mjs` — Atualizar verificador de contrato OpenAPI.
- [x] `frontend/src/features/events/__tests__/` e testes de componentes — Criar testes unitários e de integração de frontend.
- [x] `_bmad-output/implementation-artifacts/sprint-status.yaml` — Atualizar status da Story 2.3 para `review`.
