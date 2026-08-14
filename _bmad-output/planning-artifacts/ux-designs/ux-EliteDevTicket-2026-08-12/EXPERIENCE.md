---
name: EliteDevTicket
status: draft
approval: APPROVED — architecture-ready
behavioral-contract: APPROVED
visual-design-system: PENDING
high-fidelity-mockups: PENDING
product-domain-blockers: none
sources:
  - docs/01-product/Desafio-Elite-Dev-2026.pdf
  - docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md
  - docs/04-ux/UX_DIRECTION_v0.1.md
  - _bmad-output/planning-artifacts/prds/prd-EliteDevTicket-2026-08-11/prd.md
created: 2026-08-12
updated: 2026-08-12
---

# EliteDevTicket — Experience Spine

Este é o contrato de comportamento, IA, estados, interação e acessibilidade. `DESIGN.md` é o contrato visual. Ambos prevalecem sobre mocks e wireframes.

## Foundation

Aplicação web responsiva única, com experiências distintas para visitante/Customer, Organizer e Gate. Desktop é prioritário para gestão; mobile é central para descoberta, acesso ao ingresso e Gate; tablet é suportado pela mesma composição responsiva. Gate exige conexão com o backend e não possui modo offline.

Princípios:

1. O usuário sabe onde está, o que pode fazer e o que acontecerá depois.
2. Estrutura antecede expressão.
3. Backend é autoridade para tempo, estoque, pagamento e validação.
4. Estado crítico nunca é comunicado apenas por cor nem apenas por toast.
5. Nenhuma interação visual altera invariantes de Domain.

Não há UI framework aprovado. Identidade e intensidade visual referenciam `DESIGN.md`.

### Glossário canônico

| Termo | Uso no contrato |
|---|---|
| `Event` | Entidade de evento da plataforma; “evento” é usado apenas em microcopy para pessoas. |
| `TicketSector` | Setor de venda do Event; “setor” é o rótulo de interface. |
| `Reservation` | Reserva de um Customer para um único TicketSector; `HOLDING`, `CONFIRMED` e `EXPIRED` são estados canônicos. |
| hold | Período temporário de dez minutos de uma Reservation `HOLDING`; não existe antes do login CUSTOMER. |
| `Payment` | Tentativa de pagamento fake; `APPROVED` e `DECLINED` são resultados de produto. |
| `Ticket` | Ingresso unitário emitido após aprovação; `VALID` e `USED` são estados canônicos. |
| Customer | Pessoa que descobre, reserva, paga e acessa Tickets; role `CUSTOMER` quando autenticada. |
| Organizer | Pessoa que cria e gerencia seus Events; role `ORGANIZER`. |
| Gate | Operador de portaria; role `GATE`. “Portaria” descreve a experiência, não uma entidade adicional. |

## Information Architecture

### Navegação pública

- **Eventos (`/`)**: catálogo de Events `PUBLISHED`, busca simples por nome.
- **Detalhe do evento (`/events/:id`)**: conteúdo, setores, quantidade e intenção de reserva.
- **Login (`/login`)**: autenticação de contas seedadas; aceita retorno seguro à intenção anterior.
- **Ingresso compartilhado (`/t/:shareToken`)**: evento, data/hora, local, setor, estado, QR e código manual, sem dados pessoais.

### Customer autenticado

- **Checkout (`/checkout/:reservationId`)**: Reservation `HOLDING`, timer, resumo e simulação de pagamento.
- **Confirmação de compra**: sucesso e acesso imediato aos ingressos.
- **Meus Ingressos (`/my-tickets`)**: lista/detalhe com título, setor, data/hora, local, estado, QR, código e compartilhamento.

### Organizer

- **Meus Eventos**: somente Events próprios, com `DRAFT`/`PUBLISHED` e vendas abertas/encerradas derivadas.
- **Criar Evento — catálogo Ticketmaster**: pesquisar, selecionar referência e iniciar snapshot interno.
- **Editor de Evento**: conteúdo, data/local e validações de publicação.
- **Setores**: nome, descrição opcional, capacidade, disponibilidade/comprometido e preço.

### Gate

- **Selecionar evento**: todos os Events `PUBLISHED`.
- **Validação**: câmera principal, código manual fallback e resultado.

### Fechamento de superfícies

Toda necessidade MUST possui superfície. Não entram no MVP: cadastro, gestão de roles, analytics, filtros avançados, mapa de assentos, cancelamento/refund, revenda, e-mail, offline, associação Gate↔Event ou criação manual sem Ticketmaster.

## Voice and Tone

- **Customer:** direto, confiante e energizante; urgência apenas factual. “Sua reserva está garantida por 09:42”, não “Corra!”.
- **Organizer:** funcional, instrutivo e preciso. “Título bloqueado após a publicação para proteger quem já comprou.”
- **Gate:** mínimo e imperativo. “Ingresso válido — pode entrar.”

Erros respondem: o que aconteceu, se afetou a compra/entrada, o estado atual e a próxima ação. Evitar “Algo deu errado”.

Microcopy canônica:

- Login pré-hold: “Entre para reservar. A disponibilidade será verificada novamente após o login.”
- Criação: “Garantindo seus ingressos…”
- Demo: “Ambiente de demonstração — nenhuma cobrança real será realizada.”
- Recusa: “Pagamento recusado. Seus ingressos continuam reservados por {tempo}. Você pode tentar novamente.”
- Resposta incerta: “Estamos verificando o resultado. Não tente pagar novamente enquanto consultamos sua reserva.”
- Expiração: “Sua reserva expirou e os ingressos voltaram ao estoque.”
- Gate sem rede: “Sem conexão. A validação exige internet. Verifique a rede e tente novamente.”

## Screen Inventory

Referência visual de arquitetura e cobertura: [IA S01–S16](wireframes/ia-elite-dev-ticket-2026-08-12.excalidraw). O wireframe ilustra agrupamento e transições; estes spines prevalecem em caso de conflito.

| ID | Tela/estado | Público/papel | Objetivo | Estados obrigatórios |
|---|---|---|---|---|
| S01 | Catálogo de eventos | Público | Descobrir/buscar | loading, resultados, vazio, erro |
| S02 | Detalhe do evento | Público | Entender e selecionar setor/quantidade | available, sold out, sales closed, conteúdo opcional ausente |
| S03 | Login | Público | Entrar em conta seedada | idle, submitting, credenciais inválidas, sessão expirada, retorno à intenção |
| S04 | Checkout | CUSTOMER | Concluir hold vigente | normal, warning, critical, declined, verifying, expired |
| S05 | Confirmação | CUSTOMER | Confirmar sucesso e orientar | confirmado, carregando tickets, recuperação |
| S06 | Meus Ingressos | CUSTOMER | Encontrar ingressos | loading, vazio, lista, erro |
| S07 | Detalhe/QR do ingresso | CUSTOMER | Apresentar/compartilhar | VALID, USED, Web Share indisponível → copiar link |
| S08 | Ingresso compartilhado | Público | Apresentar ingresso ao portador | VALID, USED, inválido/não encontrado |
| S09 | Meus Eventos | ORGANIZER | Listar e iniciar gestão | loading, vazio, DRAFT, PUBLISHED, sales closed |
| S10 | Busca Ticketmaster | ORGANIZER | Selecionar referência | inicial, buscando, resultados, vazio, indisponível |
| S11 | Editor de evento | ORGANIZER | Criar/editar dados | draft editável, published parcialmente bloqueado, validações |
| S12 | Gestão de setores | ORGANIZER | Criar/editar/remover setores | vazio, válido, erro, associado/não removível |
| S13 | Revisão/publicação | ORGANIZER | Ver pendências e publicar | pendências, ready, publishing, success/failure |
| S14 | Seleção de evento Gate | GATE | Definir contexto | loading, lista, vazio/erro |
| S15 | Scanner Gate | GATE | Capturar QR/manual | permissão, ativa, negada, indisponível, validando, sem rede |
| S16 | Resultado Gate | GATE | Decidir entrada | VALID, INVALID, ALREADY_USED, WRONG_EVENT |

## Component Patterns

### Eventos e disponibilidade

`EventCard` sempre exibe imagem/fallback, título, data, local e preço inicial calculado como `MIN(TicketSector.price)`. A S02 mantém o status de vendas junto ao CTA. `TicketSectorCard` exibe nome, preço e disponibilidade numérica ou estado autoritativo; `LOW_AVAILABILITY` não entra enquanto não houver limiar aprovado.

### Seleção de quantidade

`QuantityStepper` limita 1–6 e respeita estoque disponível. Alterações atualizam resumo estimado, mas não garantem estoque. “Reservar” desabilita enquanto a intenção é submetida; retry/double-click nunca deve parecer criar outro hold.

### Timer e reserva ativa

Antes de “Reservar”, a interface informa que, após autenticação e confirmação de disponibilidade, o hold terá duração fixa de 10 minutos. `ReservationTimer` usa `expiresAt` autoritativo: normal 10:00–03:00, warning 02:59–01:00, critical 00:59–00:00, expired quando confirmado pelo backend. O tempo restante é texto programaticamente determinável, mas seu nome acessível não muda a cada segundo; anuncia uma única vez os marcos de 3 minutos, 1 minuto e expiração. Não pausa, reinicia nem estende, pois o limite é essencial à regra de estoque. Se Customer possuir uma HOLDING vigente no mesmo Event, a interface recupera/direciona à reserva existente.

Ao expirar, a resposta autoritativa substitui as ações de pagamento, preserva o contexto e move o foco para uma mensagem persistente com o próximo passo. Ao retornar de refresh ou aba suspensa, a UI reconcilia o estado e anuncia somente mudança significativa; o mecanismo permanece handoff para Architecture.

### Formulários

Labels persistentes; obrigatoriedade e invalidade programáticas, sem depender de asterisco ou cor; ajuda, unidade, formato, limites e erro associados ao controle. Campos relacionados usam `fieldset`/`legend` ou semântica equivalente. Ao tentar publicar, um resumo no topo recebe foco, lista cada problema como link para o campo ou setor correspondente e permite seguir ao primeiro erro, sem apagar entradas válidas.

Campos estruturais publicados permanecem visíveis e explicam o bloqueio. Quando o valor precisa continuar alcançável, usar texto estático ou `readonly`; um controle `disabled` nunca é a única fonte do valor ou da explicação.

### Login e sessão

Login usa labels persistentes, identificação programática e `autocomplete="username"`/`"current-password"`. Revelar senha é botão com nome e estado acessíveis; aviso de Caps Lock, se existir, é ajuda não bloqueante. Falha de autenticação é associada ao formulário, anunciada e não revela qual credencial falhou.

Depois do login pré-hold, a intenção segura é restaurada, o backend revalida vendas/estoque e o foco segue para o contexto restaurado. Sessão expirada explica a necessidade de entrar novamente e preserva somente dados não sensíveis já previstos; o contrato não promete extensão de sessão.

### Pagamento

`PaymentSimulationControl` oferece escolha explícita e determinística entre `APPROVED` e `DECLINED`; o backend calcula o valor. Durante o processamento, mantém o resumo e impede o reenvio. Em resposta perdida, entra em `verifying` e consulta o estado autoritativo sem iniciar cobrança automática. As saídas observáveis são completas e mutuamente exclusivas: `CONFIRMED` leva a S05; `HOLDING` com tentativa `DECLINED` retorna a S04 com nova tentativa apenas enquanto vigente; `EXPIRED` leva ao estado expirado de S04; se a consulta estiver temporariamente indisponível, a tela preserva o contexto, continua bloqueando novo pagamento e oferece “Verificar novamente”. O mecanismo de consulta permanece em Architecture.

### Confirmação e acesso aos ingressos

S05 confirma a compra independentemente do carregamento visual dos Tickets e nunca volta a exibir uma ação de pagamento. Se os Tickets não carregarem, a mensagem mantém explícito que a compra foi confirmada e oferece “Tentar carregar ingressos novamente” e “Ir para Meus Ingressos”; ambas consultam os Tickets já emitidos, sem recriar pagamento ou Reservation.

### Tickets e compartilhamento

`TicketCard` expõe dados mínimos e um Ticket por unidade emitida. Compartilhar significa obter/copiar link permanente; não gera, rotaciona, revoga nem transfere ownership. Se API de compartilhamento do navegador não existir/falhar, copiar link é fallback. Ticket `USED` e link público permanecem acessíveis, mostram USED antes de QR/código na ordem de leitura e não autorizam nova entrada.

O QR tem nome curto (“QR do ingresso”) quando participa da tarefa ou é ocultado de tecnologia assistiva quando o painel já possui nome inequívoco; o payload técnico nunca vira texto alternativo. `ManualCode` expõe valor selecionável, nome acessível e ação “Copiar código”. Agrupamento visual não altera o valor lido nem copiado.

### Scanner e resultado

Scanner pede permissão no contexto da tarefa, exibe câmera ativa e oferece “Digitar código” sempre, inclusive com permissão negada, câmera ausente ou rede indisponível. Alternar entre câmera e código manual não valida, não perde o Event selecionado e mantém inequívoco qual modo está ativo. A submissão manual exige ação explícita. Após decodificar/submeter, ambos os modos interrompem captura e impedem validações sobrepostas, com meta de resposta em até 1 s.

O resultado ocupa a área principal, inclui texto, ícone e instrução e não depende de cor, forma ou som. Após resposta autoritativa, o heading do painel (`tabindex="-1"`) recebe foco e uma única frase completa anuncia estado e instrução. `VALID`, `INVALID`, `ALREADY_USED` e `WRONG_EVENT` mantêm rótulos estáveis. “Validar próximo” mantém o Event selecionado, limpa código e resultado anteriores e devolve foco ao scanner ou ao campo manual conforme o método usado; não cria comportamento offline.

### Catálogo comportamental canônico

Os nomes abaixo correspondem ao catálogo visual de `DESIGN.md`. Estados visuais pertencem ao spine visual; esta tabela fixa uso, interação e acessibilidade. Até uma biblioteca de UI ser aprovada, nenhum item é considerado herdado.

| Componente | Uso e comportamento |
|---|---|
| `Button` | Aciona operação; desabilita durante submissão crítica, preserva rótulo e não substitui idempotência backend. |
| `IconButton` | Ação compacta com accessible name; tooltip é apenas complementar. |
| `Link` | Navegação ativável por teclado; foco visível e token sensível nunca registrado em log. |
| `Input` | Label persistente, help/error associado e valor preservado após erro recuperável. |
| `PasswordInput` | Herda `Input`; revelar/ocultar anuncia estado e não move foco. |
| `SearchInput` | Busca por botão ou Enter; loading/resultados/vazio/erro são anunciados. |
| `Select` | Seleção única por teclado/touch; expõe expanded, selected e valor atual. |
| `Textarea` | Herda `Input`; não perde conteúdo em validação ou resize. |
| `FieldMessage` | Help/error ligado ao campo; error summary leva ao primeiro erro de publicação. |
| `ChoiceControl` | Checkbox/radio semântico; label amplia alvo e Space altera estado. |
| `Badge` | Metadado não interativo; não recebe foco salvo se contiver ação explícita. |
| `StatusBadge` | Estado textual; mudança crítica também aparece em região persistente. |
| `Divider` | Separação visual; headings/regions mantêm o agrupamento semântico. |
| `Dialog` | Confirma exclusão permitida; contém foco, Escape fecha e foco retorna ao acionador. |
| `Sheet` | Navegação/controles em telas estreitas; close/Escape e retorno de foco obrigatórios. |
| `Tooltip` | Explicação breve em hover/focus; nunca contém informação essencial. |
| `Toast` | Feedback transitório complementar; informação crítica permanece inline. |
| `Alert` | Estado persistente com impacto e próxima ação. |
| `Skeleton` | Carregamento inicial estrutural, oculto da árvore acessível; status textual quando necessário. |
| `Spinner` | Busy localizado; controle/região expõe estado ocupado e impede reenvio. |
| `EmptyState` | Explica ausência e oferece somente CTA já autorizado pelo papel/MVP. |
| `ErrorState` | Preserva contexto e oferece retry seguro; não vaza stack trace/token. |
| `ConnectivityBanner` | Comunica perda/restauração; no Gate bloqueia validação, sem sugerir fila offline. |
| `Breadcrumb` | Navegação hierárquica; item atual usa `aria-current="page"`. |
| `Navbar` | Rotas públicas/Customer, estado da sessão e logout operáveis por teclado. |
| `OrganizerSidebar` | Rotas Organizer; item atual exposto e collapse preserva accessible names. |
| `EventCard` | Abre detalhe; conteúdo mínimo sempre visível e starting price vem do menor setor. |
| `EventHero` | Introduz Event sem colocar controle essencial somente sobre imagem. |
| `EventMetadata` | Agrupa data/hora/local como texto semanticamente legível. |
| `EventStatus` | Mostra estado persistido e condição de vendas separadamente. |
| `TicketmasterResultCard` | Seleção inicia snapshot interno e explica que não publica na Ticketmaster. |
| `EventForm` | DRAFT editável; PUBLISHED mantém campos estruturais visíveis/read-only e explicados. |
| `PublicationChecklist` | Lista pendências; item foca o campo/setor correspondente. |
| `TicketSectorCard` | Seleção Customer ou gestão Organizer conforme contexto, sem misturar papéis. |
| `SectorEditor` | Rejeita capacidade abaixo do comprometido e remoção com associação. |
| `AvailabilityStatus` | Exibe available/sold out/sales closed; `LOW_AVAILABILITY` não é apresentado. |
| `QuantityStepper` | Valor 1–6 limitado pelo estoque conhecido; seleção ainda não garante estoque. |
| `PriceSummary` | Estimativa local; backend continua autoridade de `Reservation.totalAmount`. |
| `CheckoutSummary` | Mantém Event/setor/quantidade/preços durante processing, declined e verifying. |
| `ReservationTimer` | Deriva de `expiresAt`; anuncia 3 min, 1 min e expiração; nunca reinicia. |
| `ActiveReservationBanner` | Leva à HOLDING vigente do Customer/Event; não cria outra Reservation. |
| `DemoEnvironmentNotice` | Persistente antes da simulação; nenhuma cobrança real. |
| `PaymentSimulationControl` | Escolha APPROVED/DECLINED; não edita valor e bloqueia reenvio enquanto busy/verifying. |
| `PaymentResult` | Declined mantém HOLDING vigente; approved só após autoridade backend. |
| `PurchaseSuccess` | Confirma compra e leva aos Tickets; recovery não duplica emissão. |
| `TicketCard` | Um por Ticket; abre detalhe, permite share e permanece acessível em USED. |
| `QRCodePanel` | QR com alternativa `ManualCode`; token nunca é exposto em log. |
| `ManualCode` | Selecionável/copiável e anunciado em grupos compreensíveis. |
| `ShareAction` | Obtém/copia link permanente; Web Share é melhoria progressiva e a cópia do link é o fallback. |
| `ScannerFrame` | Após decode bloqueia leituras sobrepostas até resultado/próximo. |
| `CameraPermissionState` | Explica denied/unavailable e leva ao `ManualValidationForm`. |
| `ManualValidationForm` | Enter valida uma vez; busy impede submissão sobreposta. |
| `GateEventSelector` | Seleciona Event PUBLISHED antes da validação; Event atual permanece visível. |
| `GateResult` | Recebe foco/anúncio, distingue quatro resultados e oferece “Validar próximo”. |

## State Patterns

### Loading

Skeleton apenas onde preserva estrutura; operações críticas usam rótulos: “Garantindo…”, “Processando…”, “Validando…”. Nunca antecipar sucesso. Contexto e total permanecem visíveis. A região em processamento expõe estado ocupado e rótulo contextual sem substituir o controle focado.

### Empty

Mensagens específicas: “Nenhum evento encontrado”, “Nenhum ingresso ainda”, “Nenhum evento criado”, “Nenhum setor criado”. CTAs aparecem apenas quando já fazem parte do MVP e do papel.

### Erro e recuperação

- Erro recuperável preserva entrada e oferece retry.
- Erro de autorização encerra ação e orienta login com o papel requerido sem expor detalhes.
- Ao acionar “Reservar”, sessão ausente ou autenticada como `ORGANIZER`/`GATE` encaminha a S03 com mensagem contextual. A intenção não sensível pode ser preservada, mas somente autenticação `CUSTOMER` retorna ao fluxo e dispara a revalidação; nenhuma Reservation existe antes disso. A troca de conta usa Logout explícito e não altera roles.
- Sessão expirada antes de ação crítica encaminha ao login e preserva intenção não sensível; após login no papel requerido, backend revalida tudo.
- Toast complementa; estado persistente fica inline.
- Falhas nunca exibem stack trace, segredo ou token completo.

### Atualizações e anúncios

- Região `status`/polite comunica resultados de busca, conclusão de loading, cópia e atualizações não urgentes.
- `alert`/assertive fica restrito a falhas que exigem ação imediata, expiração e resultado operacional Gate.
- Informação crítica permanece inline; toast é somente complementar e não recebe foco automaticamente.
- Polling e retries não repetem o mesmo anúncio. Mensagens usam vocabulário estável e não expõem enums crus em inglês sem significado em português.

### Estados de domínio representados

Event: `DRAFT`, `PUBLISHED`, vendas abertas/`SALES_CLOSED`. Reservation: `HOLDING`, `CONFIRMED`, `EXPIRED`. Payment: processamento de interface, `APPROVED`, `DECLINED`. Ticket: `VALID`, `USED`. Gate: `VALID`, `INVALID`, `ALREADY_USED`, `WRONG_EVENT`.

### Cobertura por superfície

| Superfície | Cold-load/vazio | Erro/recuperação | Foco/anúncio e condição especial |
|---|---|---|---|
| S01 | skeleton; sem resultados | retry/limpar busca | resultados anunciados; foco permanece na busca |
| S02 | skeleton; conteúdo opcional com fallback | Event ausente/indisponível; retry/voltar | CTA anuncia sold out/sales closed; seleção acessível |
| S03 | formulário idle | credenciais, sessão ou papel incorreto; corrigir/trocar conta | erro associado e foco no resumo/primeiro campo |
| S04 | recomposição da HOLDING | declined, verifying ou expired; retry/consultar/voltar | timer com anúncios limitados; busy impede reenvio |
| S05 | carregando Tickets | recovery consulta estado autoritativo | sucesso anunciado sem antecipar emissão |
| S06 | skeleton; nenhum Ticket | retry sem perder sessão | quantidade/estado da lista anunciados |
| S07 | detalhe VALID/USED | carregamento/erro; voltar a S06 | QR possui ManualCode; share confirma sem mover foco |
| S08 | carregamento público | inválido/não encontrado sem revelar dados | USED é persistente e não apresenta nova entrada |
| S09 | skeleton; nenhum Event | retry; sessão/papel → S03 | estados DRAFT/PUBLISHED/vendas encerradas textuais |
| S10 | inicial/loading/vazio | Ticketmaster indisponível; retry | resultados anunciados; nunca oferece criação manual |
| S11 | DRAFT/PUBLISHED | validação/save; preservar entradas | read-only explicado; erro leva ao campo |
| S12 | nenhum setor/lista | limite/associação/save; preservar entradas | ações por setor incluem nome do objeto |
| S13 | checklist pendente/ready | publicação falha; preservar/focar pendência | publishing busy; success só após backend |
| S14 | skeleton/lista | vazio/erro; retry/logout | seleção anuncia Event atual antes de avançar |
| S15 | solicitando/ativa/manual | denied/unavailable/sem rede; manual/retry | modo e Event atuais anunciados; captura pausa no busy |
| S16 | quatro resultados | falha de request retorna S15 preservando método | heading recebe foco; uma frase; próximo limpa estado |

## Interaction Primitives

- Enter submete busca/formulário quando inequívoco; Escape fecha overlay sem perder dados já confirmados.
- Dialog destrutivo somente para excluir Event `DRAFT` ou setor removível; tem nome, descreve objeto e consequência, recebe foco inicial deliberado, contém foco, oferece fechamento explícito e Escape e não abre outro dialog. Foco retorna ao acionador ao fechar.
- Drawer segue o mesmo ciclo de nome, foco contido, Escape, fechamento e retorno ao acionador.
- Toast não recebe foco automaticamente; quando contém ação, permanece por tempo suficiente ou até descarte explícito. Informação crítica também existe inline.
- Tabelas Organizer possuem caption/nome, headers associados e ações por linha cujo nome inclui o objeto. Em reflow, viram blocos preservando relações ou usam rolagem unidimensional numa região nomeada e operável por teclado.
- Em navegação de rota, foco programático vai ao `h1`/início do conteúdo sem manter o heading na ordem de tabulação.
- Atualizações assíncronas têm feedback imediato; nenhuma ação crítica depende só de animação.
- Debounce de busca é permitido como detalhe técnico, mas botão/Enter e estados de resposta permanecem compreensíveis.
- Cópia de link confirma inline ou em toast acessível, sem expor token no log.
- Logout encerra sessão e facilita troca entre contas seedadas.
- Quando o backend informa uma Reservation `HOLDING` vigente, o Customer recebe um acesso consistente “Continuar reserva” nas superfícies Customer autenticadas aplicáveis; o CTA sempre abre S04 existente e nunca cria outro hold. Em telas estreitas, o acesso permanece no fluxo de leitura sem ocultar conteúdo ou ações.
- S01 e S10 reutilizam o mesmo padrão de busca, mas não a mesma recuperação: catálogo público permite limpar/refazer a busca; Ticketmaster permite tentar novamente e nunca oferece criação manual. Selecionar um resultado Ticketmaster declara que será iniciado um Event interno `DRAFT` a partir do snapshot.
- Edições do Organizer possuem ação de salvar inequívoca. Navegar entre S11–S13 com alterações não salvas preserva os valores ou pede confirmação antes de descartá-los; erro de salvamento mantém entradas e contexto. O padrão é idêntico em navegação lateral e compacta.
- Após excluir um Event `DRAFT`, a navegação retorna a S09 e anuncia a confirmação. Após remover um setor, S12 permanece aberta e o foco segue para o próximo setor lógico ou para “Adicionar setor” quando a lista ficar vazia. Falha não remove o objeto e explica a razão.

## Responsive & Platform

| Superfície | Mobile | Tablet | Desktop |
|---|---|---|---|
| Catálogo | 1 coluna; busca fixa no fluxo | 2 colunas | grid 3–4 conforme largura |
| Detalhe | hero, conteúdo e compra em pilha; CTA próximo da seleção | 2 regiões quando couber | conteúdo + painel de compra sem esconder contexto |
| Checkout | resumo compacto antes do pagamento; timer persistente no fluxo | duas regiões | resumo lateral pode permanecer visível |
| Meus Ingressos | lista → detalhe; QR amplo | grid moderado | lista/grid sem miniaturizar QR |
| Organizer | navegação compacta; formulário em pilha | sidebar colapsável | sidebar + workspace; tabelas só se refluírem |
| Setores | cards/linhas empilhadas | tabela adaptável | visão densa com ações por linha |
| Gate | câmera em primeiro plano, controles grandes | layout operacional central | câmera/manual centralizados; teclado plenamente suportado |

Breakpoints exatos são decisão futura do Design System. Nenhuma superfície bloqueia portrait ou landscape. Customer, Organizer e Gate devem ser testados com zoom a 200%, viewport equivalente a 320 CSS px, reflow a 400% e overrides de text spacing; sticky timer/CTA/header nunca ocultam foco, mensagem ou ação. Scanner adapta seu enquadramento às duas orientações e o código manual permanece funcional em ambas. Câmera depende de APIs do navegador e contexto seguro; Chrome Android é alvo móvel principal e Safari iOS best effort. Chrome, Edge e Firefox atuais são alvos desktop. Digitação manual é fallback obrigatório.

## Accessibility Floor

Objetivo WCAG 2.1 AA:

1. Documento com `lang="pt-BR"`, título único e descritivo por rota/estado, HTML semântico, landmarks rotulados quando repetidos, exatamente um `h1` coerente e nomes acessíveis únicos. O primeiro controle permite “Pular para o conteúdo”.
2. Todo fluxo essencial operável por teclado; ordem de foco acompanha leitura; sem keyboard trap.
3. `focus-visible` com contraste ≥3:1 e não ocultado por sticky UI.
4. Contraste ≥4,5:1 em texto normal e ≥3:1 em texto grande/componentes essenciais.
5. Alvos ≥44×44 CSS px; zoom 200%, viewport de 320 CSS px, reflow 400% e text spacing ampliado sem perda funcional ou rolagem bidimensional da página.
6. Erros ligados por `aria-describedby`, resumo navegável de erros e instrução textual; não depender de placeholder, asterisco, ícone ou cor.
7. Status assíncrono usa live regions com prioridade definida e parcimônia. Timer não anuncia a cada segundo: apenas 3 min, 1 min e expiração.
8. Resultado Gate move foco ao heading do painel e anuncia uma frase completa uma única vez; os quatro resultados são distintos em texto, ícone e tratamento.
9. Scanner não é única via: manual funciona com teclado e leitor de tela. Permissão negada tem instrução textual.
10. QR possui equivalente por código manual e metadados legíveis; o payload não vira `alt`, o código é selecionável/copiável e o QR nunca é tratado como imagem autossuficiente.
11. Motion respeita `prefers-reduced-motion`: remove deslocamento, escala, parallax e pulsação contínua, mantendo somente mudança instantânea ou fade curto não essencial. Sem flash, piscar ou animação agressiva; feedback não depende de som ou vibração.
12. Data/hora e BRL são legíveis em pt-BR; códigos preservam agrupamento visual sem prejudicar cópia/leitura.
13. Imagens de evento têm alt contextual ou vazio quando decorativas; fallback não inventa descrição.
14. Mensagens não usam apenas ícone/cor; disabled explica motivo quando necessário.
15. Login oferece propósito/autocomplete apropriado, mensagens seguras e controles de senha acessíveis; sessão expirada preserva apenas intenção não sensível.
16. Dialogs, drawers, tabelas e toasts obedecem aos ciclos de foco e relações semânticas definidos em Interaction Primitives.

## Inspiration & Anti-patterns

- **KIKK e direção editorial de festival:** levantar hierarquia tipográfica, ritmo de cartaz e energia cultural; aplicar expressão sobretudo em discovery/hero, nunca copiar composição ou comprometer leitura.
- **Festivent e informação prática:** levantar a convivência entre desejo e dados objetivos do evento; data, local, preço e disponibilidade permanecem escaneáveis.
- **Swiss/postal/ticket impresso:** levantar grid, micro-labels, numeração, recortes e marcas editoriais para o `TicketCard`; QR, código e estado sempre vencem a metáfora visual.
- **Gráficos experimentais/acid-rave:** usar apenas como acento controlado em Customer/ticket, não em checkout, formulário, Organizer ou Gate.
- **Rejeitados:** SaaS/dashboard genérico, analytics decorativo, glassmorphism, gradientes excessivos, cardificação indiscriminada, cyberpunk/club-only, gamificação, parallax, scroll hijacking, cursor customizado e motion que atrase tarefa.

## Key Flows

### UJ-C01 — Marina compra dois ingressos

1. Marina, 27, navega no celular sem sessão CUSTOMER, busca o show e confere data/local.
2. Ela seleciona Pista Premium e quantidade 2; a UI explica que isso é intenção, não garantia.
3. Ao tocar “Reservar”, ela entra como CUSTOMER e retorna ao mesmo contexto.
4. O backend revalida vendas/estoque e cria ou recupera a HOLDING.
5. **Clímax:** o checkout abre com setor, quantidade, total e dez minutos derivados de `expiresAt`; somente agora há garantia temporária.
6. Marina escolhe aprovação simulada, recebe confirmação e encontra exatamente dois Tickets em Meus Ingressos.

**Falha/recuperação:** se vendas/estoque mudarem durante o login, retornar a S02, explicar a mudança, focar setores e exigir nova confirmação; não criar hold com a intenção antiga.

### UJ-C02 — Rafael recupera-se de recusa

1. Rafael abre uma HOLDING vigente e confere timer/resumo.
2. Ele provoca `DECLINED` no ambiente demonstrativo.
3. A tela confirma a recusa, mantém timer/reserva e oferece nova tentativa.
4. Ele escolhe `APPROVED` antes do prazo.
5. **Clímax:** a transição para sucesso só ocorre após resposta autoritativa; os Tickets aparecem sem duplicação.

**Falha/recuperação:** resposta perdida entra em `verifying`; Rafael não reenvia enquanto o sistema consulta a Reservation e vê CONFIRMED, DECLINED/HOLDING ou EXPIRED de forma inequívoca.

### UJ-C03 — Joana recebe ingresso compartilhado

1. Joana abre um link público sem login.
2. Ela vê Event, data/hora, local, setor, estado, QR e código, sem dados do comprador.
3. **Clímax:** apresenta o QR na entrada e recebe passagem simples pelo fluxo Gate.
4. Depois do uso, o mesmo link permanece acessível e mostra `USED`, sem permitir nova entrada.

**Falha/recuperação:** link inválido/não encontrado não revela dados nem token e apresenta estado persistente sem QR utilizável.

### UJ-O01 — Bruno publica um evento

1. Bruno, Organizer, entra no desktop e pesquisa a Ticketmaster.
2. Escolhe uma referência; a tela explica que criará um Event interno.
3. Completa data futura/local e cria setores; campos opcionais usam fallbacks.
4. Revisa a `PublicationChecklist` e corrige pendências.
5. **Clímax:** a revisão confirma referência, título, data, local e ao menos um setor válido; Bruno publica e vê `PUBLISHED`.

**Falha/recuperação:** Ticketmaster indisponível preserva a busca e oferece retry, sem criação manual; falha de publicação preserva dados, mostra resumo e foca a primeira pendência.

### UJ-O02 — Bruno gerencia o publicado

1. Bruno abre seu Event publicado.
2. Confere título, local, data e referência visíveis/read-only com explicação; descrição, imagem e categoria continuam editáveis.
3. Ajusta preço/capacidade de um setor.
4. Ao tentar reduzir abaixo do comprometido ou remover setor associado, recebe bloqueio preciso.
5. **Clímax:** salva somente alterações permitidas sem quebrar compras existentes.

**Falha/recuperação:** validação informa o mínimo de capacidade permitido ou a associação impeditiva, preserva os demais valores e devolve foco ao campo/ação correspondente.

### UJ-G01 — Carla valida a fila

1. Carla, Gate, usa um celular à noite e seleciona o Event.
2. Autoriza a câmera e escaneia um QR; captura pausa durante a validação.
3. **Clímax:** em meta de até 1 s, uma resposta dominante diz `INGRESSO VÁLIDO — PODE ENTRAR`.
4. Ela toca “Validar próximo”; Event permanece selecionado e o scanner volta limpo.
5. Uma repetição retorna `JÁ UTILIZADO`; Ticket de outro Event retorna `EVENTO ERRADO` sem consumo.

**Falha/recuperação:** câmera negada/ausente leva ao código manual; sem rede bloqueia consumo, explica dependência online e oferece retry, sem fila offline.

## Detailed Wireflows

### Customer

Referência low-fi: [wireflow de compra Customer](wireframes/flow-customer-purchase-2026-08-12.excalidraw), cobrindo intenção anônima, login `CUSTOMER`, revalidação, `HOLDING`, recusa, aprovação, verificação autoritativa e expiração.

`S01 catálogo → S02 detalhe → selecionar setor/1–6 → Reservar → [sem sessão CUSTOMER: S03 login/troca de conta → autenticar como CUSTOMER → restaurar intenção] → revalidar vendas/estoque → [falha: S02, focar setores e exigir nova confirmação | hold vigente: S04 existente | sucesso: S04 novo] → simular pagamento → [DECLINED vigente: S04 + retry | resposta perdida: S04 verifying → CONFIRMED/DECLINED/EXPIRED ou Verificar novamente | APPROVED: S05] → S06/S07`.

Regras: nenhum hold pré-login; seleção pré-login não garante estoque; `serverNow >= startsAt` fecha vendas; preço é snapshot na criação; expiração vence interface; double-click não reduz estoque novamente. Em falha de revalidação, setor e quantidade só permanecem selecionados se ainda forem válidos; a interface nunca reduz quantidade silenciosamente. Em S04 `EXPIRED`, ações de pagamento desaparecem e o Customer pode retornar ao Event para iniciar nova tentativa, sempre sujeita a revalidação.

### Organizer

Referência low-fi: [wireflow Organizer](wireframes/flow-organizer-event-2026-08-12.excalidraw), cobrindo Ticketmaster, `DRAFT`, setores, revisão/publicação e gestão pós-publicação com campos bloqueados.

`S09 Meus Eventos → Novo → S10 Ticketmaster → selecionar referência → S11 DRAFT → S12 setores → S13 revisão → [pendências: campos/setores destacados | ready: publicar] → S09/S11 PUBLISHED`.

Edição: `DRAFT` permite tudo e exclusão; `PUBLISHED` bloqueia title, venueName, venueAddress, startsAt, externalSource/externalId, permite description/imageUrl/category e mudanças válidas em setores; Event publicado não é excluído/cancelado. Salvar é uma ação inequívoca; transitar entre editor, setores e revisão não descarta alterações silenciosamente. Excluir DRAFT retorna a S09; remover setor mantém S12 e continuidade de foco.

### Gate

Referência low-fi: [wireflow Gate](wireframes/flow-gate-validation-2026-08-12.excalidraw), cobrindo seleção de Event, câmera/manual, validação online, quatro resultados, próximo ingresso e indisponibilidade de rede.

`S14 selecionar Event PUBLISHED → S15 [scanner ativo ↔ código manual] → [QR decodificado | código manual submetido] → validar online → S16 resultado → Validar próximo → S15 no mesmo modo, com Event mantido e captura/código/resultados anteriores limpos`.

Ramificações: permissão negada/câmera ausente → manual; rede indisponível → bloquear e retry; `WRONG_EVENT` não consome; duas validações concorrentes nunca exibem duas `VALID`.

## Edge-case Matrix

| Caso | Detecção/estado UX | Resposta e recuperação | Autoridade/impacto |
|---|---|---|---|
| Login durante compra | CUSTOMER ausente | login, restaurar intenção, revalidar | Domain preservado; sem hold anônimo |
| Sessão com role diferente na reserva | ORGANIZER/GATE autenticado | explicar exigência de CUSTOMER, permitir Logout/troca, restaurar apenas após login CUSTOMER | roles preservados |
| Estoque mudou pré-login | revalidação falha | informar indisponibilidade e voltar aos setores | backend |
| Setor/evento esgotado | disponibilidade zero | SOLD OUT; CTA bloqueado | backend |
| Evento iniciado | `serverNow >= startsAt` | SALES CLOSED; detalhe segue público | backend temporal |
| Double-click/retry | criação em andamento/repetida | CTA loading; retornar mesma reserva | idempotência backend |
| Hold vigente no Event | nova intenção | direcionar à Reservation existente | regra Domain |
| Hold vencido não limpo | `serverNow >= expiresAt` | permitir nova tentativa após resposta autoritativa | backend; scheduler não bloqueia |
| Refresh/aba suspensa | checkout retorna | recompor via `expiresAt`; nunca reiniciar timer | handoff Architecture |
| Payment DECLINED | tentativa recusada | manter HOLDING e retry enquanto vigente | Domain |
| Resposta de pagamento perdida | timeout | verificar estado; não cobrar automaticamente | handoff Architecture |
| Verificação temporariamente indisponível | `verifying` sem resposta | manter contexto e bloqueio; “Verificar novamente” | handoff Architecture |
| Expira durante pagamento | resposta autoritativa | mostrar CONFIRMED ou EXPIRED, nunca ambos | backend |
| Sessão expira | 401/estado de auth | login; preservar intenção segura; revalidar | segurança |
| Ticketmaster indisponível | busca falha | mensagem e retry; seeds mantêm outros fluxos | sem criação manual |
| Dado opcional ausente | description/image/category vazios | fallback neutro | produto |
| Publicação incompleta | validação | resumo + links/foco nos campos | Domain |
| Redução inválida de capacidade | abaixo do comprometido | explicar mínimo permitido | Domain |
| Exclusão proibida de setor | Reservation/Ticket associado | impedir e explicar associação | Domain |
| Câmera negada/ausente | browser | instrução + código manual | handoff Architecture |
| QR não decodificado | scanner ativo | manter scanner, orientação, manual | UX |
| Gate sem rede | conectividade/request falha | bloquear consumo, explicar e retry | online-only |
| Ingresso inválido | não reconhecido | INVALID; não permitir entrada | backend |
| Evento errado | ticket de outro Event | WRONG_EVENT; não consumir | backend |
| Reuso/concorrência | Ticket USED | ALREADY_USED; nunca segunda VALID | backend atômico |
| Web Share API indisponível | browser | copiar link | sem feature nova |
| Link após uso | Ticket USED | página acessível com USED | Domain |
| Tickets não carregam após confirmação | S05 já confirmado | afirmar compra confirmada; recarregar Tickets ou abrir Meus Ingressos | não repetir pagamento |
| Saída do editor com alterações | dirty state em S11–S13 | preservar ou confirmar descarte; nunca perder silenciosamente | UX |

## Design System Component Needs

### Fundação

`Button`, `IconButton`, `Link`, `Input`, `PasswordInput`, `SearchInput`, `Select`, `Textarea`, `FieldMessage`, `ChoiceControl`, `Badge`, `StatusBadge`, `Divider`, `Dialog`, `Sheet`, `Tooltip`, `Toast`, `Alert`, `Skeleton`, `Spinner`, `EmptyState`, `ErrorState`, `ConnectivityBanner`, `Breadcrumb`, `Navbar` e `OrganizerSidebar`.

### Produto

`EventCard`, `EventHero`, `EventMetadata`, `EventStatus`, `TicketmasterResultCard`, `EventForm`, `PublicationChecklist`, `TicketSectorCard`, `SectorEditor`, `AvailabilityStatus`, `QuantityStepper`, `PriceSummary`, `CheckoutSummary`, `ReservationTimer`, `ActiveReservationBanner`, `DemoEnvironmentNotice`, `PaymentSimulationControl`, `PaymentResult`, `PurchaseSuccess`, `TicketCard`, `QRCodePanel`, `ManualCode`, `ShareAction`, `ScannerFrame`, `CameraPermissionState`, `ManualValidationForm`, `GateEventSelector` e `GateResult`.

Cada componente deve especificar: anatomia, variações, estados default/hover/focus/pressed/loading/disabled/error/success, teclado, nome acessível, live region quando aplicável, comportamento responsivo e referências visuais em `DESIGN.md`.

### Handoff explícito para Design System

- **Ordem responsiva:** definir a ordem DOM canônica antes da redistribuição visual. No detalhe, contexto do Event antecede compra e o CTA fica junto da seleção; no checkout, estado/timer e resumo antecedem o controle de pagamento; na Gate, contexto do Event antecede captura/manual e resultado. CSS pode redispor regiões sem alterar leitura, foco ou precedência funcional.
- **Contratos críticos por variante:** `GateResult`, `PaymentResult`, `ReservationTimer`, `PublicationChecklist`, `Dialog` e `ActiveReservationBanner` devem documentar trigger, label, estado bloqueado, loading, live region, foco de entrada/saída e próximo CTA. Tokens, anatomia visual final e durações continuam futuros; o comportamento definido neste spine prevalece.
- **Mapa semântico de estados:** `StatusBadge`, `PaymentResult`, `ReservationTimer` e `GateResult` devem mapear, por variante, rótulo visível em pt-BR, frase anunciada, ícone, token semântico e prioridade de live region. Enum técnico pode acompanhar o significado para a avaliação, mas não o substitui.
- **Tokens acessíveis:** quando cores, tipografia e spacing deixarem de ser TBD, validar todos os pares foreground/background e estados interactive/disabled/focus, indicador de foco com contraste mínimo de 3:1 e legibilidade com text spacing ampliado. Esses detalhes não congelam valores visuais neste documento.

## Visual Coverage

| Superfícies | Cobertura | Referência |
|---|---|---|
| S01–S08 — Público/Customer | wireframe de IA + wireflow detalhado | `wireframes/ia-elite-dev-ticket-2026-08-12.excalidraw`, `wireframes/flow-customer-purchase-2026-08-12.excalidraw` |
| S09–S13 — Organizer | wireframe de IA + wireflow detalhado | `wireframes/ia-elite-dev-ticket-2026-08-12.excalidraw`, `wireframes/flow-organizer-event-2026-08-12.excalidraw` |
| S14–S16 — Gate | wireframe de IA + wireflow detalhado | `wireframes/ia-elite-dev-ticket-2026-08-12.excalidraw`, `wireframes/flow-gate-validation-2026-08-12.excalidraw` |

Todas as 16 superfícies possuem referência low-fi de composição ou transição. Nenhuma possui mockup 1:1 estilizado promovido: mockups permanecem bloqueados até a aprovação de paleta, tipografia, escalas de spacing/radius, foco, motion e breakpoints. Produzir HTML agora criaria decisões visuais não autorizadas. Os spines prevalecem sobre os wireframes.

## Architecture Handoffs — autorizados

Estes itens não são decisões UX nem novas features; Architecture deve definir mecanismos:

1. câmera, permissões, seleção de dispositivo e requisito de HTTPS/contexto seguro; câmera negada/ausente permite manual, enquanto indisponibilidade de rede/backend bloqueia QR e manual e informa que nenhum Ticket foi consumido;
2. consulta/reconciliação autoritativa após perda de resposta de pagamento, sempre consultando a tentativa existente sem disparar nova tentativa; UX aplica as saídas `CONFIRMED`, `HOLDING`/`DECLINED`, `EXPIRED` ou retry de consulta definidas acima;
3. sincronização do timer por `expiresAt` após refresh, suspensão, retomada de visibilidade e clock skew; UX nunca mostra tempo superior ao autorizado e revalida antes de ação crítica;
4. `LOW_AVAILABILITY`: permanece fora do contrato visível até limiar/regra ser aprovado; decidir se e onde derivar;
5. detecção/tratamento de indisponibilidade de rede no Gate online, sem fila ou consumo offline;
6. política segura para link compartilhado inválido/não encontrado: ao visitante, UX mostra estado público neutro e consistente, sem dados pessoais nem token completo; Architecture define status HTTP, observabilidade e proteção contra enumeração. Estados públicos `VALID` e `USED` permanecem distintos.

## Non-MVP / Candidates

Explicitamente fora: filtros avançados, analytics/KPIs, cadastro, gestão de roles, associação Gate↔Event, criação manual sem Ticketmaster, mapa de assentos, gateway real, cancelamento/refund, revenda, e-mail, app nativo, modo offline, sincronização posterior, regeneração/revogação de share link, transferência de ownership, realtime via SSE/WebSocket e `LOW_AVAILABILITY` sem decisão aprovada.

## Integridade do contrato

As superfícies S01–S16 estão ligadas às jornadas/wireflows e à matriz de estados. Hold de dez minutos, setores+quantidade, Ticketmaster, roles, APPROVED/DECLINED e autenticação CUSTOMER antes da Reservation permanecem invariantes. Handoffs e Non-MVP são limites do contrato, não autorização para resolver mecanismos ou promover funcionalidades nesta fase.
