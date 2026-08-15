---
title: 'Story 2.2 — Criar Event DRAFT a partir de referência Ticketmaster'
type: 'feature'
created: '2026-08-15'
status: 'review'
baseline_commit: '018cfc683bc0fdf27a74c345ea9d3780593912db'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md'
  - '{project-root}/docs/04-ux/UX_DIRECTION_v0.1.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O Organizer precisa iniciar a criação de um evento interno (`Event` em estado `DRAFT`) a partir do snapshot selecionado na busca Ticketmaster, garantindo identidade própria e independente no banco de dados, permitindo a reutilização do mesmo `externalId` e protegendo o acesso ao rascunho contra outros usuários ou requisições adulteradas.

**Approach:** Criar a tabela `events` via migration Flyway `V2__create_events.sql` no PostgreSQL. Implementar o módulo `events` no backend com entidade JPA em `adapters.persistence.EventEntity`, modelo de domínio `Event`, porta `EventRepository` e caso de uso `CreateDraftEventUseCase` / `GetEventUseCase`. Expor endpoints REST `POST /api/v1/events/drafts` e `GET /api/v1/events/{id}` protegidos por autenticação (`ROLE_ORGANIZER`) e verificação de propriedade (`organizerId == authenticatedUser.id`). No frontend, integrar a criação do rascunho à seleção de referência na Superfície S10 e exibir o rascunho criado na Superfície S11 (Editor de Evento), com feedback acessível, estados de loading, sucesso e erro, mantendo TypeScript estrito e conformidade OpenAPI sem drift.

## Boundaries & Constraints

**Always:**
- O evento criado nasce estritamente no estado `DRAFT` com UUID próprio gerado no backend.
- `externalId` da Ticketmaster é copiado no snapshot para o campo `external_id`, mas não é chave primária nem único no banco (pode ser reutilizado por múltiplos eventos ou pelo mesmo organizador).
- O `organizer_id` é preenchido com o ID do usuário autenticado obtido com segurança da sessão/JWT, nunca aceito cegamente como parâmetro aberto do cliente.
- Apenas usuários com `ROLE_ORGANIZER` podem criar rascunhos ou consultar seus próprios rascunhos.
- Tentativa de acesso a um `DRAFT` de outro organizador resulta em HTTP 403 `AUTH_FORBIDDEN` (ou 404 se inexistente) sem vazar dados sensíveis.
- Entidades JPA (`EventEntity`) residem exclusivamente em `adapters.persistence` e nunca são expostas no HTTP.
- DTOs próprios para requisição (`CreateDraftEventRequest`) e resposta (`EventResponse`).
- Manipulação de tempo utiliza exclusivamente o `Clock` injetado.
- Proteção CSRF obrigatória para o endpoint `POST /api/v1/events/drafts` (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header).
- Interface acessível com `aria-live` / `role="status"` para confirmações, `role="alert"` para erros, labels persistentes e foco gerenciado.

**Never:**
- Não permitir que `CUSTOMER` ou `GATE` acessem ou criem rascunhos de eventos (403 `AUTH_FORBIDDEN`).
- Não publicar evento automaticamente (a publicação com validação de setores, venue e data futura pertence à Story 2.5).
- Não criar setores ou gerenciar ingressos nesta story (escopo das Stories 2.4 e 2.5).
- Não expor dados confidenciais ou stack traces em respostas de erro.
- Não usar `Instant.now()` ou tempo não injetado.
- Não violar o isolamento modular do monólito (regras ArchUnit).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Criação de DRAFT com sucesso | `POST /api/v1/events/drafts` com payload `{title, externalId?, description?, imageUrl?, category?}` e sessão `ORGANIZER` válida | HTTP 201 Created com `EventResponse` (`id`, `organizerId`, `title`, `status="DRAFT"`, timestamps) e header `Location` | Resposta 201 |
| Reutilização de externalId | `POST /api/v1/events/drafts` com o mesmo `externalId` já usado anteriormente | HTTP 201 Created com novo UUID independente | Permitido conforme spec |
| Criação com título vazio/em branco | `POST /api/v1/events/drafts` com `{title: ""}` | HTTP 400 com envelope de erro padrão `AUTH_INVALID_REQUEST` e `fieldErrors` | Validação de campo |
| Criação por usuário não autenticado | `POST /api/v1/events/drafts` sem sessão | HTTP 401 com envelope `AUTH_UNAUTHENTICATED` | Handler de segurança |
| Criação por Customer ou Gate | `POST /api/v1/events/drafts` com sessão `CUSTOMER` ou `GATE` | HTTP 403 com envelope `AUTH_FORBIDDEN` | Handler RBAC |
| Consulta de DRAFT próprio | `GET /api/v1/events/{id}` pelo Organizer proprietário | HTTP 200 com `EventResponse` completo | Resposta 200 |
| Consulta de DRAFT alheio | `GET /api/v1/events/{id}` por outro Organizer | HTTP 403 com envelope `AUTH_FORBIDDEN` | Proteção de propriedade |
| Consulta de DRAFT por Customer/Gate | `GET /api/v1/events/{id}` por Customer/Gate | HTTP 403 com envelope `AUTH_FORBIDDEN` | Proteção de visibilidade de rascunho |
| Consulta de Event inexistente | `GET /api/v1/events/{id}` com UUID inexistente | HTTP 404 com envelope de erro `EVENT_NOT_FOUND` | Handler de erro 404 |

</frozen-after-approval>

## Code Map

- `openapi/elite-dev-ticket-v1.yaml` — Definição dos endpoints `POST /api/v1/events/drafts` e `GET /api/v1/events/{id}`, schemas `CreateDraftEventRequest`, `EventResponse`, `EventStatus` e respostas de erro.
- `backend/src/main/resources/db/migration/V3__create_events.sql` — Migration Flyway criando a tabela `events`.
- `backend/src/main/java/br/com/elitedevticket/events/` — Módulo `events`:
  - `domain/Event.java` — Modelo de domínio rico imutável para `Event`.
  - `domain/EventStatus.java` — Enum com estados `DRAFT` e `PUBLISHED`.
  - `domain/EventNotFoundException.java` e `domain/EventForbiddenException.java` — Exceções de domínio/aplicação.
  - `application/EventRepository.java` — Porta de persistência de eventos.
  - `application/CreateDraftEventUseCase.java` — Caso de uso de criação de rascunho.
  - `application/GetEventUseCase.java` — Caso de uso de consulta de evento com verificação de posse.
  - `adapters/persistence/EventEntity.java` — Entidade JPA mapeando a tabela `events`.
  - `adapters/persistence/SpringDataEventRepository.java` — Interface Spring Data JPA.
  - `adapters/persistence/JpaEventRepository.java` — Implementação da porta `EventRepository`.
  - `http/EventsController.java` — Controller REST em `/api/v1/events` com `@PreAuthorize("hasRole('ORGANIZER')")`.
  - `http/CreateDraftEventRequest.java`, `http/EventResponse.java`, `http/EventsExceptionHandler.java` — DTOs e handler de exceções.
- `backend/src/test/java/br/com/elitedevticket/events/` — Testes de unidade, integração (PostgreSQL Testcontainers) e conformidade OpenAPI.
- `frontend/src/features/events/` — Módulo frontend para eventos:
  - `api/eventsApi.ts` — Cliente HTTP typed para criação e consulta de rascunhos.
  - `components/DraftEventEditor.tsx` — Superfície S11 (Editor de Evento DRAFT) com informações do snapshot e feedback.
  - `index.ts` — Exportações públicas da feature.
- `frontend/src/features/catalog/components/TicketmasterSearch.tsx` — Integração do fluxo de "Criar rascunho" a partir da referência selecionada.
- `frontend/scripts/check-openapi-contract.mjs` — Atualização do script de validação de drift para incluir operações e schemas de `events`.

## Tasks & Acceptance

**Execution:**

- [x] `backend/src/main/resources/db/migration/V3__create_events.sql` — Criar migration da tabela `events`.
- [x] `openapi/elite-dev-ticket-v1.yaml` — Declarar `POST /api/v1/events/drafts` e `GET /api/v1/events/{id}`, schemas e respostas correspondentes.
- [x] `backend/src/main/java/br/com/elitedevticket/events/` — Implementar domínio, aplicação, adapters de persistência e controller HTTP com DTOs.
- [x] `backend/src/test/java/br/com/elitedevticket/events/` — Implementar testes unitários, testes de integração de endpoints com RBAC e teste de conformidade OpenAPI.
- [x] `frontend/src/features/events/` — Implementar cliente `eventsApi.ts` e componente `DraftEventEditor.tsx`.
- [x] `frontend/src/features/catalog/components/TicketmasterSearch.tsx` — Conectar ação "Usar como referência" à criação do rascunho de evento.
- [x] `frontend/scripts/check-openapi-contract.mjs` — Atualizar verificador de contrato OpenAPI para cobrir `eventsApi.ts`.
- [x] `frontend/src/features/events/__tests__/` e testes de componentes — Criar suíte de testes de frontend para a criação e exibição do rascunho.
- [x] `_bmad-output/implementation-artifacts/sprint-status.yaml` — Atualizar status da Story 2.2 para `review`.

