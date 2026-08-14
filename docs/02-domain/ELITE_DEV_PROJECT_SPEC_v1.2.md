# Elite Dev — Project Specification v1.2

> Documento de contexto para uso no VS Code / Codex CLI.
>
> **Status:** arquitetura funcional revisada após wireflow e congelada. Próxima fase: UX + Design System.
> **Projeto:** Plataforma de Eventos e Ingressos.

---

## 0. Regras para o Codex

Este arquivo é a referência principal de produto, domínio e arquitetura.

Ao implementar qualquer tarefa:

1. Leia este documento antes de alterar código.
2. Respeite o escopo MUST / SHOULD / COULD / WON'T.
3. Não introduza novas dependências, serviços, bancos ou padrões arquiteturais sem justificativa e aprovação.
4. Não tome decisões novas de produto, domínio, segurança ou UX por conta própria.
5. Se existir ambiguidade não coberta aqui, pare e reporte: problema, alternativas, trade-offs e arquivos afetados.
6. Para regras críticas de domínio, escreva testes antes da implementação.
7. Mantenha regra de negócio fora de Controllers.
8. Mantenha autorização no backend.
9. Não exponha entidades JPA diretamente pela API.
10. Priorize o fluxo ponta a ponta antes de itens opcionais.

### Regra de parada

```text
If implementation requires an architectural,
domain, security or product decision not covered
by the specifications:

STOP.

Do not choose an approach.

Report:
1. ambiguity
2. alternatives
3. trade-offs
4. affected files

Wait for an architectural decision.
```

---

## 0.1 Hierarquia de fontes e autoridade

Este arquivo não substitui o enunciado oficial da empresa.

Use a seguinte hierarquia quando houver conflito:

```text
1. Desafio-Elite-Dev-2026.pdf
   → fonte externa de requisitos
   → define O QUE a empresa exige

2. Project / Domain Specification MAIS RECENTE aprovada
   → fonte interna de implementação
   → define COMO decidimos resolver o desafio

3. ADRs aprovados
   → decisões técnicas ou de domínio específicas

4. UX Spec / Wireflows / Design System
   → comportamento de interface e experiência

5. Conversas, blueprints e documentos históricos
   → contexto e histórico
   → NÃO sobrescrevem uma spec aprovada
```

Regras:

```text
Challenge requirements > internal decisions
Newer approved spec/ADR > historical document
```

Se este documento conflitar com o desafio oficial, o desafio oficial vence.

---

## 0.2 Changelog — v1.1 → v1.2

Esta revisão surgiu durante o wireflow de compra.

### ADD — Domain

- vendas são fechadas quando `startsAt <= serverNow`;
- evento `PUBLISHED` passa a ter campos estruturais imutáveis no MVP:
  - `title`;
  - `venueName`;
  - `venueAddress`;
  - `startsAt`;
  - `externalSource`;
  - `externalId`.

### ADD — Application/API Reliability

- criação de Reservation deve suportar retries idempotentes;
- `POST` de criação de reserva usará `Idempotency-Key`;
- repetir a mesma intenção por falha de rede/double-click não pode criar múltiplos holds.

### NO CHANGE

- stack;
- entidades centrais;
- modelo por setores;
- Ticketmaster;
- PostgreSQL;
- hold de 10 minutos;
- Payment model;
- Ticket model;
- QR;
- compartilhamento;
- Gate validation;
- RBAC;
- estratégia de concorrência;
- Redis fora do MVP.


---

# 1. Objetivo

Construir uma **Plataforma de Eventos e Ingressos**.

Papéis:

- **ORGANIZER** — cria e gerencia eventos;
- **CUSTOMER** — navega, reserva, paga e recebe ingressos;
- **GATE** — valida ingressos na entrada.

Fluxo principal:

```text
Ticketmaster
    ↓
Organizer creates event
    ↓
Event published
    ↓
Customer browses
    ↓
Selects sector + quantity
    ↓
Reservation HOLDING
    ↓
10-minute checkout
    ↓
Payment approved / declined
    ↓
Ticket issuance
    ↓
My Tickets
    ↓
QR / share link
    ↓
Gate validation
```

Princípio:

> Entregar primeiro um fluxo simples, completo e confiável. Só depois agregar diferenciais.

---

# 2. Stack

## Frontend

- React
- TypeScript
- Vite

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Flyway

## Banco

- PostgreSQL
- PostgreSQL é o **source of truth**.

## Infra

- Docker Compose

## API externa

- Ticketmaster Discovery API

## Pagamento

```text
PaymentGateway
      ▲
      │
FakePaymentGateway
```

## Testes

- JUnit
- Mockito
- Spring Boot Test
- Testcontainers, se o prazo permitir

## Fora do MVP

- Redis
- Kafka
- Microservices
- TMDb
- Mapa de assentos
- Gateway financeiro real
- OAuth
- 2FA
- Recuperação de senha
- App nativo
- Envio de ingresso por e-mail
- Revenda
- Nota fiscal

---

# 3. Arquitetura de alto nível

```text
                         Ticketmaster
                              │
                              ▼
                       ┌──────────────┐
                       │ Spring Boot  │
                       │              │
                       │ Security     │
                       │ Controllers  │
                       │ Services     │
                       │ Repositories │
                       │ Payment GW   │
                       │ Ticketing    │
                       └──────┬───────┘
                              │
                              ▼
                        PostgreSQL
                              ▲
                              │
                       React + TypeScript
                       /       |        \
                Customer   Organizer    Gate
```

Princípios:

- um frontend;
- três experiências distintas;
- backend é a autoridade;
- Controllers tratam HTTP, não regra de negócio;
- Services concentram lógica de aplicação/domínio;
- DTOs definem contratos externos;
- operações críticas são transacionais;
- banco protege integridade com constraints;
- nenhuma tecnologia entra apenas para “parecer sofisticado”.

---

# 4. Definição do produto

A plataforma é de **eventos**, não especificamente de filmes.

Eventos compatíveis com o MVP:

- shows;
- festivais;
- apresentações;
- eventos culturais;
- eventos com pista/setores.

Modelo do MVP:

```text
SETORES + QUANTIDADE
```

Não haverá mapa individual de assentos.

Exemplos:

```text
PISTA
PISTA PREMIUM
CAMAROTE
```

---

# 5. Modelo de venda

```text
Event
  │
  ├── TicketSector
  ├── TicketSector
  └── TicketSector
```

Exemplo:

```text
Rock Festival

PISTA
capacity: 500
price: R$ 120

PISTA PREMIUM
capacity: 150
price: R$ 220

CAMAROTE
capacity: 50
price: R$ 400
```

Regras:

```text
published event must have >= 1 sector
sector.capacity > 0
sector.price >= 0
```

Listagem pública:

```text
totalCapacity = SUM(sector.capacity)
startingPrice = MIN(sector.price)
```

Exibição:

```text
A partir de R$ 120
```

---

# 6. Entidades

## 6.1 User

```text
User
────────────────
id: UUID
name
email
passwordHash
role
createdAt
```

Roles:

```text
CUSTOMER
ORGANIZER
GATE
```

## 6.2 Event

```text
Event
────────────────────────
id: UUID
organizerId: UUID

externalSource
externalId

title
description
imageUrl
category

venueName
venueAddress

startsAt
status

createdAt
updatedAt
```

Não armazenar diretamente em Event:

```text
price
capacity
availableQuantity
```

Esses valores pertencem a `TicketSector`.

## 6.3 TicketSector

```text
TicketSector
────────────────────────
id: UUID
eventId: UUID

name
description

capacity
availableQuantity
price

createdAt
updatedAt
```

Dinheiro:

```java
BigDecimal
```

Nunca usar `double`.

Invariante:

```text
0 <= availableQuantity <= capacity
```

Também proteger com constraint no PostgreSQL.

## 6.4 Reservation

Uma Reservation representa quantidade de ingressos de **um único setor**.

```text
Reservation
────────────────────────
id: UUID

customerId
eventId
sectorId

quantity
unitPrice
totalAmount

status
expiresAt
createdAt
confirmedAt
```

`unitPrice` e `totalAmount` são snapshots.

## 6.5 Payment

```text
Payment
────────────────────
id: UUID
reservationId

amount
currency
status
provider

createdAt
processedAt
declineReason
```

Defaults:

```text
currency = BRL
provider = FAKE
```

Payment representa uma **tentativa**.

## 6.6 Ticket

```text
Ticket
────────────────────
id: UUID

reservationId
eventId
sectorId
ownerUserId

status

validationToken
manualCode
shareToken

createdAt
usedAt
usedByGateUserId
```

---

# 7. State Machines

## Event

```text
DRAFT
  │
  │ publish
  ▼
PUBLISHED
```

## Reservation

```text
                    payment approved
                           │
                           ▼
HOLDING ─────────────────────────► CONFIRMED
   │
   │ 10 minutes
   ▼
EXPIRED
```

## Payment

```text
PENDING
  ├────► APPROVED
  └────► DECLINED
```

## Ticket

```text
VALID
  │
  │ gate validation
  ▼
USED
```

`INVALID` e `WRONG_EVENT` são resultados de validação, não estados persistidos do Ticket.

---

# 8. Publicação e gerenciamento de eventos

Um evento só pode passar de:

```text
DRAFT → PUBLISHED
```

se possuir:

- title;
- venue;
- `startsAt` futuro;
- conteúdo mínimo;
- pelo menos um setor;
- todos os setores válidos.

## Enquanto DRAFT

Organizer pode:

- criar setor;
- editar setor;
- excluir setor;
- editar todos os campos do evento;
- excluir evento.

## Depois de PUBLISHED

Para o MVP, campos estruturais tornam-se imutáveis:

```text
title
venueName
venueAddress
startsAt
externalSource
externalId
```

Motivo: alterar data, local, identidade principal ou referência externa depois da venda exigiria
notificações, histórico de alterações, cancelamento/refund e outras regras que estão fora do
escopo do desafio.

Continuam editáveis:

```text
description
imageUrl
category
```

Setores:

- não remover setor com reservas/tickets associados;
- capacidade pode aumentar;
- capacidade pode diminuir apenas respeitando quantidade já comprometida;
- preço pode mudar;
- reservas existentes preservam snapshot do preço.

Regra:

```text
newCapacity >= capacity - availableQuantity
```

## Regra derivada de vendas abertas

Não criaremos um novo estado persistido como `SALES_CLOSED`.

A disponibilidade de venda é derivada:

```text
salesOpen =
    event.status == PUBLISHED
    AND event.startsAt > serverNow
```

Se:

```text
event.startsAt <= serverNow
```

novas reservas são rejeitadas com:

```text
SALES_CLOSED
```

Um evento pode continuar `PUBLISHED` para consulta, mesmo sem aceitar novas vendas.

---

# 9. Hold de 10 minutos

Decisão oficial:

```text
HOLD_DURATION = 10 minutes
```

Fluxo:

```text
Customer selects sector
      ↓
Selects quantity
      ↓
Backend checks inventory
      ↓
Reservation = HOLDING
      ↓
expiresAt = serverNow + 10min
      ↓
Inventory decremented
      ↓
Checkout starts
```

Exemplo:

```text
Premium available = 20
Customer holds 2
Premium available = 18
```

Cronômetro explícito:

```text
Sua reserva está garantida por

09:42
```

Estados visuais planejados:

```text
10:00 → 03:00 = NORMAL
02:59 → 01:00 = WARNING
00:59 → 00:00 = CRITICAL
```

Frontend não é autoridade.

Backend:

```text
now >= expiresAt
→ EXPIRED
```

---

# 10. Expiração

Spring executa processo periódico:

```text
status = HOLDING
AND expiresAt <= now
```

Ao expirar:

```text
Reservation HOLDING → EXPIRED
sector.availableQuantity += reservation.quantity
```

Scheduler é limpeza, não a única proteção.

Qualquer operação crítica também verifica `expiresAt`.

A expiração precisa ser idempotente: a mesma Reservation não pode devolver estoque duas vezes.

---

# 11. Concorrência e overselling

Regra:

> O estoque pertence à primeira solicitação que o backend/banco conseguir aceitar atomicamente.

Não confiar em `clickedAt` do browser.

Motivos:

- latência;
- falsificação;
- relógios diferentes;
- backend/banco são a autoridade.

Fluxo conceitual:

```text
BEGIN

lock TicketSector

check availableQuantity >= requestedQuantity

decrement availableQuantity

create Reservation HOLDING

COMMIT
```

O lock deve ser no `TicketSector`.

Exemplo:

```text
Premium available = 1

Request A ─┐
           ├── backend/database
Request B ─┘
```

Resultado:

```text
A → HOLDING
B → INSUFFICIENT_AVAILABILITY
```

ou o inverso.

Nunca:

```text
availableQuantity < 0
```

WebSocket/SSE pode atualizar estoque visualmente depois, mas não substitui consistência transacional.

---

# 11.1 Idempotência na criação de Reservation

Esta é uma regra de confiabilidade da camada de aplicação/API, não um novo conceito de negócio.

Problema protegido:

```text
Customer clicks Reserve
        ↓
Backend creates HOLD
        ↓
response is lost
        ↓
frontend retries
```

Sem idempotência, o retry poderia criar um segundo hold.

Contrato conceitual:

```http
POST /api/customer/events/{eventId}/sectors/{sectorId}/reservations
Idempotency-Key: <UUID>
```

Comportamento:

```text
first request + key ABC
→ creates Reservation #47

retry with same key ABC
→ returns Reservation #47
→ does NOT create another hold
```

A mesma `Idempotency-Key` deve representar a mesma intenção de criação.

Se a chave for reutilizada com payload incompatível, a API deve rejeitar a requisição em vez
de criar uma nova Reservation silenciosamente.

A implementação de persistência da chave pode ser simples no MVP, mas o comportamento é
obrigatório para proteger retries e double-clicks.

Frontend também deve desabilitar temporariamente o CTA enquanto a criação estiver em
andamento, mas essa proteção visual não substitui a idempotência do backend.

---

# 12. Pagamento

Pagamento será simulado.

```text
PaymentGateway
      ▲
      │
FakePaymentGateway
```

Resultados:

```text
APPROVED
DECLINED
```

Uma Reservation pode possuir múltiplas tentativas:

```text
Reservation
│
├── Payment #1 → DECLINED
├── Payment #2 → DECLINED
└── Payment #3 → APPROVED
```

## Recusa

Recusa não libera a Reservation antes de `expiresAt`.

```text
Payment = DECLINED
Reservation = HOLDING
```

UX esperada:

```text
Pagamento recusado.

Seus ingressos continuam reservados.

04:17

[Tentar outro pagamento]
```

## Aprovação

```text
Reservation HOLDING → CONFIRMED
```

Depois:

```text
issue tickets
```

## Valor confiável

Frontend não define o valor cobrado.

Backend usa:

```text
Reservation.totalAmount
```

## Após expiração

```text
now >= expiresAt
→ RESERVATION_EXPIRED
```

Não confirmar.

## Corrida payment vs expiration

Somente uma transição deve vencer:

```text
HOLDING → CONFIRMED
```

ou:

```text
HOLDING → EXPIRED
```

Nunca ambas.

---

# 13. Emissão de ingressos

Ticket só nasce depois de pagamento aprovado.

```text
Reservation HOLDING
        │
        ▼
Payment APPROVED
        │
        ▼
Reservation CONFIRMED
        │
        ▼
N Tickets
```

Invariante:

```text
ticketsIssued == reservation.quantity
```

Confirmação deve ser idempotente.

Requisição repetida não pode gerar tickets extras.

---

# 14. QR antifraude

Cada Ticket possui:

```text
validationToken
```

Características:

- aleatório;
- criptograficamente forte;
- alta entropia;
- único;
- não incremental;
- não derivado de paymentId;
- não derivado de ticketId previsível.

Não fazer:

```text
ticketId=17
```

como credencial de validação.

---

# 15. Código manual

Fallback obrigatório para câmera.

Exemplo:

```text
AB7K-92QX-LM4P
```

Características:

- aleatório;
- único;
- digitável;
- não incremental.

QR e manualCode chegam à mesma regra:

```text
TicketValidationService
```

---

# 16. Compartilhamento

Cada Ticket possui:

```text
shareToken
```

URL pública:

```text
/t/{shareToken}
```

Regra:

```text
shareToken != validationToken
```

O share link não expõe diretamente a credencial usada pela portaria.

Compartilhar:

- não transfere ownership;
- não cria ticket novo;
- não implementa revenda;
- concede acesso ao ingresso.

O Ticket é bearer-like: quem tiver o QR válido poderá utilizá-lo.

Proteção:

```text
single-use validation
```

---

# 17. Portaria

Gate seleciona primeiro o evento:

```text
Evento atual:
Rock Festival — 21:00
```

Depois lê:

```text
QR
OU
manualCode
```

Request conceitual:

```text
eventId
ticketCode
```

Resultados obrigatórios:

```text
VALID
INVALID
ALREADY_USED
WRONG_EVENT
```

## WRONG_EVENT

```text
ticket.eventId != selectedEventId
→ WRONG_EVENT
```

Ticket permanece `VALID`.

## Double-use

Transição:

```text
VALID → USED
```

deve ser atômica.

Duas portarias concorrentes:

```text
one → VALID
other → ALREADY_USED
```

Nunca dois `VALID`.

## Câmera

MUST:

```text
Browser camera
      ↓
QR scanner
      ↓
validationToken
      ↓
API
```

Código manual é fallback.

---

# 18. Meus Ingressos

Área autenticada do CUSTOMER.

Exibir:

- evento;
- setor;
- data;
- local;
- status;
- QR;
- código manual;
- compartilhar.

---

# 19. Navegação e busca

Cliente vê apenas eventos `PUBLISHED`.

Cards:

- imagem;
- título;
- data;
- local;
- preço inicial.

Busca simples por nome será tratada como MUST.

Filtros avançados são COULD:

- data;
- local;
- preço;
- categoria.

---

# 20. Organizer UI

MUST:

- listar eventos próprios;
- criar evento;
- editar evento;
- publicar;
- gerenciar setores.

COULD:

- gráficos;
- KPIs;
- faturamento;
- analytics.

---

# 21. Autenticação, RBAC e ownership

Login:

```text
email
password
   ↓
Spring Security
   ↓
JWT
```

Senha:

```text
BCrypt
```

Não implementar no MVP:

- OAuth;
- Google Login;
- 2FA;
- password recovery;
- arquitetura complexa de refresh token.

RBAC:

```text
CUSTOMER
→ browse
→ reserve
→ pay
→ my tickets

ORGANIZER
→ own events
→ sectors
→ publish

GATE
→ validate tickets
```

Ownership:

```text
Organizer A owns Event A

Organizer B
PATCH Event A
→ 403
```

Frontend esconder controles não substitui autorização do backend.

---

# 22. Ticketmaster

Escolha oficial:

```text
Ticketmaster Discovery API
```

Uso:

```text
Organizer
   ↓
Search catalog
   ↓
Spring Boot
   ↓
Ticketmaster
   ↓
Select external item
   ↓
Create internal Event
```

## Snapshot

Ticketmaster serve para descoberta/criação.

Copiar para Event, conforme necessário:

```text
externalId
name
description
image
category
```

Depois disso nosso Event é independente.

Pertencem ao nosso domínio:

```text
startsAt
venue
sectors
capacity
price
status
```

Environment:

```text
TICKETMASTER_API_KEY
```

Criar:

```text
.env.example
```

Nunca commitar segredo real.

---

# 23. Banco, migrations e seeds

PostgreSQL é source of truth.

Entidades persistidas:

```text
User
Event
TicketSector
Reservation
Payment
Ticket
```

Flyway:

```text
V1__schema.sql
V2__seed_users.sql
V3__seed_events.sql
V4__seed_demo_purchase.sql
```

Hibernate:

```text
ddl-auto=validate
```

Responsabilidades:

```text
Flyway → schema/migrations/seeds
Hibernate → ORM/mapping validation
```

Seeds obrigatórios:

```text
1 Organizer
2 Customers
1 Gate
1 PUBLISHED Event with available tickets
```

Extras recomendados:

```text
1 DRAFT Event
1 CONFIRMED Reservation
1 VALID Ticket
```

Objetivo:

```text
Customer 1 → gate flow ready
Customer 2 → full purchase flow
```

Credenciais seedadas devem estar no README.

---

# 24. Docker

Objetivo:

```bash
docker compose up --build
```

Subir:

```text
PostgreSQL
Spring Boot
React
Seeds
```

O repositório deve ser reproduzível para o avaliador.

---

# 25. TDD

Aplicar TDD principalmente às regras críticas.

Casos mínimos:

```text
shouldCreateTenMinuteHold
shouldRejectReservationWithoutStock
shouldPreventOversellingUnderConcurrency
shouldKeepHoldAfterDeclinedPayment
shouldExpireHoldAndRestoreStock
shouldRejectPaymentAfterExpiration
shouldConfirmReservationOnlyOnce
shouldIssueExactlyRequestedTicketQuantity
shouldRejectForgedTicket
shouldRejectWrongEvent
shouldRejectAlreadyUsedTicket
shouldAllowOnlyOrganizerOwnerToEditEvent
shouldRejectReservationWhenEventAlreadyStarted
shouldRejectStructuralEventChangesAfterPublish
shouldReturnSameReservationForSameIdempotencyKey
```

Ciclo:

```text
RED
↓
GREEN
↓
REFACTOR
```

Frontend: testar comportamento, não CSS irrelevante.

---

# 26. Invariantes oficiais

**INV-01**

```text
0 <= TicketSector.availableQuantity <= TicketSector.capacity
```

**INV-02** — Apenas Event `PUBLISHED` recebe reservas.

**INV-03** — Todo Event `PUBLISHED` possui ao menos um setor válido.

**INV-04** — Reservation nasce como `HOLDING`.

**INV-05** — Hold dura 10 minutos.

**INV-06** — Reservation expirada não pode ser confirmada.

**INV-07** — Payment `DECLINED` não libera o hold antes de `expiresAt`.

**INV-08** — Payment `APPROVED` confirma Reservation uma única vez.

**INV-09** — Expiração devolve estoque uma única vez.

**INV-10** — Ticket só existe para Reservation `CONFIRMED`.

**INV-11**

```text
ticketsIssued == reservation.quantity
```

**INV-12** — Ticket `USED` nunca volta para `VALID`.

**INV-13** — Um Ticket é consumido no máximo uma vez.

**INV-14** — Ticket de Event A não valida como Event B.

**INV-15** — Organizer só administra seus próprios Events.

**INV-16** — CUSTOMER só acessa seus dados privados de reserva/ticket.

**INV-17** — Preço da Reservation não muda após criação.

**INV-18** — Frontend nunca é autoridade para total de pagamento.

**INV-19** — `shareToken` e `validationToken` têm responsabilidades distintas.

**INV-20** — Reservas concorrentes nunca levam `availableQuantity` abaixo de zero.

**INV-21** — Uma Reservation só pode ser criada se:

```text
event.status == PUBLISHED
AND event.startsAt > serverNow
```

Caso contrário:

```text
SALES_CLOSED
```

ou `EVENT_NOT_PUBLISHED`, conforme a condição.

**INV-22** — Depois de `PUBLISHED`, os campos estruturais do Event são imutáveis no MVP:

```text
title
venueName
venueAddress
startsAt
externalSource
externalId
```

**APP-INV-01** — Retry da mesma intenção de criação de Reservation com a mesma
`Idempotency-Key` não cria um novo hold.

---

# 27. Escopo

## MUST

- React + TypeScript;
- Java + Spring Boot;
- persistência;
- Ticketmaster;
- autenticação;
- CUSTOMER / ORGANIZER / GATE;
- criação e gerenciamento de eventos;
- setores por quantidade;
- reserva;
- hold 10 min;
- pagamento aprovado;
- pagamento recusado;
- Meus Ingressos;
- QR seguro;
- compartilhamento;
- câmera;
- código manual;
- VALID / INVALID / ALREADY_USED / WRONG_EVENT;
- proteção contra overselling;
- proteção contra double-use;
- busca simples;
- dados seedados;
- README claro.

## SHOULD

- PostgreSQL;
- Flyway;
- Docker Compose;
- testes;
- deploy;
- tratamento de erros;
- documentação;
- boa experiência visual.

## COULD

- filtros avançados;
- dashboard analítico;
- SSE/WebSocket;
- cancelamento/refund;
- mais E2E;
- cache Ticketmaster;
- Redis futuramente.

## WON'T — MVP

- Redis;
- mapa de assentos;
- TMDb;
- gateway financeiro real;
- microservices;
- Kafka;
- OAuth;
- recuperação de senha;
- revenda;
- e-mail;
- app nativo;
- nota fiscal.

---

# 28. Fluxo ponta a ponta oficial

```text
TICKETMASTER
     │
     ▼
Organizer selects external reference
     │
     ▼
Creates Event DRAFT
     │
     ├── date / venue
     └── sectors / capacity / price
     │
     ▼
PUBLISHED
     │
     ▼
Customer browses/searches
     │
     ▼
Selects Event
     │
     ▼
Selects Sector
     │
     ▼
Selects Quantity
     │
     ▼
Reservation HOLDING
     │
     │ 10:00
     │
     ▼
Checkout
 ┌──────────────┐
 │              │
DECLINED      APPROVED
 │              │
 │              ▼
 │        Reservation
 │         CONFIRMED
 │              │
 │              ▼
 │           Tickets
 │              │
 └─ retry       ▼
            My Tickets
                 │
             QR / Share
                 │
                 ▼
               Gate
                 │
       ┌─────────┼─────────────┐
       ▼         ▼             ▼
     VALID    INVALID     WRONG_EVENT
       │
       ▼
     USED
       │
       └── next scan
              ↓
        ALREADY_USED
```

---

# 29. Endpoints conceituais

Os nomes podem ser refinados sem mudar o domínio.

## Auth

```http
POST /api/auth/login
```

## Catalog

```http
GET /api/catalog/events
```

## Public Events

```http
GET /api/events
GET /api/events/{id}
```

## Customer

```http
POST /api/customer/events/{eventId}/sectors/{sectorId}/reservations
GET  /api/customer/reservations
POST /api/customer/reservations/{reservationId}/payments
GET  /api/customer/tickets
```

## Organizer

```http
GET    /api/organizer/events
POST   /api/organizer/events
PATCH  /api/organizer/events/{id}
DELETE /api/organizer/events/{id}
POST   /api/organizer/events/{id}/publish

POST   /api/organizer/events/{id}/sectors
PATCH  /api/organizer/events/{id}/sectors/{sectorId}
DELETE /api/organizer/events/{id}/sectors/{sectorId}
```

## Gate

```http
POST /api/gate/tickets/validate
```

## Shared Ticket

```http
GET /api/shared/tickets/{shareToken}
```

---

# 30. Erros de domínio esperados

Padronizar respostas de erro.

Exemplos:

```text
EVENT_NOT_PUBLISHED
SALES_CLOSED
EVENT_NOT_FOUND
SECTOR_NOT_FOUND
INSUFFICIENT_AVAILABILITY
RESERVATION_NOT_FOUND
RESERVATION_EXPIRED
RESERVATION_ALREADY_CONFIRMED
PAYMENT_DECLINED
TICKET_INVALID
TICKET_ALREADY_USED
WRONG_EVENT
FORBIDDEN_RESOURCE
IDEMPOTENCY_CONFLICT
VALIDATION_ERROR
```

Não vazar stack traces ou detalhes internos para o frontend.

---

# 31. README final

Deve conter:

- visão do projeto;
- stack;
- arquitetura;
- como rodar;
- Docker;
- variáveis de ambiente;
- Ticketmaster API key;
- credenciais seedadas;
- fluxo de teste;
- pagamento aprovado;
- pagamento recusado;
- QR;
- código manual;
- papéis;
- decisões arquiteturais;
- limitações;
- opcionais implementados;
- testes;
- deploy;
- uso de IA;
- ferramentas utilizadas;
- o que foi decidido manualmente;
- artefatos BMAD/contexto;
- instruções para avaliação.

---

# 32. BMAD, Codex e uso de IA

Filosofia:

> **Human-led, AI-implemented.**

## Humano

Responsável por:

- produto;
- arquitetura;
- UX;
- Design System;
- regras;
- acceptance criteria;
- revisão;
- validação.

## ChatGPT / BMAD

Usado para:

- discovery;
- documentação;
- arquitetura;
- refinamento;
- stories;
- ADRs;
- revisão de decisões.

## Codex

Usado para:

- implementação;
- testes;
- refactors;
- boilerplate;
- execução das Stories.

---

# 33. Estratégia de implementação

Ordem sugerida:

```text
E0 — Project foundation
E1 — Authentication / RBAC
E2 — Ticketmaster catalog
E3 — Event management
E4 — Public event browsing/search
E5 — Ticket sectors
E6 — Reservation + 10-minute hold
E7 — Payment simulation
E8 — Ticket issuance
E9 — My Tickets + sharing
E10 — Gate QR/manual validation
E11 — Docker + seeds
E12 — Critical E2E / hardening
E13 — Deploy
```

Cada Story deve conter:

- objetivo;
- contexto;
- acceptance criteria;
- testes;
- out-of-scope;
- Definition of Done.

---

# 34. Como o Codex deve executar uma Story

1. Ler este arquivo.
2. Ler `AGENTS.md`, se existir.
3. Ler somente os documentos relacionados.
4. Resumir o plano de implementação.
5. Escrever testes para regras críticas.
6. Implementar apenas o mínimo da Story.
7. Executar testes.
8. Reportar:
   - arquivos alterados;
   - testes executados;
   - decisões assumidas;
   - riscos;
   - pendências.
9. Não implementar itens fora da Story.

Exemplo:

```text
STORY: Create ticket reservation hold

Context:
A CUSTOMER reserves N tickets from one TicketSector.

Acceptance Criteria:

AC1
Only authenticated CUSTOMER can reserve.

AC2
Event must be PUBLISHED.

AC3
quantity > 0.

AC4
Sector must belong to Event.

AC5
availableQuantity must be >= quantity.

AC6
Create Reservation with status HOLDING.

AC7
expiresAt = server time + 10 minutes.

AC8
Decrease TicketSector.availableQuantity atomically.

AC9
Concurrent requests must never oversell.

AC10
Reservation price uses a snapshot of sector price.

TDD:
Write failing tests before implementation.

Do not implement:
- Redis
- seat map
- cancellation
- realtime availability
```

---

# 35. Decisões arquiteturais congeladas

```text
Product
→ events platform

Inventory model
→ sectors + quantity

External API
→ Ticketmaster Discovery

Backend
→ Java 21 + Spring Boot

Frontend
→ React + TypeScript + Vite

Database
→ PostgreSQL

ORM
→ JPA/Hibernate

Migrations
→ Flyway

Authentication
→ JWT + Spring Security

Password
→ BCrypt

Hold
→ 10 minutes

Payment
→ FakePaymentGateway

Declined payment
→ hold remains active

Overselling protection
→ transactional sector locking

Ticket creation
→ only after approved payment

QR
→ cryptographically random validationToken

Manual fallback
→ random manualCode

Sharing
→ separate shareToken

Double use
→ atomic VALID → USED

Redis
→ out of MVP

Seat map
→ out of MVP

Frontend split
→ one application, three experiences

Sales close
→ derived from status == PUBLISHED and startsAt > serverNow

Published event structural fields
→ immutable in MVP

Reservation retry protection
→ Idempotency-Key at application/API level
```

---

# 36. Próxima fase — UX + Design System

A arquitetura funcional está congelada.

Antes de implementação visual completa, definir:

## Marca

- nome da plataforma;
- posicionamento;
- personalidade;
- tom;
- identidade visual.

## Customer

Prioridades:

```text
discovery
emotion
event imagery
conversion
checkout clarity
ticket access
```

## Organizer

Prioridades:

```text
productivity
clarity
event management
status
inventory
```

## Gate

Prioridades:

```text
speed
contrast
camera
clear feedback
zero distraction
```

## Design System

Definir:

- typography;
- colors;
- semantic colors;
- spacing;
- grid;
- radius;
- elevation;
- motion;
- iconography.

Componentes prováveis:

```text
Button
Input
Select
Badge
EventCard
TicketCard
TicketSectorCard
ReservationTimer
CheckoutSummary
StatusMessage
ScannerFrame
Navbar
OrganizerSidebar
Dialog
Toast
Skeleton
EmptyState
```

Estados do ReservationTimer:

```text
normal
warning
critical
expired
```

A interface deve evitar aparência genérica de projeto gerado por IA.

---

# 37. Definition of Done do MVP

- [ ] projeto sobe seguindo README;
- [ ] login funciona para os três papéis;
- [ ] organizer cria Event a partir da Ticketmaster;
- [ ] organizer cria setores;
- [ ] organizer publica Event;
- [ ] customer encontra Event publicado;
- [ ] busca simples funciona;
- [ ] customer seleciona setor e quantidade;
- [ ] Reservation HOLDING é criada;
- [ ] timer de 10 minutos funciona visualmente;
- [ ] backend controla expiração;
- [ ] estoque não sofre overselling;
- [ ] evento iniciado não aceita novas reservas;
- [ ] campos estruturais de evento publicado ficam bloqueados;
- [ ] retry de criação de reserva não cria hold duplicado;
- [ ] payment approval funciona;
- [ ] payment decline funciona;
- [ ] decline mantém hold;
- [ ] expiration devolve estoque;
- [ ] tickets são emitidos após aprovação;
- [ ] My Tickets funciona;
- [ ] QR é exibido;
- [ ] share link funciona;
- [ ] gate lê QR pela câmera;
- [ ] gate aceita manualCode;
- [ ] gate retorna VALID;
- [ ] gate retorna INVALID;
- [ ] gate retorna ALREADY_USED;
- [ ] gate retorna WRONG_EVENT;
- [ ] double validation é protegida;
- [ ] seeds existem;
- [ ] Docker Compose funciona;
- [ ] testes críticos passam;
- [ ] uso de IA está documentado;
- [ ] limitações estão documentadas;
- [ ] histórico Git possui commits descritivos.

---

# 38. Filosofia final

```text
Human-led
AI-implemented
Test-protected
Domain-driven
MVP-first
```

O objetivo não é produzir o maior sistema possível.

O objetivo é produzir um sistema:

- coerente;
- explicável;
- completo;
- testável;
- seguro;
- visualmente intencional;
- fácil de avaliar.
