---
title: 'Story 2.5 — Revisar e publicar Event válido'
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

**Problem:** O Organizer precisa revisar todas as pendências obrigatórias de um evento em rascunho (`DRAFT`) antes de disponibilizá-lo para venda, com identificação clara de requisitos faltantes, navegação por teclado/foco até o campo ou setor pendente, e publicação transacional autoritativa no backend (`DRAFT → PUBLISHED`), assegurando que eventos incompletos, com data no passado, sem setores ou já publicados não possam ser publicados indevidamente.

**Approach:**
No backend:
1. Adicionar método de domínio `publish(...)` na entidade `Event` validando as invariantes de publicação (`status == DRAFT`, `title`, `venue`, `startsAt > serverNow` via `Clock`, `externalId` e pelo menos um `TicketSector` válido).
2. Criar o caso de uso `PublishEventUseCase` com anotação `@Transactional`, validação de propriedade do Organizer, consulta de setores e persistência do evento atualizado para `PUBLISHED`.
3. Expor endpoint `POST /api/v1/events/{id}/publish` em `EventsController` protegido por `@PreAuthorize("hasRole('ORGANIZER')")` e proteção CSRF.
4. Mapear o endpoint e respostas no contrato `openapi/elite-dev-ticket-v1.yaml`.

No frontend:
1. Adicionar função `publishEvent(id)` em `features/events/api/eventsApi.ts` e registrar o endpoint no verificador OpenAPI `check-openapi-contract.mjs`.
2. Implementar o componente `PublicationChecklist` (Superfície S13) com verificação de itens obrigatórios (referência Ticketmaster, título, local, data futura, setores) e opcionais (descrição, banner, categoria), foco por teclado aos campos pendentes, bloqueio de publicação enquanto houver pendências e anúncio acessível de estados ready/publishing/success/error.
3. Integrar `PublicationChecklist` no `DraftEventEditor.tsx`, tratando transição de estado após publicação, bloqueio de ações exclusivas de rascunho e confirmação de sucesso autoritativo.

## Boundaries & Constraints

**Always:**
- Somente eventos com status `DRAFT` podem ser publicados.
- Somente o Organizer proprietário do evento pode publicá-lo.
- Validação temporal estrita: `startsAt > serverNow` usando `Clock` injetado no backend.
- Exigência de pelo menos 1 setor de ingressos (`TicketSector`) válido.
- Publicação é uma transição atômica `DRAFT → PUBLISHED` persistida transacionalmente.
- Resposta autoritativa antes de confirmar o sucesso na UI.
- Ausência de descrição, imagem ou categoria não bloqueia publicação nem exibe imagem quebrada/nulos.
- Preservação de contratos OpenAPI, RBAC, CSRF e fronteiras modulares ArchUnit.

**Never:**
- Não permitir publicação por outros usuários ou organizadores não proprietários (HTTP 403 `AUTH_FORBIDDEN`).
- Não permitir publicação de evento inexistente (HTTP 404 `EVENT_NOT_FOUND`).
- Não permitir republicação de evento já `PUBLISHED` (HTTP 409 `EVENT_CANNOT_BE_MODIFIED`).
- Não alterar a disponibilidade ou capacidades dos setores durante a publicação.
- Não antecipar regras completas de edição pós-publicação da Story 2.6.
- Não antecipar catálogo Customer / Epic 3 ou lógica de Reservation/HOLD do Epic 4.
- Não usar `Instant.now()` ou `LocalDateTime.now()` diretamente no domínio/aplicação sem `Clock`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Publicar rascunho completo e válido | `POST /api/v1/events/{id}/publish` com sessão ORGANIZER dona, dados válidos, startsAt futuro e >=1 setor | HTTP 200 OK com `EventResponse` (`status: "PUBLISHED"`), UI exibe confirmação de sucesso | Transição autoritativa |
| Publicar evento já `PUBLISHED` | `POST /api/v1/events/{id}/publish` para evento já publicado | HTTP 409 `EVENT_CANNOT_BE_MODIFIED` ("Apenas eventos em rascunho podem ser publicados.") | Rejeição com conflito |
| Publicar evento sem setores | `POST /api/v1/events/{id}/publish` para evento DRAFT sem setores | HTTP 400 `AUTH_INVALID_REQUEST` com fieldError indicando ausência de setores | Checklist bloqueia CTA e foca gerenciador de setores |
| Publicar evento com `startsAt` no passado ou nulo | `POST /api/v1/events/{id}/publish` com startsAt <= clock.instant() | HTTP 400 `AUTH_INVALID_REQUEST` com fieldError `startsAt` | Checklist bloqueia CTA e foca campo de data |
| Publicar evento com título em branco | `POST /api/v1/events/{id}/publish` com title em branco | HTTP 400 `AUTH_INVALID_REQUEST` com fieldError `title` | Checklist bloqueia CTA e foca campo de título |
| Publicar evento com local/venue em branco | `POST /api/v1/events/{id}/publish` com venue em branco/nulo | HTTP 400 `AUTH_INVALID_REQUEST` com fieldError `venue` | Checklist bloqueia CTA e foca campo de local |
| Publicar evento de outro Organizer | `POST /api/v1/events/{id}/publish` por usuário não proprietário | HTTP 403 `AUTH_FORBIDDEN` | Bloqueio de autorização |
| Publicar evento inexistente | `POST /api/v1/events/{id}/publish` com ID aleatório | HTTP 404 `EVENT_NOT_FOUND` | Resposta 404 |
| Publicar sem campos opcionais (sem imagem/descrição/categoria) | `POST /api/v1/events/{id}/publish` com campos opcionais nulos mas obrigatórios preenchidos | HTTP 200 OK (`status: "PUBLISHED"`), publicação realizada com sucesso | Sucesso |

</frozen-after-approval>

## Code Map

- `openapi/elite-dev-ticket-v1.yaml` — Declaração do endpoint `/api/v1/events/{id}/publish` (`POST`), responses 200, 400, 401, 403, 404, 409.
- `backend/src/main/java/br/com/elitedevticket/events/` — Módulo `events`:
  - `domain/Event.java` — Método `publish(...)` com validações de invariantes.
  - `application/PublishEventUseCase.java` — Caso de uso transacional de publicação.
  - `http/EventsController.java` — Endpoint `POST /api/v1/events/{id}/publish`.
- `backend/src/test/java/br/com/elitedevticket/events/` — Testes de unidade e integração para publicação.
- `frontend/src/features/events/api/eventsApi.ts` — Função `publishEvent(id)`.
- `frontend/src/features/events/components/PublicationChecklist.tsx` — Componente S13 de checklist de pendências e publicação.
- `frontend/src/features/events/components/DraftEventEditor.tsx` — Integração de `PublicationChecklist`.
- `frontend/src/features/events/components/__tests__/PublicationChecklist.test.tsx` — Testes de acessibilidade, pendências, foco e submissão.
