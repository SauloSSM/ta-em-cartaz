# Tá em Cartaz — UX Interaction & Feedback Contract

**Status:** Reviewed v1.1 baseline; backend-dependent recovery paths remain explicitly flagged  
**Goal:** No important action may leave the user wondering whether the system reacted.

---

# 1. Core promise

The interface must continuously answer:

```text
Where am I?
What can I do?
What is happening now?
What happened?
What should I do next?
Is my state safe?
```

---

# 2. Async action contract

Every important async mutation follows:

```text
IDLE
↓
PENDING
↓
SUCCESS | ERROR | UNKNOWN_RESULT
```

`UNKNOWN_RESULT` matters for operations where a timeout does not prove failure, especially:

- reservation creation;
- payment;
- ticket validation if request outcome cannot be confirmed.

Never convert uncertainty into a false success or false failure.

---

# 3. Button interaction contract

All important buttons:

```text
default
hover
focus-visible
pressed
loading
disabled
```

Rules:

- prevent accidental double submission;
- loading must preserve button width where possible;
- button label should describe the actual work;
- do not use only spinner with no context;
- mobile cannot depend on hover.

Example:

```text
GARANTIR INGRESSOS →
↓
GARANTINDO SEUS INGRESSOS...
↓
RESERVA GARANTIDA
```

---

# 4. Sector selection and reservation intent

A Reservation represents **one TicketSector**. The Event Detail UI must not behave like a multi-sector cart.

Before a hold exists:

```text
choose one sector
↓
choose quantity for that sector
↓
see unit price + total
↓
reserve
```

Only one sector can be the active purchase intent at a time. If the user changes sector before creating the hold, the previous quantity/total selection must be cleared or explicitly transferred only if the product later defines such behavior.

Recommended visual behavior:

```text
unselected sector → SELECT
selected sector   → quantity stepper + selected state
sold out          → unavailable, no increment
```

Do not present simultaneously editable quantities for several sectors unless the domain is intentionally changed to a cart model.

---

# 5. Authentication branch before HOLD

Public browsing remains unauthenticated. The purchase branch is:

```text
visitor selects sector + quantity
↓
clicks Reserve
↓
if unauthenticated: preserve non-sensitive intent and go to Login
↓
authentication succeeds
↓
restore event + sector + quantity
↓
create/recover Reservation HOLDING
↓
show pending/result feedback
```

Never show `Reserva garantida` before the authenticated reservation endpoint confirms a HOLD.

Do not persist sensitive payment data as part of intent restoration.

---

# 6. Reservation creation

Before request:

```text
2 ingressos
R$ 298
[ GARANTIR INGRESSOS → ]
```

During request:

```text
[ GARANTINDO SEUS INGRESSOS... ]
```

- disable repeat clicks visually;
- backend idempotency remains authoritative.

Success:

```text
✓ Reserva garantida
Seus ingressos estão reservados.
09:42 restantes.
```

If navigating immediately to checkout, this confirmation may live at the checkout top.

Insufficient stock:

```text
Esse setor acabou de ficar indisponível
ou não possui mais a quantidade selecionada.

[ ESCOLHER OUTRA QUANTIDADE ]
```

Sales closed:

```text
Vendas encerradas
O evento continua disponível para consulta,
mas não aceita novas reservas.
```

Do not show generic `Algo deu errado` if a domain-specific error is known.

---

# 7. Reservation timer

Backend time is authoritative.

Visual phases:

```text
10:00 → 03:00  NORMAL
02:59 → 01:00  WARNING
00:59 → 00:00  CRITICAL
after expiry    EXPIRED
```

NORMAL must not look panic-inducing.

Do not:

- blink;
- pulse aggressively;
- announce every second to screen readers.

Meaningful accessibility announcements may occur at milestones such as:

- 3 minutes;
- 1 minute;
- expired.

Expired state:

```text
Sua reserva expirou.
Os ingressos foram liberados novamente.

[ VOLTAR AOS SETORES ]
```

---

# 8. Active reservation recovery

If a customer leaves checkout while HOLDING remains active:

```text
Você possui uma reserva ativa
07:22 restantes

[ CONTINUAR PAGAMENTO ]
```

Refresh must not visually create a fresh 10-minute timer.

Timer is derived from backend `expiresAt`.

---

# 9. Payment contract

The user must never be left asking:

> “Fui cobrado?”

### Idle

```text
[ FINALIZAR PAGAMENTO → ]
```

### Pending

```text
PROCESSANDO PAGAMENTO...
Não feche esta página.
```

### Approved

```text
✓ PAGAMENTO APROVADO
Seus ingressos estão prontos.

[ VER MEUS INGRESSOS → ]
```

### Declined

```text
✕ PAGAMENTO RECUSADO
Nenhuma cobrança foi realizada.
Seus ingressos continuam reservados.

04:17 restantes

[ TENTAR NOVAMENTE → ]
```

### Unknown result

If request outcome is genuinely uncertain:

```text
Estamos confirmando o resultado do pagamento.
Não tente pagar novamente ainda.
```

Then reconcile with backend status before presenting another payment action.

### Demo notice

Always make fake-payment context explicit:

```text
Ambiente de demonstração.
Nenhuma cobrança real será realizada.
```

Do **not** implement `Salvar cartão` unless a real approved product decision adds it.

---

# 10. Form feedback

Validation rules:

- field error appears close to the field;
- text explains the issue;
- border color alone is insufficient;
- focus should move only when helpful and predictable;
- preserve user-entered values after recoverable errors.

Example:

```text
Número do cartão
[ 1234 ... ]

Número inválido. Confira os dígitos informados.
```

---

# 11. Organizer actions

## Save

```text
SALVAR
↓
SALVANDO...
↓
✓ Alterações salvas
```

Small successful save may use a toast/status message.

## Publish

Publication is critical and should produce persistent page-level feedback:

```text
PUBLICANDO...
↓
✓ EVENTO PUBLICADO
Agora ele está visível para o público.
```

## Immutable field

Do not merely disable without explanation:

```text
Data do evento  🔒
Este campo não pode ser alterado após a publicação.
```

---

# 12. Ticket sharing

Sharing is per **individual Ticket**, because each Ticket owns its own `shareToken`.

When a purchase issued multiple tickets, the UI must make the current ticket identity clear (for example `Ingresso 1 de 2`) before sharing. A generic `Compartilhar compra` action must not silently share multiple ticket credentials.

Primary:

```text
[ COMPARTILHAR ESTE INGRESSO ]
```

If native share works:

- open platform share UI.

If unsupported:

```text
[ COPIAR LINK ]
```

Success:

```text
✓ Link do ingresso copiado
```

User cancellation of native share is not a scary application error.

---

# 13. Gate contract

Gate flow:

```text
SELECT EVENT
↓
SCAN / MANUAL CODE
↓
VALIDATING
↓
ONE DOMINANT RESULT
```

Do not display all four possible outcomes as permanent cards on the operational scanner screen.

### Valid

```text
✓ INGRESSO VÁLIDO
Entrada autorizada.

[ PRÓXIMO INGRESSO → ]
```

### Invalid

```text
✕ INGRESSO INVÁLIDO
Código não encontrado ou inválido.

[ TENTAR NOVAMENTE → ]
```

### Already used

```text
! INGRESSO JÁ UTILIZADO
Este ingresso já foi validado anteriormente.

[ PRÓXIMO INGRESSO → ]
```

### Wrong event

```text
↔ EVENTO ERRADO
Este ingresso pertence a outro evento.

[ PRÓXIMO INGRESSO → ]
```

Never rely on color alone.

---

# 14. Camera failure

If camera access is denied/unavailable:

```text
Não conseguimos acessar sua câmera.

Você pode permitir o acesso nas configurações
ou continuar usando o código manual.

[ TENTAR CÂMERA NOVAMENTE ]
```

Manual-code path must remain immediately usable.

---

# 15. Content loading

Use action-specific language where meaningful:

```text
Carregando eventos...
Garantindo seus ingressos...
Processando pagamento...
Validando ingresso...
```

Skeletons should reserve layout space and prevent jumps.

---

# 16. Empty states

Use context-specific states:

```text
Nenhum evento encontrado
Nenhum ingresso ainda
Nenhum evento criado
Nenhum setor criado
```

Each state should offer a useful next action where possible.

---

# 17. Network / offline

Offline banners are advisory, not authoritative.

Example:

```text
Você parece estar sem conexão.
Algumas ações podem não funcionar.
```

Actual API failures determine operation result.

For uncertain reservation/payment result, do not tell the user to blindly retry.

---

# 18. Session expiration

If session expires during a task:

- preserve the current intent where safe;
- route to login;
- after login, restore user to the intended flow;
- never silently discard a valid active reservation.

---

# 19. Feedback hierarchy

### Toast / transient
Use for:

- copied link;
- small save success;
- noncritical confirmation.

### Inline / page-level persistent
Use for:

- reservation created;
- reservation expired;
- payment processing;
- payment approved/declined;
- publication;
- gate validation;
- critical errors.

Critical outcomes must not disappear after 3 seconds.



---

# 20. Domain error → UX response contract

Frontend must map known backend/domain errors to specific user-facing meaning and recovery. Baseline mapping:

| Domain/API code | User meaning | Preferred next action |
|---|---|---|
| `EVENT_NOT_PUBLISHED` | Evento ainda não disponível para compra | Voltar aos eventos |
| `SALES_CLOSED` | Vendas encerradas | Consultar evento / voltar |
| `EVENT_NOT_FOUND` | Evento não encontrado | Voltar à listagem |
| `SECTOR_NOT_FOUND` | Setor não está mais disponível | Recarregar setores |
| `INSUFFICIENT_AVAILABILITY` | Quantidade selecionada não está mais disponível | Atualizar estoque e escolher outra quantidade/setor |
| `RESERVATION_NOT_FOUND` | Reserva não pôde ser recuperada | Voltar ao evento / revalidar sessão |
| `RESERVATION_EXPIRED` | Hold expirou | Voltar aos setores |
| `RESERVATION_ALREADY_CONFIRMED` | Compra já foi confirmada | Ir para Meus Ingressos |
| `PAYMENT_DECLINED` | Pagamento simulado recusado; hold segue ativo se ainda válido | Tentar novamente enquanto houver tempo |
| `TICKET_INVALID` | Credencial inválida | Tentar novamente/manual |
| `TICKET_ALREADY_USED` | Ingresso já consumido | Próximo ingresso |
| `WRONG_EVENT` | Ingresso é de outro evento | Próximo ingresso / trocar evento quando apropriado |
| `FORBIDDEN_RESOURCE` | Usuário não possui permissão | Voltar para área permitida |
| `IDEMPOTENCY_CONFLICT` | A mesma chave foi reutilizada com intenção diferente | Não repetir automaticamente; recuperar estado ou iniciar nova intenção explicitamente |
| `VALIDATION_ERROR` | Dados enviados são inválidos | Mostrar erros próximos aos campos |

Do not expose raw stack traces or backend exception messages.

---

# 21. Backend-dependent interaction blockers

The following behaviors are desired UX but cannot be invented by frontend code. Before implementing the affected flow, inspect the current Story/API contract:

- **payment unknown-result reconciliation**: how to refetch the authoritative result after timeout;
- **active reservation recovery on refresh**: which endpoint returns current HOLDING reservation / `expiresAt`;
- **login intent restoration**: approved transport/state mechanism;
- **demo payment outcome trigger**: the exact deterministic way an evaluator produces APPROVED vs DECLINED;
- **private/shared ticket DTOs**: which ticket credential/status fields are exposed safely.

If the API cannot support a required UX state, STOP and flag the gap rather than simulating authoritative state locally.
