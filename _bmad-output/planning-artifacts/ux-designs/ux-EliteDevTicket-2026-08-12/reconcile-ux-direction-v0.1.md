# Reconciliação — UX Direction v0.1 × spines finais

**Fonte reconciliada:** `docs/04-ux/UX_DIRECTION_v0.1.md`  
**Alvos avaliados:** `DESIGN.md` e `EXPERIENCE.md`  
**Data:** 2026-08-12  
**Resultado:** cobertura substancial completa; nenhuma decisão explícita da direção foi contradita; decisões deliberadamente abertas continuam `TBD`.

## Método e legenda

- **Coberto:** preservado de forma explícita nos spines.
- **Aprofundado:** preservado e convertido em contrato mais verificável.
- **TBD preservado:** a direção proibia congelamento nesta fase e os spines respeitam isso.
- **Ajustado para Domain:** a intenção UX foi mantida, mas o fluxo foi corrigido para respeitar regra autoritativa.
- **Adição compatível:** detalhamento necessário ao workflow, sem nova feature de produto.
- **Handoff:** necessidade identificada, sem decisão silenciosa de Domain/Architecture.

## 1. Visão, promessa e identidade

| Decisão da direção | Cobertura final | Estado |
|---|---|---|
| Produto organizado, simples, intuitivo, jovem, contemporâneo e ligado a shows/festivais | `DESIGN.md > Brand & Style`; `EXPERIENCE.md > Foundation` | Coberto |
| Evento como protagonista; plataforma como moldura visual e funcional | `DESIGN.md > Brand & Style`, componentes `EventCard/EventHero` | Coberto |
| Usuário sabe onde está, o que pode fazer e o que acontece depois | princípio 1 de `EXPERIENCE.md` | Coberto |
| Naturalidade e ausência de curva de aprendizado; complexidade visual não é objetivo | estrutura antes da expressão, IA fechada e padrões explícitos | Coberto |
| Keywords primárias e secundárias | keywords primárias incorporadas literalmente; `bold/contemporary/musical/tactile/expressive` materializadas pela direção editorial, fotografia e ticket | Coberto semanticamente |
| Anti-keywords: corporate, luxury, generic SaaS, overdesigned, childish, cyberpunk, gamified, chaotic, club-only | `DESIGN.md > Brand & Style` explicita quase toda a lista; `overdesigned` e `club-only` não aparecem literalmente, mas são cobertos por parcimônia visual e acomodação multigênero | Coberto semanticamente |
| Evitar gradients excessivos, glassmorphism, cardificação, efeitos decorativos, motion prejudicial e dashboard de IA | `DESIGN.md > Brand & Style/Do's and Don'ts`; `EXPERIENCE.md` motion e ausência de analytics | Coberto |
| “Neo-Swiss Festival Editorial”, 60/25/15 | `DESIGN.md > Brand & Style` | Coberto |
| Base limpa, estruturada, grid-based, tipográfica e com respiro | `DESIGN.md > Brand & Style/Layout & Spacing` | Coberto |
| Personalidade por tipografia, fotografia, composição, labels, tickets, grafismos e accents | `DESIGN.md > Brand & Style/Typography/Components` | Coberto |
| “Structure first. Expression second.” | contrato visual e princípio 2 do contrato de experiência | Coberto |
| Remover expressão que prejudique leitura, navegação, compreensão, acessibilidade ou conversão | hierarquia de contratos, requisitos de acessibilidade e Do's/Don'ts | Coberto |
| Fotografia Customer central; imagem comunica emoção e UI organiza decisão | `DESIGN.md`, intensidade Customer e `EventHero` | Coberto |
| Plataforma consistente através de gêneros; conteúdo do evento cria variação emocional | identidade como moldura e adaptação controlada do ticket | Coberto |
| Inspiração Swiss Punk/editorial/ticket impresso e micrografismos usados seletivamente | `DESIGN.md > Brand & Style/Shapes/TicketCard` | Coberto |
| Acid/rave apenas como accent, sem dominar checkout/forms/Organizer/Gate | `DESIGN.md > Colors` | Coberto |
| Síntese “festival culture organized by product thinking” | literal em `DESIGN.md` | Coberto |

## 2. Tema e densidade por experiência

| Decisão | Cobertura final | Estado |
|---|---|---|
| Customer predominantemente light/neutral, dark pontual em hero/confirmação/ticket/destaque | `DESIGN.md > Colors` | Coberto; “shared ticket” não é citado isoladamente, mas ticket está incluído |
| Organizer predominantemente light | `DESIGN.md > Colors` | Coberto |
| Gate predominantemente dark | `DESIGN.md > Colors` | Coberto |
| Customer: expressão alta/densidade média | `DESIGN.md > Brand & Style` | Coberto |
| Organizer: expressão baixa/média, densidade alta | idem | Coberto |
| Gate: expressão mínima, clareza máxima | idem | Coberto |
| Mesmo Design System, intensidades diferentes | navegação e componentes compartilhados com adaptação por papel | Coberto |

## 3. Experiências, IA e jornadas

### Customer

- Prioridades `DISCOVERY, DESIRE, CLARITY, CONFIDENCE, CONVERSION, ACCESS TO TICKETS`: preservadas em IA, journeys, componentes e microcopy.
- Exploração sem autenticação: catálogo, busca e detalhe são públicos; compartilhado e login também. **Coberto.**
- Login somente quando necessário: ocorre ao acionar reserva, antes de qualquer hold. **Ajustado para Domain:** a direção desenhava `Reserve → Login if necessary → HOLDING`; o spine esclarece que a seleção pré-login é apenas intenção, login antecede criação/reuso da Reservation, e estoque é revalidado. Não há hold anônimo.
- Event-first: imagem, título/artista, data e local priorizados. `EventCard/EventHero` cobrem a intenção; o campo canônico usado é título do Event. **Coberto.**
- Preço previsível “A partir de”: explicitado como `MIN(TicketSector.price)`. **Aprofundado.**
- Disponibilidade: `AVAILABLE`, `SOLD_OUT`, `SALES_CLOSED` cobertos. `LOW_AVAILABILITY` foi deliberadamente retirado do contrato visível até existir limiar aprovado. **Handoff/sem alteração silenciosa.**
- Checkout sempre mostra evento, setor, quantidade, unitário, total e tempo. **Coberto.**
- Incerteza de cobrança proibida: microcopy e estado `verifying` preservam resposta autoritativa e impedem novo pagamento automático. **Aprofundado.**
- Jornada principal, recusa com retry e ticket compartilhado foram expandidos em `UJ-C01` a `UJ-C03`, com protagonistas, clímax e recuperação. **Aprofundado.**

### Organizer

- Prioridades produtividade, controle, status, clareza e estoque; identidade recuada. **Coberto.**
- Áreas My Events/Create/Edit/Manage Sectors desdobradas em S09–S13. **Aprofundado.**
- Estados `DRAFT`, `PUBLISHED`, `SALES_CLOSED` rapidamente reconhecíveis. **Coberto.**
- Campos imutáveis continuam visíveis/bloqueados com explicação. **Coberto e aprofundado com lista canônica.**
- Capacidade, comprometido e disponibilidade interpretáveis. **Coberto.**
- Não criar dashboards/analytics falsos. **Coberto e explicitamente fora do MVP.**
- Jornadas de publicação e gestão pós-publicação expandidas em `UJ-O01/UJ-O02`. **Aprofundado.**

### Gate

- Prioridades velocidade, contraste, certeza e zero distração. **Coberto.**
- Fluxo selecionar Event → scanner → resultado; manual fallback. **Coberto e detalhado em S14–S16.**
- Uma tarefa primária; resultado dominante; navegação mínima. **Coberto.**
- Quatro resultados `VALID/INVALID/ALREADY_USED/WRONG_EVENT`, nunca apenas cor. **Coberto.**
- Mobile/tablet first; desktop/teclado também suportados. **Aprofundado.**
- `UJ-G01` inclui câmera, meta de 1 s, próximo scan, reuso, evento errado e manual. **Aprofundado.**

## 4. Fluxo de compra, timer e pagamento

| Decisão | Tratamento final | Estado |
|---|---|---|
| Discovery → Detail → Sector → Quantity → Reserve → autenticação → HOLDING → checkout 10 min → APPROVED/DECLINED | wireflow Customer completo | Coberto, com login corrigido antes da criação do hold |
| Timer NORMAL 10:00–03:00, WARNING 02:59–01:00, CRITICAL 00:59–00:00, EXPIRED | `ReservationTimer` e S04 | Coberto |
| Tempo explícito; sem urgência artificial, piscar ou motion agressivo; backend autoridade | component/state/accessibility rules | Coberto |
| Reserva ativa ao sair do checkout e CTA de retorno | o comportamento foi preservado como direcionamento à Reservation vigente e componente `ActiveReservationBanner` | Coberto; localização global exata fica para composição UI |
| Aviso de demonstração sem cobrança real | microcopy canônica e `DemoEnvironmentNotice` | Coberto |
| APPROVED/DECLINED determinísticos | `PaymentForm`/`PaymentSimulationControl` | Coberto |
| Recusa mantém hold e tempo, com retry | S04, UJ-C02, edge matrix | Coberto |
| Nenhum erro deixa dúvida “fui cobrado?” | resposta perdida usa estado `verifying` e consulta autoritativa | Aprofundado; mecanismo é handoff de arquitetura |
| `PAYMENT_PROCESSING` como estado de interface | mantido como processamento UX, sem impor novo estado de Domain `PENDING` | Compatível |

## 5. Filosofia de erro, loading, empty e motion

- Erros respondem o que ocorreu, impacto, segurança do estado e próximo passo; “Algo deu errado” evitado. **Coberto literalmente.**
- Loading contextual (“Carregando/Garantindo/Processando/Validando”), double-click bloqueado, sucesso somente após backend e contexto preservado. **Coberto.**
- Empty states específicos para busca, ingressos, eventos e setores. **Coberto.**
- Motion serve orientação: hover/transições pequenas, skeleton, scanner e confirmação. `prefers-reduced-motion` foi acrescentado. **Aprofundado.**
- Proibidos scroll hijacking, parallax excessivo, cursor custom, WebGL decorativo, mouse-following e animações longas. Os principais aparecem explicitamente; mouse-following/animações longas são cobertos pela regra funcional/parcimoniosa. **Coberto semanticamente.**

## 6. Ticket como objeto de marca

- Direção “digital collectible / printed ticket inspired”, com numeração, QR, código, setor, data, labels, recortes, micrografismos e cor: `DESIGN.md > TicketCard/Shapes`. **Coberto.**
- Funcional primeiro; QR e informação crítica prioritários. **Coberto.**
- Sensação colecionável/compartilhável e adaptação ao conteúdo do evento sem perder consistência da plataforma. **Coberto.**
- Funciona fora de Meus Ingressos: página pública compartilhada definida na IA, inventário e jornada. **Coberto.**

## 7. Screen inventory, wireflows e edge cases

O documento inicial tinha apenas IA de alto nível e fluxo principal. `EXPERIENCE.md` adiciona, sem criar features, um inventário de 16 telas/estados e três wireflows completos.

Cobertura dos edge cases explicitamente solicitados na direção:

| Edge case | Cobertura |
|---|---|
| login durante compra | S03, UJ-C01, wireflow e matriz |
| estoque alterado antes do clique/login | revalidação e retorno aos setores |
| setor/evento esgotado | `SOLD_OUT`, CTA bloqueado |
| vendas encerradas | `serverNow >= startsAt`, detalhe permanece público |
| double-click/retry de criação | loading + mesma Reservation/idempotência backend |
| refresh/aba suspensa no checkout | recomposição por `expiresAt`; handoff Architecture |
| timer expirado | S04/Expired, devolução comunicada |
| payment declined | hold + retry |
| payment response lost | `verifying`; reconciliação autoritativa; handoff Architecture |
| JWT/sessão expirada | login, intenção segura preservada, revalidação |
| offline | Gate bloqueia consumo, explica conexão obrigatória e oferece retry; sem modo offline |
| QR indisponível/não decodificado | orientação e código manual |
| câmera negada/ausente | instrução + manual; mecanismo em Architecture |
| manual code | fluxo Gate completo |
| wrong event | não consome Ticket |
| already used/concorrência | nunca segunda VALID |
| share unsupported | copiar link como fallback, sem feature nova |

Também foram adicionados casos diretamente derivados do Domain: hold vigente/vencido não limpo, expiração durante pagamento, Ticketmaster indisponível, campos opcionais, publicação incompleta, capacidade inválida, setor associado e link USED. **Adições compatíveis, não novas features.**

## 8. Responsividade e plataforma

- Direção original definia apenas Gate mobile/tablet first. O spine mantém isso e especifica comportamento para catálogo, detalhe, checkout, tickets, Organizer, setores e Gate em mobile/tablet/desktop. **Aprofundado.**
- Desktop prioritário para gestão e mobile central para descoberta/ticket/Gate são decisões de ergonomia, não superfícies novas.
- Breakpoints exatos continuam `TBD`, preservando o escopo da fase de Design System.
- Reflow desde 320 CSS px, touch targets 44×44 e ausência de dependência de hover aprofundam a meta responsiva/acessível.
- Compatibilidade e câmera/contexto seguro refletem decisões autoritativas já aprovadas; Safari iOS permanece best effort e código manual é fallback.

## 9. Acessibilidade

Todos os princípios A01–A06 foram preservados:

- estado não depende só de cor;
- foco visível;
- erro associado ao campo;
- contraste, sobretudo Gate;
- timer anunciado somente em 3 min, 1 min e expiração;
- fluxos essenciais por teclado.

O piso WCAG 2.1 AA foi aprofundado com landmarks/headings, ordem de foco, ausência de trap, contraste mensurável, touch targets, zoom/reflow, `aria-describedby`, error summary, live regions, foco/anúncio no resultado Gate, alternativa textual ao QR/scanner, `prefers-reduced-motion`, pt-BR/BRL, alt text e explicação de disabled. **Aprofundado sem impacto de Domain.**

## 10. Componentes e estados semânticos

Todos os componentes inicialmente listados possuem equivalente nos spines:

- Primitivos: Button, Input, SearchInput, Select, Badge, Skeleton, EmptyState, ErrorState, OfflineBanner e Toast.
- Eventos: EventCard, EventHero, EventMetadata, TicketSectorCard, AvailabilityBadge/Status e QuantityStepper.
- Checkout: PriceSummary, CheckoutSummary, ReservationTimer, ActiveReservationBanner, PaymentForm/SimulationControl e DemoEnvironmentNotice.
- Feedback: StatusAlert/Alert, PurchaseSuccess e ExpiredReservationState (como estado S04/padrão, sem componente isolado obrigatório).
- Tickets: TicketCard, QRCodePanel, ManualCode e ShareAction.
- Gate: ScannerFrame e GateResult.

O spine acrescenta primitives e componentes exigidos pelos wireflows (IconButton, Link, Textarea, labels/help/error, Dialog, Drawer, Breadcrumb, Navbar, Sidebar, TicketmasterResultCard, PublicationChecklist, CameraPermissionState etc.). **Adição compatível:** necessidades de Design System, não features de produto.

Estados semânticos `NEUTRAL/SUCCESS/WARNING/DANGER/INFO` aparecem como tokens requeridos. Estados de produto foram preservados, com estas precisões:

- `LOW_AVAILABILITY` não é exibido sem limiar aprovado;
- `PAYMENT_PROCESSING` é estado de interface, não imposição de `Payment.PENDING` no Domain;
- nomes de Gate permanecem os quatro resultados canônicos.

## 11. Voz e microcopy

A direção original já orientava clareza, urgência controlada, explicação de pagamento e resultado Gate inequívoco. `EXPERIENCE.md` transforma isso em tons distintos:

- Customer direto, confiante, energizante e sem pressão artificial;
- Organizer funcional, instrutivo e preciso;
- Gate mínimo e imperativo.

Foram criadas microcopies canônicas para login pré-hold, criação, demo, recusa, resposta incerta, expiração e Gate sem rede. **Adição compatível**, não regra de Domain.

## 12. Inspirações e anti-patterns

As referências KIKK, Festivent, Swiss/postal/ticket e gráficos experimentais foram absorvidas por princípio em `DESIGN.md`: grid/hierarquia editorial, IA e informação prática, linguagem de ticket impresso e accents jovens. Não há cópia literal nem dependência visual congelada.

Anti-patterns relevantes permanecem explícitos nos Do's/Don'ts e no contrato de marca. Nenhum mockup ou token foi inventado para simular completude.

## 13. Decisões deliberadamente abertas

A lista “What is intentionally NOT decided yet” foi respeitada:

- nome da plataforma e logo: não definidos;
- paleta: `colors: {}` e tokens semânticos futuros, sem hex;
- font family/scale/pesos/tracking: `TBD`;
- radius: `rounded: {}`/`TBD`;
- spacing e breakpoints: `spacing: {}`/`TBD`;
- icon library, illustration style e texture system: não congelados;
- ticket layout exato: `TBD`;
- motion durations e component variants finais: não congelados.

O spine define apenas papéis, anatomia, estados necessários e critérios, que são o nível apropriado do workflow UX.

## 14. Adições, conflitos e impactos

### Conflito real encontrado e tratamento

**Login no fluxo de compra.** A direção v0.1 posicionava visualmente “Reserve” antes de “Login if necessary”, o que poderia sugerir hold anônimo. O Domain exige CUSTOMER autenticado ao iniciar Reservation. O spine não mudou o Domain: preserva setor/quantidade como intenção local, autentica, restaura contexto, revalida e somente então cria/recupera o hold. Esta é uma correção de precisão do wireflow.

### Handoffs explicitamente isolados

1. câmera/permissões/dispositivo/HTTPS;
2. reconciliação após perda da resposta de pagamento;
3. timer por `expiresAt` após refresh/suspensão/clock skew;
4. eventual regra de `LOW_AVAILABILITY`;
5. detecção de rede no Gate online.

Nenhum mecanismo foi decidido como regra UX ou nova feature.

### Adições compatíveis, sem expansão do MVP

- rotas indicativas e IDs S01–S16;
- protagonistas e journeys detalhadas;
- microcopy canônica;
- responsive matrix;
- critérios verificáveis WCAG;
- primitives/interactions necessários;
- estados de recuperação e edge cases derivados dos requisitos já aprovados.

### Features explicitamente não adicionadas

Cadastro, gestão de papéis, analytics, filtros avançados, mapa de assentos, gateway real, cancelamento/refund, revenda, e-mail, aplicativo nativo, modo offline/fila/sincronização, associação Gate↔Event, criação manual sem Ticketmaster, regeneração/revogação de share, transferência de ownership, realtime SSE/WebSocket e `LOW_AVAILABILITY` sem regra aprovada.

## 15. Lacunas residuais e próximos TBDs

Não há lacuna bloqueante para concluir a fase UX. Permanecem deliberadamente para UI/Design System ou Architecture:

- tokens concretos de cor, tipografia, espaçamento, radius, sombra e motion;
- breakpoints numéricos e composição visual exata;
- iconografia, logo, ilustração, textura e layout final do ticket;
- anatomia visual completa e variantes finais de cada componente;
- mecanismo técnico de scanner/câmera, reconciliação de pagamento, sincronização temporal e conectividade;
- limiar/regra de `LOW_AVAILABILITY`, caso venha a ser aprovado posteriormente.

## Veredito

`DESIGN.md` e `EXPERIENCE.md` preservam a direção v0.1 e a expandem nos eixos solicitados: jornadas, wireflows, edge cases, inventário de telas, padrões de interação, responsividade, acessibilidade e necessidades do Design System. Não foi detectada mudança silenciosa de Domain nem feature nova incorporada ao MVP. O único desalinhamento textual da fonte — login depois de “Reserve” — foi explicitamente reconciliado em favor da regra autoritativa de autenticação antes da Reservation.
