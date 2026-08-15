---
title: 'Story 1.3 — Aplicar RBAC e contratos HTTP de autenticação'
type: 'feature'
created: '2026-08-15'
status: 'done'
baseline_commit: '7ce35b3bdee9690a204f96b92ec95c0e72dca734'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A sessão autenticada da Story 1.2 ainda não transforma o papel do usuário em autorização aplicada pelo backend; qualquer rota futura poderia aceitar acesso incompatível por padrão.

**Approach:** Disponibilizar RBAC real sobre Spring Security, com authorities estáveis derivadas do JWT e respostas 401/403 no envelope AD-12. Provar a matriz mínima por uma fixture exclusivamente de teste, sem criar endpoint de produto.

## Boundaries & Constraints

**Always:** Mapear `CUSTOMER`, `ORGANIZER` e `GATE` para `ROLE_CUSTOMER`, `ROLE_ORGANIZER` e `ROLE_GATE` ao instalar a autenticação do JWT. Preservar públicos health, `GET /api/v1/auth/session`, login e logout, JWT/cookies/CSRF da 1.2 e a ausência de rota de negócio. Habilitar mecanismo reutilizável de proteção por papel no backend e responder ausência de autenticação com 401 e papel incompatível com 403, sempre no envelope `{code,message,fieldErrors?,traceId,timestamp}` sem detalhe interno. OpenAPI, enum Java e tipos TypeScript permanecem conformes para todos os códigos auth. A fixture RBAC vive somente em `src/test`, é carregada apenas pelo contexto de teste e não é OpenAPI nem artefato de produção.

**Ask First:** Alterar endpoint, recurso ou comportamento de produto; proteger/implementar Event, Reservation, Ticket, Gate ou ownership; adicionar dependência; trocar o mecanismo de sessão/CSRF/JWT; criar código de erro fora de autenticação/autorização.

**Never:** Criar controller técnico em `src/main`, nova rota OpenAPI, UI/rota visual, role management, cadastro, OAuth/refresh/revogação, seeds/migrations ou testes arquiteturais da Story 1.4.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Sem sessão | Fixture protegida por papel sem `EDT_SESSION` válido | Nenhum handler protegido é executado | `401` e envelope `AUTH_UNAUTHENTICATED` seguro |
| Papel incompatível | JWT `CUSTOMER` em fixture que exige `ORGANIZER` | Nenhum handler protegido é executado | `403` e envelope `AUTH_FORBIDDEN` seguro |
| Papel compatível | JWT `ORGANIZER` na mesma fixture | Handler de teste responde sucesso | Sem expor JWT/authority interna |
| Endpoints 1.2 | Sessão, login, logout e health existentes | Permanecem acessíveis conforme contrato atual | CSRF inválido continua `403 AUTH_CSRF_INVALID` |

</frozen-after-approval>

## Code Map

- `backend/src/main/java/br/com/elitedevticket/auth/adapters/security/JwtAuthenticationFilter.java` — instala `SessionUser` com authorities vazias; ponto de derivação de `ROLE_*`.
- `backend/src/main/java/br/com/elitedevticket/auth/adapters/security/SecurityConfiguration.java` — cadeia stateless, públicos da 1.2 e handler CSRF; habilitar proteção reutilizável e ligar handlers 401/403.
- `backend/src/main/java/br/com/elitedevticket/auth/{domain/Role.java,domain/SessionUser.java}` — papéis canônicos e principal que devem continuar sendo a fonte do role.
- `backend/src/main/java/br/com/elitedevticket/auth/{adapters/security/AuthHttpErrorWriter.java,http/AuthErrorCode.java,http/ApiErrorResponse.java}` — reutilizar envelope/redação e ampliar somente códigos de autenticação/autorização.
- `openapi/elite-dev-ticket-v1.yaml` — não adicionar path; declarar respostas/componentes 401 e 403 e enum de códigos compatíveis.
- `backend/src/test/java/br/com/elitedevticket/auth/{AuthEndpointsIntegrationTest.java,OpenApiContractTest.java}` — testes HTTP com PostgreSQL e prova de drift; alojar/importar fixture de teste sem fonte principal.
- `frontend/src/app/api/authApi.ts` e `frontend/scripts/check-openapi-contract.mjs` — enum/type guard e check de schemas alcançáveis; sem mudança visual.

## Tasks & Acceptance

**Execution:**

- [x] `backend/src/main/java/br/com/elitedevticket/auth/adapters/security/{JwtAuthenticationFilter,SecurityConfiguration}.java` — derivar `ROLE_*`, habilitar proteção por papel reutilizável e preservar explicitamente os públicos da 1.2.
- [x] `backend/src/main/java/br/com/elitedevticket/auth/adapters/security/AuthHttpErrorWriter.java` e `backend/src/main/java/br/com/elitedevticket/auth/http/AuthErrorCode.java` — escrever 401 e 403 seguros no envelope existente, mantendo CSRF distinto.
- [x] `openapi/elite-dev-ticket-v1.yaml`, `backend/src/test/java/br/com/elitedevticket/auth/OpenApiContractTest.java`, `frontend/src/app/api/authApi.ts` e `frontend/scripts/check-openapi-contract.mjs` — alinhar os códigos/respostas auth, sem paths novos.
- [x] `backend/src/test/java/br/com/elitedevticket/auth/AuthEndpointsIntegrationTest.java` e fixture/configuração adjacente somente em teste — iniciar em vermelho e provar 401, 403, sucesso compatível e ausência de execução; fixture não pode alcançar `src/main`.
- [x] `frontend/src/app/api/authApi.test.ts` — cobrir parse seguro dos novos envelopes; não alterar a composição visual da SPA.

**Acceptance Criteria:**

- Given um JWT válido de cada papel, when o filtro autentica a requisição, then a autoridade Spring é exatamente o `ROLE_*` correspondente ao role do token.
- Given a superfície protegida exclusivamente de teste, when a requisição não tem sessão ou tem papel incompatível, then recebe respectivamente 401 ou 403 no envelope estável e o handler não produz efeito.
- Given `ROLE_ORGANIZER` na fixture que o exige, when a requisição é processada, then a resposta é permitida; a fixture não existe no artefato de produção nem no OpenAPI.
- Given os endpoints da Story 1.2, when os checks e testes executam, then continuam públicos conforme antes e OpenAPI, DTOs Java e tipos TypeScript detectam drift dos códigos auth.

## Spec Change Log

## Design Notes

Method security é o mecanismo escolhido porque preserva a cadeia e os paths públicos atuais, mas permite às futuras rotas declarar a exigência de papel na própria borda HTTP. A fixture usa a mesma proteção sem se tornar contrato, recurso ou controller de produção.

## Verification

**Commands:**

- `backend/mvnw test` — esperado: testes de API com PostgreSQL comprovam RBAC, erro seguro e regressão auth.
- `npm --prefix frontend test -- --run` — esperado: parse dos novos envelopes e regressão de sessão passam.
- `npm --prefix frontend run build` — esperado: TypeScript estrito e check OpenAPI passam sem mudança visual.
