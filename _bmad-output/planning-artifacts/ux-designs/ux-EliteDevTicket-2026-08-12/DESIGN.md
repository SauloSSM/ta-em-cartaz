---
name: EliteDevTicket
description: Sistema visual responsivo que combina cultura de festivais com clareza operacional para Customer, Organizer e Gate.
status: draft
sources:
  - docs/01-product/Desafio-Elite-Dev-2026.pdf
  - docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md
  - docs/04-ux/UX_DIRECTION_v0.1.md
  - _bmad-output/planning-artifacts/prds/prd-EliteDevTicket-2026-08-11/prd.md
created: 2026-08-12
updated: 2026-08-12
colors: {}
typography:
  display:
    note: 'TBD — família e escala finais não foram aprovadas; papel editorial, expressivo e de uso restrito.'
  heading:
    note: 'TBD — família e escala finais não foram aprovadas; papel estrutural de alta legibilidade.'
  body:
    note: 'TBD — família e escala finais não foram aprovadas; otimizada para leitura e formulários.'
  label:
    note: 'TBD — família e escala finais não foram aprovadas; adequada a micro-labels e metadados.'
rounded: {}
spacing: {}
components:
  event-card:
    decision-status: 'anatomia definida; tokens visuais TBD'
  ticket-card:
    decision-status: 'direção collectible/printed-ticket aprovada; composição exata TBD'
  reservation-timer:
    decision-status: 'estados e comportamento definidos em EXPERIENCE.md; tokens visuais TBD'
  gate-result:
    decision-status: 'hierarquia e semântica definidas; tokens visuais TBD'
---

# EliteDevTicket — Design Spine

Este documento é o contrato visual. `EXPERIENCE.md` define comportamento. Os dois spines prevalecem sobre mockups, wireframes e imports em caso de conflito.

## Brand & Style

Direção aprovada: **Neo-Swiss Festival Editorial** — aproximadamente 60% estrutura neo-suíça/Swiss Punk, 25% editorial de festival e 15% acentos acid/rave. A experiência deve parecer “festival culture organized by product thinking”: jovem, limpa, intuitiva, urbana, editorial, energética, direta e cultural.

A estrutura vem antes da expressão. Grid, hierarquia tipográfica, respiro e leitura sustentam o produto; fotografia, labels, números, códigos, linhas, setas, marcas de corte e micrografismos acrescentam personalidade com parcimônia. O evento é protagonista e a plataforma funciona como moldura.

A intensidade varia por contexto:

- **Customer:** expressão alta e densidade média; fotografia comunica desejo e a UI organiza a decisão.
- **Organizer:** expressão baixa/média e densidade alta; produtividade e estado dominam.
- **Gate:** expressão mínima e clareza máxima; feedback operacional domina.

Evitar aparência corporativa, luxury, SaaS genérico, cyberpunk, gamificada, infantil, caótica ou “dashboard gerado por IA”. Também evitar glassmorphism genérico, excesso de gradientes, cardificação indiscriminada e efeitos sem função.

## Colors

A paleta final está deliberadamente **TBD**, conforme a direção autoritativa. Nenhum hexadecimal é congelado neste documento.

O futuro sistema deve definir tokens semânticos para `surface`, `text`, `border`, `focus`, `primary`, `accent`, `success`, `warning`, `danger` e `info`, com pares de foreground acessíveis. Todos os pares e estados interativos, desabilitados e de foco devem ser testados nos fundos em que aparecem. As cores não podem ser o único canal de estado.

Estratégia temática aprovada:

- Customer predominantemente claro/neutro, com momentos escuros imersivos em hero, confirmação ou ticket quando isso não prejudicar a leitura.
- Organizer predominantemente claro.
- Gate predominantemente escuro, adequado a ambientes noturnos e leitura rápida.
- Acentos acid/rave podem aparecer em campanhas, badges e detalhes de tickets, nunca dominar checkout, formulários, Organizer ou Gate.

Estados Gate (`VALID`, `INVALID`, `ALREADY_USED`, `WRONG_EVENT`) precisam de combinação própria de texto, ícone, forma/tratamento e cor. Contraste mínimo segue WCAG 2.1 AA: 4,5:1 para texto normal, 3:1 para texto grande e componentes/indicadores essenciais.

## Typography

Famílias, escala, pesos e tracking finais permanecem **TBD**. A seleção futura precisa materializar quatro papéis:

- `display`: expressão editorial em heroes e títulos de eventos; uso restrito.
- `heading`: hierarquia inequívoca em todos os contextos.
- `body`: leitura longa, formulários e conteúdo operacional.
- `label`: metadados, códigos, datas e micro-labels de inspiração impressa.

O Organizer e a Gate privilegiam legibilidade sobre expressão. Texto não deve ser convertido em imagem. Zoom a 200%, reflow a 400% e overrides de text spacing não podem cortar conteúdo ou ações essenciais. All caps, quando usado em labels curtos, exige tracking e nunca substitui títulos compreensíveis.

## Layout & Spacing

Tokens, breakpoints e escala final permanecem **TBD**. A regra estrutural é grid responsivo com respiro editorial no Customer e composição mais compacta no Organizer/Gate.

- Customer: largura de leitura controlada, imagens protagonistas, cards adaptáveis e detalhe do evento com hierarquia clara entre conteúdo e compra.
- Organizer: desktop pode usar navegação lateral e áreas de trabalho amplas; em telas estreitas, formulários e setores viram pilha sem esconder estado ou ações.
- Gate: uma coluna, alvo principal central, resultado dominante e fallback manual sempre alcançável.

Nenhuma ação essencial pode depender de hover. Áreas tocáveis devem ter pelo menos 44 × 44 CSS px. Conteúdo deve funcionar a partir de 320 CSS px sem rolagem horizontal bidimensional. Layouts aceitam portrait e landscape; elementos sticky não ocultam foco, mensagens ou ações.

## Elevation & Depth

A estratégia final de sombras permanece **TBD**. Hierarquia deve vir primeiro de superfície, espaçamento, borda e tipografia. Elevação é reservada a elementos transitórios ou literalmente sobrepostos (dialog, popover, toast), nunca para transformar toda informação em cards.

No Gate, resultado não depende de sombra. No checkout, elevação não deve criar urgência artificial. Fotografia e ticket podem ter tratamento mais expressivo, preservando QR e informação funcional.

## Shapes

Raios finais permanecem **TBD**. A linguagem deve equilibrar grid editorial e referências de ticket impresso; pills ficam reservadas a badges/status e não devem virar formato universal. Recortes e marcas de corte são possíveis no ticket, mas nunca podem interferir no QR, código manual, foco ou leitura.

## Components

As especificações abaixo definem intenção visual; comportamento detalhado está em `EXPERIENCE.md`.

- **Button:** hierarquia primary/secondary/ghost/destructive; estados hover, focus-visible, pressed, loading e disabled distinguíveis. Loading mantém rótulo contextual e largura estável. O futuro token de foco precisa atingir contraste mínimo de 3:1 contra superfícies adjacentes.
- **Input/SearchInput/Select:** label persistente, ajuda e erro próximos; borda e focus ring visíveis; placeholder não substitui label.
- **Badge/StatusBadge:** texto explícito e símbolo quando necessário; nunca só cor.
- **EventCard:** imagem, título, data, local e `A partir de R$ {startingPrice}`; fallback visual para imagem/conteúdo opcional.
- **EventHero/EventMetadata:** fotografia lidera sem reduzir contraste; data, local e estado de vendas permanecem escaneáveis.
- **TicketSectorCard:** nome, preço, disponibilidade/estado, controle de quantidade e seleção evidente.
- **QuantityStepper:** alvo amplo, valor textual e limites percebidos; não depende apenas de ícones.
- **ReservationTimer:** normal, warning, critical e expired; não pisca e não anima agressivamente.
- **CheckoutSummary/PriceSummary:** setor, quantidade, preço unitário, total e evento; total em destaque, sem valor editável.
- **DemoEnvironmentNotice:** aviso persistente e inequívoco de que não há cobrança real.
- **TicketCard/QRCodePanel/ManualCode:** objeto mais expressivo/colecionável, mas QR, código, estado, evento, setor, data e local têm prioridade funcional.
- **ScannerFrame:** guia de posicionamento sem prometer leitura; estado da câmera e alternativa manual visíveis.
- **GateResult:** ocupa o foco visual com ícone, título textual e instrução curta; quatro resultados inequivocamente distintos.
- **Alert/Toast/ErrorState/EmptyState/Skeleton:** sem mensagens genéricas quando há contexto específico; toast nunca é o único lugar de informação crítica.
- **Navigation:** mesma aplicação, três experiências; controles e densidade se adaptam ao papel autenticado sem misturar tarefas.

### Catálogo visual canônico

Até que uma biblioteca de UI seja aprovada, nenhum item abaixo é considerado herdado. Os nomes desta tabela são canônicos e idênticos aos usados em `EXPERIENCE.md`. Tokens concretos continuam bloqueados pelo gate de Design System; esta tabela fixa anatomia e prioridade visual sem inventá-los.

| Componente | Contrato visual mínimo |
|---|---|
| `Button` | Variantes primary, secondary, ghost e destructive; loading mantém largura e rótulo contextual; focus-visible inequívoco. |
| `IconButton` | Ícone acompanhado por accessible name; alvo mínimo 44×44; tooltip apenas complementar. |
| `Link` | Distinguível de texto por mais de um indício; foco e visited state não prejudicam contraste. |
| `Input` | Label persistente, control, help e error próximos; placeholder nunca é label. |
| `PasswordInput` | Herda `Input`; controle de revelar senha tem nome/estado explícitos. |
| `SearchInput` | Herda `Input`; ação buscar e limpar permanecem perceptíveis sem depender de ícone. |
| `Select` | Label e valor atual sempre visíveis; estados open, selected, error e disabled distinguíveis. |
| `Textarea` | Herda `Input`; resize/reflow não cobre ações nem mensagens. |
| `FieldMessage` | Família visual para help e error; erro usa ícone/texto/tratamento, não apenas cor. |
| `ChoiceControl` | Checkbox/radio com label clicável, checked/unchecked/disabled/focus inequívocos. |
| `Badge` | Label curto; pills reservadas a metadado/status e nunca substituem explicação textual. |
| `StatusBadge` | Herda `Badge`; estado explícito por texto, símbolo e tratamento redundante. |
| `Divider` | Separação de baixa ênfase; nunca é a única indicação de agrupamento semântico. |
| `Dialog` | Superfície modal, título, descrição, ações e dismiss; destructive enfatiza objeto/consequência. |
| `Sheet` | Overlay responsivo para navegação/controles secundários; mantém título e close visível. |
| `Tooltip` | Auxílio breve, nunca conteúdo essencial nem substituto de label. |
| `Toast` | Feedback transitório complementar; crítico também permanece inline. |
| `Alert` | Mensagem persistente com título, contexto e próxima ação; sem texto genérico. |
| `Skeleton` | Replica a estrutura esperada sem sugerir conteúdo concluído. |
| `Spinner` | Usado em ação localizada com rótulo textual quando a operação é crítica. |
| `EmptyState` | Título específico, explicação curta e somente CTA já pertencente ao MVP. |
| `ErrorState` | Erro contextual, impacto e recuperação; nunca expõe stack trace/token. |
| `ConnectivityBanner` | Estado de conexão persistente; no Gate explica que validação online está bloqueada, sem sugerir modo offline. |
| `Breadcrumb` | Localização hierárquica textual; item atual não é link. |
| `Navbar` | Navegação pública/Customer com papel e sessão claros; logout alcançável. |
| `OrganizerSidebar` | Navegação de trabalho compacta; item atual, foco e collapse distinguíveis. |
| `EventCard` | Imagem/fallback, título, data, local e “A partir de” com menor preço; área acionável sem ações aninhadas ambíguas. |
| `EventHero` | Imagem lidera sem reduzir contraste de título, data, local ou estado de vendas. |
| `EventMetadata` | Grupo escaneável de data, local e informações essenciais, com ícones apenas redundantes. |
| `EventStatus` | Distingue `DRAFT`, `PUBLISHED` e vendas encerradas sem confundir estado persistido e condição derivada. |
| `TicketmasterResultCard` | Deixa origem externa visível e ação “usar como referência” inequívoca; não aparenta publicar diretamente. |
| `EventForm` | Agrupa conteúdo, data e local; campos bloqueados continuam legíveis e explicados. |
| `PublicationChecklist` | Pendências e pronto-para-publicar em lista textual; leva visualmente ao primeiro problema. |
| `TicketSectorCard` | Nome, preço, capacidade/disponibilidade/comprometido e ações permitidas com hierarquia operacional. |
| `SectorEditor` | Campos e limites no contexto do setor; ação destructive visualmente separada de salvar. |
| `AvailabilityStatus` | Exibe available/sold out/sales closed; `LOW_AVAILABILITY` não possui variante ativa. |
| `QuantityStepper` | Menos, valor, mais e limites legíveis; valor não depende da forma dos ícones. |
| `PriceSummary` | Unitário e total alinhados e diferenciados; BRL legível. |
| `CheckoutSummary` | Evento, setor, quantidade, unitário e total permanecem juntos e escaneáveis. |
| `ReservationTimer` | Normal/warning/critical/expired por texto, forma e cor; não pisca. |
| `ActiveReservationBanner` | Hold vigente e CTA de retorno evidentes sem competir com erros/pagamento. |
| `DemoEnvironmentNotice` | Aviso persistente de ausência de cobrança real, visível antes da simulação. |
| `PaymentSimulationControl` | Opções APPROVED/DECLINED explicitamente demonstrativas; não aparenta editar o valor. |
| `PaymentResult` | Resultado e efeito sobre a Reservation juntos; declined preserva timer e retry. |
| `PurchaseSuccess` | Confirmação autoritativa e acesso aos Tickets, sem antecipar emissão. |
| `TicketCard` | Título, setor, data/hora, local, estado, QR, código e share; expressão nunca compromete função. |
| `QRCodePanel` | Quiet zone e contraste preservados; não recebe textura, recorte ou overlay. |
| `ManualCode` | Código agrupado, selecionável/copiável e legível; nunca truncado. |
| `ShareAction` | Ação obter/copiar link permanente; feedback de cópia sem sugerir rotação/revogação. |
| `ScannerFrame` | Guia de enquadramento, estado de câmera e alternativa manual visíveis; sem falso feedback de leitura. |
| `CameraPermissionState` | Ícone, título, instrução e CTA/fallback manual; não bloqueia acesso ao código manual. |
| `ManualValidationForm` | Input e submissão grandes, claros e plenamente operáveis por teclado. |
| `GateEventSelector` | Evento atual dominante antes do scanner; troca de evento exige ação deliberada. |
| `GateResult` | Texto, ícone, tratamento e instrução dominantes para os quatro resultados; “Validar próximo” é a única ação primária. |

### Gate de tokens antes da finalização

O spine **não pode receber `status: final`** enquanto `colors`, `typography`, `rounded`, `spacing` e `components` não tiverem valores implementáveis e combinações críticas de contraste verificadas. Os futuros wireframes/mockups devem orientar essas decisões, mas não substituem este contrato. Permanecem detalhes de Design System — não decisões de Domain ou Architecture.

## Boas práticas e antipadrões

| Faça | Não faça |
|---|---|
| Faça o evento liderar a experiência Customer | Deixe a marca competir com a fotografia |
| Use estrutura editorial, contraste e respiro | Use caos gráfico como sinônimo de juventude |
| Exponha estado por texto, ícone e tratamento visual | Dependa apenas de verde/vermelho |
| Mantenha Organizer funcional e orientado a tarefa | Invente analytics para preencher espaço |
| Faça o resultado Gate dominar a tela | Misture navegação e decoração com a validação |
| Preserve QR e código como elementos funcionais | Sacrifique legibilidade pela estética collectible |
| Use motion para orientar e confirmar | Use parallax, scroll hijacking, cursor customizado ou WebGL decorativo |
| Sob `prefers-reduced-motion`, use mudança instantânea ou fade curto não essencial | Use deslocamento, escala ou skeleton pulsante contínuo |
| Use fallbacks neutros para imagem/descrição/categoria ausentes | Mostre espaços quebrados ou conteúdo inventado |
| Trate paleta, fontes, raios, spacing, ícones e logo como TBD | Congele decisões visuais que as fontes deixaram abertas |
