---
title: 'Story 5.3 — Reconciliar resposta de pagamento perdida sem nova cobrança'
type: 'feature'
created: '2026-08-16'
status: 'done'
baseline_commit: 'HEAD'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md'
  - '{project-root}/docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-EliteDevTicket-2026-08-12/EXPERIENCE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-EliteDevTicket-2026-08-12/DESIGN.md'
  - '{project-root}/docs/engineering/java-standards.md'
  - '{project-root}/docs/engineering/react-standards.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Quando um cliente autenticado envia uma tentativa deliberada de pagamento (com `paymentAttemptId` específico) e a resposta HTTP não é recebida devido a instabilidade de conexão, timeout ou interrupção de rede, o cliente fica em um estado de resultado incerto. O sistema não pode presumir que o pagamento foi recusado (DECLINED), nem disparar automaticamente uma nova cobrança/tentativa com outro identificador, pois a tentativa anterior pode ter sido commitada com sucesso no backend (`APPROVED`, `Reservation CONFIRMED`, `Tickets` emitidos). O cliente precisa ser capaz de verificar e reconciliar autoritativamente o resultado da tentativa existente com o mesmo `paymentAttemptId`, mantendo o contexto da reserva, resumo e bloqueio de novos pagamentos durante a verificação, inclusive sobrevivendo a recarregamentos da página (`reload`).

**Approach:**
1. **Idempotência e Reconciliação no Backend (AD-8, AD-9, AD-23)**:
   - `ProcessPaymentAttemptUseCase`: Consulta idempotente em `paymentRepository.findById(paymentAttemptId)`.
   - Valida autorização/ownership do Customer na tentativa existente (`AUTH_FORBIDDEN` caso pertença a outro usuário).
   - Valida correspondência de fingerprint canônico `v1:customerId:reservationId:simulatedOutcome`.
   - Se já persistido, retorna imediatamente o registro `Payment` existente (seja `APPROVED` ou `DECLINED`) sem reexecutar o `FakePaymentGateway`, sem duplicar emissão de `Tickets`, sem alterar estoque e sem estender hold.
2. **Ciclo de Vida do `paymentAttemptId` e Persistência Transitória no Frontend**:
   - `uncertainPayment.ts`: Gerencia o armazenamento e recuperação em `sessionStorage` (`edt.uncertain-payment.v1:{reservationId}`) para tentativas em trânsito/incertas.
   - Em caso de perda de resposta/erro de rede após submissão: preserva o `paymentAttemptId` e o `simulatedOutcome` no `sessionStorage` e coloca a UI no estado de verificação (`verifying`).
   - Após reload: restaura automaticamente o `uncertainAttempt` para a reserva e exibe o estado de verificação sem gerar novo ID.
   - Ao obter resultado definitivo autoritativo (`APPROVED`, `DECLINED`, `EXPIRED`): limpa o estado transitório do `sessionStorage`.
   - Para nova tentativa deliberada após `DECLINED`: gera novo `paymentAttemptId` (AD-9).
3. **UX Acessível da Superfície S04 (Checkout)**:
   - `CheckoutView.tsx`: Renderiza alerta acessível `payment-verifying-alert` com `role="alert"` e foco automático, explicando a incerteza de rede e garantindo ausência de duplicidade.
   - Exibe metadados da tentativa (`paymentAttemptId` e `simulatedOutcome`) e botão de ação "Verificar Novamente" (`reconcile-payment-btn`).
   - Oculta botões padrão de pagamento durante o estado `verifying` para evitar cobranças acidentais simultâneas.
   - Preserva o aviso de ambiente simulado (`DemoEnvironmentNotice`), timer e resumo do pedido.
4. **Verificações e Testes Abrangentes**:
   - Backend: Testes com Testcontainers PostgreSQL cobrindo reconciliação de `APPROVED`, `DECLINED`, concorrência múltipla de reconciliação e proteção de ownership.
   - Frontend: Testes cobrindo entrada em `verifying`, replay idêntico com o mesmo `paymentAttemptId`, resolução para `CONFIRMED`/`DECLINED`, recuperação após reload e persistência de retry após falhas consecutivas de rede.

## Boundaries & Constraints

**Always:**
- Reconciliação usa sempre o mesmo `paymentAttemptId` e `simulatedOutcome` da tentativa incerta.
- Backend é a autoridade exclusiva de estado, tempo, estoque e emissão.
- Reconciliação nunca reexecuta o gateway nem emite tickets adicionais.
- Acesso à tentativa de outro Customer é terminantemente bloqueado (HTTP 403).
- O estado incerto sobrevive ao reload da página via `sessionStorage` e é limpo após confirmação definitiva.

**Never:**
- Não criar novo `paymentAttemptId` automaticamente após erro de rede.
- Não assumir `DECLINED` por erro de rede ou timeout.
- Não implementar Meus Ingressos ou share (Epic 6) ou Gate (Epic 7).

</frozen-after-approval>
