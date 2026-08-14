# Reconciliação UX — Desafio Elite Dev 2026 (PDF oficial)

## Escopo

- **Fonte autoritativa verificada:** `docs/01-product/Desafio-Elite-Dev-2026.pdf` (5 páginas).
- **Artefatos avaliados:** `DESIGN.md` e `EXPERIENCE.md`, em seu estado atual de 12/08/2026.
- **Objetivo:** verificar cobertura das obrigações com expressão em UX, preservação da intenção qualitativa, autoria visual, conflitos e inclusão acidental de features.
- **Limite da análise:** requisitos de repositório, banco, seeds, README, prazo, submissão e deploy foram verificados quanto a contradições, mas não precisam ser duplicados nos spines de UX; permanecem sob PRD/addendum/entrega.

## Veredito

**PASS, com uma ressalva de enquadramento não bloqueante.** Os spines cobrem integralmente as superfícies e comportamentos UX exigidos pelo PDF, preservam o recorte por setores e quantidade, tornam os quatro resultados da Portaria inequívocos e carregam de forma forte a intenção de autoria visual. Não foi encontrado conflito com o desafio nem feature opcional promovida silenciosamente ao MVP.

A ressalva é que `EXPERIENCE.md` trata a recuperação de resposta perdida de pagamento (`verifying`) como estado obrigatório da tela, embora o PDF exija somente aprovação e recusa. O mesmo documento identifica corretamente o mecanismo como handoff de Architecture. Isso não altera as regras de Payment nem configura nova capacidade de produto, mas sua classificação como obrigação de MVP deve permanecer limitada a recuperação segura de estado, sem criar terceiro resultado de pagamento.

## Matriz de cobertura

| Exigência/intenção do PDF | Cobertura atual | Avaliação |
|---|---|---|
| Navegação e busca de eventos publicados, com data, local e preço (p. 2) | `EXPERIENCE.md`: S01, S02, IA pública, wireflow Customer; `DESIGN.md`: EventCard/EventHero | Coberto |
| Criação e gerenciamento de eventos pelo Organizer (pp. 1–2) | S09–S13, UJ-O01/UJ-O02 e wireflow Organizer | Coberto e aprofundado sem mudar Domain |
| Catálogo externo de shows/filmes (pp. 1–2) | Busca Ticketmaster e snapshot interno em S10/UJ-O01 | Coberto pelo recorte Ticketmaster já aprovado |
| Reserva por assento **ou** quantidade (p. 2) | S02, QuantityStepper 1–6 e fluxo setor + quantidade | Coberto por uma das alternativas permitidas |
| Pagamento simulado aprovado e recusado (p. 2) | S04/S05, PaymentForm, UJ-C01/UJ-C02 | Coberto |
| Meus Ingressos com ingresso e QR (p. 2) | S06/S07, TicketCard, QRCodePanel e UJ-C01 | Coberto |
| Compartilhamento por link gerado pela aplicação (pp. 1–2) | S07/S08, ShareAction, UJ-C03 e wireflow Customer | Coberto |
| Portaria com QR por câmera e código manual alternativo (p. 2) | S14–S16, ScannerFrame, ManualValidationForm, UJ-G01 e wireflow Gate | Coberto |
| Resultados `válido`, `inválido`, `já utilizado`, `evento errado` (p. 2) | GateResult, S16, Accessibility Floor e UJ-G01 | Coberto de modo inequívoco |
| Três papéis distintos (p. 2) | IA por visitante/Customer, Organizer e Gate; intensidades visuais distintas | Coberto |
| Mesmo estoque não vendido duas vezes; ingresso não validado duas vezes (p. 2) | Estados, wireflows e Edge-case Matrix apresentam as consequências visíveis; backend continua autoridade | Coberto no limite correto de UX |
| QR não forjável (p. 2) | UX não expõe tokens e representa `INVALID`; mecanismo permanece fora do spine | Coberto por referência comportamental; corretamente delegado |
| Fluxo ponta a ponta simples e completo antes de sofisticação (p. 5) | Três wireflows completos, 16 telas e exclusões explícitas | Coberto |
| Interface agradável, tratamento de erros, criatividade e dedicação (p. 5) | Direção visual autoral, State Patterns, Edge-case Matrix e Accessibility Floor | Coberto |

## Intenção qualitativa e autoria de IA

O PDF afirma que o escopo reduzido existe para revelar decisões, descartes e raciocínio, pede que o resultado evite a aparência reconhecível de uma interface genérica produzida por IA e recomenda o uso transparente de IA (pp. 1 e 5).

Os spines preservam bem essa intenção:

- `DESIGN.md` nomeia uma direção própria — **Neo-Swiss Festival Editorial** —, explicita sua proporção conceitual e adapta a intensidade a Customer, Organizer e Gate.
- A seção Brand & Style rejeita diretamente SaaS/dashboard genérico, glassmorphism, gradientes e cardificação indiscriminada.
- Do's and Don'ts tornam visíveis escolhas e descartes, em vez de depender de estética implícita.
- `EXPERIENCE.md` justifica cada experiência pelo trabalho do papel e evita preencher Organizer com analytics sem função.
- Paleta, fontes, spacing, raios, ícones e logo permanecem honestamente `TBD`; isso evita inventar aprovação inexistente.

A transparência operacional sobre quais ferramentas de IA foram usadas, em quais partes e quais artefatos devem ser versionados não aparece nos spines. **Não é gap de UX:** é obrigação de entrega/README já pertencente aos artefatos de produto e projeto. Os próprios spines, quando finalizados, devem ser versionados conforme o PDF.

## Itens opcionais e controle de escopo

O PDF marca como opcionais busca/filtro em conjunto, painel do Organizer, cancelamento com devolução ao estoque, mapa de assentos em tempo real, Docker Compose, testes e deploy (pp. 3–4), e dispensa nota fiscal, revenda, app nativo, recuperação de senha e envio por e-mail (p. 4).

O recorte dos spines é seguro:

- Busca simples permanece no MVP porque também é exigida explicitamente entre os requisitos funcionais do PDF; **filtros avançados** ficam fora.
- Organizer recebe somente gestão essencial, sem analytics/KPIs inventados.
- Mapa de assentos, cancelamento/refund, revenda, e-mail, app nativo, recuperação de senha e modo offline permanecem fora.
- Docker Compose, testes e deploy não foram convertidos em componentes ou fluxos de produto.
- `ShareAction` com fallback de copiar link não adiciona um canal novo; apenas torna operável o compartilhamento obrigatório.
- Tablet responsivo não cria aplicativo ou superfície de produto independente; é adaptação da aplicação web.

## Conflitos e ampliações acidentais

### Conflitos reais

Nenhum conflito real foi encontrado entre o PDF e `DESIGN.md`/`EXPERIENCE.md`.

### Ambiguidade do PDF resolvida conservadoramente

O PDF exige “navegação e busca” na p. 2, mas inclui “busca e filtro” na lista opcional da p. 4. Os spines mantêm busca simples por nome e deixam filtros avançados fora. Essa é a leitura de menor escopo que satisfaz o requisito obrigatório.

### Ressalva não bloqueante: pagamento em verificação

- **Origem no spine:** S04 inclui `verifying`; Voice and Tone, Payment e Edge-case Matrix descrevem resposta perdida e consulta ao estado autoritativo.
- **Origem no PDF:** somente `APPROVED` e `DECLINED` são obrigatórios.
- **Avaliação:** não é terceiro estado de produto nem novo resultado de pagamento; é estado transitório da interface para evitar reenvio inseguro. O mecanismo está corretamente reportado em Architecture Handoffs.
- **Limite recomendado:** Architecture pode definir reconciliação/consulta; não deve derivar disso processamento assíncrono complexo, webhook, fila ou novo escopo de gateway sem aprovação.

## Implicações de Architecture/Domain, sem alteração UX silenciosa

Os spines corretamente deixam como autoridades externas:

1. atomicidade de estoque e validação de uso único;
2. autenticidade/imprevisibilidade de QR e código;
3. câmera, permissões, compatibilidade e contexto seguro;
4. tempo, expiração, clock skew e recomposição do timer;
5. reconciliação após resposta perdida de pagamento;
6. detecção de rede na Gate online, sem consumo offline.

Nada nesses handoffs modifica o modelo de setores + quantidade, a duração de dez minutos, Ticketmaster, papéis ou os resultados de pagamento aprovados.

## Conclusão

O conjunto atual é fiel ao PDF oficial e está pronto para continuar o fechamento do BMAD UX. Não há requisito UX obrigatório ausente, conflito de regra ou feature opcional silenciosamente promovida. A recuperação de resposta perdida de pagamento deve permanecer enquadrada como estado seguro de interface com mecanismo a decidir em Architecture, sem criar um terceiro resultado de Payment.
