# EliteDevTicket — instruções para agentes

## Autoridade

Antes de implementar, siga esta ordem de autoridade:

1. Desafio oficial, PRD e decisões de Domain aprovadas;
2. `ARCHITECTURE-SPINE.md`, ADRs e OpenAPI aprovado;
3. `EXPERIENCE.md` e `DESIGN.md` para comportamento e interface;
4. padrões de engenharia aplicáveis abaixo;
5. convenções locais.

Se houver ambiguidade ou conflito entre fontes superiores, pare e reporte-o; não invente nem adapte uma regra silenciosamente.

## Regras por área

- Para mudanças em Java, Spring Boot, PostgreSQL, Flyway, integrações de backend ou testes de backend, leia e siga [`docs/engineering/java-standards.md`](docs/engineering/java-standards.md).
- Para mudanças em React, TypeScript, Vite, interface, acessibilidade, estado de cliente ou testes de frontend, leia e siga [`docs/engineering/react-standards.md`](docs/engineering/react-standards.md).
- Para mudanças full-stack, contratos HTTP/OpenAPI, autenticação ou fluxos que cruzem frontend e backend, leia e siga ambos os padrões antes de editar.

## Limites de execução

- Não introduza novas bibliotecas, infraestrutura, padrões arquiteturais ou funcionalidades sem cobertura nas fontes aprovadas.
- Preserve as invariantes de Domain e Architecture, inclusive autoridade do backend para tempo, estoque, preço, autorização e transições de estado.
- Ao concluir uma Story, informe arquivos alterados, verificações executadas, decisões assumidas, riscos e pendências.
