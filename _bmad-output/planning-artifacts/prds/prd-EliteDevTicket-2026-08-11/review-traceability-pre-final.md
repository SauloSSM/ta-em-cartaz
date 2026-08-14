# Revisão formal de consistência e rastreabilidade — EliteDevTicket

## Escopo e método

Artefatos auditados:

- `prd.md`
- `addendum.md`
- `reconcile-desafio-oficial.md`
- `reconcile-project-spec-v1.2.md`

A revisão verificou continuidade e unicidade dos identificadores, referências internas, cobertura das jornadas, relação entre métricas e requisitos, consistência do vocabulário e das máquinas de estado, conflitos internos, separação entre produto e implementação, itens abertos e cobertura das duas fontes reconciliadas.

## Veredito

**APROVADO PARA POLIMENTO E FINALIZAÇÃO, sem achado crítico ou alto.** O conjunto está consistente, rastreável e materialmente completo. Há duas melhorias não bloqueantes de rastreabilidade explícita; nenhuma exige reabrir discovery ou alterar uma regra de produto aprovada.

## Contagens e integridade dos identificadores

| Família | Intervalo | Quantidade | Continuidade | Unicidade |
|---|---:|---:|---|---|
| Jornadas | UJ-01 a UJ-03 | 3 | Contínua | Sem duplicatas |
| Métricas | SM-01 a SM-13 | 13 | Contínua | Sem duplicatas |
| Requisitos não funcionais | NFR-01 a NFR-20 | 20 | Contínua | Sem duplicatas |
| Requisitos funcionais | FR-01 a FR-53 | 53 | Contínua | Sem duplicatas |

Não foram encontrados IDs órfãos, saltos, colisões ou referências a IDs inexistentes. A contagem correta é **53 FRs**, não 52.

## Cobertura das jornadas

### UJ-01 — Organizer

Coberta diretamente por FR-01 a FR-19, especialmente autenticação/autorização, catálogo Ticketmaster, snapshot, rascunho, listagem, exclusão de DRAFT, setores, pendências, publicação e regras pós-publicação. SM-10 e SM-11 cobrem execução ponta a ponta e estabilidade. Não há etapa material da jornada sem requisito correspondente.

### UJ-02 — Customer

Coberta por FR-01 a FR-04 e FR-20 a FR-42: descoberta pública, autenticação no início da reserva, hold, timer, preço capturado, repetição segura, concorrência, expiração, pagamento aprovado/recusado, emissão, Meus Ingressos e compartilhamento. SM-01 a SM-06 e SM-10/11 cobrem seus principais invariantes e resultado operacional.

### UJ-03 — Participante e Gate

Coberta por FR-38 a FR-52: acesso ao ingresso próprio/compartilhado, seleção do evento, câmera, fallback manual, quatro resultados, consumo único, concorrência e auditoria. SM-07 a SM-11 e NFR-05/NFR-10 dão cobertura à correção, velocidade e clareza operacional.

## Rastreabilidade das métricas

| Métrica | Requisitos/evidências relacionados | Situação |
|---|---|---|
| SM-01 | FR-25, FR-29; addendum/Consistência e concorrência | Coberta |
| SM-02 | FR-27, FR-28; addendum/Idempotency-Key | Coberta |
| SM-03 | FR-31; addendum/expiração | Coberta |
| SM-04 | FR-33, FR-34 | Coberta |
| SM-05 | FR-35, FR-36 | Coberta |
| SM-06 | FR-35, FR-37 | Coberta |
| SM-07 | FR-46, FR-47, FR-50 | Coberta |
| SM-08 | FR-48 | Coberta |
| SM-09 | FR-51, NFR-10 | Coberta |
| SM-10 | Fluxo UJ-01 a UJ-03; FR-53; NFR-19 | Coberta |
| SM-11 | UJ-01 a UJ-03 e todas as capacidades funcionais | Coberta, de natureza transversal |
| SM-12 | Addendum/Estratégia de testes; FR-53 | Coberta, porém com rastreabilidade explícita melhorável |
| SM-13 | FR-53, NFR-19, NFR-20; addendum/Entrega e uso de IA | Coberta |

Todas as SM possuem suporte verificável. A maior parte está também referenciada nos cabeçalhos das capacidades funcionais. SM-12 depende principalmente do addendum, o que é apropriado para estratégia de testes, mas a ligação poderia ser declarada com maior precisão.

## Consistência de domínio e estados

- **Event:** `DRAFT` e `PUBLISHED` são usados de forma consistente. Não há estado de cancelamento introduzido; exclusão só existe para `DRAFT`.
- **Reservation:** `HOLDING`, `CONFIRMED` e `EXPIRED` permanecem coerentes. “Hold vigente” é corretamente definido por estado e tempo (`serverNow < expiresAt`), impedindo que atraso do scheduler bloqueie nova reserva.
- **Payment:** `APPROVED` e `DECLINED` são resultados de tentativas individuais e não foram confundidos com estados da Reservation.
- **Ticket:** `VALID` e `USED` são estados persistidos; `INVALID`, `ALREADY_USED` e `WRONG_EVENT` aparecem corretamente como resultados da validação, não como estados do Ticket.
- **Quantidade comprometida:** o glossário a define como `capacity - availableQuantity`; FR-18/19 impedem redução abaixo desse limite e preservam reservas já precificadas.
- **Autoridade temporal:** `serverNow >= startsAt` fecha vendas inclusive no instante inicial, de forma consistente em regras, NFR-04 e FR-23.

Não foram encontradas transições impossíveis, nomes de estados conflitantes ou uso inconsistente dos papéis `ORGANIZER`, `CUSTOMER` e `GATE`.

## Referências internas

- A referência de FR-15 a `§11` resolve corretamente para “Questões abertas não bloqueantes”.
- Todos os blocos de “Rastreabilidade” apontam para UJs e SMs existentes.
- Não há links ou referências numéricas quebradas.
- O addendum não depende de identificadores inexistentes e preserva sua função de apoio técnico.

## Separação entre PRD e addendum

A separação é adequada:

- O PRD contém comportamentos observáveis, invariantes, limites, jornadas, métricas e requisitos.
- O addendum contém stack, arquitetura, JWT/BCrypt, PostgreSQL/Flyway, locking, `Idempotency-Key`, constraints, gateway fake, estratégia de testes, alternativas descartadas e obrigações de entrega/IA.
- O mecanismo de idempotência foi corretamente retirado do FR-28 e mantido no contrato técnico.
- Não há endpoint, controller, ORM ou mecanismo de lock prescrito no corpo do PRD.

Os seis grupos de detalhes técnicos citados em `reconcile-project-spec-v1.2.md` — DTOs/camadas, endpoints e erros, validação de schema/migrations, segredo Ticketmaster, modelo/state machines detalhados e frameworks de teste — são candidatos legítimos para arquitetura/API contract. Sua ausência do PRD não é gap de produto.

## Questões abertas e assumptions

- Não existem tags `[ASSUMPTION]` nem `[NOTE FOR PM]` pendentes.
- As três questões abertas são de UX/compatibilidade e não bloqueiam arquitetura, implementação do domínio ou decomposição em épicos.
- Fallback visual, limiares do timer e validação prática da câmera têm owner/etapa implícita clara (UX ou implementação) e preservam comportamentos obrigatórios já definidos.

## Cobertura das fontes

### PDF oficial

Cobertura material completa dos fluxos obrigatórios, papéis, persistência, proteção contra venda/uso duplo, integração externa, pagamento fake, seeds, README, prazo, repositório público, submissão, transparência no uso de IA e bônus de deploy. Os três ajustes sugeridos pela reconciliação foram absorvidos: autoria/UX não genérica, rejeição explícita de QR/código falsificado em FR-49 e redação condicional correta sobre versionamento de artefatos produzidos.

### Project Specification v1.2

Cobertura completa das regras de produto e invariantes. As decisões posteriores — limite de seis ingressos, um hold vigente por Customer/Event, acesso público ao catálogo, Gate online, auditoria, logout e permanência do share link após uso — complementam a v1.2 sem contradizê-la. Nenhuma regra foi silenciosamente substituída.

## Achados

### M-01 — SM-12 tem ligação indireta com o artefato de testes (médio-baixo)

**Evidência:** SM-12 exige que “todos os testes críticos definidos” passem; a estratégia que define suas famílias está no addendum, enquanto o bloco 8.9 associa SM-12 somente a FR-53.

**Impacto:** não há lacuna funcional, mas a rastreabilidade automática ou uma futura decomposição pode interpretar FR-53 (seeds) como a única realização de SM-12.

**Correção proposta:** durante o polimento, tornar explícita a ligação de SM-12 à seção “Estratégia de testes” do addendum ou criar uma pequena nota de verificação/critério de entrega, sem transformá-la em feature da aplicação.

### L-01 — SM-11 é transversal e não possui critério de teste delimitado (baixo)

**Evidência:** SM-11 combina conclusão de todos os happy paths com “zero respostas HTTP 500”; os cabeçalhos de quase todas as capacidades apontam para ela, mas não enumeram cenários mínimos além das jornadas.

**Impacto:** a intenção é clara e não bloqueante, porém a interpretação de “happy paths” pode variar na futura suíte E2E.

**Correção proposta:** derivar posteriormente três cenários E2E mínimos, um por UJ, mantendo o PRD como está ou acrescentando apenas uma referência aos UJ-01 a UJ-03 na definição de SM-11.

## Conclusão

O PRD pode avançar para polimento final. Não há conflito interno, quebra de numeração, fonte sem cobertura, decisão técnica indevidamente promovida a requisito de produto ou questão bloqueante. As duas observações acima são melhorias de precisão de rastreabilidade, não correções de escopo.
