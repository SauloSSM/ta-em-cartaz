---
title: 'Story 8.2 — Documentar avaliação, limitações e uso responsável de IA'
type: 'documentation'
created: '2026-08-17'
status: 'done'
baseline_commit: 'NO_VCS'
review_loop_iteration: 0
context:
  - '{project-root}/README.md'
  - '{project-root}/docs/02-domain/ELITE_DEV_PROJECT_SPEC_v1.2.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-EliteDevTicket-2026-08-12/ARCHITECTURE-SPINE.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** O projeto precisa fornecer um caminho de avaliação (*evaluation path*) claro, rápido e honesto no `README.md` para que qualquer avaliador consiga executar a aplicação, navegar pelas três jornadas principais (Organizer, Customer, Gate), compreender a matriz de cobertura dos requisitos do desafio, entender as decisões de arquitetura e trade-offs, checar as limitações conscientes do MVP e avaliar o uso responsável e transparente de IA no desenvolvimento.

**Approach:** Atualizar e estruturar o `README.md` com um guia objetivo "START HERE", instruções de execução completas (Docker e local), credenciais demo seedadas, passo a passo detalhado das 3 jornadas (com os 4 desfechos da Gate e links compartilháveis), matriz de cobertura de requisitos, registro de limitações reais confirmadas pelo projeto, trade-offs técnicos justificados e documentação transparente do uso de IA (framework BMAD, agentes, aceleradores, verificação humana e testes reais para casos críticos, sem segredos ou narrativas fictícias).

## Boundaries & Constraints

**Always:** Manter a documentação técnica já validada na Story 8.1. Apresentar dados factuais e caminhos reais que existem no código. Não inventar narrativa pessoal de candidato (usar placeholders explícitos). Documentar limitações sem convertê-las em desculpas. Garantir transparência no uso de IA e nos testes executados.

**Never:** Não alterar código funcional, migrations, banco de dados, regras de negócio, segurança ou UI. Não versionar credenciais ou segredos reais. Não avançar para a Story 8.3.

</frozen-after-approval>

## Tasks & Acceptance

**Execution:**
- [x] Atualizar `README.md` com seção "START HERE", jornadas do Organizer, Customer e Gate, matriz de requisitos, limitações conscientes, trade-offs de engenharia e uso transparente de IA.
- [x] Atualizar `sprint-status.yaml` refletindo Story 8.2 em status `review`.
