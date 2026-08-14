# Revisão editorial — estrutura

Este conjunto existe para ajudar designers, engenharia e revisores a implementar e verificar a experiência do EliteDevTicket sem alterar as decisões de produto e Domain.

Modelo aplicado: **Estratégico/Contextual (pirâmide)**, com blocos de referência para consulta aleatória.

Baseline: `DESIGN.md` (2.194 palavras) e `EXPERIENCE.md` (6.324 palavras), contagem local aproximada porque o utilitário do skill não pôde ser executado neste ambiente.

| Pass | Original Text | Revised Text | Changes |
|---|---|---|---|
| structure | `DESIGN.md` §Components: resumo seguido do “Catálogo visual canônico” | PRESERVE | O resumo oferece orientação antes da referência detalhada; para leitores humanos, a repetição parcial melhora a navegação. |
| structure | `EXPERIENCE.md` §Component Patterns: explicações seguidas do “Catálogo comportamental canônico” | PRESERVE | A primeira parte ensina os fluxos críticos; a tabela funciona como contrato consultável. Fundi-las reduziria a compreensão. |
| structure | `EXPERIENCE.md` §Key Flows, §Detailed Wireflows e §Edge-case Matrix | PRESERVE | As seções servem, respectivamente, à narrativa humana, à transição entre telas e à consulta de exceções; não são redundância verdadeira. |
| structure | Handoffs e limites no fim de `EXPERIENCE.md` | PRESERVE | A posição final funciona como fronteira contratual e evita que mecanismos de Architecture ou itens Non-MVP sejam confundidos com decisões UX. |

Resumo: nenhuma mudança estrutural segura foi recomendada ou aplicada. Redução estimada: 0 palavras (0%). A densidade é compatível com um contrato UX que também funciona como referência de implementação. Nenhum ID, componente, fluxo, jornada, link ou handoff foi removido ou movido.
