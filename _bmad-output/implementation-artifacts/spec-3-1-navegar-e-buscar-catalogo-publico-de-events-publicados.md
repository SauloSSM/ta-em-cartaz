---
title: 'Story 3.1 — Navegar e buscar catálogo público de Events publicados'
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

**Problem:** Visitantes (autenticados ou anônimos) precisam navegar e buscar eventos públicos disponíveis para compra sem necessidade de login prévio, visualizando exclusivamente eventos com status `PUBLISHED` com imagem/fallback, título, data, local, preço inicial (`startingPrice = MIN(TicketSector.price)`) e indicação clara de vendas encerradas quando aplicável (`serverNow >= startsAt`), com busca textual simples por título e estados acessíveis de carregamento, resultados, vazio e erro.

**Approach:**
No backend:
1. No repositório `EventRepository` e `SpringDataEventRepository`, adicionar métodos para consultar exclusivamente eventos `PUBLISHED`, ordenados por data de início/criação, com suporte opcional a filtro por título (case-insensitive substring).
2. Criar o caso de uso `ListPublicEventsUseCase` no pacote `events.application`, calculando `startingPrice = MIN(TicketSector.price)` a partir dos setores do evento e `salesClosed = event.startsAt() != null && !clock.instant().isBefore(event.startsAt())` usando `Clock` injetado.
3. Expor endpoint público `GET /api/v1/events` em `EventsController` sem exigência de autenticação, aceitando parâmetro opcional `search`.
4. Mapear o endpoint, parâmetros, responses e schemas `PublicEventResponse` e `PublicEventListResponse` no contrato `openapi/elite-dev-ticket-v1.yaml`.

No frontend:
1. Em `features/events/api/eventsApi.ts`, declarar os tipos `PublicEventResponse`, `PublicEventListResponse` e a função cliente `listPublicEvents(search?: string)`.
2. Implementar o componente `EventCard` (Superfície S01 / DESIGN.md) exibindo imagem com fallback, título, categoria, data formatada (`America/Sao_Paulo`), local (`venueName`/`venueAddress`), preço inicial formatado em BRL (`A partir de R$ ...`) e badge/aviso "Vendas encerradas" quando `salesClosed` for verdadeiro.
3. Implementar o componente `PublicEventCatalog` (Superfície S01) com campo de busca simples por título, botão de buscar e limpar busca, anúncios acessíveis para leitor de tela (`aria-live="polite"`), e estados específicos de loading (skeleton/status), vazio (com e sem busca) e erro (com retry acessível).
4. Integrar o catálogo público na navegação da SPA (`App.tsx`), permitindo acesso imediato a visitantes anônimos e clientes autenticados.

## Boundaries & Constraints

**Always:**
- Acesso público irrestrito: visitantes anônimos ou usuários de qualquer papel podem consultar o catálogo.
- Backend é autoridade do filtro: somente eventos com `status == PUBLISHED` são retornados; eventos `DRAFT` jamais podem aparecer.
- `startingPrice = MIN(TicketSector.price)` calculado a partir dos setores ativos do evento.
- Eventos publicados com `startsAt <= serverNow` permanecem visíveis no catálogo, mas comunicam claramente que as vendas estão encerradas (`salesClosed == true`).
- Apenas busca simples por título/nome; sem filtros avançados fora do escopo da Story 3.1.
- Sem implementação de detalhe completo / compra da Story 3.2.
- Sem implementação de Reservation / HOLD do Epic 4.
- Conformidade total com OpenAPI, ArchUnit, TypeScript estrito e WCAG 2.1 AA (semântica, landmarks, contraste, foco por teclado).

**Never:**
- Nunca retornar ou exibir eventos com status `DRAFT` na listagem pública.
- Não depender apenas do frontend para filtrar eventos publicados.
- Não usar `Instant.now()` ou `System.currentTimeMillis()` sem `Clock` injetado.
- Não usar `double` ou `float` para valores monetários; usar `BigDecimal` no backend e formatação BRL determinística no frontend.
- Não usar `any`, `@ts-ignore` ou cast cego no TypeScript.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Listagem pública padrão | `GET /api/v1/events` sem autenticação | HTTP 200 OK com `{ events: PublicEventResponse[] }` contendo apenas eventos `PUBLISHED` | Retorna lista de eventos |
| Busca por título com correspondência | `GET /api/v1/events?search=rock` | HTTP 200 OK com eventos cujo título contenha "rock" (case-insensitive) | Retorna correspondências |
| Busca por título sem correspondência | `GET /api/v1/events?search=inexistente` | HTTP 200 OK com `{ events: [] }` | UI exibe estado vazio específico com opção de limpar busca |
| Catálogo sem eventos publicados | `GET /api/v1/events` quando só existem DRAFTs ou banco vazio | HTTP 200 OK com `{ events: [] }` | UI exibe "Nenhum evento publicado no momento" |
| Evento com vendas encerradas | Evento `PUBLISHED` com `startsAt <= clock.instant()` | `salesClosed: true`, `startingPrice` calculado normalmente | UI exibe badge "Vendas encerradas" |
| Evento com múltiplos setores | Evento com setores de R$ 100, R$ 250 e R$ 400 | `startingPrice: 100.00` | UI exibe "A partir de R$ 100,00" |
| Falha na rede / backend indisponível | Falha na requisição HTTP | UI exibe estado de erro acessível com botão "Tentar novamente" | Retry reexecuta a busca |

</frozen-after-approval>

## Code Map

- `openapi/elite-dev-ticket-v1.yaml` — Declaração do endpoint `GET /api/v1/events`, query parameter `search`, schemas `PublicEventResponse` e `PublicEventListResponse`.
- `backend/src/main/java/br/com/elitedevticket/events/` — Módulo `events`:
  - `http/PublicEventResponse.java` — Record DTO de saída do evento público.
  - `http/PublicEventListResponse.java` — Record DTO de lista de eventos públicos.
  - `application/EventRepository.java` — Método `findPublished(String titleSearch)`.
  - `adapters/persistence/SpringDataEventRepository.java` — Consultas JPA para eventos publicados.
  - `adapters/persistence/JpaEventRepository.java` — Implementação do método `findPublished`.
  - `application/ListPublicEventsUseCase.java` — Caso de uso que busca eventos e computa `startingPrice` e `salesClosed`.
  - `http/EventsController.java` — Endpoint `GET /api/v1/events`.
- `backend/src/test/java/br/com/elitedevticket/events/` — Testes de unidade e integração para o catálogo público.
- `frontend/src/features/events/api/eventsApi.ts` — Tipos `PublicEventResponse`, `PublicEventListResponse` e função `listPublicEvents`.
- `frontend/src/features/events/components/EventCard.tsx` — Componente visual do card de evento público.
- `frontend/src/features/events/components/PublicEventCatalog.tsx` — Componente da página/superfície S01 de catálogo e busca.
- `frontend/src/features/events/components/__tests__/PublicEventCatalog.test.tsx` — Testes de acessibilidade, estados de loading, vazio, resultados e erro.
