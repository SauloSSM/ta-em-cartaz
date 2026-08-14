# Epic 1 Context: Acesso seguro às contas de demonstração

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Estabelecer a base executável do MVP e o acesso seguro às contas de demonstração. Organizer, Customer e Gate devem poder usar identidades provisionadas, receber somente as permissões do seu papel e encerrar a sessão para trocar de conta. Isso cria a fundação de autenticação, autorização, contrato HTTP e fronteiras modulares da qual dependem os fluxos de evento, reserva, pagamento, ingresso e validação.

## Stories

- Story 1.1: Disponibilizar a fundação executável e contas provisionadas
- Story 1.2: Autenticar e encerrar sessão de contas provisionadas
- Story 1.3: Aplicar RBAC e contratos HTTP de autenticação
- Story 1.4: Estabelecer fronteiras modulares verificáveis

## Requirements & Constraints

- O MVP aceita apenas usuários provisionados com os papéis `ORGANIZER`, `CUSTOMER` e `GATE`; não há cadastro público, recuperação de senha nem administração de papéis pela UI.
- A autorização é sempre aplicada pelo backend. Controles de interface não são uma barreira de segurança suficiente. Operações incompatíveis com o papel ou com a propriedade do recurso devem ser recusadas sem efeito; nas capacidades futuras, Organizer administra somente seus Events e Customer acessa somente suas Reservations e Tickets.
- A sessão deve permitir login e logout seguros para alternância de contas durante a avaliação. O JWT não pode ser exposto ao JavaScript, e mutações devem ter proteção CSRF compatível com a SPA.
- Em `local` e `test`, os seeds devem fornecer credenciais documentáveis para pelo menos um Organizer, um Customer e um Gate. Seeds e credenciais de demonstração são proibidos em `prod`; segredos também não podem ser versionados.
- A execução local deve colocar SPA React, API Spring Boot e PostgreSQL em funcionamento. Flyway é o único responsável por schema e seeds; Hibernate apenas valida o schema. Nesta etapa inicial, a migration cria somente os dados de `User` necessários.
- Contratos HTTP ficam sob `/api/v1`, com OpenAPI versionado como autoridade para requests, responses, autenticação e erros. Erros usam um envelope estável com código, mensagem segura, erros de campo opcionais, traceId e timestamp; nunca revelam stack trace, SQL, classes internas ou segredos.
- A qualidade cobre autenticação, RBAC, ownership, erros e ausência de efeito de operações negadas com testes de API. Testes devem ser determinísticos: relógio, UUIDs e geradores relevantes são injetáveis quando necessário; não usar `Thread.sleep`, ordem compartilhada ou dados fora do contexto do teste.
- O sistema deve oferecer feedback imediato em operações assíncronas e manter acessibilidade: teclado, foco, labels, semântica, contraste e mensagens compreensíveis. Não introduzir tokens visuais ou biblioteca de UI ainda não aprovados.

## Technical Decisions

- Usar monólito modular por capacidade. O backend começa com `shared` para HTTP, configuração e cross-cutting mínimo, e `auth` para identidade, JWT, CSRF, RBAC e o port de lock consumido futuramente por reservations. Dentro de cada módulo, HTTP adapta DTOs, application coordena casos de uso e transações, domain contém regras, e adapters tratam persistência ou integrações. Ports existem somente em fronteiras reais, integrações e seams de teste.
- A estrutura do frontend separa `app` (bootstrap, rotas, sessão e cliente de API), `features` (incluindo auth) e `shared` (componentes comportamentais e utilitários). A mesma aplicação adapta controles e densidade ao papel autenticado sem misturar tarefas.
- A stack aprovada é Java 21 LTS, Spring Boot 4.0.7, React 19.2.x, TypeScript 5.x, Vite 7.3.x, Node 22.12+ LTS, PostgreSQL 17.x, Flyway gerenciado pelo Spring Boot e Docker Compose atual. Não adicionar bibliotecas, infraestrutura ou padrões fora dessas decisões.
- JWT é assinado com HS256 e entregue em cookie `HttpOnly`, `SameSite=Lax`, com `Secure` em `demo` e `prod`; assinatura e expiração são validadas. O TTL é configurável, com padrão de oito horas e expiração alinhada à do cookie, sem refresh token. O segredo é externo, aleatório, com pelo menos 256 bits, distinto por ambiente e obrigatório fora de desenvolvimento.
- Senhas são protegidas com BCrypt, com cost configurável por ambiente e padrão explícito para hashes novos; testes verificam autenticação e parâmetros do hash produzido, não um hash literal. O token CSRF legível pela SPA é renovado após login e logout. SPA e API compartilham a mesma origem lógica, e CORS restringe origens configuradas.
- Perfis são `local`, `test`, `demo` e `prod`. `local` contém seeds, `test` é isolado, `demo` usa HTTPS, cookies Secure, segredos externos e contas seedadas documentadas, e `prod` permanece sem credenciais demo. Docker Compose sobe frontend, backend e PostgreSQL; a SPA e `/api` operam sob a mesma origem lógica.
- O build deve impedir controller para repository direto, entidade JPA em DTO HTTP e import de internos entre módulos. DTOs Java e tipos TypeScript devem ser verificados contra OpenAPI para detectar drift.
- Logs estruturados em stdout carregam traceId e redigem cookies, Authorization, CSRF, tokens, códigos e outros segredos. Liveness/readiness expõem apenas estado mínimo; readiness verifica banco e migrations, não Ticketmaster.

## UX & Interaction Patterns

- A experiência é uma SPA responsiva com superfícies distintas para Customer, Organizer e Gate. Após autenticação, a navegação e os controles devem refletir o papel sem expor ou misturar tarefas de outros papéis.
- Login, erro de autenticação e logout devem ser acessíveis: mensagens associadas ao formulário, estado não autenticado claro quando aplicável, foco e teclado completos. O frontend nunca deve apresentar o JWT.
- Rotas devem manter `lang=pt-BR`, título único, landmarks, `h1`, skip link e foco após navegação. Formulários usam labels persistentes, ajuda e erro ligados aos controles; placeholder não substitui label. Em tela estreita, zoom e reflow, conteúdo e ações essenciais continuam acessíveis.

## Cross-Story Dependencies

- Story 1.1 fornece a execução, schema inicial e contas provisionadas para as Stories 1.2 a 1.4.
- Sessão, RBAC, contrato de erro e fronteiras definidos neste Epic são pré-requisitos para todos os Epics seguintes. Em especial, `reservations` deve usar o port explícito de `auth` para bloquear Customer, sem acessar entidade ou repository de auth.
- A intenção pré-login de compra e sua limpeza no logout pertencem ao fluxo posterior de reservas, mas a fundação de sessão e CSRF deste Epic precisa preservá-las como contrato futuro.
