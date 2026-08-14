# Revisão editorial — prosa

Este conjunto documental existe para ajudar avaliadores e responsáveis por UX, arquitetura e implementação a compreender o MVP, verificar seus requisitos e executar a entrega sem reinterpretar decisões de produto.

**Estilo preservado:** tom técnico e direto; termos de domínio em inglês e estados em código; requisitos normativos; explicações breves voltadas a leitores humanos. A revisão segue o Microsoft Writing Style Guide e considera as mudanças já aplicadas em `review-structure.md`.

| Pass | Original Text | Revised Text | Changes |
| --- | --- | --- | --- |
| prose | PRD FR-15: “devem reservar uma apresentação válida” | “devem oferecer uma apresentação válida” | Corrige escolha lexical que sugere retenção/reserva, conceito já usado com outro sentido no domínio. |
| prose | PRD FR-21: “não fazem parte do MUST do MVP” | “não fazem parte do escopo obrigatório do MVP” | Remove mistura desnecessária de português e inglês sem alterar prioridade. |
| prose | PRD §§2–3, FR-28 e FR-37: “retry”, “double-click”, “retries” | “nova tentativa”, “clique duplo”, “novas tentativas” | Padroniza a linguagem de comportamento observável no PRD; os termos técnicos permanecem adequadamente no addendum. |
| prose | PRD §§6, 8 e 9; addendum §§Segurança e Decisões: “ownership” | “propriedade” | Usa um termo português consistente para o mesmo conceito em todas as ocorrências. |
| prose | PRD FR-32: “pagamento fake” | “pagamento simulado” | Alinha o requisito ao termo usado no restante do documento. |
| prose | PRD FR-26: “sem aceitar do frontend autoridade sobre esses valores” | “o frontend não pode definir esses valores” | Remove construção indireta e torna a responsabilidade inequívoca. |
| prose | PRD §6: “câmera é o fluxo móvel principal e entrada manual, o fallback obrigatório” | “a câmera é o fluxo móvel principal, e a entrada manual é o fallback obrigatório” | Corrige artigos e paralelismo. |
| prose | PRD FR-53: “uma Gate” | “um usuário `GATE`” | Distingue a pessoa provisionada da superfície operacional. |
| prose | Addendum §Decisões: “todas as Gates acessam eventos publicados” | “todos os usuários `GATE` acessam eventos publicados” | Corrige referência ao papel e evita tratar a interface como pessoa. |
| prose | Addendum §§Decisões e Entrega: “share link”, “setup” | “link compartilhado”, “configuração” | Mantém a prosa em português onde não há ganho técnico no anglicismo. |

Todas as correções acima são inequívocas e podem ser aplicadas sem alterar decisões, regras ou identificadores. Não há correções incertas pendentes.
