---
title: EliteDevTicket — React Engineering Standards v1.0
status: approved-for-use
scope: frontend
project: EliteDevTicket
---

# EliteDevTicket — React Engineering Standards v1.0

## 0. Autoridade e escopo

Estas regras orientam implementação React/TypeScript no EliteDevTicket.

Elas não podem sobrescrever decisões de produto, domínio, arquitetura, UX ou Design System.

Ordem de autoridade:

1. Desafio oficial;
2. PRD / Project Specification / decisões de Domain aprovadas;
3. `ARCHITECTURE-SPINE.md` e ADRs aprovados;
4. `EXPERIENCE.md` e `DESIGN.md`;
5. estas Engineering Standards;
6. convenções locais e preferências de implementação.

Se houver conflito com fonte superior, pare e reporte o conflito. Não adapte silenciosamente a regra superior para obedecer este arquivo.

Dependências novas não podem ser introduzidas apenas porque são citadas aqui. O `ARCHITECTURE-SPINE.md` é a autoridade sobre bibliotecas e tooling aprovados.

## 1. Protocolo de execução

### MUST

- TypeScript em modo `strict`.
- Não usar `any`, `@ts-ignore` ou cast apenas para silenciar o compilador.
- Não deixar `TODO`, código comentado ou logs de debug na entrega final.
- Testes acompanham a implementação quando a Story exige teste.
- Regras críticas seguem TDD: teste falhando → implementação mínima → refactor.
- Se a implementação exigir decisão não coberta pelas fontes aprovadas, pare e reporte.
- Ao finalizar a Story, reporte arquivos alterados, testes executados, resultado, decisões assumidas, riscos e pendências.

### Comentários

Não comentar o óbvio. Comentários são permitidos somente para explicar por que existe algo que o código não consegue comunicar sozinho, como workaround de bug externo, restrição de browser, decisão de compatibilidade ou comportamento contraintuitivo exigido por fonte externa.

## 2. Estado: ordem de decisão

Antes de criar estado de cliente, percorra esta ordem:

1. Pode ser derivado de props, dados já carregados ou outro estado? → derivar.
2. É estado autoritativo vindo do backend? → usar a camada de server-state aprovada pela Architecture.
3. Pertence à URL? → usar rota/search params quando isso melhorar navegação, compartilhamento ou recuperação.
4. É estado do DOM? → preferir DOM/ref.
5. É local, simples e independente? → `useState`.
6. Possui transições relacionadas ou múltiplos valores que mudam juntos? → `useReducer`.
7. É compartilhado por uma árvore e muda pouco? → Context.
8. É realmente global, frequente e multi-consumidor? → store externa somente se aprovada pela Architecture.

### MUST

- Não armazenar estado derivado.
- Não copiar server-state para `useState`.
- Não modelar estados impossíveis com vários booleanos quando uma união discriminada resolve melhor.
- Atualização baseada no valor anterior usa forma funcional.
- Estado local não pode duplicar autoridade do backend sobre hold, pagamento, ticket ou disponibilidade.

### SHOULD

- Mais de 3 estados locais em um componente é sinal de revisão, não erro automático.
- Se vários estados mudam juntos, reavaliar `useReducer` ou extração de responsabilidade.

## 3. `useEffect`

`useEffect` é para sincronização com sistemas externos ao React.

### MUST

Não usar `useEffect` para calcular estado derivado, buscar dados quando a camada de server-state aprovada já cobre o caso, reagir a clique/submit que pode ser tratado no handler, copiar query result para estado local, sincronizar dois estados locais que deveriam ser um único modelo ou esconder dependências omitindo-as do array.

Quando houver timer, listener, observer, câmera/media stream ou assinatura externa:

- cleanup é obrigatório;
- efeito deve tolerar StrictMode;
- cada efeito deve possuir uma responsabilidade clara.

### SHOULD

- Efeito longo ou difícil de ler deve virar custom hook ou função dedicada.
- Preferir handlers e funções puras sempre que não houver sistema externo envolvido.

## 4. Server state e API

A biblioteca concreta é definida pela Architecture.

Se TanStack Query for aprovada:

### MUST

- queries e mutations em hooks/casos de uso por feature;
- query keys centralizadas por feature;
- funções HTTP em camada `api/` separada;
- não chamar `fetch` diretamente dentro de componente;
- não copiar cache para `useState`;
- erros devem ser tratados explicitamente;
- retries e polling só com justificativa funcional;
- invalidação/refetch devem preservar autoridade do backend.

### SHOULD

- `staleTime` explícito quando a semântica do dado exigir previsibilidade;
- `select` para transformação de resposta quando isso evitar transformação duplicada na UI;
- mutation otimista apenas quando rollback é seguro e o comportamento realmente melhora a UX.

Para Reservation, Payment, Ticket e Gate validation, não antecipar sucesso local antes da confirmação autoritativa.

## 5. Componentes

### MUST

- Um componente deve possuir responsabilidade clara.
- Lógica de domínio não vive em componente.
- Componente não chama infraestrutura diretamente.
- Componentes definidos fora de outros componentes.
- Export nomeado, salvo exigência de framework.
- JSX sem ternário aninhado.
- `key` estável; índice é proibido quando a lista pode reordenar, remover ou inserir.
- Elementos semânticos primeiro.
- Nenhuma ação essencial depende apenas de hover.

### SHOULD

Os limites abaixo são heurísticas de revisão, não leis:

- componente acima de ~150 linhas;
- mais de ~5 props;
- mais de ~3 estados locais;
- JSX com muitas ramificações.

Ao ultrapassar, reavaliar coesão e composição. Não criar objetos/configurações artificiais apenas para “passar” no limite.

## 6. Props e tipos

### MUST

- Props com `type` explícito.
- Não usar `React.FC` como padrão obrigatório.
- Estados complexos preferem união discriminada.
- Não usar `object`, `Function` ou `any`.
- Dados externos são tratados como não confiáveis na fronteira.
- Validação runtime usa a ferramenta aprovada pela Architecture, quando necessária.
- Tipos HTTP devem derivar ou conformar ao contrato OpenAPI aprovado.

### SHOULD

- Agrupar props quando elas formam um conceito real de domínio ou apresentação.
- Não agrupar apenas para reduzir contagem.
- Handlers devem ter nomes de intenção, não nomes genéricos.

## 7. Custom hooks

### MUST

- Nome começa com `use`.
- Um hook possui uma responsabilidade clara.
- Hook não retorna JSX.
- Regras dos Hooks sempre respeitadas.

### SHOULD

- Lógica pura fica fora do hook quando puder ser testada sem React.
- Retorno nomeado é preferível quando houver vários valores.
- Não criar custom hook sem ganho real de coesão ou reutilização.

## 8. Formulários

A biblioteca concreta é definida pela Architecture.

Se React Hook Form + Zod forem aprovados:

### MUST

- schema runtime na fronteira do formulário;
- validação também existe no backend;
- submissão usa o estado da mutation/form, não booleanos paralelos;
- erros associados aos campos;
- dados inválidos não podem ser “forçados” via cast.

### SHOULD

- inputs não controlados quando isso simplificar;
- componente controlado somente quando houver motivo de interação real.

## 9. Performance

### MUST

- Não aplicar otimização prematura que torne o código mais complexo.
- Não usar memoização como reflexo.
- Não introduzir virtualização, lazy loading avançado ou store externa sem necessidade observável.

### SHOULD

- medir antes de `memo`, `useMemo` ou `useCallback` extensivos;
- resolver primeiro causa estrutural de rerender;
- lazy-load de rota pesada quando houver benefício real.

React Compiler, virtualização e outras otimizações são decisões de implementação/tooling, não requisitos automáticos.

## 10. Acessibilidade

### MUST

- HTML semântico.
- Inputs com label associado.
- `focus-visible` preservado.
- Modal/dialog com foco gerenciado e retorno ao acionador.
- Ícone sem texto possui nome acessível.
- Estado relevante assíncrono é anunciado quando necessário.
- Não comunicar estado somente por cor.
- Contraste conforme o piso WCAG 2.1 AA do projeto.
- Scanner Gate sempre possui fallback manual.
- Timer não deve anunciar cada segundo; seguir `EXPERIENCE.md`.
- QR nunca é a única representação funcional do ticket.

`EXPERIENCE.md` é a autoridade sobre interação, foco, live regions, estados Gate e comportamento do timer.

## 11. Testes

Ferramentas concretas seguem a Architecture.

Quando Vitest + Testing Library + user-event + MSW estiverem aprovados:

### MUST

- testar comportamento observável;
- queries por acessibilidade preferidas;
- interação com `userEvent`;
- rede mockada na fronteira HTTP;
- bug corrigido começa com teste que reproduz o bug;
- testes independentes e determinísticos;
- regras críticas seguem TDD.

### SHOULD

- `getByTestId` apenas quando não houver query semântica adequada;
- evitar snapshots grandes de árvore;
- componente puramente visual sem comportamento não precisa de teste unitário só para aumentar cobertura.

Cobertura percentual não é meta isolada.

## 12. Estrutura por feature

### MUST

A organização principal é por feature/capacidade, coerente com o monólito modular.

Exemplo conceitual:

```text
src/
  features/
    events/
    reservations/
    payments/
    tickets/
    gate/
    auth/
  shared/
    ui/
    lib/
```

- código de uma feature não alcança internals de outra;
- imports cruzados passam por API pública da feature;
- camada HTTP/API fica isolada de JSX;
- não usar `utils.ts` genérico como depósito de funções.

A estrutura exata deve respeitar `ARCHITECTURE-SPINE.md`.

## 13. Regras específicas do EliteDevTicket

### MUST

- Seleção pré-login é apenas intenção; não representa hold.
- Reservation/Payment/Ticket exibidos na UI nunca substituem estado autoritativo do backend.
- Timer é derivado de `expiresAt`/`serverNow` conforme Architecture/Experience.
- Após resposta de pagamento incerta, UI entra em verificação; não repete pagamento automaticamente.
- Gate sem rede não produz decisão de entrada.
- `LOW_AVAILABILITY` não existe enquanto não houver decisão aprovada.
- Design tokens TBD não podem ser inventados.
- Componentes frontend devem consumir o Design System aprovado quando ele existir.

## 14. MUST / SHOULD / COULD

### MUST

- TypeScript strict.
- Sem `any` / `@ts-ignore`.
- Sem estado derivado.
- Sem duplicar server-state em estado local.
- API isolada dos componentes.
- Sem lógica de domínio em componente.
- Acessibilidade conforme Experience.
- Testes de comportamento nas Stories que exigem teste.
- TDD para regras críticas.
- Package-by-feature.
- Respeito ao OpenAPI e às fronteiras da Architecture.

### SHOULD

- Early returns.
- Componentes pequenos e coesos.
- Poucos estados locais.
- Poucas props.
- Funções puras.
- Composição.
- Query key factory, `staleTime` explícito e hooks por caso de uso quando TanStack Query estiver aprovada.

### COULD

- React Compiler.
- Virtualização.
- Otimizações avançadas.
- Mutation testing frontend.
- Store externa.
- Optimistic updates sofisticados.

Nenhum item COULD entra automaticamente numa Story.

## 15. Checklist de entrega

- [ ] nenhuma decisão contradiz fonte superior;
- [ ] nenhum `any`, `@ts-ignore` ou cast corretivo;
- [ ] nenhum estado derivado armazenado;
- [ ] nenhum server-state duplicado em estado local;
- [ ] nenhum `fetch` direto em componente;
- [ ] nenhum efeito usado como cola entre estados;
- [ ] nenhuma regra de domínio no JSX/componente;
- [ ] nenhuma dependência nova sem aprovação arquitetural;
- [ ] estados críticos acessíveis e não dependentes só de cor;
- [ ] testes relevantes executados;
- [ ] Design System TBD não foi inventado;
- [ ] relatório final da Story foi produzido.
