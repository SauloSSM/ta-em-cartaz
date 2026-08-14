# Revisão — Consistência de interação

**Artefatos avaliados:** `DESIGN.md` e `EXPERIENCE.md` atuais  
**Fontes de controle:** PDF oficial, Project Specification v1.2, UX Direction v0.1, reconciliações, `.memlog.md` e `IMPACT-REPORT.md`  
**Data:** 2026-08-12  
**Veredito:** **PASS CONDICIONAL** — nenhum conflito com fonte autoritativa e nenhuma alteração de Domain necessária; há cinco correções de especificação comportamental antes da finalização.

## Escopo e critérios

A revisão percorreu S01–S16, UJ-C01–UJ-G01, os três wireflows, componentes, estados, edge cases e matriz responsiva. Foram comparados gatilho, estado anterior, feedback, destino de sucesso, destino de falha, recuperação, terminologia, preservação de contexto, autorização, paridade entre input modalities e comportamento entre larguras.

Classificação de destino:

- **Correção necessária antes da finalização do UX:** lacuna ou ambiguidade no contrato comportamental atual.
- **Melhoria recomendada:** aumenta clareza ou testabilidade, sem bloquear o handoff.
- **Handoff para Architecture:** UX define o resultado observável; Architecture decide o mecanismo.
- **Detalhe futuro de Design System:** anatomia/tokens/variantes visuais sem mudança funcional.

## Findings

### IC-01 — Retorno de autenticação não cobre papel incorreto de forma determinística

- **Severidade:** Alta
- **Destino:** Correção necessária antes da finalização do UX
- **Evidência:** S03 cobre login e retorno à intenção; UJ-C01 cobre o visitante anônimo. `Erro e recuperação` menciona “papel correto”, mas o wireflow não define o que ocorre quando já existe sessão `ORGANIZER` ou `GATE`, ou quando o login concluído não é `CUSTOMER`.
- **Risco:** o CTA “Reservar” pode parecer inerte, substituir uma sessão sem explicação ou perder setor/quantidade. Isso também torna inconsistente a troca entre contas seedadas.
- **Correção proposta:** especificar que “Reservar” exige sessão `CUSTOMER`; sessão ausente ou de outro papel encaminha a S03 com mensagem contextual. A intenção não sensível é preservada, mas somente login `CUSTOMER` retorna ao fluxo e dispara revalidação. Deve existir ação explícita para encerrar/trocar sessão, sem criar gestão de papéis.

### IC-02 — Destinos após falha de revalidação e expiração estão incompletos

- **Severidade:** Alta
- **Destino:** Correção necessária antes da finalização do UX
- **Evidência:** o wireflow Customer diz “S02 com mensagem” quando vendas/estoque falham, e S04 possui `expired`, mas não fixa seleção restaurada/limpa, CTA seguinte nem comportamento quando apenas a quantidade deixou de caber.
- **Risco:** recuperação diferente entre login, refresh e expiração; possibilidade de mostrar uma seleção inválida como se ainda fosse reservável.
- **Correção proposta:** ao falhar a revalidação, retornar/focar a seleção de setores em S02, mostrar o motivo autoritativo e exigir nova confirmação. Preservar setor e quantidade apenas quando ainda válidos; ajustar ou limpar quando inválidos, nunca automaticamente reduzir quantidade sem confirmação. Em `EXPIRED`, remover ações de pagamento e oferecer retorno ao Event para uma nova tentativa, sujeita a nova revalidação.

### IC-03 — Estado `verifying` não possui saída observável completa

- **Severidade:** Alta
- **Destino:** Correção necessária antes da finalização do UX
- **Evidência:** Payment define entrada em `verifying` e proíbe novo envio; a matriz cita consulta autoritativa. Não estão enumeradas as saídas de UI para `CONFIRMED`, `HOLDING` após `DECLINED`, `EXPIRED` ou falha prolongada de consulta.
- **Risco:** tela sem saída, reenvio inseguro ou mensagem incompatível com o estado final.
- **Correção proposta:** definir transições: `CONFIRMED → S05`; `HOLDING + DECLINED → S04 declined` com retry somente se vigente; `EXPIRED → S04 expired`; consulta temporariamente indisponível → manter contexto, não autorizar novo pagamento e oferecer “Verificar novamente”. O mecanismo continua no handoff Architecture.

### IC-04 — “Validar próximo” não define reset operacional da Gate

- **Severidade:** Alta
- **Destino:** Correção necessária antes da finalização do UX
- **Evidência:** S16 oferece “Validar próximo”, mas não diz o que é mantido ou limpo. Câmera e código manual coexistem em S15, sem contrato explícito de alternância.
- **Risco:** reenvio do mesmo QR/código, perda desnecessária do Event selecionado ou validações sobrepostas na fila.
- **Correção proposta:** “Validar próximo” mantém o Event selecionado, limpa código/resultados anteriores, encerra qualquer submissão pendente e retorna foco ao scanner ativo ou ao campo manual conforme o modo atual. Alternar câmera ↔ manual não valida nem perde o Event; submissão manual exige ação inequívoca e fica bloqueada enquanto `validando`.

### IC-05 — Confirmação e Meus Ingressos têm recuperação vaga

- **Severidade:** Média
- **Destino:** Correção necessária antes da finalização do UX
- **Evidência:** S05 lista “carregando tickets, recuperação”, mas não descreve CTA ou precedência; UJ-C01 afirma que os Tickets aparecem. S06/S07 cobrem acesso posterior.
- **Risco:** uma falha de carregamento após pagamento aprovado pode ser interpretada como falha da compra ou incentivar novo pagamento.
- **Correção proposta:** S05 deve confirmar a compra independentemente do carregamento visual dos Tickets, nunca reexibir pagamento, e oferecer “Tentar carregar ingressos novamente” e “Ir para Meus Ingressos”. A mensagem deve afirmar que a compra foi confirmada; a recuperação consulta os Tickets já emitidos.

### IC-06 — Localização e persistência do acesso à Reservation vigente não estão fechadas

- **Severidade:** Média
- **Destino:** Melhoria recomendada
- **Evidência:** `ActiveReservationBanner` aparece nos componentes/reconciliação, e a regra manda recuperar/direcionar à HOLDING vigente, mas IA e responsive matrix não fixam onde o retorno fica disponível depois que o Customer sai do checkout.
- **Risco:** comportamento não uniforme entre catálogo, detalhe e mobile; hold existente fica tecnicamente ativo mas difícil de reencontrar.
- **Recomendação:** definir um padrão único de acesso persistente e responsivo para “Continuar reserva”, exibido quando o backend informa HOLDING vigente. O CTA sempre abre S04 existente e nunca cria novo hold. A composição exata pode ficar para mock/Design System.

### IC-07 — Organizer carece de contrato uniforme para alterações não salvas

- **Severidade:** Média
- **Destino:** Melhoria recomendada
- **Evidência:** S11–S13 e Interaction Primitives tratam validação, dialogs e Escape, mas não definem salvar, descartar ou navegar entre editor, setores e revisão com mudanças pendentes.
- **Risco:** perda silenciosa ou divergência entre desktop (sidebar) e mobile (navegação compacta).
- **Recomendação:** explicitar se edições são salvas por ação ou automaticamente. Se por ação, navegação com mudanças pendentes deve preservar dados ou pedir confirmação; erros nunca apagam entradas. Isto não altera campos editáveis nem regras pós-publicação.

### IC-08 — Exclusão de DRAFT e setor não fixam destino/foco após confirmação

- **Severidade:** Baixa
- **Destino:** Melhoria recomendada
- **Evidência:** dialog destrutivo está previsto, mas o pós-sucesso não está especificado.
- **Risco:** implementações inconsistentes e foco perdido.
- **Recomendação:** exclusão de Event DRAFT retorna a S09 com confirmação persistente/acessível; remoção de setor mantém S12 e move foco para o próximo item lógico ou para “Adicionar setor” quando a lista ficar vazia. Falha preserva o objeto e explica a razão.

### IC-09 — Busca pública e Ticketmaster compartilham padrão, mas suas ações e recuperação precisam permanecer distintas

- **Severidade:** Baixa
- **Destino:** Melhoria recomendada
- **Evidência:** ambas possuem inicial/buscando/resultados/vazio/erro, enquanto apenas Ticketmaster é dependência obrigatória para criar Event.
- **Risco:** microcopy genérica pode sugerir criação manual na indisponibilidade da Ticketmaster ou esconder que catálogo público continua utilizável.
- **Recomendação:** manter o mesmo primitive de busca, porém com CTAs contextuais: S01 limpa/refaz a busca; S10 tenta novamente e não oferece criação manual. Selecionar resultado em S10 deve declarar que inicia um Event interno em DRAFT a partir do snapshot.

### IC-10 — Estados inválido e não encontrado do link compartilhado devem ter política consistente

- **Severidade:** Média
- **Destino:** Handoff para Architecture
- **Evidência:** S08 agrupa “inválido/não encontrado”, enquanto logs/tokens sensíveis e privacidade são requisitos explícitos.
- **Risco:** respostas distinguíveis podem facilitar enumeração de `shareToken`; UX precisa de uma mensagem recuperável sem revelar detalhes sensíveis.
- **Resultado UX recomendado:** para visitante, usar estado público neutro e consistente quando o link não puder apresentar um Ticket, sem dados pessoais ou token completo. Architecture deve definir status HTTP, observabilidade e proteção contra enumeração. `VALID` e `USED` permanecem distintos como requerido.

### IC-11 — Timer após suspensão e mudança de conectividade exige contrato de sincronização

- **Severidade:** Alta
- **Destino:** Handoff para Architecture
- **Evidência:** o UX exige `expiresAt`, refresh/suspensão, backend temporal e expiração autoritativa; o mecanismo já está corretamente segregado.
- **Risco:** contagem local divergente, pagamento mostrado após expiração ou salto sem revalidação.
- **Resultado UX requerido:** recompor a contagem ao retomar visibilidade, revalidar antes de ação crítica e nunca mostrar tempo maior que o autorizado. Architecture decide clock offset, endpoint e política de refresh/polling.

### IC-12 — Pagamento em resposta perdida precisa de mecanismo idempotente de reconciliação

- **Severidade:** Alta
- **Destino:** Handoff para Architecture
- **Evidência:** IC-03 fecha saídas UX; `IMPACT-REPORT.md` já isola o mecanismo.
- **Risco:** cobrança simulada repetida, Reservation inconsistente ou tela eternamente incerta.
- **Resultado UX requerido:** consultar o estado existente sem disparar nova tentativa; Architecture define endpoint, identificador e repetição segura. Não implica adicionar `PENDING` ao Domain/PRD.

### IC-13 — Gate online precisa distinguir câmera indisponível de backend/rede indisponível

- **Severidade:** Média
- **Destino:** Handoff para Architecture
- **Evidência:** S15 lista ambos; manual resolve câmera, mas não resolve falta de rede.
- **Risco:** operador migrar para código manual esperando validar offline.
- **Resultado UX requerido:** câmera negada/ausente conduz ao manual; rede/backend indisponível bloqueia tanto QR quanto manual, explica que nenhum Ticket foi consumido e oferece retry. Architecture decide detecção e confirmação do resultado autoritativo.

### IC-14 — Paridade responsiva deve preservar ordem de leitura e ação, não somente redispor regiões

- **Severidade:** Média
- **Destino:** Detalhe futuro de Design System
- **Evidência:** matriz responsiva define colunas/pilhas, mas não fixa ordem quando o layout de duas regiões colapsa.
- **Risco:** timer, total, status de vendas ou ação primária mudarem de precedência entre desktop e mobile.
- **Detalhe a fechar:** especificar por componente a ordem DOM canônica. No detalhe: contexto do Event antes da compra e CTA junto da seleção; no checkout: estado/timer e resumo antes do controle de pagamento; na Gate: contexto do Event, captura/manual, resultado. CSS pode redispor sem alterar ordem semântica.

### IC-15 — Componentes críticos precisam de contratos de foco e loading por variante

- **Severidade:** Média
- **Destino:** Detalhe futuro de Design System
- **Evidência:** a lista pede estados e acessibilidade genericamente; GateResult, ReservationTimer, PublicationChecklist, PaymentResult e dialogs têm necessidades específicas.
- **Risco:** padrões visualmente coerentes mas operacionalmente diferentes.
- **Detalhe a fechar:** documentar, por variante, trigger, label, estado bloqueado, live-region, foco de entrada/saída e próximo CTA. Priorizar `GateResult`, `PaymentResult`, `ReservationTimer`, `PublicationChecklist`, `Dialog` e `ActiveReservationBanner`.

## Auditoria de consistência por fluxo

| Fluxo | Gatilho e navegação | Estados/recuperação | Terminologia | Responsividade | Resultado |
|---|---|---|---|---|---|
| Customer discovery → reserva | coerente; login antes do hold preservado | gaps IC-01/02/06 | `Event`, setor, Reservation/HOLDING coerentes | estrutura coerente; ordem a detalhar | PASS condicional |
| Checkout → pagamento → tickets | CTA e bloqueio de reenvio coerentes | gaps IC-03/05; handoff IC-12 | `APPROVED`/`DECLINED`, `CONFIRMED`/`EXPIRED` não conflitam | timer/resumo presentes em todas as larguras | PASS condicional |
| Ticket próprio/compartilhado | acesso, cópia e permanência após USED coerentes | política pública em IC-10 | `VALID`/`USED` coerentes | QR não deve ser miniaturizado; manual preservado | PASS |
| Organizer criação/publicação | Ticketmaster → DRAFT → review → PUBLISHED coerente | gaps IC-07/08/09 | campos bloqueados/editáveis e estados coerentes | desktop/mobile cobertos | PASS condicional |
| Gate seleção → validação | Event preserva contexto; QR/manual equivalentes | gap IC-04; handoff IC-13 | quatro resultados canônicos coerentes | mobile/tablet/desktop e teclado cobertos | PASS condicional |

## Checagens explícitas

- Hold de 10 minutos preservado; nenhuma Reservation antes de login `CUSTOMER`.
- Modelo setores + quantidade 1–6 preservado; nenhuma indicação de assentos.
- Ticketmaster permanece obrigatório para criação e não é identidade única do Event.
- Roles e limites de autorização preservados; nenhuma UI de cadastro/gestão de roles.
- Pagamento determinístico `APPROVED`/`DECLINED` preservado; `verifying` é estado de interface, não novo estado de Domain.
- Gate permanece online; código manual não é modo offline.
- Nenhum opcional (`LOW_AVAILABILITY`, analytics, filtros, refund etc.) foi promovido ao MVP.
- Nenhum conflito real com PDF, Domain v1.2 ou UX Direction reconciliada foi encontrado.

## Gate de finalização

Antes de marcar os spines como finais, incorporar IC-01 a IC-05 como precisão comportamental. IC-06 a IC-09 são melhorias recomendadas e podem ser incorporadas sem ampliar escopo. IC-10 a IC-13 devem permanecer explicitamente como resultados UX encaminhados à Architecture. IC-14 e IC-15 devem constar como contratos a fechar no Design System/mockups, sem congelar tokens visuais.
