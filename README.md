# EliteDevTicket

Base executável do MVP: uma SPA React, API Spring Boot e PostgreSQL. Contas provisionadas já podem iniciar e encerrar sessão; RBAC e endpoints de negócio pertencem às próximas Stories.

## Pré-requisitos

- Docker Compose atual para a execução local completa;
- Java 21 para executar o backend fora do Docker;
- Node.js 22.12+ LTS (a versão está registrada em `frontend/.nvmrc`) para executar o frontend fora do Docker.

## Configuração de Ambiente (.env)

Consulte o arquivo `.env.example` para obter os nomes e propósitos de todas as variáveis suportadas. Para desenvolvimento e avaliação local, nenhum segredo externo é obrigatório:
- `TICKETMASTER_API_KEY`: opcional (caso não informada, a busca externa opera com fallback de erro gracioso sem interromper o sistema);
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`: configuradas com defaults locais prontos para execução.

## Execução local com Docker

Para subir todo o ambiente de uma só vez (SPA, API e PostgreSQL):

```sh
docker compose up --build
```

- **Frontend (SPA):** acesse `http://localhost:5173`. O Vite encaminha chamadas `/api` internamente para a API, preservando a mesma origem lógica no navegador.
- **Backend (API):** disponível em `http://localhost:8080`.
- **Probes operacionais de readiness e liveness:**
  - Liveness: `http://localhost:8080/actuator/health/liveness`
  - Readiness: `http://localhost:8080/actuator/health/readiness` (avalia integridade de conexão ao banco e migrations Flyway).

## Execução fora do Docker

1. **Subir apenas o PostgreSQL:**
```sh
docker compose up -d postgres
```

2. **Executar o Backend:**
```sh
# Linux/macOS
./backend/mvnw -f backend/pom.xml spring-boot:run

# Windows (PowerShell)
.\backend\mvnw.cmd -f backend/pom.xml spring-boot:run
```
O perfil `local` é ativado por padrão com conexão em `jdbc:postgresql://localhost:5432/elitedevticket` (usuário `elitedevticket`, sem senha).

3. **Executar o Frontend:**
```sh
npm --prefix frontend install
npm --prefix frontend run dev
```
Acesse `http://localhost:5173`.

4. **Executar Testes:**
```sh
# Backend (testes de unidade, contratos e arquitetura)
backend/mvnw test

# Frontend (testes unitários, de componentes e de integração)
npm --prefix frontend test
```

## Perfis e Dados de Demonstração

Flyway é o único dono do schema e o Hibernate executa somente com `ddl-auto=validate`.

- `local` e `test`: aplicam `db/migration` e `db/seed/demo` com a configuração apropriada ao ambiente.
- `demo`: aplica as mesmas migrations e seeds, mas exige `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` externos e `AUTH_JWT_SECRET`.
- `prod`: aplica somente `db/migration`; não há seed, credenciais ou eventos demo.

### Contas Provisionadas (Senha: `password`)

| Papel | E-mail | Propósito |
| --- | --- | --- |
| `ORGANIZER` | `organizer@demo.elitedevticket.local` | Criar, editar, configurar setores e gerenciar eventos |
| `CUSTOMER` | `customer.one@demo.elitedevticket.local` | Navegar no catálogo, reservar ingressos, pagar e visualizar ingressos próprios |
| `CUSTOMER` | `customer.two@demo.elitedevticket.local` | Segundo comprador com ingressos próprios |
| `GATE` | `gate@demo.elitedevticket.local` | Validação de ingressos na portaria (manual e QR Code) |

### Eventos e Ingressos Seedados para Avaliação

Os seeds de demonstração inicializam o catálogo com eventos `PUBLISHED` e ingressos emitidos respeitando todas as invariantes do domínio (Customer → Reservation CONFIRMED → Payment APPROVED → Ticket):

1. **Event A — "Show Acústico de Demonstração (Event A)"**
   - Setor `Pista Premium`: R$ 150,00 (98 disponíveis de 100)
   - Setor `Camarote VIP`: R$ 320,00 (50 disponíveis de 50 — disponível para novas compras)
2. **Event B — "Festival Indie Brasil (Event B)"**
   - Setor `Pista Geral`: R$ 120,00 (199 disponíveis de 200)

### Demonstração dos Resultados de Validação da Portaria (Gate)

Para testar os 4 desfechos da portaria com o operador `gate@demo.elitedevticket.local`:

| Teste | Evento Selecionado no Gate | Código Manual / QR | Resultado Esperado |
| --- | --- | --- | --- |
| **Ingresso Válido** | `Show Acústico Demo (Event A)` | `DEM0A1C0DE` (ou `DEM0-A1C0-DE`) | **VALID** (marca como usado) |
| **Ingresso Já Utilizado** | `Show Acústico Demo (Event A)` | `DEM0A1C0DE` (tentativa subsequente) | **ALREADY_USED** |
| **Ingresso Já Usado no Seed** | `Show Acústico Demo (Event A)` | `DEM0A2C0DE` (ou `DEM0-A2C0-DE`) | **ALREADY_USED** |
| **Evento Incorreto** | `Show Acústico Demo (Event A)` | `DEM0B1C0DE` (pertence ao Event B) | **WRONG_EVENT** (não consome) |
| **Código Inexistente** | Qualquer Evento | `INVALID999` | **INVALID** |

