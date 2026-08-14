---
title: EliteDevTicket — Java Engineering Standards v1.0
status: approved-for-use
scope: backend
project: EliteDevTicket
---

# EliteDevTicket — Java Engineering Standards v1.0

## 0. Autoridade e escopo

Estas regras orientam implementação Java no EliteDevTicket.

Elas não sobrescrevem produto, Domain ou Architecture.

Ordem de autoridade:

1. Desafio oficial;
2. PRD / Project Specification / Domain aprovados;
3. `ARCHITECTURE-SPINE.md` e ADRs;
4. OpenAPI aprovado para contrato HTTP;
5. estas Engineering Standards;
6. convenções locais.

Se uma regra deste arquivo conflitar com fonte superior, a fonte superior vence.

Não introduza biblioteca, padrão ou separação estrutural porque este documento “prefere”. A Architecture decide dependências, módulos e mecanismos.

## 1. Protocolo de execução

### MUST

- Java 21 como baseline.
- Testes acompanham a implementação quando a Story exige teste.
- Regras críticas seguem TDD.
- Não deixar `TODO`, `FIXME`, código morto ou logs de debug na entrega.
- Não inventar requisito em caso de ambiguidade.
- Ao terminar a Story, reportar arquivos alterados, testes executados, resultados, decisões assumidas, riscos e pendências.

### Comentários

Não comentar o que o código já diz. Comentários são permitidos para explicar workaround externo, restrição de biblioteca/protocolo, decisão de segurança não evidente, decisão de concorrência contraintuitiva ou referência a comportamento externo relevante.

## 2. Fluxo de controle e métodos

### MUST

- Preferir guard clauses quando reduzem aninhamento.
- Não manter `else` desnecessário após `return`/`throw`.
- Não esconder expressão de negócio complexa em condição ilegível.
- Métodos devem possuir responsabilidade clara e um nível de abstração coerente.
- Não usar parâmetro booleano quando ele representa dois comportamentos semanticamente diferentes.
- Não mutar argumentos de entrada sem contrato explícito.

### SHOULD

Os números abaixo são heurísticas de revisão, não limites absolutos:

- método acima de ~20 linhas;
- mais de ~3 parâmetros;
- aninhamento acima de 1–2 níveis;
- complexidade ciclomática elevada.

Ao ultrapassar, reavaliar clareza e coesão. Não extrair micro-métodos ou criar `record` artificial apenas para satisfazer contagem.

Polimorfismo, strategy ou `switch` exaustivo são usados quando realmente simplificam variações. Não substituir todo `if` por uma hierarquia de classes.

## 3. Classes e design

### MUST

- Responsabilidade clara.
- Preferir composição a herança concreta.
- Constructor injection.
- Dependências obrigatórias como `final`.
- Sem field injection.
- Regra de negócio não vive em Controller.
- Não usar nomes genéricos como `Util`, `Helper`, `Manager` ou `Impl` quando houver nome de responsabilidade mais claro.

### SHOULD

- Interfaces pequenas e orientadas ao consumidor.
- Criar interface/port apenas quando existe fronteira real, integração, desacoplamento arquitetural ou ganho de teste.
- Evitar abstração cerimonial.
- Classes grandes devem ser revisadas, mas quantidade de campos não define SRP por si só.

Uma entidade de domínio pode legitimamente possuir muitos atributos.

## 4. Domain, persistência e DTOs

### MUST

- Entidades JPA nunca são expostas diretamente pelo contrato HTTP.
- DTOs de entrada/saída são tipos próprios.
- Controllers traduzem HTTP para casos de uso/aplicação.
- Transações seguem as fronteiras definidas no `ARCHITECTURE-SPINE.md`.
- Migrations via Flyway.
- `ddl-auto=validate`.
- `BigDecimal` para dinheiro.
- Nenhum `double`/`float` para valor monetário.
- Backend é autoridade temporal.
- `Clock` deve ser injetável/testável nas regras que dependem de tempo.

### Importante

A separação entre entidade de domínio e entidade JPA não é obrigatória por princípio.

Seguir a decisão do `ARCHITECTURE-SPINE.md`.

Não duplicar `Reservation`/`ReservationJpaEntity`/mapper apenas para obedecer uma preferência genérica. Separar quando houver benefício arquitetural explícito.

### SHOULD

- `record` para DTOs, comandos e value objects imutáveis quando fizer sentido.
- associações JPA LAZY por padrão, ajustadas conscientemente.
- evitar `CascadeType.ALL` indiscriminado.
- índices explícitos para consultas críticas identificadas pela Architecture.

## 5. Tipos, nulls e imutabilidade

### MUST

- Não retornar `null` quando há alternativa semântica melhor.
- Coleções retornam vazias em vez de `null`.
- `Optional` preferencialmente em retorno, não como campo de entidade.
- Não usar `Optional.get()`.
- Objetos imutáveis por padrão quando o modelo permitir.
- Não usar estado estático mutável.
- Validação de input HTTP na borda e invariantes reforçadas no domínio/aplicação.
- UUIDs, relógio e geradores de token/ID usados em regras críticas devem ser testáveis/injetáveis quando determinismo for necessário.

### SHOULD

- Evitar primitive obsession quando um value object realmente carrega invariantes.
- Não criar value object para toda `String` sem benefício real.

## 6. Exceções e erros

### MUST

- Não usar `catch (Throwable)`.
- Evitar `catch (Exception)` genérico sem motivo técnico forte.
- Não engolir exceção.
- Ao encapsular, preservar a causa.
- Não logar e relançar a mesma exceção no mesmo nível.
- Erros HTTP seguem o catálogo/contrato OpenAPI aprovado.
- Cliente nunca recebe stack trace, SQL, segredo ou detalhe interno.
- Mensagens de log não incluem credenciais sensíveis.

Exceções de domínio/aplicação devem refletir significado de negócio quando isso melhorar o mapeamento de erro.

## 7. TDD

### MUST para regras críticas

Ciclo:

1. Red — teste falha pelo motivo correto.
2. Green — implementação mínima.
3. Refactor — melhorar mantendo verde.

Aplicar prioritariamente a:

- hold de 10 minutos;
- expiração;
- overselling;
- locking;
- idempotência;
- ownership/RBAC;
- pagamento;
- corrida pagamento × expiração;
- emissão exata;
- Gate validation;
- replay de attempts;
- WRONG_EVENT sem consumo;
- double-use;
- invariantes de capacidade/disponibilidade.

Não é necessário TDD cerimonial para DTO trivial, getter, configuração mecânica ou wiring sem comportamento.

### Ferramentas

Seguir as versões aprovadas pela Architecture.

Baseline esperado:

- JUnit 5;
- AssertJ;
- Mockito quando necessário;
- Spring Boot Test para integração;
- PostgreSQL real para concorrência;
- Testcontainers preferido quando aprovado/disponível.

### MUST

- domínio testado sem Spring quando possível;
- tempo controlado com `Clock`;
- sem `Thread.sleep`;
- testes independentes;
- sem ordem compartilhada;
- bug corrigido começa com teste que reproduz o bug;
- mocks apenas em fronteiras onde o mock é útil;
- tipos de terceiro são encapsulados por adapters antes de mockar;
- concorrência, constraints e locking não usam H2 como evidência.

### SHOULD

- fake pode ser melhor que mock em certas portas;
- `@ParameterizedTest` para variações de dados;
- Test Data Builder quando reduzir ruído;
- uma asserção lógica por teste, sem fragmentar comportamento coerente artificialmente.

### COULD

- PIT/mutation testing em regras críticas se houver tempo.

Mutation testing não faz parte do DoD obrigatório do MVP.

## 8. Arquitetura modular

### MUST

- organização principal por capacidade/feature;
- respeitar fronteiras do modular monolith;
- módulo não acessa package interno de outro módulo;
- Controllers não acessam Repositories diretamente;
- contratos públicos não expõem entidades JPA;
- dependências intermodulares passam pelas fronteiras aprovadas;
- ArchUnit ou verificação equivalente protege as regras arquiteturais aprovadas.

Estrutura conceitual:

```text
auth/
catalog/
events/
reservations/
payments/
tickets/
gate/
```

A organização interna exata segue o Architecture Spine.

Ports and Adapters são leves: criar ports onde existe fronteira real, não uma interface por classe.

## 9. Persistência e concorrência

### MUST

- PostgreSQL é a fonte de verdade.
- Flyway é o dono do schema.
- SQL parametrizado.
- Operações críticas seguem locking/transações do Architecture Spine.
- Toda mutação de capacidade/disponibilidade respeita as invariantes aprovadas.
- Lock order aprovado deve ser preservado.
- Se múltiplos `TicketSector` forem bloqueados na mesma transação, usar ordenação determinística definida na Architecture.
- Expiração semanticamente ocorre quando `serverNow >= expiresAt`; scheduler é cleanup.
- Holds vencidos não podem continuar autorizando pagamento nem bloquear nova intenção válida.
- Payment approval, confirmation e ticket issuance seguem a unidade transacional aprovada.
- Gate `VALID → USED` e persistência do `ValidationAttempt` seguem atomicidade aprovada.
- `WRONG_EVENT` nunca consome Ticket.

Não criar mecanismo de concorrência alternativo sem ADR/aprovação.

## 10. Segurança

### MUST

- nenhum segredo versionado;
- segredo JWT externo conforme Architecture;
- senha com BCrypt conforme configuração aprovada;
- nunca logar senha, JWT, Authorization, cookies sensíveis, QR payload, `validationToken`, `manualCode` completo ou `shareToken`;
- identificadores sensíveis gerados por CSPRNG conforme Architecture;
- manual code segue formato e normalização aprovados;
- fingerprint de attempts não persiste credencial original;
- ownership e RBAC aplicados no backend;
- erro para cliente não revela internals.

Rate limiting permanece COULD/hardening enquanto não for promovido por decisão superior.

## 11. Integrações

### MUST

- Ticketmaster acessada somente via adapter/porta aprovada;
- API key somente por variável de ambiente;
- timeouts explícitos;
- retry apenas conforme política aprovada;
- payload externo mapeado para snapshot interno; não propagar objeto bruto ao frontend;
- FakePaymentGateway segue contrato aprovado;
- nenhuma integração nova sem decisão arquitetural.

Virtual threads, circuit breaker, cache, Redis, Kafka ou outras infraestruturas não entram automaticamente.

## 12. Linguagem Java

### SHOULD

- `record` para dados imutáveis apropriados;
- `switch` expression quando melhora clareza;
- sealed types apenas para hierarquias fechadas reais;
- `var` somente quando o tipo permanece óbvio;
- try-with-resources para recursos fecháveis.

### MUST

- nada de Lombok `@Data` em entidade;
- não introduzir Lombok se a Architecture não aprovou;
- se Lombok já estiver aprovado, usar apenas onde reduz boilerplate sem esconder invariantes.

Recursos modernos de Java 21 são ferramentas, não obrigação estética.

## 13. Performance

### MUST

- não criar N+1 conhecido em fluxo crítico;
- não usar stream paralelo sem medição;
- não usar efeito colateral obscuro em streams;
- não otimizar antes de identificar necessidade.

### SHOULD

- streams curtos quando aumentam legibilidade;
- loop explícito quando fica mais claro;
- batch/paginação quando o volume exigir.

Virtual threads são decisão de Architecture/performance, não regra automática deste documento.

## 14. Regras específicas do EliteDevTicket

### MUST

- `expiresAt = serverNow + 10 minutos` sem pausa, extensão ou reinício.
- Reservation expirada nunca confirma.
- Payment DECLINED não libera hold vigente.
- Payment APPROVED confirma uma única vez.
- emissão total = `reservation.quantity`.
- `availableQuantity` nunca fica negativa.
- alteração de capacidade preserva quantidade comprometida.
- retry de Reservation não cria novo hold.
- PaymentAttempt e ValidationAttempt obedecem idempotência/fingerprint aprovados.
- Gate replay da mesma tentativa retorna o resultado persistido.
- Ticket `USED` nunca volta a `VALID`.
- `WRONG_EVENT` não muta Ticket.
- `shareToken` e `validationToken` têm responsabilidades distintas.
- `LOW_AVAILABILITY` não existe no contrato atual.
- catálogo não possui cache persistente no MVP.
- scheduler não é autoridade temporal.

## 15. MUST / SHOULD / COULD

### MUST

- respeitar fonte superior;
- TDD em regras críticas;
- constructor injection;
- controllers sem regra;
- DTOs próprios;
- JPA não exposta em HTTP;
- Clock testável;
- BigDecimal;
- PostgreSQL real para testes de concorrência;
- Flyway + validate;
- package-by-feature;
- fronteiras modulares verificadas;
- segurança/redaction;
- transações e lock order aprovados.

### SHOULD

- guard clauses;
- métodos curtos e coesos;
- interfaces pequenas;
- composição;
- records quando adequados;
- domínio testado sem Spring;
- Test Data Builder quando útil.

### COULD

- PIT;
- virtual threads;
- otimizações avançadas;
- abstrações adicionais justificadas;
- hardening extra.

Itens COULD não entram numa Story sem necessidade ou aprovação.

## 16. Checklist de entrega

- [ ] fonte superior respeitada;
- [ ] teste crítico escrito antes da produção;
- [ ] testes relevantes executados;
- [ ] nenhum segredo/log sensível;
- [ ] Controllers sem regra de negócio;
- [ ] nenhum acesso intermodular proibido;
- [ ] nenhum DTO público expõe entidade JPA;
- [ ] tempo usa autoridade/injeção aprovada;
- [ ] dinheiro usa `BigDecimal`;
- [ ] lock order e transações preservados;
- [ ] concorrência crítica validada com PostgreSQL real;
- [ ] migrations válidas;
- [ ] nenhuma dependência/padrão novo sem aprovação;
- [ ] relatório final da Story produzido.
