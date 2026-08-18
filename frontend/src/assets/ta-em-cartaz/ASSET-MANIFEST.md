# Tá em Cartaz — Asset Pack v2

Pacote curado a partir das **6 telas-âncora SVG fornecidas**. O objetivo é reduzir a liberdade visual do agente e preservar a identidade do Canva sem transformar toda a interface em imagens estáticas.

## Como ler as prioridades

- **P0 — crítico:** deve entrar no sistema visual final ou orientar diretamente um componente crítico.
- **P1 — recomendado:** melhora muito a fidelidade, mas pode ser reconstruído em CSS quando fizer sentido.
- **P2 — referência:** não deve ser hardcoded como conteúdo final; serve para composição, proporção e comparação visual.

## Tipos

- **EXTRACTED:** recorte/extração direta dos SVGs enviados.
- **HELPER:** máscara/SVG reutilizável gerado a partir da silhueta exata extraída; usa `currentColor` quando possível.
- **REFERENCE:** composição preservada apenas para o agente comparar com a implementação.
- **DEMO:** imagem específica dos exemplos do Canva; não deve substituir `event.imageUrl` dinâmico.

---

## P0 — identidade e marca

| Asset | Tipo | Onde usar | Regra de implementação |
|---|---|---|---|
| `brand/tc-seal-black.png` | EXTRACTED | Navbar, Organizer, páginas institucionais | Assinatura principal compacta. |
| `brand/tc-seal-pink.png` | EXTRACTED | Footer, blocos editoriais, estados promocionais | Acento de marca; uso pontual. |
| `brand/tc-badge-square-black.png` | EXTRACTED | Ticket | Variante quadrada do TC usada no ingresso. |
| `brand/tc-culture-seal-pink.png` | EXTRACTED | Ticket / ingresso compartilhado | Selo circular editorial. |
| `brand/ta-em-cartaz-wordmark-black.png` | EXTRACTED | Cabeçalhos | Usar quando a reprodução tipográfica do wordmark não for idêntica. |
| `brand/ta-em-cartaz-lockup-black.png` | EXTRACTED | Navbar/header | Lockup TC + wordmark pronto. |
| `decorative/paper-texture.png` | EXTRACTED | Fundo global e superfícies editoriais | Base visual do produto. Não usar como background pesado repetido sem otimização. |

## P0 — linguagem gráfica recorrente

| Asset | Tipo | Onde usar | Regra de implementação |
|---|---|---|---|
| `decorative/scribble-pink-underline-long.png` | EXTRACTED | Títulos grandes: Organizer, Meus Ingressos, Checkout | Repetir como assinatura editorial. |
| `decorative/scribble-pink-underline-short.png` | EXTRACTED | Títulos menores / seções | Variante curta. |
| `decorative/scribble-pink-zigzag.png` | EXTRACTED | Cards, selos, pequenas áreas editoriais | Usar com moderação. |
| `decorative/rays-black-three.png` | EXTRACTED | Hero, stickers, Checkout | Acento visual de 3 riscos. |
| `decorative/burst-green.png` | EXTRACTED | Home/Event hero | Versão texturizada verde. |
| `decorative/burst-asterisk-shape.svg` | HELPER | Organizer, Event, Home | Recolorável via `currentColor`; preferir para variantes preta/branca/verde. |
| `decorative/sticker-viva-agora-pink-round.png` | EXTRACTED | Home/Event | Sticker circular principal. |
| `decorative/sticker-viva-agora-green-square.png` | EXTRACTED | Checkout | Variante verde quadrada. |
| `decorative/sticker-cultura-move-green.png` | EXTRACTED | Home/Event/Ticket | Mensagem de marca. |
| `decorative/sticker-show-pink.png` | EXTRACTED | Evento/Ticket | Badge editorial de categoria `SHOW`. |

## P0 — ticket / Meus Ingressos

| Asset | Tipo | Onde usar | Regra de implementação |
|---|---|---|---|
| `ticket/ticket-main-shape.svg` | HELPER | `TicketCard` | **Crítico.** Estrutura recolorável do corpo principal; usar `currentColor`/CSS e não uma imagem amarela fixa. |
| `ticket/ticket-layout-reference.png` | REFERENCE | `TicketCard`, PublicSharedTicket | Fonte de verdade para proporção, serrilha, stub de QR, divisórias e hierarquia. |
| `ticket/ticket-main-reference-yellow.png` | REFERENCE | `TicketCard` | Referência de textura/cor/composição; não hardcodar artista ou textos. |
| `brand/tc-badge-square-black.png` | EXTRACTED | `TicketCard` | Marca do ticket. |
| `brand/tc-culture-seal-pink.png` | EXTRACTED | `TicketCard` | Selo recorrente. |

### Regra crítica do ticket dinâmico

O ticket final deve ser **componente**, não screenshot. A cor pode variar sem perder identidade. A imagem do evento vem de `event.imageUrl`/Ticketmaster e recebe enquadramento editorial (`object-fit`, crop, grayscale/contrast/mask). QR, código manual, dados, status e textos continuam HTML/React.

## P0 — seleção de setor

| Asset | Tipo | Onde usar | Regra de implementação |
|---|---|---|---|
| `sector/sector-tab-shape.svg` | HELPER | `SectorRow` | **Base recolorável** para os setores. `currentColor` / `--sector-color`. |
| `sector/sector-tab-texture-overlay.png` | HELPER | `SectorRow` | Textura neutra sobre a cor do setor. |
| `sector/sector-tab-orange.png` | EXTRACTED | Referência/fallback | Variante exata 01. |
| `sector/sector-tab-pink.png` | EXTRACTED | Referência/fallback | Variante exata 02. |
| `sector/sector-tab-yellow.png` | EXTRACTED | Referência/fallback | Variante exata 03. |
| `sector/sector-list-structure-reference.png` | REFERENCE | Event Detail | Fonte de verdade para ritmo, divisórias, quantidade e resumo. |
| `sector/order-summary-bar-shape.svg` | HELPER | Resumo fixo de seleção | Barra preta editorial, reutilizável. |

### Regra de cor dos setores

A estrutura é única. A cor é dado de apresentação (`--sector-color`). Não duplicar markup para setor laranja/rosa/amarelo/verde. Estados `sold-out/disabled` devem perder saturação e manter legibilidade.

## P0 — Organizer dinâmico

| Asset | Tipo | Onde usar | Regra de implementação |
|---|---|---|---|
| `organizer/organizer-thumb-shape.svg` | HELPER | Event rows Organizer | Frame reutilizável/recolorável para thumbnail do evento. |
| `organizer/organizer-thumb-orange.png` | EXTRACTED | referência/fallback | Fundo exato laranja. |
| `organizer/organizer-thumb-pink.png` | EXTRACTED | referência/fallback | Fundo exato rosa. |
| `organizer/organizer-thumb-yellow.png` | EXTRACTED | referência/fallback | Fundo exato amarelo. |
| `references/organizer-structure-reference.png` | REFERENCE | Organizer | Estrutura sem artistas/textos finais, útil para implementar rows e ações. |

### Regra crítica das imagens no Organizer

Os recortes de artistas em `optional-demo/` são **DEMO**. Na aplicação real, usar a imagem do Ticketmaster dentro do frame e aplicar tratamento editorial consistente. O layout não pode depender de PNG transparente de artista.

## P1 — Home / categorias / hero

| Asset | Tipo | Onde usar |
|---|---|---|
| `decorative/hero-crowd-torn.png` | EXTRACTED | Hero Home |
| `decorative/event-crowd-strip.png` | EXTRACTED | Event hero / colagens |
| `decorative/torn-orange-halftone.png` | EXTRACTED | Home/Checkout |
| `decorative/torn-pink-panel.png` | EXTRACTED | Home |
| `brand/culture-connect-stamp-black.png` | EXTRACTED | Home/Footer |
| `categories/category-shows.png` | EXTRACTED | Home CategoryStrip |
| `categories/category-festivals.png` | EXTRACTED | Home CategoryStrip |
| `categories/category-cultura.png` | EXTRACTED | Home CategoryStrip |
| `categories/category-perto-de-voce.png` | EXTRACTED | Home CategoryStrip |
| `categories/category-strip-reference.png` | REFERENCE | Comparação do conjunto |

## P1 — Event hero

| Asset | Tipo | Onde usar | Nota |
|---|---|---|---|
| `decorative/event-torn-orange-panel.png` | EXTRACTED | fundo do hero de evento | Pode ficar atrás da imagem dinâmica. |
| `decorative/event-torn-pink-vertical.png` | EXTRACTED | fundo do hero | Acento secundário. |
| `decorative/event-pink-circle.png` | EXTRACTED | hero | Geometria de apoio. |
| `decorative/event-hero-color-collage-reference.png` | REFERENCE | hero | Mostra a relação espacial dos elementos. |
| `references/event-structure-reference.png` | REFERENCE | Event Detail | Estrutura geral sem conteúdo final. |

## P1 — Checkout

| Asset | Tipo | Onde usar |
|---|---|---|
| `decorative/checkout-corner-left-collage.png` | EXTRACTED | canto inferior esquerdo |
| `decorative/checkout-corner-yellow-halftone.png` | EXTRACTED | canto inferior direito |
| `decorative/sticker-viva-agora-green-square.png` | EXTRACTED | painel do timer |
| `references/checkout-structure-reference.png` | REFERENCE | layout e proporções |

## P1 — Gate

| Asset | Tipo | Onde usar | Regra |
|---|---|---|---|
| `gate/tc-badge-white.png` | EXTRACTED | header escuro Gate | Variante compacta para fundo preto. |
| `gate/gate-scanner-corners.svg` | HELPER | scanner | `currentColor`; não usar mockup de celular como asset. |
| `references/gate-states-reference.png` | REFERENCE | Gate | Fonte de verdade para hierarquia dos 4 estados. |

Os ícones de `VALID`, `INVALID`, `ALREADY_USED` e `WRONG_EVENT`, bem como grids/glows de fundo, devem continuar como SVG/CSS/componentes, não screenshots.

## P2 — referências integrais

A pasta `references/` contém as seis telas-âncora renderizadas e composições estruturais. Elas devem ser usadas para **visual diff/review**, nunca importadas diretamente como página final.

- `home-anchor.png`
- `event-anchor.png`
- `checkout-anchor.png`
- `ticket-anchor.png`
- `gate-anchor.png`
- `organizer-anchor.png`

## DEMO — não hardcodar

`optional-demo/` contém artistas e thumbnails específicos dos mockups. Servem para teste visual e stress test apenas. Em runtime, o conteúdo real deve vir do Ticketmaster ou do fallback de marca.

---

# O que deliberadamente NÃO virou asset

Estes elementos devem ser código/CSS para preservar responsividade, acessibilidade e estados:

- inputs, buttons, checkboxes e form controls;
- ícones de calendário/local/relógio/plus/minus;
- status badges (`PUBLICADO`, `RASCUNHO`, etc.);
- QR Code e código manual;
- serrilhas repetitivas e dotted dividers simples;
- grids de bolinhas/halftone simples (`radial-gradient`);
- linhas, borders, cards e containers;
- ícones e cores de resultado do Gate;
- mockups dos celulares;
- texto dos eventos, preços, datas e locais;
- fotos fixas de artistas para conteúdo real.

Isso evita transformar a UI em um collage de screenshots e mantém o design realmente dinâmico.

# Uso sugerido no repositório

Copiar os assets de runtime para algo como:

```text
frontend/src/assets/ta-em-cartaz/
├── brand/
├── categories/
├── decorative/
├── gate/
├── organizer/
├── sector/
└── ticket/
```

Manter `references/` fora do bundle de produção, por exemplo em `docs/ui-reference/`.

# Princípio para o Antigravity

> **Os SVGs/telas são a fonte de verdade de composição. Os assets fixos carregam a identidade. O conteúdo variável continua sendo dados da aplicação. Nenhuma superfície pode depender de uma foto específica dos mockups para continuar bonita.**
