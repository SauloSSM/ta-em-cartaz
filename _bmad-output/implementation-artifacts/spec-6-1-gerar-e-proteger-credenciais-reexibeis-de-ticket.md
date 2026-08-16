---
title: 'Story 6.1 — Gerar e proteger credenciais reexibíveis de Ticket'
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

**Problem:** Cada ingresso (Ticket) emitido após uma compra confirmada precisa possuir credenciais criptográficas de entrada (para apresentação física/digital via QR e código manual) e de compartilhamento (para envio de link seguro a terceiros). Essas credenciais precisam ser:
1. Imprevisíveis e não deriváveis de identificadores públicos/sequenciais (como ticketId, reservationId, ordinal ou timestamp);
2. Distintas em responsabilidade e valor (`validationToken` != `shareToken`);
3. Resilientes a erros humanos de digitação no caso do código manual (`manualCode`), utilizando alfabeto Crockford Base32 livre de ambiguidades, agrupamento visual legível e normalização transparente;
4. Persistentes e reexibíveis de forma estável, garantindo que consultas repetidas, reloads ou logins do comprador não regenerem os segredos nem alterem o ingresso;
5. Protegidas contra vazamentos em logs, respostas públicas, stack traces ou terceiros.

**Approach:**
1. **Infraestrutura Herdada da Story 5.2**:
   - Tabela `tickets` (migration `V9__create_tickets.sql`) com constraints `UNIQUE` para `validation_token`, `manual_code`, `share_token` e `(reservation_id, ordinal)`.
   - Entidade de persistência `TicketEntity` mapeando os atributos e índices necessários.
   - `TicketIssuanceService` coordenando emissão atômica de 1..N ingressos após confirmação de pagamento, preservando ingressos existentes caso já emitidos.
2. **Geração e Normalização de Credenciais (AD-13)**:
   - `TicketCredentialGenerator`:
     - `generateValidationToken()`: Gera token de alta entropia (32 bytes via `SecureRandom`, 64 caracteres hexadecimais).
     - `generateShareToken()`: Gera token de alta entropia distinto (32 bytes via `SecureRandom`, 64 caracteres hexadecimais).
     - `generateManualCode()`: Gera 10 caracteres do alfabeto Crockford Base32 (`0123456789ABCDEFGHJKMNPQRSTVWXYZ`), excluindo expressamente letras ambíguas (`I`, `L`, `O`, `U`).
     - `normalizeManualCode()`: Normaliza caixa para maiúsculas, remove espaços e separadores, e mapeia caracteres ambíguos (`I`/`i` -> `1`, `L`/`l` -> `1`, `O`/`o` -> `0`).
     - `formatGrouped()`: Formata para exibição em blocos `XXXX-XXXX-XX`.
3. **Invariantes de Domínio e Persistência**:
   - `Ticket`: Invariantes explícitas no construtor validando não-nulos, ordinal entre 1 e 6, credenciais não em branco e `validationToken != shareToken`.
   - `JpaTicketRepository`: Normalização transparente em `findByManualCode` antes do lookup SQL, e guarda de segurança com trim em `findByValidationToken` e `findByShareToken`.
4. **Verificação Abrangente com Testcontainers PostgreSQL**:
   - `TicketPersistenceIntegrationTest`: Validação de persistência, constraints de unicidade no banco, normalização de busca por código manual e isolamento entre Customers.
   - `TicketDomainTest`: Validação de limites, restrições de nulidade e regras de negócio do record `Ticket`.
   - `TicketCredentialGeneratorTest`: Validação de 1000 iterações com 0 colisões, entropia de tokens e invariância matemática na normalização.
   - `TicketIssuanceServiceTest`: Validação de emissão com ordinais 1..N, independência de credenciais e estabilidade de reexibição sem regerar tokens.

## Boundaries & Constraints

**Always:**
- `validationToken` e `shareToken` gerados com CSPRNG de 32 bytes e alta entropia.
- `validationToken` != `shareToken` para cada ingresso.
- `manualCode` gerado em Crockford Base32 (10 caracteres) e normalizado antes de qualquer lookup.
- As três credenciais são persistidas no banco e reexibíveis sem mutação ou regeneração.
- Isolamento estrito entre Customers na consulta de ingressos.
- Segredos nunca são logados nem enviados a serviços externos.

**Never:**
- Não usar IDs previsíveis como credenciais de validação ou compartilhamento.
- Não implementar tela "Meus Ingressos" (Story 6.2) nem página pública (Story 6.3) nesta Story.
- Não implementar validação de portaria/Gate (Epic 7).
- Não alterar migrations V1–V9.

</frozen-after-approval>
