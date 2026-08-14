---
title: 'Story 1.1 — Disponibilizar a fundação executável e contas provisionadas'
type: 'feature'
created: '2026-08-13'
status: 'done'
baseline_commit: 'NO_VCS'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O repositório não possui ainda uma base executável para o MVP. Sem frontend React, API Spring Boot, PostgreSQL, schema controlado por Flyway e identidades provisionadas, não é possível iniciar os fluxos de autenticação e as capacidades subsequentes com ambiente reproduzível.

**Approach:** Criar o cold-start full-stack aprovado, organizado como monólito modular, com Docker Compose e perfis de ambiente. A primeira migration deve criar somente o modelo mínimo de `User` e semear, fora de `prod`, as contas necessárias; a autenticação e as rotas protegidas pertencem às Stories 1.2 e 1.3.

## Boundaries & Constraints

**Always:** Usar Java 21, Spring Boot 4.0.7, React 19.2.x, TypeScript estrito, Vite 7.3.x, Node 22.12+ LTS, PostgreSQL 17.x, Flyway e Docker Compose conforme Architecture Spine. Organizar backend por capacidades, iniciando `shared` e `auth`; manter a estrutura aprovada para os demais módulos sem implementar suas regras. Flyway é o único dono de schema e seeds; Hibernate usa `ddl-auto=validate`. A migration desta Story cria apenas `User`, incluindo papel e credencial BCrypt. Profiles `local`, `test`, `demo` e `prod` preservam o contrato: `local`, `test` e `demo` recebem exatamente um `ORGANIZER`, dois `CUSTOMER` e um `GATE`; são credenciais de demonstração intencionalmente conhecidas e documentáveis no README para avaliação. A proibição de versionamento cobre segredos e credenciais reais de produção; `prod` não contém seeds, contas demo nem segredos. A SPA e `/api` devem operar sob mesma origem lógica. Todo código e configuração devem evitar exposição ou log de senhas, JWTs e segredos.

**Ask First:** Interromper para decisão humana se o cold-start exigir dependência, versão de patch, ferramenta de geração OpenAPI, mecanismo de proxy ou padrão de segurança não autorizado pela Architecture.

**Never:** Não implementar login, logout, JWT, CSRF, RBAC, endpoints de negócio, cadastro público, administração de papéis, Event/TicketSector, OpenAPI de autenticação ou Docker image de produção. Não versionar segredos ou credenciais reais de produção. Credenciais demo aprovadas para `local`/`test`/`demo` são uma exceção intencional e documentável. Não usar callbacks, placeholders condicionais, código Java de seed ou `outOfOrder` para os seeds Flyway. Não usar H2 como banco substituto de PostgreSQL nem introduzir Redis, Kafka, microsserviços, biblioteca de UI, cache ou outro serviço.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|---------------------------|----------------|
| Inicialização local | Perfil `local` com variáveis não sensíveis e Docker Compose | Frontend, API e PostgreSQL sobem; Flyway aplica schema e seeds mínimos de `User`; Hibernate valida o schema | Falha de inicialização deve apontar configuração ausente sem revelar segredo |
| Inicialização produtiva | Perfil `prod` sem seed/demo credential | Aplicação não inclui contas demo e exige somente as configurações externas das capacidades já implementadas | Falha cedo, com mensagem operacional segura |
| Seed de conta | Perfis `local`, `test` ou `demo` incluem `classpath:db/migration` e `classpath:db/seed/demo`; `prod` inclui somente a primeira location | Há exatamente um `ORGANIZER`, dois `CUSTOMER` e um `GATE`, com hashes BCrypt e credenciais demo documentáveis; `prod` não recebe conta demo | Senhas não são registradas nem retornadas por runtime |
| Saúde operacional | API inicializada e PostgreSQL disponível ou indisponível | Liveness/readiness retornam somente estado operacional mínimo; readiness considera banco e migrations | Não expõe exceção, SQL, configuração, segredo ou detalhe interno |

</frozen-after-approval>

## Code Map

- `_bmad-output/planning-artifacts/epics.md:215-231` — escopo e critérios de aceitação autoritativos da Story 1.1.
- `_bmad-output/implementation-artifacts/epic-1-context.md` — contexto consolidado: Story 1.1 precede 1.2–1.4; limita o schema inicial a `User`.
- `_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md:62-66` — fronteiras do monólito modular.
- `_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md:176-186` — perfis, execução, Flyway, PostgreSQL, testes e Docker Compose.
- `_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md:214-248` — versões aprovadas e árvore estrutural inicial.
- `docs/engineering/java-standards.md` — regras de Spring, JPA, Flyway e testes backend.
- `docs/engineering/react-standards.md` — TypeScript estrito e organização da SPA.
- `AGENTS.md` — hierarquia de fontes e relatório obrigatório da Story.
- Raiz do repositório — não há `backend/`, `frontend/`, `pom.xml`, `package.json`, Docker Compose, lockfile ou OpenAPI existente; estes arquivos serão criados nesta Story somente quando autorizados pelos limites acima.

## Tasks & Acceptance

**Execution:**

- [x] `backend/pom.xml`, Maven Wrapper, `backend/src/main/java/.../shared`, `backend/src/main/java/.../auth` e `backend/src/main/resources/application*.yaml` — inicializar a API Spring Boot 4.0.7 com Java 21, perfil/configuração externa, Actuator mínimo com liveness/readiness operacionalmente minimalistas e JPA/Flyway configurados para `ddl-auto=validate`; criar somente o modelo/persistência de User necessário aos seeds.
- [x] `backend/src/main/resources/db/migration/V1__create_users.sql`, `backend/src/main/resources/db/seed/demo/V2__seed_demo_users.sql` e configuração Flyway por profile — usar `classpath:db/migration` para schema universal; adicionar `classpath:db/seed/demo` somente em `local`, `test` e `demo`. O seed contém um Organizer, dois Customers e um Gate com hashes BCrypt; `prod` usa somente migrations universais. A pasta de seed não fica sob `db/migration` e `outOfOrder` permanece desabilitado.
- [x] `backend/src/test/...` — antes da implementação de configuração/persistência crítica, criar testes que falham ao verificar migration comum, schema validável, contas e papéis em `local`/`test`/`demo`, e ausência de conta demo em `prod`, usando PostgreSQL real; tornar verde e refatorar sem alterar comportamento.
- [x] `frontend/package.json`, lockfile do frontend, `frontend/.nvmrc` ou registro equivalente da faixa Node aprovada, `frontend/tsconfig*.json`, `frontend/vite.config.ts`, `frontend/src/app`, `frontend/src/features/auth`, `frontend/src/shared` e `frontend/src/main.tsx` — inicializar React/Vite com TypeScript estrito, estrutura aprovada, uma superfície inicial sem autenticação e encaminhamento de `/api` no desenvolvimento; não criar UI de login.
- [x] `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile` e `.env.example` — subir PostgreSQL, API e SPA na mesma origem lógica prevista, usando apenas variáveis de exemplo sem valores secretos.
- [x] `README.md` — documentar pré-requisitos, Node 22.12+ LTS, execução reprodutível por Maven Wrapper e lockfile, perfis, variáveis não secretas e credenciais de demonstração conhecidas somente para os ambientes permitidos.
- [x] `_bmad-output/implementation-artifacts/sprint-status.yaml` — marcar exclusivamente a Story 1.1 como `in-progress` no início e `review` após as verificações; marcar Epic 1 como `in-progress`, sem modificar qualquer outra Story.

**Acceptance Criteria:**

- Given um checkout limpo, when o ambiente local aprovado é iniciado, then frontend React, API Spring Boot e PostgreSQL ficam executáveis com a estrutura modular do Architecture Spine.
- Given a aplicação conectada ao PostgreSQL, when Flyway aplica migrations, then ele é o único dono do schema, Hibernate executa com `ddl-auto=validate` e o schema inicial contém somente o necessário para `User`.
- Given os perfis `local`, `test` ou `demo`, when Flyway usa as locations comum e de seed demo, then há um `ORGANIZER`, dois `CUSTOMER` e um `GATE` com senha protegida por BCrypt e credenciais demo documentáveis.
- Given o perfil `prod`, when é iniciado, then não existem credenciais demo nem segredos versionados e ele exige somente configuração externa das capacidades já implementadas; ausência de configuração obrigatória falha de modo seguro.
- Given a API em execução, when liveness ou readiness é consultado, then ela devolve apenas estado operacional mínimo; readiness considera banco e migrations, sem incluir detalhes internos ou Ticketmaster.
- Given a base inicial, when os testes e builds aplicáveis são executados pelos comandos reproduzíveis do Maven Wrapper e do lockfile frontend, then migrações, configuração e tipos estritos passam sem introduzir autenticação ou qualquer capacidade de Story posterior.

## Spec Change Log

## Design Notes

A primeira migration contém apenas usuários. Event, TicketSector, disponibilidade e demais dados de domínio serão introduzidos incrementalmente pelas Stories que possuem essas capacidades, sem antecipar tabelas futuras. O conjunto final mínimo de identidades já é estabelecido nesta Story — um Organizer, dois Customers e um Gate —. Flyway usa `classpath:db/migration` em todos os profiles e adiciona `classpath:db/seed/demo` somente em `local`, `test` e `demo`; a location de seed é irmã, não descendente, de `db/migration`.

Testcontainers é congelado em 2.0.5. Os módulos usam uma única versão, fornecida pelo dependency management do Spring Boot quando disponível ou pelo import único do `testcontainers-bom` em 2.0.5, sem versões independentes por artefato.

## Verification

**Commands:**

- `docker compose config` — esperado: configuração válida, sem segredos concretos.
- `backend/mvnw test` — esperado: testes de migration/seeds, liveness/readiness e build backend passam com PostgreSQL real configurado.
- `npm --prefix frontend ci && npm --prefix frontend run build` — esperado: instalação a partir do lockfile, TypeScript estrito e build Vite passam em Node 22.12+ LTS.
- `docker compose up --build` — esperado: frontend, API e PostgreSQL iniciam; Flyway conclui antes de Hibernate validar.
