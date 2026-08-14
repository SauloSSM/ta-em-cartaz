# Reconciliação — Desafio Elite Dev 2026 (PDF oficial)

## Escopo da verificação

- **Fonte:** `Docs/Desafio-Elite-Dev-2026.pdf`
- **Artefatos comparados:** `prd.md` e `addendum.md`
- **Objetivo:** verificar cobertura, contradições, perda de intenção qualitativa, obrigações de entrega e uso de IA e atribuições indevidas.

## Veredito

**Cobertura substancialmente completa, com um gap qualitativo médio e dois ajustes de precisão recomendados.** Os fluxos funcionais obrigatórios, stack permitida, dados de teste, documentação, prazo, entrega, uso de IA e bônus de deploy estão representados. Não foi encontrada contradição funcional bloqueante com o PDF.

## Matriz de cobertura

| Tema do PDF | Cobertura nos artefatos | Avaliação |
|---|---|---|
| Navegação, busca e detalhes de eventos publicados | PRD FR-20 a FR-23 | Coberto |
| Criação e gerenciamento pelo Organizer | PRD FR-05 a FR-19 | Coberto e detalhado |
| Reserva por quantidade | PRD FR-24 a FR-31 | Coberto; escolha legítima entre as duas alternativas do PDF |
| Pagamento aprovado e recusado | PRD FR-32 a FR-37 | Coberto |
| Meus Ingressos, QR e compartilhamento | PRD FR-38 a FR-42 | Coberto |
| Gate: QR por câmera, código manual e quatro resultados | PRD FR-43 a FR-52 | Coberto |
| Três papéis e autorização | PRD FR-01 a FR-04 | Coberto |
| Persistência e proteção contra venda/uso duplo | PRD SM-01 a SM-08, FR-29, FR-35, FR-37 e FR-50; addendum | Coberto e fortalecido |
| React + backend permitido + banco documentado | Addendum, seção Stack; PRD NFR-19/NFR-20 e restrições | Coberto |
| README detalhado e declaração de limitações | PRD NFR-19 e NFR-20; addendum Entrega e uso de IA | Coberto |
| Seeds: 1 Organizer, 2 Customers, 1 Gate e evento publicado avaliável | PRD FR-53 | Coberto com terminologia de domínio mais correta |
| Prazo de sete dias | PRD registra recebimento em 11/08 e entrega em 18/08/2026 | Coberto conforme confirmação do candidato |
| Repositório público, commits descritivos e formulário | PRD §10; addendum Entrega e uso de IA | Coberto |
| Declaração de ferramentas/partes com e sem IA | PRD §10; addendum Entrega e uso de IA | Coberto |
| Versionamento de artefatos produzidos | PRD §10; addendum | Coberto; como os artefatos existem, a recomendação se torna aplicável |
| Deploy opcional com bônus de 1 ponto | PRD §10; addendum | Coberto |

## Gaps, conflitos e correções propostas

### 1. Intenção qualitativa do desafio está sub-representada (médio)

**Origem:** o PDF insiste que o escopo é pequeno para tornar visível como o candidato pensa; pede evitar interface genérica de IA, mostrar autoria, explicar escolhas e descartes, e valoriza interface agradável, tratamento de erros, iniciativa, criatividade e dedicação.

**Estado atual:** o PRD captura clareza, segurança, explicabilidade e personalidades distintas das três superfícies. O addendum registra alternativas descartadas e obrigação de documentar IA. Entretanto, não preserva explicitamente a expectativa de identidade visual deliberada/não genérica nem de explicar no README as escolhas autorais de UX e escopo.

**Correção proposta:** acrescentar em restrições/critérios de qualidade que a entrega deve demonstrar decisões autorais, evitar aparência genérica e documentar no README as principais escolhas de produto/UX e alternativas descartadas. Isso não cria feature nova; preserva o critério de avaliação do PDF.

### 2. Segurança do QR está descrita como imprevisibilidade, mas não como rejeição de falsificação (baixo)

**Origem:** o PDF exige ingresso com QR que não possa ser forjado.

**Estado atual:** PRD FR-39 e addendum exigem identificadores aleatórios, únicos e não previsíveis, além de validação online. Isso aponta para o resultado correto, mas não declara diretamente que um payload inventado ou adulterado deve ser rejeitado como `INVALID`.

**Correção proposta:** explicitar, em FR-39 ou no requisito de validação inválida, que QR/código adulterado ou não emitido pelo sistema não pode ser aceito. A implementação do mecanismo permanece no addendum/arquitetura.

### 3. Atribuição das obrigações de IA pode ser formulada com mais precisão (baixo)

**Origem:** o PDF recomenda o uso de IA e diz que ele não retira pontos; exige/solicita relatar ferramentas, partes assistidas e partes sem IA. Para artefatos como specs, PRD, BMAD ou contexto, instrui versioná-los **se foram produzidos**.

**Estado atual:** PRD §10 apresenta conjuntamente esses itens como “restrições da avaliação” e declara que artefatos devem ser versionados. Neste projeto a consequência é correta, pois os artefatos foram efetivamente produzidos, mas a redação pode sugerir que produzir PRD/BMAD era obrigatório no enunciado.

**Correção proposta:** escrever “artefatos de PRD/BMAD/contexto de IA produzidos durante o projeto devem ser versionados” e preservar que o uso de IA é recomendado/permitido, enquanto a transparência sobre seu uso é a obrigação relevante.

## Ambiguidades do PDF resolvidas sem conflito

- O PDF traz “navegação e busca” como requisito funcional e, depois, “busca e filtro” entre opcionais. O PRD adota busca simples por título como obrigatória e deixa filtros avançados fora do MUST. Essa interpretação preserva o requisito obrigatório sem inflar o escopo.
- O PDF permite mapa de assentos ou quantidade de ingressos. O recorte por setores e quantidade é compatível e está devidamente explicado.
- O PDF usa a expressão informal “evento publicado com ingressos disponíveis” para os seeds. O PRD usa corretamente `TicketSectors` com estoque disponível, pois no domínio detalhado o `Ticket` só nasce após pagamento aprovado. Não há perda do objetivo de avaliabilidade.
- Cancelamento com devolução ao estoque, Docker Compose, testes e aplicação publicada aparecem como opcionais. O PRD pode excluir cancelamento e ainda priorizar os demais sem contradizer o desafio.

## Omissões não encontradas

Não foram encontradas omissões nos fluxos obrigatórios, papéis, integração externa, pagamento simulado, seeds, README, prazo, repositório público, envio pelo formulário, transparência no uso de IA ou bônus de deploy.
