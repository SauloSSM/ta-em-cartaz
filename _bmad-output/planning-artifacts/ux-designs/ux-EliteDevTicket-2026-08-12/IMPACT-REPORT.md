# Relatório de impactos — BMAD UX

**Status:** parada obrigatória antes da redação dos spines  
**Data:** 2026-08-12

Este relatório separa descobertas de UX que afetam Domain ou Architecture. Nenhuma regra de domínio foi alterada e nenhuma feature foi adicionada ao MVP.

## 1. Conflito de ordem no fluxo de reserva

**Origem UX:** `docs/04-ux/UX_DIRECTION_v0.1.md` descreve `Reserve → Login if necessary → HOLDING`.

**Origem Domain:** a Project Specification v1.2 e o PRD aprovado exigem autenticação como `CUSTOMER` antes de iniciar a Reservation. O hold pertence a um Customer e a criação deve respeitar ownership, limite de hold vigente e idempotência.

**Risco:** criar hold anônimo mudaria ownership, autenticação e regras de reserva.

**Correção proposta, sem mudança de Domain:**

`Escolher setor e quantidade → acionar Reservar → se anônimo, Login → retornar com a intenção preservada → backend cria ou recupera a Reservation vigente → Checkout HOLDING`.

A intenção anterior ao login é estado transitório de interface; não reduz estoque nem cria Reservation.

## 2. Câmera, permissões e contexto seguro

**Origem:** Gate exige câmera como fluxo principal e código manual como fallback.

**Impacto Architecture:** a implementação precisa validar APIs de câmera, HTTPS/contexto seguro, permissão negada, câmera ausente e seleção de dispositivo. UX pode definir os estados e a recuperação, mas não escolher biblioteca ou mecanismo.

**Tratamento UX proposto:** estados `solicitando permissão`, `câmera ativa`, `permissão negada`, `câmera indisponível`, `QR não lido` e fallback sempre visível para código manual.

## 3. Resposta perdida de pagamento

**Origem:** UX Direction lista `payment response lost`; Domain determina que backend é autoritativo e que apenas `CONFIRMED` ou `EXPIRED` vence.

**Impacto Architecture:** é necessário um meio de consultar/reconciliar o estado autoritativo da Reservation/Payment após timeout ou perda de resposta. Polling, retry e endpoints não são decisões de UX.

**Tratamento UX proposto:** manter contexto, informar que o resultado está sendo verificado e somente mostrar sucesso ou recusa após resposta autoritativa. Não repetir cobrança automaticamente pela interface.

## 4. Sincronização do timer

**Origem:** hold fixo de dez minutos; backend é autoridade temporal.

**Impacto Architecture:** a interface precisa receber `expiresAt` autoritativo e reconciliar expiração após refresh, suspensão da aba ou relógio local divergente. O mecanismo de sincronização não pertence ao UX.

**Tratamento UX proposto:** timer derivado de `expiresAt`, rechecagem autoritativa em ações críticas e estado expirado quando o backend assim determinar. Nunca pausar, reiniciar ou prorrogar o hold.

## 5. `LOW_AVAILABILITY`

**Origem:** UX Direction propõe o estado visual `LOW_AVAILABILITY`, mas Domain não define seu limiar.

**Impacto potencial:** se o backend precisar calcular ou expor um estado novo, há impacto de API/Domain. Se for derivado apenas no frontend, ainda falta uma regra aprovada e consistente.

**Correção proposta:** não usar `LOW_AVAILABILITY` no contrato final até existir limiar aprovado. Manter apenas disponibilidade numérica e estados autoritativos `AVAILABLE`, `SOLD_OUT` e `SALES_CLOSED` que possam ser derivados sem nova regra.

## 6. Operação offline

**Origem:** `offline` aparece como edge case e `OfflineBanner` como componente candidato.

**Decisão preservada:** Gate é online; não existe validação offline nem sincronização posterior no MVP.

**Tratamento UX permitido:** detectar perda de conexão, bloquear validação, explicar que a conexão é necessária e oferecer nova tentativa. Fila local, consumo offline ou sincronização posterior seriam novas features e permanecem proibidos.

## Decisão necessária para retomar

Confirmar a correção do fluxo de login antes da reserva e autorizar que os demais itens sejam documentados como **handoffs para Architecture**, usando apenas os tratamentos UX descritos acima.
