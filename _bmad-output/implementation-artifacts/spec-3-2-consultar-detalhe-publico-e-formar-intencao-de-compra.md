---
title: 'Story 3.2 — Consultar detalhe público e formar intenção de compra'
type: 'feature'
created: '2026-08-15'
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

**Problem:** Visitantes anônimos ou clientes autenticados precisam consultar os detalhes completos de um evento publicado (Superfície S02) sem necessidade de autenticação prévia, compreendendo banner/fallback, título, descrição, categoria, data/hora (`America/Sao_Paulo`), local (`venueName` e `venueAddress`), setores de ingressos (`TicketSectors`), preços em BRL e disponibilidade autoritativa (`availableQuantity`). O usuário precisa selecionar um setor disponível e uma quantidade consciente (1–6 itens via `QuantityStepper`, limitada pelo estoque conhecido), formando uma intenção de compra transitória no frontend (em `sessionStorage` por até 15 minutos per AD-11), sem criar Reservation/HOLD, sem decrementar estoque e sem persistir compras antecipadamente. Ao tentar prosseguir, o visitante anônimo é encaminhado para login preservando a intenção, e clientes autenticados (`CUSTOMER`) podem prosseguir até o limite desta Story, enquanto outros papéis (`ORGANIZER`, `GATE`) são informados de incompatibilidade. Eventos com vendas encerradas (`startsAt <= serverNow` decidido no backend) continuam consultáveis, comunicando `SALES_CLOSED` e bloqueando a compra.

**Approach:**
No backend:
1. Em `EventsController`, liberar os endpoints `GET /api/v1/events/{id}` e `GET /api/v1/events/{eventId}/sectors` para acesso público (removendo `@PreAuthorize("isAuthenticated()")`), permitindo que visitantes anônimos e qualquer papel consultem eventos com status `PUBLISHED` e seus respectivos setores.
2. Garantir que `GetEventUseCase` e `ListTicketSectorsUseCase` continuem protegendo estritamente eventos em status `DRAFT` (retornando `403 Forbidden` com envelope padrão quando acessados por visitantes anônimos, clientes ou organizadores que não sejam proprietários do rascunho).
3. No contrato `openapi/elite-dev-ticket-v1.yaml`, atualizar a documentação e os requisitos de segurança de `GET /api/v1/events/{id}` e `GET /api/v1/events/{eventId}/sectors` para incluir autenticação opcional (`SessionCookie` ou anônimo `{}`).
4. Adicionar testes de integração (com Testcontainers) e testes de contrato OpenAPI para cobrir acesso anônimo a eventos publicados e proteção de rascunhos.

No frontend:
1. Em `features/events/model/purchaseIntention.ts`, criar modelo e helpers de persistência para intenção de compra em `sessionStorage` (`eventId`, `ticketSectorId`, `quantity` 1–6, `internalReturnPath`, `createdAt`), com validação e expiração estrita de 15 minutos (AD-11).
2. Criar o componente `QuantityStepper` (DESIGN.md / EXPERIENCE.md) com controles `+` / `-`, limites 1–6 respeitando `availableQuantity`, avisos claros de que a seleção é intenção e não garantia de estoque, e rótulos acessíveis.
3. Criar o componente `TicketSectorCard` exibindo nome, descrição, preço em BRL, disponibilidade numérica autoritativa ou badge "Esgotado" (`SOLD_OUT`), sem variante `LOW_AVAILABILITY` (AD-22).
4. Implementar a Superfície S02 (`PublicEventDetail.tsx`) com `EventHero`, `EventMetadata`, lista de setores, `QuantityStepper`, resumo de preço estimado (`PriceSummary`), aviso de vendas encerradas quando `salesClosed`, estados de loading (skeleton/status), erro (com retry/voltar), não encontrado (404) e acesso negado (403).
5. Integrar a transição entre Catálogo (`PublicEventCatalog`), Detalhe do Evento (`PublicEventDetail`) e Login (`LoginForm` / S03) na navegação da aplicação (`App.tsx` e `AuthenticatedSession.tsx`), permitindo retorno ao catálogo, formação de intenção e redirecionamento seguro com mensagem contextual.
6. Criar suíte completa de testes unitários e de integração no frontend para todos os cenários e critérios de acessibilidade (WCAG 2.1 AA).

## Boundaries & Constraints

**Always:**
- Detalhe de evento `PUBLISHED` é público e não requer autenticação.
- Evento `DRAFT` jamais é acessível publicamente (retorna 403 Forbidden para anônimos e papéis não-proprietários).
- Backend é autoridade sobre publicação, vendas abertas (`salesClosed`) e disponibilidade de setores (`availableQuantity`).
- `QuantityStepper` restringe quantidade entre 1 e 6 e ao estoque disponível conhecido.
- Formar intenção transitória no frontend em `sessionStorage` com TTL de 15 minutos contendo apenas `eventId`, `ticketSectorId`, `quantity`, `internalReturnPath`, `createdAt`.
- Preservar intenção no redirecionamento para login de usuários anônimos.
- Usuários autenticados com papel `CUSTOMER` podem formar intenção, sem criação antecipada de `Reservation` ou `HOLD`.
- Outros papéis (`ORGANIZER`, `GATE`) não podem comprar ou formar hold como cliente sem login apropriado.
- Exibir datas e horas no fuso `America/Sao_Paulo` (pt-BR).
- Nunca expor nem usar threshold `LOW_AVAILABILITY` (AD-22).
- Tratar loading, erro, evento inexistente, vendas encerradas e setor esgotado com acessibilidade total (WCAG 2.1 AA).
- Preservar OpenAPI, ArchUnit, TypeScript strict e sem drift de contratos.

**Never:**
- Nunca criar `Reservation`, `HOLD` ou decrementar estoque nesta Story (não antecipar Epic 4).
- Nunca persistir dados de compra no backend nesta Story.
- Nunca permitir acesso público ou por outros usuários a eventos em `DRAFT`.
- Nunca expor credenciais, segredos ou tokens.
- Não usar `any`, `@ts-ignore` ou bypass de tipos no TypeScript.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Consulta de Evento `PUBLISHED` por visitante anônimo | `GET /api/v1/events/{id}` sem sessão | HTTP 200 OK com `EventResponse` completo | Resposta 200 |
| Consulta de Setores de Evento `PUBLISHED` por visitante anônimo | `GET /api/v1/events/{eventId}/sectors` sem sessão | HTTP 200 OK com `TicketSectorListResponse` e `availableQuantity` | Resposta 200 |
| Tentativa de acesso público a Evento `DRAFT` | `GET /api/v1/events/{id}` sem sessão ou com `CUSTOMER` | HTTP 403 com envelope `AUTH_FORBIDDEN` | Handler RBAC |
| Evento inexistente | `GET /api/v1/events/{id}` com UUID aleatório | HTTP 404 com envelope `EVENT_NOT_FOUND` | UI exibe "Evento não encontrado" com botão de voltar |
| Evento com vendas encerradas (`startsAt <= serverNow`) | Detalhe de evento iniciado | UI comunica `SALES_CLOSED` ("Vendas encerradas"), desabilita seleção/stepper e bloqueia CTA | Consulta preservada sem compra |
| Setor esgotado (`availableQuantity == 0`) | Detalhe com setor sem estoque | Setor exibe badge "Esgotado" (`SOLD_OUT`), não permite seleção para reserva | Impede seleção de quantidade |
| Seleção de quantidade 1–6 com limite de estoque | Setor com 3 ingressos disponíveis | Stepper permite apenas 1, 2 ou 3 ingressos | Desabilita botão `+` no limite |
| Visitante anônimo clica em "Reservar" | Intenção válida selecionada | Intenção salva em `sessionStorage` (15 min) e redireciona para login com aviso explicativo | Preserva contexto para restauração |
| Cliente autenticado (`CUSTOMER`) clica em "Reservar" | Intenção válida selecionada | Intenção salva e feedback de confirmação da intenção exibido (fronteira da Story 3.2) | Não cria Reservation |
| Organizador ou Portaria clica em "Reservar" | Usuário logado com `ORGANIZER` ou `GATE` | UI informa que a operação exige conta de Cliente (`CUSTOMER`) e oferece troca de conta | Bloqueia compra com outros papéis |

</frozen-after-approval>

## Code Map

- `openapi/elite-dev-ticket-v1.yaml` — Atualização de segurança para `GET /api/v1/events/{id}` e `GET /api/v1/events/{eventId}/sectors` com acesso público opcional.
- `backend/src/main/java/br/com/elitedevticket/events/` — Módulo `events`:
  - `http/EventsController.java` — Remover `@PreAuthorize("isAuthenticated()")` em `getEvent` e `listTicketSectors`.
- `backend/src/test/java/br/com/elitedevticket/events/` — Testes backend:
  - `EventsEndpointsIntegrationTest.java` — Testes de integração para acesso anônimo a eventos publicados e setores, bloqueio de rascunhos e erros.
  - `EventsOpenApiContractTest.java` — Ajuste de asserções de segurança para `getEvent` e `listTicketSectors`.
- `frontend/src/features/events/` — Módulo frontend:
  - `model/purchaseIntention.ts` — Tipos e helpers de `sessionStorage` para intenção de compra (AD-11).
  - `model/__tests__/purchaseIntention.test.ts` — Testes unitários de persistência, validação e expiração de intenção.
  - `components/QuantityStepper.tsx` — Componente de controle de quantidade 1–6 acessível.
  - `components/TicketSectorCard.tsx` — Componente de card de setor com preço BRL e disponibilidade autoritativa.
  - `components/PublicEventDetail.tsx` — Componente da página/superfície S02 de detalhe público do evento.
  - `components/__tests__/QuantityStepper.test.tsx` — Testes de acessibilidade e limites do stepper.
  - `components/__tests__/PublicEventDetail.test.tsx` — Testes de detalhe, seleção, vendas encerradas, erro e intenção.
  - `index.ts` — Exportação dos novos componentes e utilitários.
- `frontend/src/app/App.tsx` e `AuthenticatedSession.tsx` — Integração de navegação e redirecionamento de login com intenção.
- `frontend/src/app/App.test.tsx` — Teste de integração do fluxo completo da Story 3.2.
