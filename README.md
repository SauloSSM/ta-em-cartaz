# EliteDevTicket

Base executável do MVP: uma SPA React, API Spring Boot e PostgreSQL. Contas provisionadas já podem iniciar e encerrar sessão; RBAC e endpoints de negócio pertencem às próximas Stories.

## Pré-requisitos

- Docker Compose atual para a execução local completa;
- Java 21 para executar o backend fora do Docker;
- Node.js 22.12+ LTS (a versão está registrada em `frontend/.nvmrc`) para executar o frontend fora do Docker.

## Execução local com Docker

```sh
docker compose up --build
```

Abra `http://localhost:5173`. O Vite encaminha `/api` internamente para a API, preservando a mesma origem lógica no navegador. A API também fica disponível em `http://localhost:8080`; os probes operacionais são `/actuator/health/liveness` e `/actuator/health/readiness`.

O Compose usa PostgreSQL apenas na rede interna e autenticação `trust` para desenvolvimento local. Não use essa configuração fora do ambiente local.

## Execução fora do Docker

```sh
backend/mvnw test
npm --prefix frontend ci
npm --prefix frontend run build
```

Para iniciar a API localmente, configure `SPRING_PROFILES_ACTIVE=local` e `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` conforme necessário. `DATABASE_PASSWORD` é uma credencial e não deve ser versionada quando o banco exigir senha.

O profile padrão é `prod`. Ele exige explicitamente `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` externos, falhando na inicialização se algum estiver ausente. O profile `prod` aplica apenas a migration comum: não usa defaults de conexão, seeds, contas demo ou segredos versionados.

## Autenticação e sessão

A SPA inicializa a sessão em `GET /api/v1/auth/session` e usa o cookie CSRF `XSRF-TOKEN` com o header `X-XSRF-TOKEN` no login e logout. O JWT fica somente no cookie HttpOnly `EDT_SESSION`; não o copie para código ou armazenamento do navegador.

`local` e `test` geram uma chave JWT efêmera quando nenhuma é informada. `demo`, `prod` e o profile padrão exigem `AUTH_JWT_SECRET` externo em Base64 com pelo menos 32 bytes e usam cookies `Secure`, proteção que não pode ser desativada nesses profiles por `AUTH_COOKIES_SECURE`. O TTL padrão é `PT8H` (`AUTH_JWT_TTL`), o custo BCrypt padrão é 10 (`AUTH_BCRYPT_COST`) e origens CORS adicionais, quando necessárias, são configuradas em `AUTH_CORS_ALLOWED_ORIGINS`.

## Perfis e dados de demonstração

Flyway é o único dono do schema e o Hibernate executa somente com `ddl-auto=validate`.

- `local` e `test`: aplicam `db/migration` e `db/seed/demo` com a configuração de banco apropriada ao ambiente.
- `demo`: aplica as mesmas migrations e seeds, mas exige `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` externos.
- `prod`: aplica somente `db/migration`; não há seed ou credencial demo.

As credenciais abaixo são intencionalmente públicas apenas para os perfis permitidos e usam a senha `password`:

| Papel | E-mail |
| --- | --- |
| ORGANIZER | organizer@demo.elitedevticket.local |
| CUSTOMER | customer.one@demo.elitedevticket.local |
| CUSTOMER | customer.two@demo.elitedevticket.local |
| GATE | gate@demo.elitedevticket.local |

Não versione segredos nem credenciais reais de produção.
