# Reconciliação — ELITE_DEV_PROJECT_SPEC_v1.2.md

## Escopo

- **Fonte:** `Docs/ELITE_DEV_PROJECT_SPEC_v1.2.md`
- **Destinos comparados:** `prd.md` e `addendum.md`
- **Método:** comparação das regras de produto, invariantes, estados, escopo MUST/SHOULD/COULD/WON'T, intenção qualitativa e separação entre requisitos de produto e decisões de implementação.
- **Resultado geral:** a especificação v1.2 está materialmente coberta. Não foi encontrado conflito de produto que exija bloquear a finalização. O PRD incorpora decisões posteriores e mais precisas tomadas no discovery; elas complementam ou resolvem ambiguidades da v1.2 sem contrariá-la.

## 1. Cobertura de produto e domínio

| Área da v1.2 | Cobertura | Evidência / observação |
|---|---|---|
| Três papéis e fluxo ponta a ponta | Completa | Visão, objetivo, UJ-01 a UJ-03 e FR-01 a FR-53 cobrem Organizer, Customer e Gate. |
| Modelo por setores e quantidade | Completa | Limites, FR-12, FR-14, FR-18, FR-19, FR-24 a FR-31; mapa de assentos explicitamente fora do escopo. |
| Event `DRAFT -> PUBLISHED` | Completa | FR-09 a FR-19, incluindo listagem, exclusão de rascunho, publicação e imutabilidade estrutural. |
| Regras pós-publicação | Completa | FR-16 a FR-19 preservam campos bloqueados, conteúdo editável, proteção de setor comprometido, capacidade e snapshot de preço. |
| Fechamento derivado de vendas | Completa | NFR-03/04, FR-23/24: `serverNow >= startsAt` fecha novas reservas, sem novo estado persistido. |
| Hold de 10 minutos | Completa | Limites 5.2 e FR-25, FR-27, FR-30, FR-31. Frontend não é autoridade. |
| Expiração e devolução de estoque | Completa | SM-03/05, FR-31, addendum de consistência; restituição exatamente uma vez. |
| Concorrência e overselling | Completa | SM-01, FR-29 e addendum registram atomicidade/locking no setor e disponibilidade nunca negativa. |
| Retry/double-click idempotente | Completa | SM-02 e FR-28 descrevem comportamento observável; `Idempotency-Key` e conflito de payload estão corretamente no addendum/API contract. |
| Pagamento simulado | Completa | FR-32 a FR-36 cobrem APPROVED/DECLINED, múltiplas tentativas, valor autoritativo, recusa sem perder hold e corrida com expiração. |
| Emissão exata de Tickets | Completa | SM-06 e FR-37: Ticket nasce após aprovação, exatamente por quantidade e sem duplicação. |
| QR e código manual seguros | Completa | FR-38/39/44/45 e addendum preservam identificadores únicos, não previsíveis e fallback manual. |
| Compartilhamento | Completa | Seção 6.3 e FR-40 a FR-42 cobrem token distinto, link público permanente, ausência de ownership transfer e estado USED. |
| Gate e quatro resultados | Completa | FR-43 a FR-52 cobrem seleção do evento, câmera/manual, VALID, INVALID, ALREADY_USED, WRONG_EVENT, double-use e auditoria. |
| Meus Ingressos | Completa | UJ-02 e FR-38 incluem evento, QR, código manual e estado; setor/data/local são exigidos na jornada e nos dados do Event/Ticket. |
| Busca simples e catálogo público | Completa | FR-20 a FR-22; acesso público foi explicitado e autenticação só começa na reserva. |
| RBAC e ownership | Completa | FR-01 a FR-04 e limites 5.1; autorização é responsabilidade do backend. |
| Ticketmaster como snapshot | Completa | UJ-01, seção 6.2 e FR-05 a FR-08; indisponibilidade e reutilização de `externalId` foram esclarecidas. |
| Seeds e avaliabilidade | Completa | SM-10/13, FR-53, NFR-19/20 e restrições da avaliação. A redação corrige a ambiguidade “available tickets” para setores com estoque, coerente com Ticket só existir após aprovação. |
| Escopo excluído | Completa | Seção 9 preserva as exclusões da v1.2 e acrescenta exclusões decididas no discovery. |

## 2. Invariantes oficiais

Todos os invariantes funcionais da seção 26 da v1.2 estão preservados:

- `0 <= availableQuantity <= capacity` e ausência de overselling;
- Event publicado com ao menos um setor válido;
- vendas fechadas quando `startsAt <= serverNow`;
- campos estruturais de Event publicado imutáveis;
- Reservation de um único setor, preço e total capturados;
- hold de dez minutos e expiração verificada pelo backend;
- retry da mesma intenção sem novo hold;
- recusa mantendo Reservation em `HOLDING` enquanto vigente;
- aprovação e expiração mutuamente exclusivas;
- quantidade emitida igual a `reservation.quantity`;
- Ticket emitido apenas após pagamento aprovado;
- validação de uso único e `WRONG_EVENT` sem consumo;
- tokens de validação e compartilhamento distintos;
- ownership e RBAC no backend.

As regras posteriores de máximo de seis ingressos, apenas um hold vigente por Customer/Event, acesso público ao catálogo, operação online da Gate e auditoria mínima são extensões aprovadas no discovery, não conflitos.

## 3. Ambiguidades da v1.2 resolvidas no discovery

Não exigem correção nem nova decisão:

1. **“Conteúdo mínimo” para publicação:** a v1.2 não enumera com precisão e sugere `title`, `venue`, data futura e conteúdo mínimo. O PRD resolve com a decisão aprovada: referência Ticketmaster, título, data futura, nome/endereço do local e ao menos um setor válido; descrição, imagem e categoria são opcionais.
2. **Seed com “available tickets”:** a v1.2 usa linguagem incompatível com seu próprio modelo, no qual Ticket só nasce após aprovação. FR-53 usa corretamente Event PUBLISHED com TicketSectors e estoque disponível.
3. **Criação manual sem Ticketmaster:** a v1.2 descreve criação via catálogo mas não explicita o fallback de indisponibilidade. O PRD define, por decisão aprovada, que não há criação manual no MVP e que seeds mantêm o restante avaliável.
4. **Comportamento do share link após uso:** a v1.2 define acesso bearer-like e single use, mas não determina a tela após consumo. O PRD estabelece que o link permanece acessível mostrando `USED`.
5. **Hold ativo:** a v1.2 usa estado e expiração, porém não trata atraso do scheduler para a restrição Customer/Event. FR-27 usa a definição correta e observável: `HOLDING` e `serverNow < expiresAt`.

## 4. Intenção qualitativa

A intenção qualitativa da v1.2 foi preservada e aprofundada:

- **Customer:** descoberta, emoção, imagem do evento, clareza no checkout e acesso simples ao ingresso aparecem na visão e em UJ-02.
- **Organizer:** produtividade, clareza, gestão, estado e inventário aparecem na visão, UJ-01 e nos FRs de gerenciamento.
- **Gate:** velocidade, contraste, feedback claro e ausência de distração aparecem na visão, UJ-03, SM-09, NFR-05/10 e FR-51.
- A diretriz de evitar uma interface genérica “gerada por IA” não está formulada como requisito funcional no PRD. Isso é apropriado como orientação futura de UX/Design System, mas deve permanecer visível em um artefato de UX posterior.

## 5. Separação PRD x addendum

### Posicionamento correto

- Stack, arquitetura monolítica, PostgreSQL/Flyway, JWT/BCrypt, locking, `Idempotency-Key`, gateway fake, estratégia de testes e uso de IA estão corretamente no `addendum.md`.
- Comportamentos observáveis e invariantes permanecem no PRD, independentemente do mecanismo técnico.
- Endpoints conceituais não foram transportados ao PRD, o que evita acoplar requisitos de produto ao contrato técnico prematuramente.

### Detalhes técnicos da v1.2 ainda não preservados explicitamente no addendum

Estes itens não criam lacuna funcional no PRD, mas são candidatos a arquitetura/API contract ou a uma expansão do addendum:

1. Controllers restritos a HTTP, regras em Services e entidades JPA não expostas diretamente; contratos via DTOs.
2. Catálogo de endpoints conceituais e respostas de erro padronizadas (`SALES_CLOSED`, `INSUFFICIENT_AVAILABILITY`, `IDEMPOTENCY_CONFLICT` etc.).
3. `ddl-auto=validate`, divisão de responsabilidades Flyway/Hibernate e sugestão de migrations/seeds.
4. `TICKETMASTER_API_KEY`, `.env.example` e proibição de commitar segredo real.
5. Entidades/campos e state machines detalhados, inclusive `Payment` como tentativa e `INVALID`/`WRONG_EVENT` como resultados, não estados persistidos de Ticket.
6. Frameworks de teste concretos (JUnit, Mockito, Spring Boot Test e Testcontainers condicionado ao prazo).

Esses detalhes devem ser preservados na próxima etapa técnica; inseri-los no corpo do PRD seria inadequado.

## 6. Omissões ou conflitos que afetam a finalização

**Nenhum conflito bloqueante encontrado.**

Há duas observações de rastreabilidade não bloqueantes:

- O PRD expressa o resultado da Gate em até um segundo como meta de desempenho, enquanto a v1.2 apenas pede resposta clara/rápida; isso é uma decisão posterior aprovada e não um conflito.
- O logout e a auditoria de validação foram acrescentados após a v1.2; são requisitos aprovados no discovery e não ampliam materialmente o produto.

## 7. Veredito

**APROVADO PARA FINALIZAÇÃO.** A Project Specification v1.2 está reconciliada com o PRD e o addendum. Não há regra de produto perdida, contradita ou silenciosamente substituída. Recomenda-se apenas transportar os seis grupos de detalhes técnicos listados na seção 5 para arquitetura/API contract, mantendo-os fora do PRD.
