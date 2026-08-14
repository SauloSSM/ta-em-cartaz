# Revisão aprofundada de acessibilidade

**Artefatos avaliados:** `DESIGN.md` e `EXPERIENCE.md` atuais  
**Fontes de controle:** PDF oficial, Project Specification v1.2, UX Direction v0.1, reconciliações, `.memlog.md` e `IMPACT-REPORT.md`  
**Referência:** WCAG 2.1 nível AA  
**Escopo:** Customer, Organizer e Gate; desktop e mobile  
**Data:** 2026-08-12

## Veredito

**PASS CONDICIONAL.** A direção é consistente com WCAG 2.1 AA e não conflita com Domain, mas sete lacunas comportamentais precisam ser incorporadas antes da finalização para tornar o contrato implementável e testável. Nenhuma exige nova feature de produto, alteração do hold de 10 minutos ou mudança das regras de autenticação, pagamento e validação.

Não foi identificado conflito real com fonte autoritativa. Nenhum finding exige `Critical STOP`.

## Findings

### A11Y-01 — Navegação estrutural e identificação de página incompletas

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 1.3.1, 2.4.1, 2.4.2, 2.4.6, 3.1.1
- **Evidência:** o Accessibility Floor cita landmarks e headings, porém não define idioma do documento, título de página por rota, link para pular conteúdo repetido nem o comportamento do `h1` após navegação.
- **Correção requerida:** declarar `lang="pt-BR"`; título de documento único e descritivo por tela/estado; exatamente um título principal coerente por superfície; landmarks semânticos e rotulados quando repetidos; primeiro controle como “Pular para o conteúdo”; após mudança de rota, posicionar foco programaticamente no `h1`/início do conteúdo com anúncio compreensível, sem inserir o título na ordem de tabulação permanente.

### A11Y-02 — Contrato de autenticação e sessão insuficiente

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 1.3.5, 3.3.1, 3.3.2, 3.3.3
- **Evidência:** S03 possui estados de erro e sessão expirada, mas não especifica `autocomplete`, identificação programática dos campos, comportamento de revelar senha nem segurança da mensagem de falha.
- **Correção requerida:** campos com labels persistentes e `autocomplete="username"`/`"current-password"`; botão de revelar senha com nome e estado acessíveis; Caps Lock pode ser comunicado como ajuda não bloqueante; erro de credenciais associado ao formulário e anunciado sem revelar qual credencial falhou; retorno pós-login preserva a intenção de compra e leva o foco ao contexto restaurado. Sessão expirada deve explicar que é necessário entrar novamente e preservar apenas dados seguros já previstos, sem prometer extensão de sessão.

### A11Y-03 — Anúncios de conteúdo dinâmico precisam de política explícita

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 4.1.3, 3.2.2, 3.2.4
- **Evidência:** há orientação genérica para live regions, mas não se diferencia atualização informativa de interrupção urgente nas buscas, publicação, reserva, pagamento, cópia e validação.
- **Correção requerida:** reservar região `status`/polite para resultados de busca, loading concluído, cópia de link e atualizações não urgentes; usar `alert`/assertive somente para falhas que exigem ação imediata, expiração do hold e resultado operacional da Gate. Mensagens persistentes devem existir no DOM além de toast. Loading deve expor estado ocupado e rótulo contextual sem trocar controles de modo que perca foco. A mesma mensagem não deve ser anunciada repetidamente em polling/retry.

### A11Y-04 — Limite temporal do hold necessita contrato acessível completo

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 2.2.1, 2.2.2, 4.1.3
- **Evidência:** o timer tem marcos de anúncio e não reinicia, mas falta explicar antes da criação que o limite é fixo e não pode ser estendido, além do comportamento quando a aba recupera foco ou o tempo expira enquanto outro controle está ativo.
- **Correção requerida:** comunicar, antes de “Reservar”, que a reserva confirmada após login terá duração fixa de 10 minutos; no checkout, oferecer tempo restante como texto programaticamente determinável, sem atualização de nome acessível a cada segundo; anunciar uma única vez nos marcos de 3 minutos, 1 minuto e expiração. A expiração autoritativa substitui as ações de pagamento, preserva o contexto e move foco para uma mensagem persistente com próximo passo. Não adicionar pausa, extensão ou reinício: o limite é essencial à regra de estoque e continua controlado pelo backend. Ao retornar de aba suspensa, reconciliar e anunciar apenas mudança significativa; mecanismo permanece handoff de Architecture.

### A11Y-05 — Formulários complexos e publicação precisam de semântica de erro mais precisa

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 1.3.1, 3.3.1, 3.3.2, 3.3.3, 4.1.2
- **Evidência:** labels, erro inline e resumo já existem, porém faltam requisitos para agrupamento, formato/limites e ligação bidirecional entre resumo e campos/setores.
- **Correção requerida:** marcar obrigatoriedade e erro programaticamente sem depender de asterisco/cor; associar ajuda, unidade, formato, mínimo/máximo e erro ao controle; agrupar campos relacionados com `fieldset`/`legend` ou equivalente; resumo de publicação deve listar cada problema como link para o campo/setor correspondente; após submit inválido, focar o resumo e permitir seguir ao primeiro erro. Entradas válidas permanecem intactas. Campos bloqueados pós-publicação devem preferir leitura estática ou `readonly` quando precisarem permanecer focáveis; controles `disabled` não podem ser a única fonte da explicação.

### A11Y-06 — Gate requer especificação de foco e ciclo operacional

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 2.1.1, 2.4.3, 3.2.1, 4.1.2, 4.1.3
- **Evidência:** o resultado recebe foco/announcement, mas não define alvo, prioridade dos anúncios, pausa de leitura nem retorno para a próxima validação.
- **Correção requerida:** após resposta autoritativa, interromper captura/submissão sobreposta; focar o heading do painel de resultado (`tabindex="-1"`) e anunciar uma única frase completa contendo estado e instrução. `VALID`, `INVALID`, `ALREADY_USED` e `WRONG_EVENT` devem manter textos estáveis e não depender de cor, forma ou som. “Validar próximo” limpa o resultado anterior e devolve foco ao scanner ou ao campo manual conforme o método usado. Permissão negada, câmera ausente e rede indisponível devem manter “Digitar código” alcançável por teclado. Não introduzir validação offline.

### A11Y-07 — Tabelas, diálogos, drawers e toasts não têm contrato testável suficiente

- **Severidade:** alta
- **Disposição:** correção necessária antes da finalização do UX
- **Critérios relacionados:** 1.3.1, 2.1.2, 2.4.3, 4.1.2, 4.1.3
- **Evidência:** os componentes são inventariados e há retorno de foco, mas faltam regras de estrutura e ciclo completo.
- **Correção requerida:** tabelas Organizer devem ter caption/nome acessível, headers associados e ações por linha com nome que inclua o objeto; em narrow/reflow, transformar em blocos sem perder relações, ou permitir rolagem unidimensional dentro de região nomeada e operável por teclado. Dialog/Drawer deve ter nome, descrição quando útil, foco inicial deliberado, contenção de foco, Escape, fechamento explícito e retorno ao acionador; nunca abrir dialog aninhado. Toast é suplementar, não recebe foco automaticamente e respeita tempo suficiente ou persistência quando contém ação; informação crítica também aparece inline.

### A11Y-08 — Orientação, zoom e conteúdo sticky devem ser explicitamente testados por superfície

- **Severidade:** média
- **Disposição:** melhoria recomendada
- **Critérios relacionados:** 1.3.4, 1.4.10, 1.4.12, 2.4.7
- **Evidência:** reflow 320 px/400% e foco não oculto já estão previstos, mas a tabela responsiva não proíbe bloqueio de orientação e não cobre text spacing.
- **Recomendação:** suportar portrait e landscape sem travar orientação; validar Customer, Organizer e Gate com 200% zoom, viewport equivalente a 320 CSS px e overrides de text spacing; sticky timer/CTA/header não devem ocultar foco, mensagem ou ações. Scanner pode adaptar o enquadramento, mas código manual precisa funcionar em ambas as orientações.

### A11Y-09 — QR e código manual precisam de representação assistiva definida

- **Severidade:** média
- **Disposição:** melhoria recomendada
- **Critérios relacionados:** 1.1.1, 1.3.1, 2.4.6
- **Evidência:** o QR possui equivalente manual, mas o contrato não diz se a imagem deve ser anunciada nem como o código agrupado é lido/copied.
- **Recomendação:** tratar QR como funcionalmente equivalente ao código e aos metadados adjacentes: fornecer nome curto (“QR do ingresso”) quando a imagem participa da tarefa ou ocultá-la de tecnologia assistiva quando o painel já tem nome inequívoco; nunca serializar o payload técnico no `alt`. Exibir código em texto selecionável, com nome acessível e ação “Copiar código”; agrupamento visual não deve inserir caracteres que alterem o valor copiado. Estado USED deve anteceder QR/código na ordem de leitura.

### A11Y-10 — Movimento e feedback não visual podem ser mais verificáveis

- **Severidade:** média
- **Disposição:** melhoria recomendada
- **Critérios relacionados:** 2.2.2, 2.3.1, 2.3.3
- **Evidência:** `prefers-reduced-motion`, ausência de flash e animação agressiva já estão previstos, mas falta definir o delta.
- **Recomendação:** sob redução de movimento, remover deslocamentos/escala/parallax e manter apenas mudança instantânea ou fade curto não essencial; nenhum feedback operacional depende de vibração, som ou animação. Skeletons não devem pulsar de forma contínua quando redução estiver ativa.

### A11Y-11 — Semântica dos estados necessita vocabulário e mapeamento únicos

- **Severidade:** média
- **Disposição:** detalhe futuro de Design System
- **Critérios relacionados:** 3.2.4, 4.1.2
- **Evidência:** a nomenclatura visual inclui estados de Event, Reservation, Payment, Ticket e Gate, mas o mapeamento acessível por componente ainda está futuro.
- **Orientação:** documentar em cada `StatusBadge`, `PaymentResult`, `ReservationTimer` e `GateResult` o rótulo visível, frase anunciada, ícone, token semântico e prioridade de live region. Não expor enum cru em inglês quando a interface estiver em pt-BR; manter o termo técnico apenas onde ajuda o avaliador, acompanhado do significado em português.

### A11Y-12 — Tokens de foco, contraste, espaçamento e estados aguardam definição

- **Severidade:** média
- **Disposição:** detalhe futuro de Design System
- **Critérios relacionados:** 1.4.3, 1.4.11, 1.4.12, 2.4.7
- **Evidência:** DESIGN.md deixa cores, tipografia, spacing e rounded como TBD de forma intencional.
- **Orientação:** na futura materialização dos tokens, testar todos os pares foreground/background e estados interactive/disabled/focus; focus indicator com contraste mínimo de 3:1; preservar legibilidade com text spacing; distinguir estado disabled sem torná-lo ilegível. Isso não bloqueia a decisão visual agora, mas bloqueia declarar conformidade da implementação.

### A11Y-13 — Câmera e perda de rede conservam handoffs técnicos corretos

- **Severidade:** informativa
- **Disposição:** handoff para Architecture
- **Critérios relacionados:** suporte aos requisitos comportamentais já definidos
- **Evidência:** `IMPACT-REPORT.md` e EXPERIENCE.md separam corretamente UX de mecanismo.
- **Handoff preservado:** Architecture define APIs/permissões/contexto seguro, detecção de indisponibilidade e reconciliação; UX exige alternativa manual equivalente, mensagens textuais, foco previsível e ausência de fila/consumo offline. Seleção de dispositivo não deve ser presumida como requisito funcional adicional sem decisão explícita.

### A11Y-14 — Reconciliação temporal e de pagamento conserva handoffs corretos

- **Severidade:** informativa
- **Disposição:** handoff para Architecture
- **Critérios relacionados:** 2.2.1 e 4.1.3 na apresentação
- **Evidência:** timer por `expiresAt` e estado `verifying` estão corretamente especificados como experiência, sem escolher polling/endpoints.
- **Handoff preservado:** Architecture define clock skew, refresh/aba suspensa e consulta após resposta perdida; UX deve aplicar A11Y-03 e A11Y-04 sem anunciar polling repetitivo nem iniciar nova cobrança.

## Cobertura por contexto

### Customer

- Navegação pública, seleção pré-login, login, checkout, timer, recusa, confirmação, Meus Ingressos e link público são cobertos.
- Correções prioritárias: restauração de foco após login; contrato do timer; mensagens de pagamento; ordem de leitura do estado do Ticket antes de QR/código.

### Organizer

- Listagem, Ticketmaster, editor, setores, revisão/publicação e restrições pós-publicação são cobertos.
- Correções prioritárias: resumo de erros navegável; agrupamento de formulários; explicação acessível de campos bloqueados; semântica responsiva de tabelas e ações por objeto.

### Gate

- Seleção do Event, câmera, manual, conectividade, quatro resultados e próximo ciclo são cobertos.
- Correções prioritárias: alvo/retorno de foco, anúncio único e estável, interrupção de capturas sobrepostas e fallback sempre alcançável.

## Checklist de aceitação para finalizar UX

Considerar esta lente resolvida quando `EXPERIENCE.md` incorporar A11Y-01 a A11Y-07 e preservar os handoffs A11Y-13/A11Y-14. A11Y-08 a A11Y-10 são melhorias recomendadas que podem ser incorporadas sem alterar escopo; A11Y-11/A11Y-12 devem permanecer como contrato de detalhe futuro do Design System.

Nenhuma correção proposta altera Domain, adiciona feature ao MVP, resolve mecanismo arquitetural ou reabre discovery.
