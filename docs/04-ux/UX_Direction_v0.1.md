# Elite Dev — UX Direction v0.1

**Status:** direção inicial de UX aprovada para alimentar o `bmad-ux`  
**Projeto:** Plataforma de Eventos e Ingressos  
**Base:** Project Specification v1.2 + referências visuais selecionadas

---

## 1. Experience Vision

A plataforma deve fazer o usuário sentir que está entrando em um produto:

- organizado;
- simples;
- intuitivo;
- jovem;
- contemporâneo;
- ligado à cultura de grandes shows e festivais.

A interface não deve competir com os eventos.

O evento é o protagonista.

A plataforma funciona como uma moldura visual e funcional que organiza a descoberta, reduz fricção durante a compra e transmite segurança durante todo o processo.

### Experience Statement

> Uma plataforma jovem e contemporânea para descobrir e viver grandes eventos, com uma experiência limpa, direta e intuitiva. A interface funciona como uma moldura editorial para artistas e festivais, combinando organização e clareza com detalhes urbanos que dão personalidade sem competir com o conteúdo.

---

# 2. Core Experience Promise

O usuário deve conseguir sentir:

```text
“Eu sei onde estou.”
“Eu sei o que posso fazer.”
“Eu sei o que vai acontecer depois.”
```

A experiência não deve exigir aprendizado.

O objetivo não é impressionar pela complexidade da interface.

O objetivo é fazer a experiência parecer natural.

---

# 3. Brand Keywords

## Primary

```text
YOUNG
CLEAN
INTUITIVE
URBAN
EDITORIAL
ENERGETIC
DIRECT
CULTURAL
```

## Secondary

```text
BOLD
CONTEMPORARY
MUSICAL
TACTILE
EXPRESSIVE
```

---

# 4. Anti-Keywords

A plataforma não deve parecer:

```text
CORPORATE
LUXURY
GENERIC SAAS
OVERDESIGNED
CHILDISH
CYBERPUNK
GAMIFIED
CHAOTIC
CLUB-ONLY
```

Também deve evitar:

```text
excesso de gradients
glassmorphism genérico
cards iguais em toda página
efeitos apenas decorativos
animações que atrapalham leitura
dashboard genérico de IA
```

---

# 5. Visual Direction

Nome conceitual atual:

> **Neo-Swiss Festival Editorial**

Composição aproximada:

```text
60% Neo-Swiss / Swiss Punk
25% Festival Editorial
15% Acid / Rave accents
```

Esses percentuais representam intensidade visual, não regras matemáticas.

---

# 6. Visual Foundation

A base visual deve ser:

```text
limpa
estruturada
grid-based
tipográfica
com bastante respiro
```

A personalidade aparece através de:

```text
tipografia
fotografia
composição
labels
tickets
grafismos pontuais
acentos de cor
```

Não através de excesso de elementos.

---

# 7. Organização antes de expressão

Princípio:

> **Structure first. Expression second.**

Se uma decisão gráfica piorar:

- leitura;
- navegação;
- compreensão;
- acessibilidade;
- conversão;

ela deve ser removida.

A estética deve reforçar a experiência, nunca dificultá-la.

---

# 8. Papel da Fotografia

Fotografia deve ser um elemento central da experiência do Customer.

Usar:

- artistas;
- palcos;
- multidões;
- performances;
- festivais;
- iluminação;
- momentos de show.

A imagem comunica o desejo.

A interface comunica a decisão.

Por isso:

```text
EVENT IMAGE
→ emoção

UI
→ organização
```

---

# 9. Papel da identidade da plataforma

A marca da plataforma não deve dominar a identidade de cada evento.

Ela precisa acomodar:

```text
festival eletrônico
show de rap
rock
pop internacional
MPB
sertanejo
festival multicultural
```

sem parecer outro produto em cada página.

Portanto:

> A identidade da plataforma vive principalmente na tipografia, grid, componentes, navegação e detalhes gráficos.

As fotografias e conteúdos dos eventos fornecem grande parte da variação emocional.

---

# 10. Graphic Language

A linguagem gráfica pode se inspirar em:

- Swiss Punk;
- pôsteres de festival;
- design editorial;
- tickets impressos;
- selos;
- etiquetas;
- recortes;
- códigos;
- elementos de impressão.

Possíveis elementos:

```text
micro-labels
small caps
números grandes
datas em destaque
códigos
linhas
setas
marcas de recorte
micrografismos
texturas muito discretas
```

Evitar usar todos simultaneamente.

---

# 11. Acid / Rave Accents

Elementos acid/rave devem funcionar como **accent**, não como fundação.

Podem aparecer em:

- campanhas;
- detalhes de tickets;
- badges;
- elementos gráficos;
- banners especiais;
- momentos promocionais.

Não devem dominar:

- checkout;
- formulários;
- Organizer;
- Gate.

---

# 12. Theme Strategy

A experiência será **híbrida**, mas com lógica.

Não alternar light/dark arbitrariamente.

## Customer

Predominantemente:

```text
LIGHT / NEUTRAL
```

com momentos dark mais imersivos.

Possíveis áreas dark:

- hero de evento;
- ticket detail;
- confirmação;
- shared ticket;
- áreas de destaque.

## Organizer

Predominantemente:

```text
LIGHT
```

Motivo:

- produtividade;
- legibilidade;
- densidade de informação;
- gestão.

## Gate

Predominantemente:

```text
DARK
```

Motivo:

- uso provável em ambiente noturno;
- contraste;
- foco;
- leitura rápida;
- feedback visual imediato.

---

# 13. Expression Density

Cada experiência usa o mesmo Design System, mas com intensidades diferentes.

## Customer

```text
Brand Expression
██████████████████ HIGH

Information Density
████████░░░░░░░░░░ MEDIUM
```

## Organizer

```text
Brand Expression
██████░░░░░░░░░░░░ LOW / MEDIUM

Information Density
██████████████░░░░ HIGH
```

## Gate

```text
Brand Expression
███░░░░░░░░░░░░░░░ MINIMAL

Information Clarity
██████████████████ MAXIMUM
```

---

# 14. Customer Experience

A experiência Customer deve priorizar:

```text
DISCOVERY
DESIRE
CLARITY
CONFIDENCE
CONVERSION
ACCESS TO TICKETS
```

O usuário deve navegar pelos eventos sem ser obrigado a autenticar.

Áreas públicas:

```text
Home / Events
Search
Event Detail
Shared Ticket
Login
```

Áreas autenticadas:

```text
Checkout
My Tickets
```

Autenticação deve acontecer apenas quando realmente necessária.

---

# 15. Customer UX Principles

## C01 — Discovery without interruption

Não exigir login para explorar eventos.

## C02 — Event first

Imagem, artista, data e local devem ter prioridade visual.

## C03 — Pricing clarity

Sempre mostrar preço de forma previsível.

Exemplo:

```text
A partir de R$ 149
```

## C04 — Availability clarity

Setores devem comunicar claramente:

```text
AVAILABLE
LOW AVAILABILITY
SOLD OUT
SALES CLOSED
```

## C05 — Purchase confidence

Durante checkout, o usuário sempre precisa saber:

```text
o que está comprando
quantos ingressos
qual setor
quanto vai pagar
quanto tempo resta
```

## C06 — Never create payment uncertainty

Nenhum erro deve deixar dúvida sobre:

> “Fui cobrado?”

---

# 16. Organizer Experience

A experiência Organizer deve priorizar:

```text
PRODUCTIVITY
CONTROL
STATUS
CLARITY
INVENTORY
```

Não precisa parecer um festival.

A identidade visual deve recuar.

Áreas principais:

```text
My Events
Create Event
Edit Event
Manage Sectors
```

O usuário deve reconhecer rapidamente:

```text
DRAFT
PUBLISHED
SALES CLOSED
```

e disponibilidade dos setores.

---

# 17. Organizer UX Principles

## O01 — Task-oriented

A tela existe para concluir tarefas.

## O02 — Status visible

Estado do evento nunca deve estar escondido.

## O03 — Safe editing

Campos imutáveis depois de `PUBLISHED` devem estar claramente bloqueados.

Não apenas desaparecer.

A interface deve explicar por quê.

## O04 — Inventory clarity

Capacidade e disponibilidade devem ser fáceis de interpretar.

## O05 — No fake analytics

Não criar dashboards, gráficos ou KPIs apenas para preencher espaço.

---

# 18. Gate Experience

A experiência Gate deve priorizar:

```text
SPEED
CONTRAST
CERTAINTY
ZERO DISTRACTION
```

Fluxo:

```text
Select Event
     ↓
Scan QR
     ↓
Immediate Result
```

Fallback:

```text
Manual Code
```

---

# 19. Gate UX Principles

## G01 — One primary task

Validar ingresso.

## G02 — Large feedback

Resultado deve dominar a tela.

## G03 — Four explicit outcomes

```text
VALID
INVALID
ALREADY USED
WRONG EVENT
```

## G04 — Never rely only on color

Exemplo:

```text
✓ INGRESSO VÁLIDO
```

e não apenas fundo verde.

## G05 — Minimal navigation

Nenhum elemento irrelevante deve competir com scanner e resultado.

## G06 — Mobile / tablet first

A experiência Gate deve funcionar especialmente bem em dispositivos móveis.

---

# 20. Purchase Experience

Fluxo principal:

```text
Event Discovery
      ↓
Event Detail
      ↓
Choose Sector
      ↓
Choose Quantity
      ↓
Reserve
      ↓
Login if necessary
      ↓
HOLDING
      ↓
10-minute Checkout
      ↓
Payment
   /       \
DECLINED   APPROVED
   │          │
 retry        ▼
   │      Success
   │          │
   └────── My Tickets
```

---

# 21. Reservation Timer

O timer é um componente funcional e visual importante.

Estados:

```text
NORMAL
WARNING
CRITICAL
EXPIRED
```

Faixas planejadas:

```text
10:00 → 03:00 = NORMAL
02:59 → 01:00 = WARNING
00:59 → 00:00 = CRITICAL
```

Princípios:

- mostrar explicitamente tempo restante;
- não criar urgência artificial desde o início;
- não piscar;
- não usar animações agressivas;
- backend continua sendo a autoridade.

---

# 22. Active Reservation

Se o usuário sair do checkout enquanto a reserva estiver ativa, a interface deve comunicar:

```text
Você possui uma reserva ativa.

07:22 restantes

[Continuar pagamento]
```

Esse comportamento pode virar:

```text
ActiveReservationBanner
```

---

# 23. Payment UX

O FakePaymentGateway deve parecer uma experiência de checkout controlada.

Sempre comunicar:

> **Ambiente de demonstração — nenhuma cobrança real será realizada.**

A experiência deve permitir testar:

```text
APPROVED
DECLINED
```

sem depender de gateway externo.

Em recusa:

```text
Pagamento recusado.

Seus ingressos continuam reservados.

04:17

[Tentar novamente]
```

---

# 24. Error Philosophy

Erro deve responder quatro perguntas:

```text
O que aconteceu?
Isso afetou minha compra?
O que posso fazer agora?
Meu estado atual está seguro?
```

Evitar:

```text
Algo deu errado.
```

Sempre que houver informação mais útil disponível.

---

# 25. Important Edge Cases

UX precisa considerar:

```text
login durante compra
estoque alterado antes do clique
setor esgotado
evento esgotado
vendas encerradas
double-click
retry de criação
refresh no checkout
timer expirado
payment declined
payment response lost
JWT expirado
offline
QR indisponível
camera denied
manual code
wrong event
already used ticket
share unsupported
```

---

# 26. Loading States

Loading deve comunicar operação real.

Exemplos:

```text
Carregando eventos...
Garantindo seus ingressos...
Processando pagamento...
Validando ingresso...
```

Durante ações críticas:

- bloquear double-click;
- não comunicar sucesso antes do backend;
- não esconder contexto desnecessariamente.

---

# 27. Empty States

Criar estados específicos para:

```text
Nenhum evento encontrado
Nenhum ingresso ainda
Nenhum evento criado
Nenhum setor criado
```

Não usar o mesmo empty state genérico em toda aplicação.

---

# 28. Accessibility Principles

## A01

Estado nunca depende somente de cor.

## A02

Focus state deve ser claramente visível.

## A03

Erros devem estar associados aos campos.

## A04

Contraste adequado, especialmente no Gate.

## A05

Timer não deve ser anunciado pelo screen reader a cada segundo.

Atualizações importantes:

```text
3 minutos restantes
1 minuto restante
Reserva expirada
```

## A06

Interações essenciais devem funcionar por teclado quando aplicável.

---

# 29. Motion Principles

> Motion exists to orient, not impress.

Usar:

- hover discreto;
- pequenas transições;
- mudança de estado;
- skeleton;
- feedback de scanner;
- confirmação.

Evitar:

```text
scroll hijacking
parallax excessivo
cursor customizado
WebGL decorativo
elementos seguindo mouse
animações longas
```

---

# 30. Tickets as Brand Objects

Tickets terão importância especial na identidade da plataforma.

Eles não devem parecer apenas cards de banco de dados.

Direção:

> **Digital collectible / printed ticket inspired.**

Podem utilizar:

- numeração;
- QR;
- código;
- setor;
- data;
- pequenos labels;
- recortes;
- micrografismos;
- cores mais expressivas;
- composição editorial.

A experiência do ticket pode ter mais personalidade que o checkout.

---

# 31. Ticket Principles

## T01 — Functional first

QR e informações importantes continuam prioridade.

## T02 — Collectible feeling

O usuário deve sentir vontade de guardar / compartilhar.

## T03 — Event adaptation

A imagem e conteúdo do evento podem influenciar visualmente o ticket.

## T04 — Platform consistency

Mesmo com diferentes eventos, ainda deve ser reconhecível como ticket da plataforma.

## T05 — Shareable

Deve funcionar visualmente bem fora de “Meus Ingressos”.

---

# 32. Initial Information Architecture

```text
PUBLIC
│
├── Home / Events
├── Search
├── Event Detail
├── Login
└── Shared Ticket


CUSTOMER
│
├── Checkout
└── My Tickets


ORGANIZER
│
├── My Events
├── Create Event
├── Edit Event
└── Manage Sectors


GATE
│
└── Ticket Validation
```

---

# 33. Initial Component Needs

Esta NÃO é ainda a lista final do Design System.

Componentes identificados por necessidades reais:

```text
Button
Input
SearchInput
Select
Badge

EventCard
EventHero
EventMetadata

TicketSectorCard
AvailabilityBadge
QuantityStepper

PriceSummary
CheckoutSummary

ReservationTimer
ActiveReservationBanner

PaymentForm
DemoEnvironmentNotice

StatusAlert
PurchaseSuccess
ExpiredReservationState

TicketCard
QRCodePanel
ManualCode
ShareButton

ScannerFrame
GateResult

Skeleton
EmptyState
ErrorState
OfflineBanner
Toast
```

---

# 34. Semantic States

Design System deverá suportar:

```text
NEUTRAL
SUCCESS
WARNING
DANGER
INFO
```

Estados de produto:

```text
AVAILABLE
LOW_AVAILABILITY
SOLD_OUT
SALES_CLOSED

DRAFT
PUBLISHED

HOLDING
CONFIRMED
EXPIRED

PAYMENT_PROCESSING
PAYMENT_DECLINED

TICKET_VALID
TICKET_USED

GATE_VALID
GATE_INVALID
GATE_ALREADY_USED
GATE_WRONG_EVENT
```

---

# 35. Reference Interpretation

As referências devem ser usadas por princípio, não copiadas literalmente.

## KIKK Festival

Referência para:

- tipografia;
- hierarquia;
- grid;
- ritmo editorial;
- contraste;
- organização de seções.

## Festivent

Referência para:

- arquitetura de informação;
- programação;
- bilheteria;
- organização de conteúdo;
- informações práticas.

## Swiss / Postal / Ticket references

Referência para:

- ticket identity;
- numeração;
- labels;
- recortes;
- impressão;
- sensação colecionável.

## Experimental colorful graphics

Referência para:

- energia;
- juventude;
- accents;
- campanhas;
- micrografismos.

---

# 36. What is intentionally NOT decided yet

Ainda NÃO congelar:

```text
nome da plataforma
logo
paleta final
font family final
font scale final
radius
spacing tokens
icon library
illustration style
texture system
exact ticket layout
motion durations
component variants
```

Essas decisões pertencem à próxima fase de UI / Design System.

---

# 37. UX Success Criteria

A UX será considerada bem-sucedida se:

### Customer

Conseguir:

```text
descobrir → entender → reservar → pagar → encontrar ticket
```

sem precisar aprender como o produto funciona.

### Organizer

Conseguir:

```text
criar → configurar → publicar → gerenciar
```

com clareza de estado e poucas decisões desnecessárias.

### Gate

Conseguir:

```text
selecionar evento → escanear → decidir entrada
```

em poucos segundos.

---

# 38. Direction Summary

A experiência deve parecer:

> **festival culture organized by product thinking.**

Não queremos remover a personalidade para ganhar clareza.

Também não queremos sacrificar clareza para demonstrar personalidade.

A direção final procura o equilíbrio:

```text
EDITORIAL
+
URBAN
+
YOUTHFUL
+
STRUCTURED
+
INTUITIVE
```

---

# 39. Instruction for BMAD UX

Ao executar `bmad-ux`, usar como inputs obrigatórios:

```text
1. Desafio oficial da Elite Dev
2. Project Specification v1.2
3. UX Direction v0.1
```

O workflow pode:

- expandir journeys;
- desenvolver wireflows;
- criar screen inventory;
- propor interaction patterns;
- identificar responsive behavior;
- aprofundar accessibility;
- identificar edge cases;
- sugerir necessidades de componentes.

O workflow NÃO deve:

- alterar decisões do Domain silenciosamente;
- adicionar features ao MVP sem marcá-las;
- introduzir mapa de assentos;
- alterar hold de 10 minutos;
- alterar modelo de setores;
- substituir Ticketmaster;
- inventar regras de pagamento;
- alterar papéis;
- decidir arquitetura.

Caso encontre necessidade que afete Domain ou Architecture:

```text
FLAG THE DISCOVERY.

Do not silently resolve it.
```

Essa descoberta será revisada antes da próxima fase.