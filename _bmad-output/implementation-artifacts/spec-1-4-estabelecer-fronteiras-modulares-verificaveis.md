---
title: 'Story 1.4 — Estabelecer fronteiras modulares verificáveis'
type: 'feature'
created: '2026-08-15'
status: 'done'
baseline_commit: 'ce14ab1524a78cd4f5e24fa120b4b356a3149792'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Sem testes arquiteturais automatizados no build, futuras Stories e capacidades poderiam introduzir acoplamentos proibidos (como Controllers chamando Repositories diretamente, entidades JPA vazando em contratos HTTP ou acesso direto a adapters de outros módulos), além de testes com dependência temporal frágil (`Thread.sleep`, relógio não injetável).

**Approach:** Estabelecer suíte de testes arquiteturais automatizados com ArchUnit no backend, protegendo as fronteiras modulares do monólito modular pragmático (AD-1, AD-21), isolamento de persistence/JPA adapters, proibição de injeção de campos, ausência de classes cerimoniais `*Impl` ou genéricas `*Helper`/`*Util`, desacoplamento de camadas e determinismo temporal nos testes e código de produção.

## Boundaries & Constraints

**Always:** Usar ArchUnit 1.4.2 gerenciado com scope de teste. Garantir que as regras impeçam:
- Controller → Repository direto;
- Entidade JPA em DTO/camada HTTP;
- Entidades JPA fora de `adapters.persistence`;
- Acesso intermodular a packages `adapters` de outros módulos;
- `shared` dependendo de módulos de feature;
- Classes cerimoniais `*Impl` ou nomes genéricos `*Helper`/`*Util`/`*Manager`;
- Field injection (`@Autowired` em fields);
- Application/domain dependendo de camada HTTP;
- Chamadas a tempo não determinístico (`Instant.now()`, `System.currentTimeMillis()`) no código de produção;
- `Thread.sleep` em testes.

**Ask First:** Adicionar nova dependência de produção, alterar estrutura de módulos aprovada ou modificar regras de negócio das Stories 1.1–1.3.

**Never:** Avançar para Epic 2, quebrar contratos e testes existentes das Stories 1.1, 1.2 e 1.3, introduzir bibliotecas não aprovadas ou fazer commit/push.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Execução do build / testes arquiteturais | Execução de `mvn test` | Todas as regras ArchUnit são avaliadas e passam | Violação arquitetural causa falha explícita no build |
| Tentativa de Controller acessar Repository | Controller declara dependência direta a `@Repository` ou persistence adapter | ArchUnit detecta dependência proibida e falha | Falha com mensagem explicativa citando AD-1/AD-21 |
| Tentativa de vazar JPA Entity em HTTP | DTO ou Controller depende de classe `@Entity` | ArchUnit detecta acoplamento proibido e falha | Falha com mensagem explicativa citando AD-12/AD-21 |
| Módulo não implementado | Avaliação de regra de isolamento de adapters em módulo futuro sem classes ainda | `allowEmptyShould(true)` permite avaliação segura sem falso-positivo | Avaliação bem-sucedida para módulos futuros |
| Determinismo temporal | Testes e produção usam `Clock` injetado | Verificação impede `Instant.now()`/`System.currentTimeMillis()` e `Thread.sleep` | Falha imediata na detecção |

</frozen-after-approval>

## Code Map

- `backend/pom.xml` — adicionada dependência `com.tngtech.archunit:archunit-junit5:1.4.2` com `scope=test`.
- `backend/src/test/java/br/com/elitedevticket/architecture/ArchitectureBoundariesTest.java` — suíte de testes arquiteturais executáveis cobrindo todas as invariantes e regras das fronteiras modulares.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — rastreamento do status da Story 1.4 como review.

## Tasks & Acceptance

**Execution:**
- [x] Adicionar `archunit-junit5` (versão 1.4.2 aprovada) em `backend/pom.xml` exclusivamente com escopo de teste.
- [x] Implementar `ArchitectureBoundariesTest.java` verificando:
  - Isolamento Controller → Application → Adapters (proibição de Controller → Repository);
  - Isolamento HTTP → JPA Entities;
  - Localização restrita de entidades JPA em `adapters.persistence`;
  - Isolamento modular de pacotes internos (`adapters`) entre capacidades;
  - Independência do módulo `shared` em relação aos módulos de feature;
  - Proibição de classes cerimoniais `*Impl` e nomes genéricos `*Helper`/`*Util`/`*Manager`;
  - Proibição de injeção de dependência via atributos (`@Autowired` em fields);
  - Desacoplamento das camadas `application` e `domain` em relação à camada `http`;
  - Determinismo temporal (uso de `Clock` injetável, proibição de chamadas sem relógio);
  - Proibição de `Thread.sleep` na suíte de testes.
- [x] Executar a suíte completa de testes no backend e frontend garantindo regressão zero.

**Acceptance Criteria:**
- Given os módulos iniciais `auth` e `shared`, when o build é executado, then testes arquiteturais impedem controller→repository direto, entidade JPA em DTO HTTP e acesso a package interno de outro módulo, and ports são usados somente nas fronteiras reais, sem interface cerimonial por classe.
- Given regras críticas de autenticação, when testes são criados ou alterados, then `Clock`, UUIDs e geradores relevantes são determinísticos/injetáveis quando necessário, and nenhum teste depende de `Thread.sleep`, ordem compartilhada ou dados fora do seu contexto.

## Verification

**Commands:**
- `backend/mvnw test` — esperado: 60 testes executados (50 testes de integração e unitários + 10 verificações arquiteturais ArchUnit), 0 falhas.
- `npm --prefix frontend test` — esperado: 15 testes passando, 0 falhas.
- `npm --prefix frontend run build` — esperado: TypeScript e OpenAPI drift check passando.
