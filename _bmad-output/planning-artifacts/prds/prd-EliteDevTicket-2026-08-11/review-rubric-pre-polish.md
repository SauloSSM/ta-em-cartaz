# PRD Quality Review — EliteDevTicket

## Overall verdict

O PRD está **apto para orientar implementação**, com uma tese clara, recorte honesto e regras críticas de concorrência, expiração, emissão e validação descritas por consequências observáveis. O principal risco para a passagem direta a UX, arquitetura e stories não é falta de substância, mas a ausência de um glossário/rastreabilidade sistemática e alguns requisitos qualitativos que ainda não possuem um critério verificável.

## Decision-readiness — strong

As decisões relevantes aparecem de forma inequívoca: catálogo Ticketmaster obrigatório, setores quantitativos em lugar de mapa de assentos, pagamento simulado, Gate online e share link permanente. Os custos dessas escolhas estão explicitados em “Fora do escopo” (§9) e em “Decisões consideradas e excluídas do MVP” no addendum, permitindo que um decisor entenda tanto o recorte quanto o que foi deliberadamente abandonado.

As três questões abertas (§11) são realmente não bloqueantes e encaminhadas ao estágio correto. Não há tensões importantes disfarçadas como considerações neutras.

## Substance over theater — strong

A visão é específica ao EliteDevTicket (§1: “Ticketmaster como ponto de partida”, “reserva temporária de dez minutos”, “validação de uso único”), e as diferenças entre Organizer, Customer e Gate determinam requisitos reais. As NFRs são majoritariamente específicas ao contexto, com metas temporais, compatibilidade, acessibilidade e auditoria concretas; não há personas, inovação ou seções decorativas.

## Strategic coherence — strong

O documento mantém uma tese consistente: demonstrar um ciclo pequeno, completo, seguro e explicável sem intervenção manual (§§1–2). O escopo, as jornadas, as métricas e os FRs convergem para essa tese, sobretudo nos guardrails de confiança (SM-01–SM-08) e nas contramétricas (§3.4). A preparação para avaliação também é tratada como parte da qualidade da entrega, não como expansão artificial do produto.

## Done-ness clarity — adequate

As regras de domínio mais arriscadas são excepcionalmente testáveis: FR-27 define hold vigente por `serverNow < expiresAt`; FR-29 proíbe estoque negativo; FR-31 exige devolução exatamente uma vez; FR-35 resolve a corrida aprovação–expiração; FR-50 limita validação concorrente a um `VALID`. A maior parte dos FRs descreve uma consequência observável suficiente para derivar critérios de aceite.

### Findings

- **medium** Requisitos qualitativos ainda não definem evidência de conclusão (§8, FR-05, FR-06, FR-13 e FR-15) — Expressões como “informações suficientes”, “linguagem clara”, “identificar claramente” e “fallbacks adequados” permitem implementações divergentes e dificultam aceite objetivo. *Fix:* enumerar o conjunto mínimo de campos da referência Ticketmaster, o conteúdo mínimo do erro/estado de retry, a forma observável de associar pendências aos campos e os estados mínimos de fallback; quando o detalhe for deliberadamente de UX, referenciar uma decisão UX identificada.
- **medium** Algumas NFRs não são verificáveis como escritas (§7.2, NFR-06–NFR-07; §7.4, NFR-11–NFR-12) — “ações comuns”, “praticamente imediato”, “versões atuais” e “best effort” não definem conjunto de operações, marco temporal nem matriz de teste. *Fix:* nomear as ações críticas medidas, definir um limiar de início de feedback (por exemplo, até 100 ms) e registrar versões-alvo/matriz mínima na baseline de entrega.
- **low** “Nenhum HTTP 500” cobre apenas uma classe de falha (§3, SM-11) — um happy path pode falhar com 4xx, navegação quebrada ou resposta semanticamente errada e ainda satisfazer a métrica isolada. *Fix:* formular SM-11 como conclusão bem-sucedida dos happy paths sem erro inesperado, mantendo zero HTTP 500 como guardrail adicional.

## Scope honesty — strong

O PRD torna as exclusões fáceis de localizar (§9) e reforça limites operacionais onde poderiam surgir expectativas implícitas (§5). Não há `[ASSUMPTION]` sem resolução nem `[NOTE FOR PM]` pendente; as três questões abertas têm baixo impacto e destino explícito. O recorte é coerente com um projeto de avaliação de uma semana.

## Downstream usability — adequate

Os FRs, NFRs, SMs e UJs têm sequências contínuas e nomes estáveis, e a separação entre comportamento de produto e mecanismos técnicos no addendum é útil para arquitetura e stories. Entretanto, a extração downstream ainda exige inferência sobre vocabulário e sobre a cobertura entre jornadas, requisitos e métricas.

### Findings

- **high** Falta um glossário canônico de entidades, estados e tempos (§§2–8) — `Event`, `TicketSector`, `Reservation`, `Payment`, `Ticket`, hold “ativo/vigente”, disponibilidade e quantidade “comprometida” carregam regras essenciais, mas suas definições e máquinas de estado precisam ser reconstruídas de vários trechos. Isso aumenta o risco de UX, arquitetura e stories divergirem justamente nos limites concorrentes. *Fix:* adicionar um glossário compacto com identidade/ownership, estados válidos, significado de disponibilidade e comprometimento, e definições temporais; manter mecanismos de persistência no addendum.
- **medium** A rastreabilidade é parcial e assimétrica (§8) — somente alguns FRs declaram “Realiza UJ-…”, e nenhuma relação sistemática conecta SMs aos FRs que as tornam verdadeiras. *Fix:* incluir em cada grupo de capacidade uma linha “Jornadas/Métricas relacionadas” ou anotar todos os FRs de forma uniforme, sem necessidade de matriz extensa.

## Shape fit — adequate

A combinação de jornadas com especificação por capacidades é apropriada para uma plataforma web multiator e chain-top. As jornadas preservam as diferenças emocionais e operacionais entre as superfícies sem introduzir uma seção de personas artificial.

### Findings

- **medium** As jornadas não possuem protagonistas nomeados (§4, UJ-01–UJ-03) — “O Organizer”, “O Customer” e “o participante” descrevem papéis abstratos; isso enfraquece a continuidade contextual e torna mais fácil perder situações reais, sobretudo no compartilhamento em que comprador e portador podem ser pessoas distintas. *Fix:* nomear protagonistas mínimos e manter o contexto inline (por exemplo, organizadora, cliente/comprador, participante que recebeu o link e operador Gate), sem criar uma seção separada de personas.

## Mechanical notes

- IDs contínuos e únicos: `SM-01`–`SM-13`, `UJ-01`–`UJ-03`, `NFR-01`–`NFR-20` e `FR-01`–`FR-53`; não foram encontrados gaps ou duplicatas.
- Não há Assumptions Index, mas também não há tags `[ASSUMPTION]`; o roundtrip está vazio e consistente.
- Há drift leve entre português e inglês/case (`evento`/`Event`, `reserva`/`Reservation`, `setor`/`TicketSector`, “Gate” como experiência, papel e interface). Um glossário deve fixar quando o termo indica entidade de domínio, papel ou superfície.
- As referências explícitas a UJ-01–UJ-03 resolvem, porém a cobertura é incompleta conforme o achado de rastreabilidade.
- O frontmatter ainda informa `updated: 2026-08-11`, embora a revisão ocorra em 2026-08-12; corrigir na finalização.
