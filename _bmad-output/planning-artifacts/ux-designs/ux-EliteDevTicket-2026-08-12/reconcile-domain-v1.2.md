# Reconciliação — Project Specification v1.2 × UX final atual

**Fonte reconciliada:** `docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md`  
**Artefatos avaliados:** `DESIGN.md` e `EXPERIENCE.md`  
**Data:** 2026-08-12  
**Escopo:** decisões, estados, invariantes, desvios, features acidentais e impactos de arquitetura. Nenhum spine foi alterado.

## Veredito

**PASS com observações documentais não bloqueantes.** Os spines preservam as decisões congeladas da v1.2, não alteram máquinas de estado, não introduzem feature nova no MVP e isolam corretamente os mecanismos ainda dependentes de Architecture. Não foi encontrado conflito que exija mudar uma regra de domínio.

Há, porém, quatro comportamentos presentes em `EXPERIENCE.md` cuja autoridade não é a v1.2 isoladamente. Eles são compatíveis com o PRD aprovado e com decisões posteriores do produto, mas devem continuar rastreados a essas fontes para não parecerem derivados da spec v1.2: limite de 1–6, uma HOLDING vigente por Customer/Event, escolha determinística APPROVED/DECLINED e acesso Gate a todos os Events publicados.

## 1. Matriz de conformidade do domínio

| Contrato v1.2 | Referência | Cobertura nos spines | Resultado |
|---|---|---|---|
| Um frontend, três experiências | §§3, 35 | `EXPERIENCE/Foundation`, IA e `DESIGN/Brand & Style` diferenciam Customer, Organizer e Gate | Conforme |
| Modelo `TicketSector + quantidade`, sem mapa de assentos | §§4–6, 35 | Detalhe, seletor de setor/quantidade, wireflow Customer e Non-MVP | Conforme |
| `startingPrice = MIN(TicketSector.price)` | §5 | `DESIGN/EventCard` e `EXPERIENCE/Component Patterns` | Conforme |
| Event `DRAFT → PUBLISHED` | §7 | Organizer inventory, forms, flows e domain states | Conforme |
| Publicação somente com dados/setores válidos | §8, INV-03 | `S13`, formulários e UJ-O01; checklist respeita referência, título, data/local e setor válido | Conforme; conteúdo mínimo foi refinado por fonte de produto posterior |
| DRAFT totalmente editável e excluível | §8 | Wireflow Organizer e dialog destrutivo | Conforme |
| Campos estruturais imutáveis após publicação | §8, INV-22 | UJ-O02, forms e Organizer wireflow enumeram os seis campos | Conforme |
| Campos não estruturais editáveis | §8 | description, imageUrl e category permanecem editáveis | Conforme |
| Setor associado não removível; capacidade respeita comprometido; preço preserva snapshot | §8, INV-17 | S12, UJ-O02 e edge cases | Conforme |
| Vendas derivadas: PUBLISHED e `startsAt > serverNow` | §8, INV-21 | catálogo/detalhe, states, Customer flow e edge matrix | Conforme |
| Evento iniciado continua consultável | §8 | S02 e edge matrix | Conforme |
| Hold exato de 10 minutos, backend autoridade | §§9–10, INV-05/06 | Timer, microcopy, wireflows, refresh/suspensão e edge cases | Conforme |
| Timer normal/warning/critical/expired | §9 | `ReservationTimer` em DESIGN e EXPERIENCE | Conforme |
| Expiração idempotente e scheduler não é única proteção | §10, INV-09 | UX não tenta implementar; apresenta expiração autoritativa e handoff de reconciliação | Conforme |
| Estoque atômico; nunca overselling | §11, INV-01/20 | Revalidação, conflito de estoque e autoridade backend estão explícitos | Conforme |
| Retry/double-click não cria outro hold | §11.1, APP-INV-01 | CTA loading + mesma reservation, sem atribuir consistência à UI | Conforme |
| Reservation de um único setor e snapshots de preço | §6.4, INV-17 | Checkout e wireflow Customer preservam setor único e snapshot | Conforme |
| Payment `PENDING → APPROVED/DECLINED` | §7 | UX representa “processamento de interface”, APPROVED e DECLINED; não cria requisito visual autônomo PENDING | Conforme; separação técnica adequada |
| DECLINED mantém HOLDING e permite retry | §12, INV-07 | Microcopy, S04, UJ-C02 e edge matrix | Conforme |
| Backend cobra `Reservation.totalAmount` | §12, INV-18 | Payment e CheckoutSummary deixam total não editável/autoritativo | Conforme |
| Corrida payment × expiration tem um vencedor | §12 | Estado `verifying` e edge matrix não antecipam sucesso | Conforme; mecanismo é handoff Architecture |
| Tickets apenas após aprovação e em quantidade exata | §13, INV-10/11 | UJ-C01/C02, confirmação e TicketCard por unidade | Conforme |
| Ticket `VALID → USED`; nunca volta | §7, INV-12/13 | Tickets, link compartilhado e Gate flows | Conforme |
| `INVALID`/`WRONG_EVENT` são resultados, não estados persistidos | §7 | `States` distingue Ticket VALID/USED de resultados Gate; inventário usa estados de apresentação | Conforme |
| Token QR seguro, manual code obrigatório | §§14–15 | QR, código manual e scanner; não redefine geração | Conforme |
| `shareToken != validationToken`; bearer-like, sem ownership transfer | §16, INV-19 | UJ-C03, page pública, share pattern e Non-MVP | Conforme |
| Gate seleciona Event antes de validar | §17 | S14→S15→S16 | Conforme |
| Resultados VALID/INVALID/ALREADY_USED/WRONG_EVENT | §17 | GateResult, S16, flow e accessibility | Conforme |
| WRONG_EVENT não consome; double-use atômico | §17, INV-13/14 | UJ-G01, flow e edge matrix | Conforme |
| Câmera MUST; manual obrigatório | §17, §27 | ScannerFrame, S15 e responsive/platform | Conforme |
| Meus Ingressos com conteúdo mínimo | §18 | IA, S06/S07 e Ticket components cobrem evento/setor/data/local/status/QR/code/share | Conforme |
| Público vê somente PUBLISHED; busca simples MUST | §19 | S01/S02 e catálogo | Conforme |
| Organizer administra somente próprios Events | §§20–21, INV-15 | IA Organizer e S09 | Conforme |
| RBAC/ownership permanecem backend | §21 | Foundation e error handling; UI não é tratada como proteção | Conforme |
| Ticketmaster via backend e snapshot interno | §22 | S10, UJ-O01 e Organizer flow | Conforme |
| Sem criação manual independente | Escopo oficial + decisão posterior | Explicitamente Non-MVP e indisponibilidade oferece retry | Conforme ao conjunto autoritativo; não é frase literal da v1.2 |
| Erros de domínio padronizados sem stack trace | §30 | Error patterns e edge cases traduzem os erros em recuperação contextual | Conforme |
| Itens WON'T/COULD não entram silenciosamente | §§2, 27 | Fechamento, Non-MVP/Candidates e DESIGN “não inventar analytics” | Conforme |

## 2. Papéis, acesso e ownership

`EXPERIENCE.md` mantém corretamente a separação de responsabilidades da §21:

- visitante/Customer navega; a Reservation só é iniciada após autenticação CUSTOMER;
- Customer acessa checkout e seus Tickets;
- Organizer lista e administra apenas seus Events;
- Gate seleciona o contexto e valida;
- ocultação de controles não é apresentada como substituta da autorização backend.

Não há UI de cadastro, gestão de roles, recuperação de senha, OAuth, associação Gate↔Event ou acesso cruzado a recursos privados.

**Observação de rastreabilidade:** “Gate vê todos os Events PUBLISHED” está em `EXPERIENCE/IA`, mas a v1.2 apenas exige que Gate selecione primeiro o Event e não descreve associação. A regra é compatível com o PRD aprovado, não deve ser atribuída apenas à §17/§21.

## 3. Máquinas de estado e semântica visual

### Event

Não foi criado estado persistido extra. `sales closed` aparece corretamente como condição derivada, não como novo estado Event. O uso visual de `SALES_CLOSED` não altera `PUBLISHED`.

### Reservation

Somente HOLDING, CONFIRMED e EXPIRED aparecem como estados de domínio. Os níveis normal/warning/critical são estados visuais do timer, não estados persistidos.

### Payment

O spine usa “processando/verifying” como estado de interface e preserva APPROVED/DECLINED como resultados. Isso não rejeita nem promove `PENDING` a feature de produto, em conformidade com a v1.2 e com o PRD final.

### Ticket e Gate

Ticket permanece apenas VALID/USED. Os quatro resultados Gate são apresentados como feedback operacional. `INVALID` e `WRONG_EVENT` não foram modelados como estado do Ticket. USED continua acessível em My Tickets e no share link, sem nova entrada.

## 4. Desvios e features acidentais

### Nenhuma feature acidental incorporada

Não foram incorporados ao MVP: filtros avançados, analytics/KPIs, seat map, gateway real, cancelamento/refund, revenda, e-mail, app nativo, offline, sincronização posterior, share-token rotation/revocation, transferência de ownership, SSE/WebSocket ou LOW_AVAILABILITY.

Os seguintes itens são padrões de recuperação/compatibilidade, não novas capacidades de domínio:

- fallback de copiar link quando Web Share API falha;
- estados de câmera negada/indisponível;
- tratamento de sessão expirada;
- skeleton/toast/offline banner como componentes genéricos;
- retorno seguro após login e restauração de intenção não sensível.

### Regras adicionais cuja origem deve permanecer explícita

1. **Quantidade 1–6:** aparece no `QuantityStepper`; não está na v1.2, mas foi aprovada no PRD.
2. **Máximo de uma HOLDING vigente por Customer/Event:** aparece em Timer/edge cases; não está na v1.2, mas foi aprovada no PRD.
3. **Escolha explícita e determinística APPROVED/DECLINED:** a v1.2 define FakePaymentGateway e ambos os resultados, mas não a forma de provocá-los. A UI explícita vem do PRD/decisão de avaliação.
4. **Gate acessa todos os PUBLISHED:** não é explicitado pela v1.2; vem do PRD.

Esses quatro pontos não constituem conflito, mas merecem rastreabilidade ao PRD para evitar que uma futura leitura da v1.2 isolada os trate como inferência UX.

## 5. Erros e edge cases

A cobertura é forte e compatível com §30. O spine trata indisponibilidade, expiração, autorização e validação com mensagem contextual e próxima ação, sem vazar stack trace ou token completo.

Coberturas críticas confirmadas:

- estoque muda antes/depois de login;
- disputa pelo último estoque;
- double-click/retry de criação;
- hold vencido ainda não limpo pelo scheduler;
- refresh/aba suspensa e clock skew;
- DECLINED com hold vigente;
- resposta de pagamento perdida;
- corrida expiração × pagamento;
- Ticketmaster indisponível, sem criação manual;
- publicação incompleta;
- redução de capacidade inválida e remoção proibida de setor;
- câmera negada/ausente e QR não decodificado;
- Gate sem rede;
- INVALID, WRONG_EVENT e ALREADY_USED;
- link compartilhado após USED.

**Precisão:** `share unsupported` em S07 é um estado de capacidade de compartilhamento do navegador, não de domínio; o padrão subsequente esclarece corretamente que o link permanente continua obtível/copíavel.

## 6. Impactos de arquitetura identificados — sem decisão UX

Os cinco handoffs listados em `EXPERIENCE.md` estão corretamente isolados:

1. biblioteca/integração de câmera, permissões, troca de dispositivo e HTTPS/secure context;
2. consulta/reconciliação autoritativa após resposta de pagamento perdida;
3. sincronização do timer por `expiresAt` após refresh, suspensão e clock skew;
4. LOW_AVAILABILITY fora do contrato até haver regra aprovada;
5. detecção e UX de indisponibilidade do Gate online, sem fila/consumo offline.

Adicionalmente, os seguintes mecanismos continuam responsabilidade de Architecture e não foram decididos pelo UX:

- persistência e conflito da `Idempotency-Key`;
- lock transacional do TicketSector;
- corrida atômica CONFIRMED×EXPIRED;
- confirmação/emissão idempotente;
- consumo atômico VALID→USED;
- geração e armazenamento seguros de validationToken/manualCode/shareToken;
- forma de recuperar intenção após login sem confiar em estoque/preço do cliente.

Nenhum desses pontos exige parar a reconciliação: todos preservam o comportamento definido e aguardam mecanismo técnico.

## 7. Responsividade, câmera e acessibilidade

Os spines aprofundam, sem reinterpretar, as prioridades da §36:

- Customer: mobile central, fotografia e conversão com clareza;
- Organizer: desktop prioritário, produtividade e reflow em telas estreitas;
- Gate: câmera em primeiro plano, uma tarefa por vez e fallback manual.

Gate permanece online-only e browser-based; nenhuma fila offline foi introduzida. Câmera depende de contexto seguro e suporte do browser, corretamente enviada a Architecture.

A meta WCAG 2.1 AA foi traduzida em requisitos verificáveis (teclado, foco, contraste, reflow, targets, errors, live regions, reduced motion). O timer não anuncia cada segundo, e o Gate usa texto+ícone+tratamento+cor, preservando clareza sem mudar semântica.

## 8. Design System e domínio

Os componentes de produto refletem entidades e estados reais sem criar novos agregados ou features. Destaques corretos:

- EventCard deriva starting price do menor setor;
- TicketSectorCard/Row preserva setor+quantidade;
- ReservationTimer diferencia estados visuais de domínio;
- PaymentSimulationControl não controla valor;
- TicketCard/QRCodePanel/ManualCode preservam credenciais distintas;
- GateResult modela resultados de validação, não estados persistidos.

`OfflineBanner` na lista de fundação deve ser entendido como padrão de conectividade/erro, especialmente para comunicar que Gate exige internet; não autoriza modo offline.

## 9. Conclusão e ações

**Conflitos reais:** nenhum.  
**Alterações silenciosas de Domain:** nenhuma.  
**Novas features MVP:** nenhuma.  
**Impactos Architecture:** identificados e segregados.  
**Correção obrigatória nos spines:** nenhuma.

Ação documental recomendada para a fase de rastreabilidade: associar explicitamente ao PRD aprovado as quatro regras adicionais listadas na seção 4. Isso não requer mudar o comportamento dos spines.
