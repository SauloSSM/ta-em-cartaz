# Addendum técnico — EliteDevTicket

Este documento preserva decisões de implementação e processo extraídas da especificação do projeto. Elas apoiam arquitetura e execução, mas não definem por si só o comportamento do produto.

## Consistência e concorrência

- Transações e locking no estoque do setor protegem contra overselling.
- Um processo periódico pode localizar holds expirados, mas operações críticas também verificam expiração pelo relógio do servidor e não dependem exclusivamente do scheduler.
- Criação de reserva usa `Idempotency-Key` para retry e double-click; a mesma chave e o mesmo payload retornam a Reservation original, enquanto a reutilização com payload incompatível produz conflito explícito no contrato da API.
- Aprovação, expiração, emissão e validação devem ser idempotentes ou atomicamente protegidas conforme a operação.
- Constraints do banco reforçam quantidades, unicidade de tokens/códigos e transições críticas.

## Segurança dos ingressos

- `validationToken`, `manualCode`, `shareToken` e demais identificadores sensíveis devem ser gerados por uma fonte criptograficamente segura, com entropia adequada ao formato e ao risco, preservando unicidade e imprevisibilidade.
- O token de compartilhamento deve ser distinto do token de validação.
- Tokens e códigos completos não devem aparecer em logs.
- O link compartilhado concede acesso de apresentação, mas não transfere propriedade.

## Stack e forma da solução

- Frontend: React, TypeScript e Vite.
- Backend: Java 21, Spring Boot, Spring Security e Spring Data JPA/Hibernate.
- Persistência: PostgreSQL como fonte de verdade e Flyway para migrations e seeds.
- Infraestrutura local: Docker Compose.
- Arquitetura: monólito com um frontend responsivo e experiências distintas para Customer, Organizer e Gate.
- Autenticação: JWT; senhas protegidas com BCrypt.
- Valores monetários: representação decimal exata, sem ponto flutuante binário.

## Integrações

- Ticketmaster Discovery API é usada somente como catálogo para iniciar um evento interno.
- O Event persiste um snapshot dos campos importados e segue independente de alterações posteriores na Ticketmaster.
- Indisponibilidade da Ticketmaster não deve impedir a avaliação dos fluxos apoiados por seeds.
- Pagamentos usam uma abstração de gateway com implementação fake e resultados determinísticos.
- A arquitetura pode representar Payment com um estado técnico transitório `PENDING` antes de `APPROVED` ou `DECLINED`, se isso simplificar a implementação. A ausência de `PENDING` no PRD não rejeita essa possibilidade nem cria um novo requisito de produto.

## Estratégia de testes

- Priorizar testes automatizados das regras de domínio, transições de estado, expiração, idempotência, concorrência de estoque, emissão exata e consumo único do ingresso. Essa estratégia fornece a evidência exigida por SM-12.
- Testes concorrentes devem demonstrar ausência de overselling e de dupla validação válida.
- O fluxo principal deve permanecer reproduzível pela interface com os dados seedados.
- Os três cenários E2E mínimos são: (1) Organizer cria, configura e publica um Event a partir da Ticketmaster, conforme UJ-01; (2) Customer descobre, reserva, percorre pagamentos recusado e aprovado e recebe os Tickets, conforme UJ-02; (3) Gate seleciona o Event e valida por QR ou código manual, cobrindo os resultados esperados, conforme UJ-03.

## Decisões consideradas e excluídas do MVP

- TMDb e filmes: permitidos pelo desafio, mas o recorte escolhe eventos e shows via Ticketmaster.
- Mapa de assentos: substituído por setores com inventário quantitativo.
- Gateway de pagamento real: substituído por simulação determinística.
- Redis, Kafka e microsserviços: complexidade não necessária para o objetivo da avaliação.
- Cadastro público, recuperação de senha e gestão de papéis: substituídos por usuários seedados.
- Associação Gate–Event: todos os usuários `GATE` acessam eventos publicados.
- Operação offline e sincronização posterior: Gate requer comunicação online com o backend.
- Revogação ou expiração do link compartilhado: fora do MVP.

## Entrega e uso de IA

As obrigações normativas de prazo, repositório, submissão, README, artefatos de planejamento, declaração de uso de IA e deploy estão centralizadas no PRD §10.

Como implicações de execução, o repositório deve manter commits descritivos ao longo do período; o README deve permitir configuração e avaliação autônoma, documentar credenciais, dados, limitações e os caminhos demonstráveis; e a documentação deve tornar verificáveis as escolhas autorais, as alternativas descartadas e a revisão humana das partes assistidas por IA.
