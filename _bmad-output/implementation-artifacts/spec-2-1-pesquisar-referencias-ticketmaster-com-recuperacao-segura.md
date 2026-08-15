---
title: 'Story 2.1 — Pesquisar referências Ticketmaster com recuperação segura'
type: 'feature'
created: '2026-08-15'
status: 'done'
baseline_commit: '018cfc683bc0fdf27a74c345ea9d3780593912db'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O Organizer precisa iniciar a criação de eventos a partir de dados reais e estruturados da Ticketmaster sem redigitar informações básicas, mas a integração não pode expor chaves de API ao cliente, propagar payloads brutos não mapeados, degradar a experiência com esperas indefinidas em falhas de rede/rate limit, nem permitir criação manual não autorizada em caso de indisponibilidade do catálogo externo.

**Approach:** Implementar a integração backend-only com a Ticketmaster Discovery API através da porta `CatalogProvider` e do adapter `TicketmasterDiscoveryAdapter` no módulo `catalog`, operando sob orçamento temporal rígido (budget total configurável de aproximadamente 5 segundos, incluindo eventual retry; no máximo 1 retry automático apenas para falhas transitórias de rede, timeout ou HTTP 5xx; HTTP 429 tratado imediatamente como rate limit/indisponibilidade sem retry ou backoff longo; demais HTTP 4xx sem retry; sem cache nem circuit breaker no MVP). Expor o endpoint `GET /api/v1/catalog/events` protegido por RBAC (`ROLE_ORGANIZER`), alinhado ao contrato OpenAPI versionado com envelope de erro AD-12 (`CATALOG_UNAVAILABLE`). No frontend, entregar a Superfície S10 acessível com formulário de busca, estados de loading com anúncio sonoro/aria-live, resultados em `TicketmasterResultCard` com fallback de imagem, estado vazio, recuperação com ação explícita do usuário "Tentar novamente" preservando a busca e ação "Usar como referência".

## Boundaries & Constraints

**Always:**
- Ticketmaster Discovery API v2 (`/discovery/v2/events.json`) é o único catálogo externo do sistema.
- Integração backend-only: `TICKETMASTER_API_KEY` reside exclusivamente na configuração do backend (`catalog.ticketmaster.api-key`) e nunca é enviada ao navegador ou logada.
- O módulo `catalog` expõe a porta `CatalogProvider` (em `domain`/`application`) implementada por `TicketmasterDiscoveryAdapter` (em `adapters.ticketmaster`).
- Orçamento temporal (budget) total configurável de até aproximadamente 5 segundos, incluindo eventual retry.
- No máximo 1 retry automático apenas para falha transitória de rede, timeout ou HTTP 5xx.
- HTTP 429 (Rate Limit) da Ticketmaster não recebe retry automático e é tratado imediatamente como rate limit/indisponibilidade sem espera longa ou backoff.
- Demais erros HTTP 4xx (400, 401, 403, 404, etc.) da Ticketmaster nunca sofrem retry automático.
- Sem cache (Redis, Caffeine, Spring Cache) e sem circuit breaker no MVP (AD-18).
- Mapeamento estrito para snapshot aprovado: `externalId`, `title`, `description`, `imageUrl`, `category`. O payload bruto JSON da Ticketmaster nunca é persistido nem retornado à SPA.
- `externalId` não é chave primária nem globalmente único entre Events internos (pode ser reutilizado por múltiplos eventos/organizadores).
- Endpoint `GET /api/v1/catalog/events` exige autenticação ativa e papel `ROLE_ORGANIZER` via Spring Security/Method Security (anônimo -> 401 `AUTH_UNAUTHENTICATED`, `CUSTOMER`/`GATE` -> 403 `AUTH_FORBIDDEN`).
- Falha da Ticketmaster após esgotar tentativas/budget responde HTTP 503 com código `CATALOG_UNAVAILABLE` no envelope padrão `{code, message, fieldErrors?, traceId, timestamp}`.
- Superfície S10 (Busca Ticketmaster) no frontend: busca acessível com label persistente, feedback de carregamento (`aria-live="polite"`), lista de `TicketmasterResultCard`, estado vazio claro ("Nenhum evento encontrado"), e estado de indisponibilidade com mensagem explicativa e ação explícita de "Tentar novamente" (novo acionamento humano) que preserva a query digitada.
- Ação "Usar como referência" declara a seleção com feedback acessível (a criação física de Event `DRAFT` pertence exclusivamente à Story 2.2).
- OpenAPI versionado em `openapi/elite-dev-ticket-v1.yaml` atualizado e validado por testes de drift (backend e frontend).

**Ask First:**
- Adicionar dependências externas não aprovadas (ex: Resilience4j, Spring Cloud, bibliotecas de UI adicionais).
- Modificar contratos HTTP de autenticação ou schemas já estabelecidos no Epic 1.
- Alterar as propriedades de configuração padrão de ambiente.

**Never:**
- Não implementar criação de Event interno `DRAFT`, tabelas JPA de eventos ou setores (escopo da Story 2.2).
- Não persistir payload bruto da Ticketmaster em banco de dados ou storage.
- Não expor `TICKETMASTER_API_KEY` ou segredos ao frontend, em headers públicos ou em logs.
- Não oferecer opção de criação manual de eventos quando a Ticketmaster estiver indisponível.
- Não utilizar cache persistente ou em memória no MVP.
- Não executar retry automático para status HTTP 4xx recebidos da Ticketmaster (incluindo 429, tratado imediatamente como indisponibilidade).
- Não violar o isolamento de pacotes do monólito modular verificado pelo ArchUnit.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Busca com sucesso | `GET /api/v1/catalog/events?keyword=Rock` com sessão `ORGANIZER` válida; Ticketmaster retorna 200 com eventos | HTTP 200 com array de `CatalogEventReference` (`externalId`, `title`, `description`, `imageUrl`, `category`) no formato aprovado | N/A |
| Busca sem resultados | `GET /api/v1/catalog/events?keyword=NenhumResultado` com Ticketmaster retornando 0 eventos | HTTP 200 com array vazio `[]`; UI renderiza estado vazio acessível | N/A |
| Busca com termo em branco / ausente | `GET /api/v1/catalog/events` com keyword vazia | HTTP 200 com lista vazia ou eventos gerais conforme busca sem filtro; validação segura de parâmetros | Resposta segura |
| Falha transitória recuperada no retry | Falha de rede, timeout ou 500 na 1ª tentativa da Ticketmaster; 2ª tentativa responde 200 dentro do budget (<= 5s) | HTTP 200 com os dados mapeados retornados após retry transparente | Falha inicial é absorvida pelo adapter |
| Timeout / Falha persistente | Ticketmaster inacessível, timeout esgotado (> 5s total) ou 5xx repetido | HTTP 503 com envelope `CATALOG_UNAVAILABLE`; UI exibe mensagem de indisponibilidade e botão de "Tentar novamente" humano preservando a busca, sem opção de criação manual | Envelope AD-12 sem stack trace ou dados confidenciais |
| Rate Limit Ticketmaster (429) | Ticketmaster responde 429 Too Many Requests | Backend não executa retry automático; trata imediatamente como rate limit/indisponibilidade sem espera ou backoff; retorna HTTP 503 com envelope `CATALOG_UNAVAILABLE`; UI oferece botão "Tentar novamente" ao usuário | Não retenta em loop; erro seguro ao cliente |
| Erro de cliente Ticketmaster (4xx) | Ticketmaster responde 400/401/403/404 (ex: chave inválida ou parâmetro rejeitado) | Backend não executa retry automático; loga erro operacional e retorna HTTP 503 `CATALOG_UNAVAILABLE` ou 400 seguro ao cliente | Não expõe chave de API nem detalhes da requisição externa |
| Requisição não autenticada | `GET /api/v1/catalog/events` sem cookie de sessão | HTTP 401 com envelope `AUTH_UNAUTHENTICATED` | Handler de segurança padrão |
| Requisição por Customer ou Gate | `GET /api/v1/catalog/events` com JWT de `CUSTOMER` ou `GATE` | HTTP 403 com envelope `AUTH_FORBIDDEN` | Handler RBAC padrão |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/epics.md:296-313` — Critérios de aceitação autoritativos da Story 2.1.
- `_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md:164-169` — AD-18 (Integração Ticketmaster), AD-12 (Contratos e erros), AD-21 (Testes de piso).
- `docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md:1280-1345` — Seção 22 (Ticketmaster Discovery API, snapshot de dados e chave de ambiente).
- `docs/engineering/java-standards.md` — Regras de integração, timeouts, DTOs e testes backend.
- `docs/engineering/react-standards.md` — Padrões React 19, TypeScript estrito, estado de servidor e acessibilidade.
- `openapi/elite-dev-ticket-v1.yaml` — Definição do endpoint `GET /api/v1/catalog/events`, schemas de resposta, query param `keyword` e códigos de erro de catálogo.
- `backend/src/main/resources/application*.yaml` — Propriedades de configuração `catalog.ticketmaster.*` (api-key, base-url, timeouts, max-retries).
- `backend/src/main/java/br/com/elitedevticket/catalog/` — Novo módulo modular do catálogo:
  - `domain/CatalogEventReference.java` — Modelo imutável representando a referência externa mapeada.
  - `application/CatalogProvider.java` — Porta de busca no catálogo externo.
  - `application/SearchCatalogUseCase.java` — Caso de uso de aplicação para busca de referências.
  - `adapters/ticketmaster/TicketmasterDiscoveryAdapter.java` — Adapter que consome a Discovery API via `RestClient`, com budget total de até aproximadamente 5s, no máximo 1 retry para falha de rede/timeout/5xx, tratamento imediato de 429 sem retry/backoff, sem retry para demais 4xx e sem cache.
  - `adapters/ticketmaster/TicketmasterProperties.java` — Propriedades validadas de integração da Ticketmaster.
  - `http/CatalogController.java` — Controller REST em `/api/v1/catalog/events` protegido com `@PreAuthorize("hasRole('ORGANIZER')")`.
  - `http/CatalogErrorCode.java` e `http/CatalogExceptionHandler.java` — Mapeamento de erros de catálogo para `ApiErrorResponse`.
- `backend/src/test/java/br/com/elitedevticket/catalog/` — Testes unitários do adapter (WireMock/MockRestServiceServer), testes de segurança/RBAC e testes de drift OpenAPI.
- `frontend/src/features/catalog/` — Superfície S10 e clientes de API:
  - `api/catalogApi.ts` — Cliente HTTP typed para `/api/v1/catalog/events`.
  - `components/TicketmasterSearch.tsx` — Componente principal da busca com formulário, aria-live, empty state e retry pelo usuário.
  - `components/TicketmasterResultCard.tsx` — Card de resultado exibindo título, imagem com fallback, categoria, descrição e botão de referência.
- `frontend/src/features/catalog/__tests__/` — Testes de componente e acessibilidade da Superfície S10.
- `frontend/scripts/check-openapi-contract.mjs` — Atualização do check de drift para cobrir operações e tipos de catálogo.

## Tasks & Acceptance

**Execution:**

- [x] `openapi/elite-dev-ticket-v1.yaml` — Declarar `GET /api/v1/catalog/events` com query parameter `keyword` (opcional), segurança por `SessionCookie`, respostas 200 (`CatalogSearchResponse`), 400 (`AuthInvalidRequest`), 401 (`AuthUnauthenticated`), 403 (`AuthForbidden`) e 503 (`CatalogUnavailableResponse`), e os schemas correspondentes.
- [x] `backend/src/main/resources/application*.yaml` e `application-test.yaml` — Configurar propriedades `catalog.ticketmaster` com `api-key`, `base-url`, `connect-timeout`, `read-timeout` e `max-retries`, permitindo override por `TICKETMASTER_API_KEY`.
- [x] `backend/src/main/java/br/com/elitedevticket/catalog/{domain,application}` — Implementar o modelo `CatalogEventReference`, a porta `CatalogProvider` e o caso de uso `SearchCatalogUseCase`.
- [x] `backend/src/main/java/br/com/elitedevticket/catalog/adapters/ticketmaster` — Implementar `TicketmasterDiscoveryAdapter` usando `RestClient` com budget total configurado de até aproximadamente 5s, retry automático máximo de 1 vez apenas para falha transitória (rede/timeout/5xx), tratamento imediato de 429 como rate limit/indisponibilidade sem retry ou backoff longo, sem retry para demais 4xx e sem cache.
- [x] `backend/src/main/java/br/com/elitedevticket/catalog/http` — Implementar `CatalogController` com `@PreAuthorize("hasRole('ORGANIZER')")`, DTOs de resposta e tratamento de exceção gerando envelope AD-12 com código `CATALOG_UNAVAILABLE`.
- [x] `backend/src/test/java/br/com/elitedevticket/catalog` — Criar suíte de testes cobrindo adapter (sucesso, parsing, campos ausentes/fallback, 5xx transitório com retry, 5xx persistente gerando 503, 429 tratado imediatamente como 503 sem retry automático, 4xx sem retry), testes de autorização RBAC do endpoint e testes de conformidade OpenAPI.
- [x] `frontend/src/features/catalog/api/catalogApi.ts` — Implementar cliente HTTP typed consumindo `/api/v1/catalog/events` e tratando respostas e erros com envelope seguro.
- [x] `frontend/src/features/catalog/components/{TicketmasterSearch,TicketmasterResultCard}.tsx` — Implementar Superfície S10 com formulário de busca, estados visuais (idle, loading com aria-live, lista de cards com imagem/fallback, vazio claro, erro com botão de ação do usuário "Tentar novamente" preservando termo digitado) e botão "Usar como referência" com anúncio acessível, sem opção de criação manual.
- [x] `frontend/src/features/catalog/__tests__/` e `frontend/scripts/check-openapi-contract.mjs` — Implementar testes de componente e atualizar script de drift para verificar tipos e rotas de catálogo.
- [x] `_bmad-output/implementation-artifacts/sprint-status.yaml` — Atualizar rastreamento da Story 2.1 e Epic 2.

**Acceptance Criteria:**

- Given um Organizer autenticado na busca de catálogo, when pesquisa uma referência por palavra-chave, then a API usa exclusivamente `CatalogProvider` / adapter backend e devolve título, imagem, descrição e categoria quando fornecidos pelo catálogo.
- Given a interface de busca do catálogo (Superfície S10), when o usuário realiza uma pesquisa, then a UI exibe campo de busca, indicador de loading, lista de resultados, estado vazio quando não houver correspondências e ação "Usar como referência" com anúncio acessível.
- Given uma falha transitória de rede, timeout ou 5xx na Ticketmaster, when a requisição é processada pelo adapter, then no máximo um retry automático é executado dentro do budget total de aproximadamente 5 segundos.
- Given HTTP 429 (rate limit) ou indisponibilidade persistente da Ticketmaster, when a busca falha, then o backend não executa retry automático e retorna HTTP 503 com código `CATALOG_UNAVAILABLE`, and a UI informa indisponibilidade e oferece ação explícita de "Tentar novamente" ao usuário preservando a busca, sem criação manual.
- Given uma requisição não autenticada ou de usuário com papel diferente de `ORGANIZER` (`CUSTOMER` ou `GATE`), when tenta acessar o catálogo, then o backend recusa com 401 ou 403 no envelope padrão sem executar a consulta externa.
- Given a execução dos checks automatizados de build no backend e frontend, then nenhum drift é detectado em relação ao OpenAPI e as regras de arquitetura modular do ArchUnit permanecem satisfeitas.

## Spec Change Log

## Design Notes

- O `TicketmasterDiscoveryAdapter` utiliza `RestClient` nativo do Spring Framework 6+ / Spring Boot 4 sem adicionar bibliotecas terceiras de cliente HTTP.
- O parser de payload da Ticketmaster mapeia com segurança a hierarquia `_embedded.events[]`, extraindo com segurança campos opcionais: `name` -> `title`, `images` (selecionando imagem de resolução adequada ou primeira disponível) -> `imageUrl`, `classifications[0].segment.name` / `genre.name` -> `category`, `info` / `description` -> `description`.
- A Superfície S10 é integrada na experiência do Organizer quando autenticado, permitindo a transição fluida para a futura Story 2.2 através do evento de seleção da referência.

## Verification

**Commands:**

- `backend/mvnw test` — esperado: todos os testes unitários do adapter, endpoints de catálogo com RBAC, conformidade OpenAPI e regras ArchUnit passam sem erros.
- `npm --prefix frontend test -- --run` — esperado: testes de componente da Superfície S10 (busca, loading, lista, vazio, erro/retry) e verificação de contrato OpenAPI passam.
- `npm --prefix frontend run build` — esperado: compilação TypeScript estrita e build Vite passam com sucesso.
- `docker compose config` — esperado: configuração válida e segura.
