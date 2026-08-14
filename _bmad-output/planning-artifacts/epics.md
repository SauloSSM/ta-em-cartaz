---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - docs/01-product/Desafio-Elite-Dev-2026.pdf
  - docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md
  - _bmad-output/planning-artifacts/prds/prd-EliteDevTicket-2026-08-11/prd.md
  - _bmad-output/planning-artifacts/prds/prd-EliteDevTicket-2026-08-11/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-EliteDevTicket-2026-08-12/EXPERIENCE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-EliteDevTicket-2026-08-12/DESIGN.md
  - docs/engineering/java-standards.md
  - docs/engineering/react-standards.md
---

# EliteDevTicket - Epic Breakdown

## Overview

Este documento decompõe exclusivamente o escopo MVP aprovado em Epics e Stories implementáveis. PRD/Domain preservam autoridade de comportamento; Architecture Spine preserva autoridade técnica; `EXPERIENCE.md` preserva o contrato comportamental; `DESIGN.md` só informa decisões visuais já aprovadas. Tokens e mockups high-fi permanecem pendentes e não serão inventados.

## Requirements Inventory

### Functional Requirements

- FR-01: Autenticar usuários provisionados como `ORGANIZER`, `CUSTOMER` ou `GATE`.
- FR-02: Aplicar autorização por papel no backend.
- FR-03: Restringir Organizer aos próprios Events e Customer às próprias Reservations/Tickets.
- FR-04: Encerrar sessão por logout.
- FR-05: Permitir ao Organizer pesquisar referências Ticketmaster e ver dados disponíveis.
- FR-06: Informar indisponibilidade Ticketmaster e permitir retry, sem criação manual.
- FR-07: Criar Event interno `DRAFT` a partir de snapshot Ticketmaster.
- FR-08: Permitir reutilizar o mesmo `externalId` em múltiplos Events internos.
- FR-09: Permitir edição integral de Event e TicketSectors em `DRAFT`.
- FR-10: Listar Events próprios, distinguindo `DRAFT` e `PUBLISHED`.
- FR-11: Excluir somente Event próprio `DRAFT`; publicado não exclui/cancela.
- FR-12: Adicionar, editar e remover TicketSectors quando o estado permitir.
- FR-13: Exibir pendências e bloquear publicação incompleta.
- FR-14: Publicar apenas Event com referência, título, data futura, local completo e setor válido.
- FR-15: Aceitar ausência de descrição/imagem/categoria e exibir fallbacks públicos válidos.
- FR-16: Bloquear campos estruturais depois de publicar.
- FR-17: Permitir editar descrição, imagem e categoria após publicar.
- FR-18: Impedir remoção de setor associado a Reservation/Ticket e reduzir capacidade abaixo do comprometido.
- FR-19: Permitir alteração válida de capacidade/preço preservando snapshots existentes.
- FR-20: Expor catálogo público de Events publicados com card e `startingPrice = MIN(TicketSector.price)`.
- FR-21: Permitir busca pública simples por título, sem filtros avançados obrigatórios.
- FR-22: Expor detalhe público de Event, setores, preços e disponibilidade.
- FR-23: Manter Event consultável, mas fechar novas reservas em `serverNow >= startsAt`.
- FR-24: Exigir `CUSTOMER` para criar Reservation de um setor, quantidade inteira 1–6, com vendas abertas e estoque.
- FR-25: Criar `HOLDING`, baixar disponibilidade atomicamente e expirar em 10 minutos do servidor.
- FR-26: Capturar preço unitário/total no backend no momento da Reservation.
- FR-27: Manter no máximo uma `HOLDING` vigente por Customer/Event e recuperar a existente sem nova baixa.
- FR-28: Repetir intenção de reserva sem criar outro hold ou baixar estoque novamente.
- FR-29: Impedir overselling sob concorrência.
- FR-30: Exibir timer claro do hold no checkout.
- FR-31: Expirar idempotentemente e restituir estoque exatamente uma vez.
- FR-32: Oferecer resultado determinístico `APPROVED` ou `DECLINED` no pagamento simulado.
- FR-33: Registrar múltiplas tentativas de Payment enquanto hold vigente.
- FR-34: Manter hold após `DECLINED`, sem confirmar ou devolver estoque antes do prazo.
- FR-35: Confirmar aprovação uma única vez; corrida com expiração tem transição final única.
- FR-36: Recusar Payment de Reservation expirada.
- FR-37: Emitir idempotentemente exatamente um Ticket por unidade confirmada.
- FR-38: Exibir Meus Ingressos com Event, setor, data/hora, local, estado, QR, código manual e share.
- FR-39: Gerar identificadores únicos, imprevisíveis e separados para validação, manual e share.
- FR-40: Permitir obter/copiar link público permanente, sem rotação/revogação/transferência.
- FR-41: Exibir no share Event, data/hora, local, setor, estado, QR e código, sem PII do Customer.
- FR-42: Manter link acessível após uso, exibir `USED` e não permitir nova entrada.
- FR-43: Permitir ao `GATE` listar/selecionar qualquer Event `PUBLISHED` antes da validação.
- FR-44: Ler QR por câmera quando browser/dispositivo/contexto seguro suportarem.
- FR-45: Oferecer entrada manual como fallback obrigatório.
- FR-46: Retornar `VALID` e consumir Ticket `VALID` do Event selecionado atomicamente.
- FR-47: Retornar `ALREADY_USED` sem nova mutação para Ticket usado.
- FR-48: Retornar `WRONG_EVENT` sem consumir Ticket de outro Event.
- FR-49: Retornar `INVALID` sem vazar detalhes para credencial forjada/desconhecida.
- FR-50: Sob concorrência, produzir no máximo uma validação `VALID` por Ticket.
- FR-51: Comunicar resultado Gate por texto, ícone e tratamento visual, nunca só cor.
- FR-52: Auditar tentativa Gate com dados permitidos, sem token/código completo.
- FR-53: Fornecer seeds: 1 Organizer, 2 Customers, 1 Gate e Event `PUBLISHED` com setores/estoque.

### NonFunctional Requirements

- NFR-01: Operar em BRL e exibir `R$`.
- NFR-02: Apresentar datas em `America/Sao_Paulo` e persistir instantes sem ambiguidade.
- NFR-03: Backend é autoridade temporal para hold, expiração e vendas.
- NFR-04: Recusar reserva em `serverNow >= startsAt`.
- NFR-05: Meta Gate de resultado até 1 s após QR/código submetido.
- NFR-06: Meta de ações comuns até 2 s no ambiente de avaliação.
- NFR-07: Feedback visual imediato durante qualquer operação assíncrona.
- NFR-08: Buscar WCAG 2.1 AA.
- NFR-09: Garantir teclado, foco, labels, semântica, contraste e mensagens compreensíveis.
- NFR-10: Gate nunca depende apenas de cor.
- NFR-11: Suportar Chrome/Edge/Firefox atuais no desktop e Chrome Android; registrar versões verificadas.
- NFR-12: Verificar Safari iOS best effort e documentar limitações sem impedir fallback manual.
- NFR-13: Excluir browsers legados.
- NFR-14: Scanner depende de câmera/contexto seguro; manual é fallback obrigatório.
- NFR-15: Persistir horário e resultado de cada Payment.
- NFR-16: Persistir dados mínimos de cada tentativa Gate.
- NFR-17: Nunca gravar tokens, códigos completos, JWT, senha ou segredos em log/auditoria.
- NFR-18: Erros públicos não expõem stack trace, internos ou segredos.
- NFR-19: README cobre banco, env, credenciais, execução e reprodução dos fluxos de avaliação.
- NFR-20: README declara incompletudes/limitações conhecidas.

### Additional Requirements

- Monólito modular por capacidade: `auth`, `catalog`, `events`, `reservations`, `payments`, `tickets` e `gate`, com ports/adapters apenas em fronteiras reais.
- Stack seed: Java 21, Spring Boot 4.0.7, React 19.2.x, TypeScript 5.x, Vite 7.3.x, Node 22.12+ LTS, PostgreSQL 17.x, Flyway e Docker Compose.
- OpenAPI versionado é autoridade do contrato HTTP `/api/v1`; DTO Java/tipos TS devem conformar e ter verificação automatizada de drift.
- JWT HS256 em cookie HttpOnly, CSRF para mutações, TTL padrão 8 h, segredo externo >=256 bits e BCrypt configurável por ambiente.
- Intenção pré-login em `sessionStorage` por 15 min apenas com Event, setor, quantidade, retorno interno e data; sem hold, preço, token ou Reservation antes do login CUSTOMER.
- PostgreSQL/Flyway são fonte de schema/seeds; Hibernate usa `ddl-auto=validate`; dinheiro usa decimal exato/`BigDecimal`; `Clock` é testável.
- Toda mutação de `TicketSector.capacity`/`availableQuantity` usa `PESSIMISTIC_WRITE`, constraints e a fórmula aprovada de capacidade comprometida.
- Locks seguem `Customer → Reservation → TicketSector`; conjuntos são ordenados por UUID crescente.
- Hold fixa `expiresAt = serverNow + 10 min`; expiração semântica é `serverNow >= expiresAt`; scheduler é cleanup a cada 30 s, em lotes pequenos/transação por Reservation, com reconciliação lazy para evitar falsa escassez.
- Criar Reservation é idempotente por `(customerId, Idempotency-Key)` e payload canônico; hash divergente retorna `IDEMPOTENCY_CONFLICT`.
- Payment e emissão usam transação única; Tickets têm unicidade por `(reservation_id, ordinal)`.
- PaymentAttempt e ValidationAttempt possuem UUID de cliente, `UNIQUE(attempt_id)`, fingerprint v1 canônico, claim/result/replay atômicos, conflito de fingerprint e efeito único concorrente.
- `WRONG_EVENT` é avaliado antes do consumo; ValidationAttempt e `VALID → USED` fazem commit na mesma transação.
- Tokens de QR/share usam CSPRNG; manualCode Crockford-like com 10 caracteres, normalizado e único; valores reexibíveis não são logados.
- Ticketmaster somente via adapter backend, budget total ~5 s e no máximo um retry para rede/timeout/5xx; sem cache/circuit breaker, sem segredo/payload bruto no cliente.
- Gate é estritamente online; `QrDecoder` abstrai câmera, exige contexto seguro, encerra tracks na saída e pausa após leitura até resultado/próximo.
- Timer usa `serverNow`/`expiresAt` e `performance.now()` apenas para apresentação; reconcilia em refresh, visibilidade, antes de pagar, zero e erro.
- Logs estruturados stdout + traceId + redaction; liveness/readiness mínimas e sem detalhes internos; Ticketmaster não integra readiness.
- Perfis local/test/demo/prod; Docker Compose sobe SPA, API e PostgreSQL; credenciais seedadas apenas local/test/demo.
- Testes: unitários para domínio, API para RBAC/erros/idempotência, contract para adapters, E2E UJ-01..03; concorrência/locks/constraints/Flyway com PostgreSQL real (Testcontainers preferido; Compose como fallback); ArchUnit ou equivalente no build.
- Não introduzir Redis, Kafka, microsserviços, cache persistente Ticketmaster, mapa de assentos, gateway real, offline Gate, `LOW_AVAILABILITY`, rate limiting obrigatório, SSE/WebSocket ou demais itens fora do MVP.

### UX Design Requirements

- UX-DR-01: Implementar uma SPA responsiva com experiências distintas: Customer emocional/editorial, Organizer operacional e Gate utilitária/contrastante, sem inventar tokens visuais TBD.
- UX-DR-02: Implementar S01–S16 e seus estados obrigatórios: catálogo, detalhe, login, checkout/confirmação/tickets/share, gestão Organizer e seleção/scanner/resultado Gate.
- UX-DR-03: Permitir descoberta e seleção pública; ao reservar sem CUSTOMER, preservar só intenção segura, autenticar, revalidar no backend e retornar ao contexto; nenhuma retenção pré-login.
- UX-DR-04: Exibir disponibilidade, `SOLD_OUT` e `SALES_CLOSED` sem criar `LOW_AVAILABILITY`; não reduzir quantidade silenciosamente após revalidação.
- UX-DR-05: Implementar checkout com resumo persistente, timer normal/warning/critical/expired, anúncio somente nos marcos 3 min/1 min/expiração e recuperação de hold vigente.
- UX-DR-06: Implementar pagamento fake com aviso de demonstração, ações determinísticas, busy anti-reenvio e estado `verifying` que apenas consulta a tentativa existente.
- UX-DR-07: Implementar confirmação de compra e recuperação de carregamento de Tickets sem repetir Payment/Reservation.
- UX-DR-08: Implementar My Tickets, detalhe e compartilhamento permanente com Web Share progressivo e cópia como fallback; QR sempre tem código manual equivalente.
- UX-DR-09: Implementar superfícies Organizer com busca Ticketmaster, DRAFT, checklist de publicação, campos publicados read-only explicados, setores e confirmação de exclusões permitidas.
- UX-DR-10: Implementar Gate mobile/tablet-first: selecionar Event, câmera/manual, bloqueio de capturas simultâneas, quatro resultados dominantes e “Validar próximo” que mantém Event/modo e limpa o estado anterior.
- UX-DR-11: Tratar câmera negada/ausente e QR ilegível com fallback manual; perda de rede bloqueia toda validação e comunica que nada foi consumido.
- UX-DR-12: Oferecer loading, vazio, erro contextual e retry seguro por superfície, preservando entrada/contexto e sem stack/token.
- UX-DR-13: Implementar semântica por rota: `lang=pt-BR`, título único, landmarks, h1, skip link, foco após rota e teclado completo.
- UX-DR-14: Implementar formulários acessíveis: labels, help/error ligados, fieldset/legend, resumo navegável de erros, valores bloqueados ainda legíveis e foco no erro.
- UX-DR-15: Implementar diálogo/drawer/toast/tabela com contratos de foco, Escape, retorno ao acionador, anúncio apropriado e reflow/scroll nomeado.
- UX-DR-16: Implementar live regions com prioridade e deduplicação; Gate foca/anuncia o heading uma vez; estados críticos persistem inline e nunca dependem apenas de toast/cor/som.
- UX-DR-17: Respeitar `prefers-reduced-motion`, alvo mínimo 44×44, zoom 200%, 320 CSS px, reflow 400%, text spacing e layout sem rolagem bidimensional de página.
- UX-DR-18: Implementar matriz responsiva aprovada: catálogo 1/2/3–4 colunas, detalhe empilhado/duas regiões, checkout resumo antes de pagamento, Organizer compacto/pilha/sidebar e Gate central/câmera-first.
- UX-DR-19: Usar os componentes comportamentais canônicos de `EXPERIENCE.md`; definir/consumir tokens, paleta, tipografia, spacing, raios e mockups somente após aprovação do Design System.

### FR Coverage Map

- FR-01–04 → Epic 1 — Acesso seguro às contas de demonstração.
- FR-05–19 → Epic 2 — Organizer cria e publica eventos vendáveis.
- FR-20–23 → Epic 3 — Visitante descobre eventos e escolhe uma intenção de compra.
- FR-24–31 → Epic 4 — Customer garante ingressos temporariamente com segurança.
- FR-32–37 → Epic 5 — Customer conclui o pagamento e recebe a emissão correta.
- FR-38–42 → Epic 6 — Customer acessa e compartilha seus ingressos.
- FR-43–52 → Epic 7 — Gate valida a entrada com certeza operacional.
- FR-53 → Epic 8 — Avaliador executa e verifica o MVP de forma autônoma.

## Epic List

### Epic 1: Acesso seguro às contas de demonstração

Organizer, Customer e Gate autenticam-se em contas provisionadas, recebem somente as permissões do seu papel e podem encerrar sessão para trocar de conta com segurança.

**FRs covered:** FR-01, FR-02, FR-03, FR-04.

### Epic 2: Organizer cria e publica eventos vendáveis

Organizer usa a Ticketmaster como referência para criar seu próprio Event, configura-o em rascunho, administra setores e publica/gerencia somente alterações permitidas.

**FRs covered:** FR-05 a FR-19.

### Epic 3: Visitante descobre eventos e escolhe uma intenção de compra

Visitantes autenticados ou não navegam, buscam e entendem Events publicados, seus setores, disponibilidade e condição de vendas antes de iniciar a reserva.

**FRs covered:** FR-20 a FR-23.

### Epic 4: Customer garante ingressos temporariamente com segurança

Customer autenticado cria ou recupera um hold de um setor, acompanha seu tempo e encontra proteção contra retry, expiração incorreta, disputa de estoque e overselling.

**FRs covered:** FR-24 a FR-31.

### Epic 5: Customer conclui o pagamento e recebe a emissão correta

Customer simula pagamento aprovado ou recusado, recupera-se de resposta incerta e recebe exatamente os Tickets emitidos para uma compra aprovada.

**FRs covered:** FR-32 a FR-37.

### Epic 6: Customer acessa e compartilha seus ingressos

Customer encontra seus Tickets, apresenta QR/código manual e compartilha um link público permanente sem expor seus dados pessoais.

**FRs covered:** FR-38 a FR-42.

### Epic 7: Gate valida a entrada com certeza operacional

Gate seleciona um Event, lê QR ou código manual e toma uma decisão inequívoca, online e de uso único para cada ingresso.

**FRs covered:** FR-43 a FR-52.

### Epic 8: Avaliador executa e verifica o MVP de forma autônoma

O avaliador encontra dados de demonstração, execução reproduzível, documentação e evidência de qualidade para percorrer o MVP ponta a ponta.

**FRs covered:** FR-53.

**Dependências:** Epic 1 → Epic 2 → Epic 3 → Epic 4 → Epic 5; a partir do Epic 5, Epic 6 e Epic 7 podem avançar de forma independente. Epic 8 consolida a entrega; migrations, execução mínima e suporte de teste surgem incrementalmente nas Stories anteriores.

## Epic 1: Acesso seguro às contas de demonstração

Organizer, Customer e Gate autenticam-se em contas provisionadas, recebem somente as permissões do seu papel e podem encerrar sessão para trocar de conta com segurança.

### Story 1.1: Disponibilizar a fundação executável e contas provisionadas

As an avaliador,
I want iniciar uma base local com contas provisionadas,
So that posso acessar os papéis do MVP sem cadastro público ou configuração manual de dados.

**Acceptance Criteria:**

**Given** um checkout limpo do projeto
**When** a configuração local aprovada é iniciada
**Then** frontend React, API Spring Boot e PostgreSQL ficam executáveis com as versões e estrutura modular definidas no Architecture Spine
**And** Flyway é o único dono do schema, Hibernate usa `ddl-auto=validate`, e a migration cria somente os dados de `User` necessários nesta etapa.

**Given** o ambiente local ou de teste
**When** os seeds são aplicados
**Then** existem credenciais documentáveis para ao menos um `ORGANIZER`, um `CUSTOMER` e um `GATE`
**And** não há cadastro público, gerenciamento de papéis pela UI, segredo versionado ou credencial demo em perfil `prod`.

### Story 1.2: Autenticar e encerrar sessão de contas provisionadas

As a usuário provisionado,
I want entrar e sair com minhas credenciais,
So that acesso a experiência correspondente ao meu papel e posso trocar de conta durante a avaliação.

**Acceptance Criteria:**

**Given** credenciais seedadas válidas
**When** o usuário envia o login
**Then** a API autentica senha BCrypt, cria JWT HS256 em cookie `HttpOnly` com TTL configurável padrão de 8 horas e expiração alinhada ao cookie
**And** a SPA recebe/renova o token CSRF necessário para mutações e apresenta o estado de sessão sem expor JWT ao JavaScript.

**Given** credenciais inválidas, sessão expirada ou logout solicitado
**When** a autenticação é processada
**Then** a resposta é segura e a UI associa a mensagem ao formulário, preserva acessibilidade e retorna ao estado não autenticado quando aplicável
**And** logout invalida a sessão local, renova CSRF para a SPA e limpa intenção pré-login permitida.

### Story 1.3: Aplicar RBAC e contratos HTTP de autenticação

As a usuário de cada papel,
I want que a API aceite somente minhas operações autorizadas,
So that controles de interface não sejam a única proteção do sistema.

**Acceptance Criteria:**

**Given** a API `/api/v1` e uma sessão autenticada
**When** uma rota protegida é chamada por papel incompatível ou sem sessão válida
**Then** o backend rejeita a operação sem executar seu efeito
**And** o erro segue o envelope aprovado `{code, message, fieldErrors?, traceId, timestamp}` sem detalhes internos.

**Given** contratos de login, logout, sessão e erros
**When** DTOs Java e tipos TypeScript são verificados no build
**Then** eles conformam ao OpenAPI versionado
**And** checks automatizados detectam drift de contrato.

**Given** matrizes de papel e acesso protegido
**When** testes de API são escritos antes da regra de autorização correspondente
**Then** comprovam acesso permitido, 401/403 seguro e ausência de efeito em operação proibida
**And** os mesmos contratos serão aplicados às operações de ownership introduzidas nos Epics de Event, Reservation e Ticket.

### Story 1.4: Estabelecer fronteiras modulares verificáveis

As a equipe de implementação,
I want fronteiras arquiteturais executáveis desde as primeiras capacidades,
So that os módulos futuros preservem isolamento, ownership e testabilidade.

**Acceptance Criteria:**

**Given** os módulos iniciais `auth` e `shared`
**When** o build é executado
**Then** testes arquiteturais impedem controller→repository direto, entidade JPA em DTO HTTP e acesso a package interno de outro módulo
**And** ports são usados somente nas fronteiras reais, sem interface cerimonial por classe.

**Given** regras críticas de autenticação
**When** testes são criados ou alterados
**Then** `Clock`, UUIDs e geradores relevantes são determinísticos/injetáveis quando necessário
**And** nenhum teste depende de `Thread.sleep`, ordem compartilhada ou dados fora do seu contexto.

## Epic 2: Organizer cria e publica eventos vendáveis

Organizer usa a Ticketmaster como referência para criar seu próprio Event, configura-o em rascunho, administra setores e publica/gerencia somente alterações permitidas.

### Story 2.1: Pesquisar referências Ticketmaster com recuperação segura

As an Organizer,
I want pesquisar referências de eventos no catálogo Ticketmaster,
So that começo a criação com dados úteis sem publicar diretamente no catálogo externo.

**Acceptance Criteria:**

**Given** um Organizer autenticado na busca de catálogo
**When** pesquisa uma referência
**Then** a API usa somente `CatalogProvider`/adapter backend e devolve título, imagem, descrição e categoria quando fornecidos
**And** a UI mostra busca, loading, resultados, vazio e ação “usar como referência” com anúncio acessível.

**Given** timeout, rede, 5xx ou 429 da Ticketmaster
**When** o budget total configurado de aproximadamente cinco segundos se encerra, com no máximo um retry permitido para falha transitória
**Then** a UI informa indisponibilidade e oferece retry preservando a busca
**And** não oferece criação manual, cache persistente, payload bruto ou segredo externo ao cliente.

### Story 2.2: Criar Event interno em rascunho a partir do snapshot

As an Organizer,
I want iniciar um Event `DRAFT` usando uma referência Ticketmaster,
So that posso configurar um evento próprio e independente.

**Acceptance Criteria:**

**Given** um resultado Ticketmaster selecionado por seu Organizer
**When** ele confirma o uso como referência
**Then** o sistema cria Event interno `DRAFT` com identidade própria e snapshot dos dados disponíveis
**And** permite reutilizar o mesmo `externalId`, inclusive pelo mesmo Organizer.

**Given** outro Organizer ou uma requisição adulterada
**When** tenta acessar ou alterar o DRAFT alheio
**Then** o backend retorna acesso proibido sem expor dados privados
**And** o frontend não trata ocultação de CTA como substituto da autorização.

### Story 2.3: Editar, listar e excluir Events em rascunho

As an Organizer,
I want configurar livremente meus rascunhos e excluí-los quando não servirem,
So that preparo somente eventos completos para publicação.

**Acceptance Criteria:**

**Given** Events próprios `DRAFT` e `PUBLISHED`
**When** o Organizer abre Meus Eventos
**Then** vê somente seus Events, com estado textual identificável e condição derivada de vendas quando aplicável
**And** loading, vazio, erro e retry preservam navegação acessível.

**Given** um Event `DRAFT` próprio
**When** o Organizer edita seus campos ou confirma a exclusão em diálogo acessível
**Then** os dados são salvos ou o Event é excluído e a navegação retorna à lista com foco/feedback coerentes
**And** Event `PUBLISHED` nunca é excluído ou cancelado.

### Story 2.4: Configurar TicketSectors do rascunho

As an Organizer,
I want criar e editar setores com capacidade e preço,
So that meu Event tenha inventário vendável antes da publicação.

**Acceptance Criteria:**

**Given** um Event `DRAFT` próprio
**When** o Organizer adiciona, edita ou remove TicketSectors
**Then** cada setor aceita nome, descrição opcional, capacidade maior que zero e preço BRL maior ou igual a zero
**And** o formulário associa labels, limites e erros ao campo, preservando valores válidos após falha.

**Given** setores configurados
**When** a interface lista ou edita seus dados
**Then** preço, capacidade e disponibilidade são apresentados sem inventar `LOW_AVAILABILITY`
**And** o backend mantém dinheiro em `BigDecimal` e não expõe entidades JPA no HTTP.

### Story 2.5: Revisar e publicar Event válido

As an Organizer,
I want revisar pendências antes de publicar,
So that não disponibilizo um Event incompleto para compra.

**Acceptance Criteria:**

**Given** um Event `DRAFT`
**When** o Organizer abre a revisão de publicação
**Then** `PublicationChecklist` identifica referência Ticketmaster, título, data futura, nome/endereço do local e ao menos um setor válido
**And** cada pendência leva por teclado/foco ao campo ou setor correspondente e bloqueia publicação.

**Given** todas as condições obrigatórias são satisfeitas
**When** o Organizer publica
**Then** o backend transiciona `DRAFT → PUBLISHED` uma única vez e a UI mostra sucesso apenas após resposta autoritativa
**And** ausência de descrição, imagem ou categoria não bloqueia publicação nem resulta em imagem quebrada, nulo técnico ou conteúdo inventado nas superfícies públicas.

### Story 2.6: Gerenciar Event publicado sem quebrar compromissos existentes

As an Organizer,
I want atualizar somente dados e setores permitidos após publicar,
So that informações essenciais para compradores permaneçam confiáveis.

**Acceptance Criteria:**

**Given** um Event `PUBLISHED` próprio
**When** o Organizer abre o editor
**Then** `title`, `venueName`, `venueAddress`, `startsAt`, `externalSource` e `externalId` permanecem visíveis, legíveis e bloqueados com motivo
**And** `description`, `imageUrl` e `category` podem ser alterados.

**Given** um TicketSector publicado
**When** o Organizer altera preço ou capacidade
**Then** preço novo não modifica snapshots de Reservations existentes
**And** a alteração usa `PESSIMISTIC_WRITE`, exige `newCapacity >= committed` e define `newAvailableQuantity = newCapacity - committed`.

**Given** TicketSector com qualquer Reservation ou Ticket associado
**When** o Organizer tenta removê-lo ou reduzir capacidade abaixo do comprometido
**Then** o backend impede a mutação e informa a razão sem perder os demais valores
**And** testes TDD com PostgreSQL real cobrem constraints, lock e invariante `0 <= availableQuantity <= capacity`.

## Epic 3: Visitante descobre eventos e escolhe uma intenção de compra

Visitantes autenticados ou não navegam, buscam e entendem Events publicados, seus setores, disponibilidade e condição de vendas antes de iniciar a reserva.

### Story 3.1: Navegar e buscar catálogo público de Events publicados

As a visitante,
I want encontrar Events publicados por navegação ou título,
So that descubro rapidamente o que está disponível.

**Acceptance Criteria:**

**Given** Events em estados e ownerships variados
**When** visitante autenticado ou não abre o catálogo ou busca por título
**Then** vê somente `PUBLISHED` com imagem/fallback, título, data, local e `startingPrice = MIN(TicketSector.price)`
**And** busca simples possui estados loading, resultados, vazio e erro compreensíveis, sem filtros avançados.

**Given** a superfície pública em desktop ou mobile
**When** navegação, foco, zoom/reflow e leitor de tela são usados
**Then** catálogo mantém semântica, h1/landmarks, busca operável por teclado e feedback não dependente apenas de cor
**And** a composição segue o comportamento responsivo aprovado, sem inventar breakpoints/tokens visuais.

### Story 3.2: Consultar detalhe público e formar intenção de compra

As a visitante,
I want entender setores, preços, disponibilidade e vendas de um Event,
So that escolho uma quantidade conscientemente antes de reservar.

**Acceptance Criteria:**

**Given** um Event `PUBLISHED`
**When** visitante abre seu detalhe
**Then** vê conteúdo disponível/fallback, data/hora/local, TicketSectors, preço e disponibilidade autoritativa
**And** `QuantityStepper` restringe a intenção a 1–6 e ao estoque conhecido sem prometer garantia.

**Given** Event sem vendas abertas, setor esgotado ou Event inexistente
**When** o detalhe é consultado
**Then** a UI comunica `SALES_CLOSED`, `SOLD_OUT` ou estado de erro contextual e preserva a consulta pública quando aplicável
**And** `serverNow >= startsAt` é decidido no backend, datas/horas são apresentadas em `America/Sao_Paulo`, e não há estado persistido novo ou `LOW_AVAILABILITY`.

## Epic 4: Customer garante ingressos temporariamente com segurança

Customer autenticado cria ou recupera um hold de um setor, acompanha seu tempo e encontra proteção contra retry, expiração incorreta, disputa de estoque e overselling.

### Story 4.1: Restaurar intenção após login CUSTOMER e criar hold válido

As a visitante que quer comprar,
I want entrar como CUSTOMER antes de reservar e retornar à minha seleção,
So that o sistema valide estoque e vendas sem criar reserva anônima.

**Acceptance Criteria:**

**Given** visitante selecionou Event, setor e quantidade válidos no detalhe
**When** aciona Reservar sem sessão CUSTOMER ou com papel incompatível
**Then** a SPA guarda somente `eventId`, `ticketSectorId`, `quantity`, rota interna permitida e `createdAt` em `sessionStorage` por no máximo 15 minutos
**And** encaminha ao login com mensagem de que disponibilidade será revalidada, sem criar Reservation, preço ou retenção de estoque.

**Given** login CUSTOMER bem-sucedido com intenção ainda válida
**When** a intenção é restaurada
**Then** o backend revalida Event publicado, vendas abertas, setor, quantidade 1–6 e disponibilidade antes de criar `HOLDING`
**And** se falhar, a UI retorna ao setor, explica a mudança, não reduz quantidade silenciosamente e exige nova confirmação.

### Story 4.2: Criar Reservation atômica com preço capturado

As a Customer,
I want garantir temporariamente a quantidade escolhida de um setor,
So that tenho dez minutos para concluir a compra com valor definido.

**Acceptance Criteria:**

**Given** Customer autenticado, vendas abertas e setor com estoque suficiente
**When** cria uma Reservation
**Then** `reservations` bloqueia Customer por port de `auth`, aplica ordem `Customer → Reservation → TicketSector`, cria `HOLDING` e decrementa disponibilidade atomicamente
**And** fixa `expiresAt = serverNow + 10 minutos` exatos, sem pausa, reinício ou extensão.

**Given** a Reservation criada
**When** o checkout a recebe
**Then** `unitPrice` e `totalAmount` são snapshots calculados pelo backend em BRL e a response inclui `serverNow`, `expiresAt` e status
**And** testes TDD comprovam tempo determinístico via `Clock`, setor/evento inválido, vendas fechadas, quantidade inválida e preço não controlado pelo frontend.

### Story 4.3: Proteger retry, hold vigente e overselling

As a Customer,
I want que retry ou clique duplo não me cobre inventário duas vezes,
So that a reserva continue confiável mesmo sob falha de rede ou concorrência.

**Acceptance Criteria:**

**Given** uma criação de Reservation com `Idempotency-Key`
**When** a mesma chave e intenção canônica são repetidas, inclusive concorrentemente
**Then** o backend retorna a mesma Reservation sem nova baixa de estoque
**And** mesma chave com payload incompatível retorna `IDEMPOTENCY_CONFLICT`.

**Given** Customer possui `HOLDING` vigente no mesmo Event ou várias requisições disputam o último estoque
**When** cria nova intenção
**Then** a HOLDING existente é recuperada/direcionada e somente requisições que cabem no estoque criam hold
**And** testes TDD com PostgreSQL real demonstram efeito único, locks/constraints e `availableQuantity` nunca negativa.

### Story 4.4: Expirar e reconciliar holds sem falsa escassez

As a Customer,
I want que um hold vencido devolva inventário uma única vez,
So that posso tentar novamente sem ficar bloqueado por limpeza atrasada.

**Acceptance Criteria:**

**Given** `serverNow >= expiresAt` para Reservation `HOLDING`
**When** scheduler ou operação crítica a reconcilia
**Then** a Reservation torna-se `EXPIRED` e devolve a quantidade exatamente uma vez
**And** ela nunca autoriza pagamento ou impede novo hold apenas por ainda estar persistida como `HOLDING`.

**Given** criação de novo hold pode exigir expirar holds anteriores, inclusive de outro setor
**When** a reconciliação lazy executa
**Then** bloqueia Reservations por UUID e TicketSectors distintos por UUID antes de revalidar estoque
**And** scheduler de 30 s é apenas cleanup, com testes PostgreSQL reais cobrindo repetição e ausência de falsa escassez.

### Story 4.5: Exibir checkout e timer autoritativo do hold

As a Customer,
I want acompanhar claramente minha Reservation enquanto ela está vigente,
So that sei o que está reservado, por quanto tempo e como continuar.

**Acceptance Criteria:**

**Given** uma `HOLDING` vigente
**When** Customer abre, recarrega ou retorna ao checkout
**Then** vê Event, setor, quantidade, preços snapshot, total e timer normal/warning/critical derivado de `serverNow`/`expiresAt`
**And** a UI usa `performance.now()` somente para apresentação e reconcilia no refresh, visibilidade, zero, erro e antes de pagar.

**Given** Customer tem hold vigente ou hold expirado
**When** navega nas superfícies Customer
**Then** acesso “Continuar reserva” leva ao checkout existente sem criar novo hold; expiração remove ações de pagamento, foca mensagem persistente e oferece retorno ao Event
**And** timer anuncia apenas 3 min, 1 min e expiração, nunca a cada segundo nem por cor isolada.

## Epic 5: Customer conclui o pagamento e recebe a emissão correta

Customer simula pagamento aprovado ou recusado, recupera-se de resposta incerta e recebe exatamente os Tickets emitidos para uma compra aprovada.

### Story 5.1: Processar tentativa simulada recusada de forma idempotente

As a Customer,
I want simular uma tentativa `DECLINED` e tentar novamente enquanto há tempo,
So that consigo demonstrar a recuperação sem perder meu hold.

**Acceptance Criteria:**

**Given** Customer proprietário de Reservation `HOLDING` vigente
**When** escolhe explicitamente `DECLINED` com novo `paymentAttemptId`
**Then** backend bloqueia/revalida Reservation, persiste Payment com horário, resultado, valor snapshot e provider fake, sem alterar a Reservation
**And** a UI mantém resumo/timer, exibe aviso de demonstração e permite nova tentativa somente enquanto vigente.

**Given** retry concorrente do mesmo `paymentAttemptId`
**When** fingerprint v1 canônico `(customerId da sessão, reservationId, simulatedOutcome)` coincide
**Then** claim/processamento/result persistido são atômicos e a resposta reproduz o resultado original
**And** fingerprint divergente retorna conflito, sem duplicar tentativa ou efeito, conforme testes TDD de replay, conflito e concorrência.

### Story 5.2: Aprovar pagamento, confirmar Reservation e emitir Tickets atomicamente

As a Customer,
I want que aprovação simulada conclua minha compra uma única vez,
So that recebo exatamente os ingressos que reservei.

**Acceptance Criteria:**

**Given** Customer proprietário de `HOLDING` vigente e tentativa `APPROVED`
**When** o backend processa a tentativa
**Then** na mesma transação bloqueia/revalida Reservation, persiste Payment aprovado, muda Reservation para `CONFIRMED` e emite exatamente `quantity` Tickets
**And** cada Ticket possui ordinal único por Reservation, identificadores seguros e nenhum Ticket é emitido para Reservation não confirmada.

**Given** aprovação concorre com expiração, reprocessamento ou retry
**When** transações são executadas
**Then** somente `CONFIRMED` ou `EXPIRED` vence e replays não emitem Tickets adicionais
**And** testes TDD com PostgreSQL real cobrem corrida payment×expiration, confirmação única, emissão exata e constraints de ordinal.

### Story 5.3: Reconciliar resposta de pagamento perdida sem nova cobrança

As a Customer,
I want verificar uma tentativa cujo response se perdeu,
So that não inicio pagamento duplicado por incerteza de rede.

**Acceptance Criteria:**

**Given** a SPA enviou `paymentAttemptId` conhecido e não recebeu resposta
**When** checkout entra em `verifying`
**Then** consulta a tentativa/Reservation existente com o mesmo ID e nunca envia nova tentativa automaticamente
**And** mantém contexto, resumo e bloqueio de novo pagamento durante a verificação.

**Given** a consulta autoritativa resolve ou falha temporariamente
**When** o resultado é retornado
**Then** UI segue somente para `CONFIRMED`/sucesso, `HOLDING`+`DECLINED`, `EXPIRED` ou “Verificar novamente”
**And** sucesso não volta a oferecer pagamento, e falha de consulta não inventa estado de domínio novo.

## Epic 6: Customer acessa e compartilha seus ingressos

Customer encontra seus Tickets, apresenta QR/código manual e compartilha um link público permanente sem expor seus dados pessoais.

### Story 6.1: Gerar e proteger credenciais reexibíveis de Ticket

As a Customer,
I want credenciais de ingresso seguras e legíveis,
So that posso apresentá-las sem que sejam previsíveis ou confundidas entre si.

**Acceptance Criteria:**

**Given** emissão de Ticket confirmada
**When** suas credenciais são geradas
**Then** `validationToken` e `shareToken` usam CSPRNG, alta entropia e unicidade, e possuem responsabilidades distintas
**And** `manualCode` usa 10 caracteres Crockford-like sem ambiguidade, é único na forma normalizada e é exibido agrupado sem mudar identidade/cópia.

**Given** logs, auditoria, erros ou respostas públicas
**When** Ticket é consultado, compartilhado ou validado
**Then** não há senha, JWT, token, QR payload, manualCode completo, share URL completa ou fingerprint exposto
**And** valores necessários para QR, código e link permanecem persistidos para reexibição autorizada.

### Story 6.2: Listar e abrir Tickets próprios

As a Customer,
I want acessar Meus Ingressos após uma compra confirmada,
So that encontro cada unidade emitida e suas informações de entrada.

**Acceptance Criteria:**

**Given** Tickets emitidos para Customers diferentes
**When** Customer abre Meus Ingressos ou detalhe de Ticket
**Then** vê somente seus Tickets, um por unidade, com título do Event, setor, data/hora, local, estado, QR, código manual e ação de compartilhamento
**And** loading, vazio, erro e retry são acessíveis e não induzem novo Payment/Reservation após compra confirmada.

**Given** Ticket `VALID` ou `USED`
**When** é apresentado na interface
**Then** estado textual precede a credencial quando usado e QR possui equivalente ManualCode selecionável/copiável
**And** payload de QR nunca vira alt text nem log de cliente.

### Story 6.3: Compartilhar Ticket em página pública permanente

As a Customer,
I want obter e copiar o link público de um Ticket,
So that outra pessoa possa apresentá-lo sem receber minha propriedade ou dados pessoais.

**Acceptance Criteria:**

**Given** Customer proprietário de Ticket
**When** usa Compartilhar
**Then** recebe/copia link permanente baseado em `shareToken`, sem regeneração, rotação, revogação ou transferência de ownership
**And** Web Share API é melhoria progressiva; cópia de link acessível é sempre fallback.

**Given** visitante abre link válido, usado ou inválido
**When** a página pública é carregada
**Then** link válido/used mostra somente Event, data/hora, local, setor, estado, QR e código manual, sem PII do Customer
**And** `USED` permanece acessível e não permite entrada; link inválido/não encontrado retorna estado neutro sem revelar token/dados.

## Epic 7: Gate valida a entrada com certeza operacional

Gate seleciona um Event, lê QR ou código manual e toma uma decisão inequívoca, online e de uso único para cada ingresso.

### Story 7.1: Selecionar contexto de trabalho da Gate

As a operador Gate,
I want selecionar o Event publicado antes de validar,
So that cada ingresso seja conferido contra a entrada correta.

**Acceptance Criteria:**

**Given** usuário com papel `GATE`
**When** abre a área Gate
**Then** pode listar e selecionar qualquer Event `PUBLISHED`, sem associação Gate↔Event no MVP
**And** seleção, loading, vazio/erro e retry são operáveis por teclado e anunciam o Event atual antes de avançar.

**Given** sessão sem papel GATE ou Event não publicado
**When** tenta acessar ou selecionar
**Then** backend recusa sem iniciar validação
**And** frontend preserva navegação mínima e não oferece capacidade offline.

### Story 7.2: Validar código manual com resultado atômico e auditável

As a operador Gate,
I want validar um código manual de Ticket no Event selecionado,
So that tomo a decisão de entrada correta mesmo sem câmera.

**Acceptance Criteria:**

**Given** Gate, Event selecionado e código manual submetido online
**When** a validação é processada
**Then** retorna outcome funcional `VALID`, `INVALID`, `ALREADY_USED` ou `WRONG_EVENT`, nunca como erro HTTP de negócio
**And** `WRONG_EVENT` é decidido antes de consumo e não altera Ticket.

**Given** Ticket `VALID` do Event selecionado, usado, de Event diferente ou credencial desconhecida
**When** Gate valida
**Then** somente o primeiro caso faz `VALID → USED` com `usedAt`/`usedByGateUserId`; usado retorna `ALREADY_USED`; diferente retorna `WRONG_EVENT`; desconhecido retorna `INVALID`
**And** nenhuma resposta vaza token, código ou detalhe interno.

### Story 7.3: Garantir replay e concorrência seguros da validação

As a operador Gate,
I want que falhas e tentativas simultâneas preservem o resultado original,
So that um ingresso nunca seja consumido duas vezes nem pareça inconsistente.

**Acceptance Criteria:**

**Given** frontend envia `validationAttemptId` UUID e entrada por método manual ou QR
**When** backend recebe a tentativa
**Then** cria/claim `ValidationAttempt` único, com fingerprint v1 de `(gateUserId da sessão, selectedEventId, method, digest da credencial normalizada)`
**And** nunca persiste a credencial original, QR payload ou manualCode completo.

**Given** mesmo ID com fingerprint igual, fingerprint diferente ou chamadas concorrentes
**When** a tentativa é resolvida
**Then** igual retorna resultado persistido original, inclusive `VALID`; diferente retorna conflito explícito; concorrência executa no máximo um efeito
**And** claim, resultado, auditoria mínima e eventual Ticket `VALID → USED` fazem commit na mesma transação PostgreSQL.

**Given** regras críticas de Gate
**When** a suíte é executada em PostgreSQL real
**Then** cobre replay `VALID`, dupla validação com no máximo um `VALID`, `WRONG_EVENT` sem consumo, `INVALID` e redaction de auditoria
**And** o registro contém attemptId, Gate, Event, Ticket quando identificado, método, resultado e `processedAt`.

### Story 7.4: Operar câmera QR e resultado Gate em interface acessível

As a operador Gate,
I want usar câmera como fluxo principal e código manual como fallback,
So that valido rapidamente em mobile/tablet e continuo operando quando a câmera falha.

**Acceptance Criteria:**

**Given** Gate com Event selecionado em browser suportado e contexto seguro
**When** inicia scanner
**Then** `QrDecoder` usa caminho de câmera compatível, prefere traseira quando possível, permite seleção de dispositivo quando suportada e pausa decoding após leitura até resultado/“Validar próximo”
**And** tracks são encerrados ao sair da rota e retorno de aba oculta restaura estado com segurança.

**Given** permissão negada, câmera ausente, QR ilegível ou scanner indisponível
**When** Gate tenta capturar
**Then** recebe instrução textual e acesso imediato ao formulário manual
**And** alternar modo não valida, não perde Event selecionado e não inicia nova tentativa até submissão explícita.

**Given** resultado autoritativo ou rede/backend indisponível
**When** a tela responde
**Then** Gate apresenta texto, ícone, tratamento e instrução inequívocos, move foco ao heading e anuncia uma frase uma vez, com meta de até 1 s em condição normal
**And** sem rede bloqueia QR e manual, informa que nenhum Ticket foi consumido, permite retry online e nunca enfileira/sincroniza offline.

## Epic 8: Avaliador executa e verifica o MVP de forma autônoma

O avaliador encontra dados de demonstração, execução reproduzível, documentação e evidência de qualidade para percorrer o MVP ponta a ponta.

### Story 8.1: Preparar dados e execução reproduzíveis para avaliação

As an avaliador,
I want subir o sistema e encontrar contas/dados de demonstração claros,
So that consigo executar os fluxos sem editar banco ou código.

**Acceptance Criteria:**

**Given** ambiente `local`, `test` ou `demo`
**When** Flyway aplica migrations e seeds
**Then** existem no mínimo um Organizer, dois Customers, um Gate e Event `PUBLISHED` com TicketSectors/estoque disponível
**And** dados adicionais necessários para demonstrar fluxos não substituem esses mínimos nem existem em `prod`.

**Given** avaliador segue o README
**When** executa `docker compose up --build` ou caminho documentado equivalente
**Then** SPA, API e PostgreSQL sobem na mesma origem lógica com `/api` encaminhado corretamente
**And** configuração falha cedo para secrets obrigatórios ausentes em demo/prod, sem versionar segredo.

### Story 8.2: Documentar avaliação, limitações e uso responsável de IA

As an avaliador,
I want instruções completas e honestas do projeto,
So that consigo configurá-lo, testá-lo e entender as escolhas feitas.

**Acceptance Criteria:**

**Given** README final
**When** avaliador o consulta
**Then** encontra banco, variáveis de ambiente, Ticketmaster API key, credenciais seedadas, execução, papéis, arquitetura, testes e passos para criação/publicação, APPROVED/DECLINED, share e quatro outcomes Gate
**And** encontra versões de browsers verificadas, limitações de Safari/câmera, funcionalidades incompletas e comportamentos divergentes conhecidos.

**Given** entrega para o desafio
**When** documentação é revisada
**Then** declara ferramentas IA usadas, partes assistidas, partes sem IA, processo/revisão humana, artefatos BMAD/contexto e escolhas UX/produto relevantes
**And** não promete deploy, opcional ou hardening que não foi implementado.

### Story 8.3: Verificar jornadas ponta a ponta e invariantes críticas

As a equipe de avaliação,
I want evidência automatizada dos fluxos e regras de maior risco,
So that o MVP seja demonstravelmente confiável além do happy path manual.

**Acceptance Criteria:**

**Given** a suíte de testes do projeto
**When** é executada antes da entrega
**Then** inclui E2E para UJ-01 Organizer, UJ-02 Customer e UJ-03 Gate com dados reproduzíveis
**And** inclui testes de API para RBAC/ownership/erros/idempotência, contract tests de adapters e checks OpenAPI/arquitetura.

**Given** regras de hold, estoque, Payment e Gate
**When** testes críticos são executados com PostgreSQL real (Testcontainers preferido; Compose aceitável como fallback)
**Then** comprovam hold de dez minutos, expiração única, overselling, replay/conflito de idempotência, corrida Payment×expiração, emissão exata, VALID único, replay VALID, WRONG_EVENT sem consumo e double-use
**And** nenhum banco em memória é usado como evidência para locks, constraints, Flyway ou concorrência.

**Given** happy paths de Organizer, Customer e Gate
**When** são percorridos por UI/E2E
**Then** não há HTTP 500 inesperado e operações assíncronas apresentam feedback até sucesso/falha
**And** são verificadas as metas de 2 segundos para ações comuns e 1 segundo para resultado Gate em condições normais, além de Chrome/Edge/Firefox atuais em desktop, Chrome Android e Safari iOS best effort; as Stories de UI verificam comportamento acessível aprovado, sem exigir tokens, paleta, tipografia, spacing, raios ou mockups high-fi ainda pendentes.
