---
title: 'Story 1.2 — Autenticar e encerrar sessão de contas provisionadas'
type: 'feature'
created: '2026-08-14'
status: 'in-review'
baseline_commit: '529fe05e5798b0226a3f85666fa8fbb6669ec4b6'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** As contas da Story 1.1 ainda não iniciam nem encerram sessão, impedindo acesso e troca segura durante a avaliação.

**Approach:** Entregar login/logout full-stack acessível com BCrypt, JWT HS256 stateless em cookie HttpOnly e CSRF, guiado por um OpenAPI mínimo coerente com backend e frontend.

## Boundaries & Constraints

**Always:** Definir primeiro no OpenAPI `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` e `GET /api/v1/auth/session`. Erros seguem AD-12 `{code,message,fieldErrors?,traceId,timestamp}`, só com códigos auth. Usar Spring Security, BCrypt e JWT HS256; `EDT_SESSION` é HttpOnly/SameSite=Lax/Path=/, Secure em `demo`/`prod`, expira com o JWT e tem TTL padrão configurável de 8 h. Segredo externo CSPRNG >=256 bits é obrigatório fora de dev. `XSRF-TOKEN` tem HttpOnly=false, atributos por ambiente e Secure em `demo`/`prod`; header `X-XSRF-TOKEN`; rotacionar após login/logout. JWT nunca chega ao JavaScript. Preservar health público, mesma origem, CORS restrito e acessibilidade pt-BR.

**Ask First:** Alterar contrato; adicionar dependência além de Security/JOSE do Boot, `vitest`, Testing Library, `user-event` e `jsdom`, inclusive ferramenta de drift; criar revogação, roteamento ou UI library.

**Never:** Implementar RBAC/ownership, negócio, cadastro, recuperação, refresh, OAuth/2FA, blacklist/Redis ou ArchUnit da 1.4. Não criar/popular/restaurar/consumir intenção pré-login; apenas reservar e remover `edt.purchase-intent.v1` no logout, nunca `sessionStorage.clear()`. Não alterar migrations/seeds da 1.1 nem inventar Design System.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Bootstrap | Sem cookie ou JWT inválido | `GET /session`: `200`, anônimo, sem JWT e com CSRF | Não revelar causa |
| Login válido | CSRF + conta seedada | `200`, usuário `{id,email,role}` e cookies renovados | Nunca expor senha |
| Login inválido | E-mail/senha incorretos | Sem sessão; `401` indistinguível; preservar e-mail | Envelope AD-12 |
| Logout | Sessão presente/ausente + CSRF | `204`; JWT expirado, CSRF novo, SPA anônima; remover só `edt.purchase-intent.v1` | Idempotente |
| CSRF inválido | Login/logout | Nenhuma mutação | `403` seguro |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/epics.md:233` — ACs 1.2; linhas 251–272 limitam a 1.3.
- `_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md:116` — JWT/BCrypt/CSRF; linha 132: OpenAPI/erros.
- `backend/pom.xml:29` e `application*.yaml` — Security/JOSE e configuração auth por ambiente.
- `backend/src/main/java/br/com/elitedevticket/auth/adapters/persistence/{UserEntity,UserRepository}.java` — reutilizar User/Role e adicionar lookup por e-mail, sem migration.
- `frontend/src/{app/App.tsx,features/auth/index.ts}` e `vite.config.ts` — sessão sobre proxy same-origin.

## Tasks & Acceptance

**Execution:**
- [x] `openapi/elite-dev-ticket-v1.yaml` — definir endpoints, DTOs, códigos auth, cookies/CSRF e envelope AD-12 completo, sem catálogo posterior.
- [x] `backend/pom.xml`, `backend/src/main/resources/application*.yaml` — incluir Security/JOSE e propriedades validadas de BCrypt, JWT, CORS/cookies; falhar se segredo obrigatório faltar.
- [x] `backend/src/main/java/br/com/elitedevticket/auth/{domain,application,adapters}` — implementar lookup, BCrypt, JWT com `Clock`, filtro stateless, cookies e rotação CSRF nas camadas existentes.
- [x] `backend/src/main/java/br/com/elitedevticket/auth/http` — adaptar casos de uso ao OpenAPI; manter health público e sem autorização por papel.
- [x] `backend/src/test/java/br/com/elitedevticket/auth` — cobrir por TDD credenciais, BCrypt, JWT, profiles, cookies, CSRF, endpoints e vazamentos; PostgreSQL real na persistência.
- [x] `frontend/package.json`, `frontend/vitest.config.ts` e testes — Vitest/`jsdom` em dev, Testing Library/user-event e `fetch` mockado, sem MSW.
- [x] `frontend/src/app/{api,session}` e `frontend/src/features/auth` — criar cliente same-origin, estado discriminado e UI semântica; no logout remover somente `edt.purchase-intent.v1`, sem criar seu objeto.
- [x] `frontend/src/app/App.tsx` — compor bootstrap, identidade/papel e troca, sem navegação protegida.
- [x] `backend`, `frontend` e configuração de build — automatizar conformidade do OpenAPI com DTOs Java e tipos TypeScript; Ask First se exigir ferramenta não aprovada.

**Acceptance Criteria:**
- Given o OpenAPI versionado, when os builds executam, then check automatizado detecta drift de DTOs Java/tipos TypeScript sem contratos posteriores.
- Given conta válida, when autentica com CSRF, then a SPA mostra identidade/papel e JWT fica só no cookie HttpOnly.
- Given credenciais ou JWT inválidos, when processados, then não há sessão/vazamento e a UI mostra erro genérico associado e focável.
- Given sessão ou anonimato, when logout recebe CSRF, then é idempotente, renova CSRF, remove autenticação e somente a chave `edt.purchase-intent.v1`, permitindo trocar de conta.
- Given `demo`/`prod`, when inicia, then exige segredo >=256 bits e cookies Secure; `local`/`test` não versionam segredo.

## Spec Change Log

## Design Notes

`SessionResponse` usa `authenticated`; `user` só é obrigatório quando `true`. Ausência/expiração vira anônimo. Logout expira cookie e remove a chave reservada, sem criar intenção. A 1.3 expande o OpenAPI sem redefinir endpoints.

## Verification

**Commands:**
- `backend/mvnw test` — esperado: testes novos e regressão 1.1 passam com PostgreSQL real.
- `npm --prefix frontend test -- --run` — esperado: testes Vitest/jsdom de sessão e acessibilidade passam.
- `npm --prefix frontend run build` — esperado: TypeScript estrito e Vite passam sem nova dependência runtime.
- `docker compose config` — esperado: configuração válida sem segredo JWT versionado.

## Suggested Review Order

**Contrato e borda HTTP**

- Comece pelo contrato autoritativo de login, sessão, cookies, CSRF e erros.
  [`elite-dev-ticket-v1.yaml:8`](../../openapi/elite-dev-ticket-v1.yaml#L8)

- Veja como os três endpoints traduzem HTTP para os casos de uso.
  [`AuthController.java:21`](../../backend/src/main/java/br/com/elitedevticket/auth/http/AuthController.java#L21)

- Confira a cadeia stateless, CORS restrito e proteção CSRF.
  [`SecurityConfiguration.java:111`](../../backend/src/main/java/br/com/elitedevticket/auth/adapters/security/SecurityConfiguration.java#L111)

**Segurança da sessão**

- Valide segredos, TTL, BCrypt, cookies e profiles seguros.
  [`AuthProperties.java:15`](../../backend/src/main/java/br/com/elitedevticket/auth/adapters/security/AuthProperties.java#L15)

- Revise emissão HS256, claims mínimas e expiração determinística.
  [`JwtService.java:18`](../../backend/src/main/java/br/com/elitedevticket/auth/adapters/security/JwtService.java#L18)

- Acompanhe autenticação stateless e descarte seguro de JWT inválido.
  [`JwtAuthenticationFilter.java:18`](../../backend/src/main/java/br/com/elitedevticket/auth/adapters/security/JwtAuthenticationFilter.java#L18)

- Confirme alinhamento entre expiração do JWT e cookie HttpOnly.
  [`SessionCookieService.java:11`](../../backend/src/main/java/br/com/elitedevticket/auth/adapters/security/SessionCookieService.java#L11)

**Fluxo da SPA**

- Examine o cliente same-origin e a fronteira runtime do contrato.
  [`authApi.ts:61`](../../frontend/src/app/api/authApi.ts#L61)

- Siga as transições discriminadas de bootstrap, login e logout.
  [`useSession.ts:32`](../../frontend/src/app/session/useSession.ts#L32)

- Veja a composição acessível dos estados de sessão.
  [`App.tsx:4`](../../frontend/src/app/App.tsx#L4)

**Provas e proteção contra drift**

- Audite operações e DTOs Java contra o OpenAPI.
  [`OpenApiContractTest.java:38`](../../backend/src/test/java/br/com/elitedevticket/auth/OpenApiContractTest.java#L38)

- Confira matriz HTTP, PostgreSQL real, CORS, cookies e vazamentos.
  [`AuthEndpointsIntegrationTest.java:26`](../../backend/src/test/java/br/com/elitedevticket/auth/AuthEndpointsIntegrationTest.java#L26)

- Feche pela experiência observável de login, erro, logout e troca.
  [`App.test.tsx:13`](../../frontend/src/app/App.test.tsx#L13)
