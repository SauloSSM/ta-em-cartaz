# EliteDevTicket

Plataforma completa de catálogo de eventos, venda de ingressos com reserva temporária (*hold*), emissão de ingressos digitais com QR Code e validação em portaria (*Gate*).

---

## ⚡ START HERE — Guia Rápido de Avaliação

Para inicializar e validar a aplicação completa em **menos de 3 minutos**:

1. **Suba todo o ambiente com Docker Compose:**
   ```sh
   docker compose up --build
   ```
2. **Abra o navegador em:**
   - **Frontend (SPA):** [`http://localhost:5173`](http://localhost:5173)
   - **Backend API:** [`http://localhost:8080`](http://localhost:8080)
   - **Health Checks:** [`http://localhost:8080/actuator/health/readiness`](http://localhost:8080/actuator/health/readiness)
3. **Acesse com uma das contas de demonstração provisionadas (senha padrão: `password`):**
   - `organizer@demo.elitedevticket.local` → Gestão e publicação de eventos e setores
   - `customer.one@demo.elitedevticket.local` → Compra, checkout com hold de 10 min e ingressos
   - `gate@demo.elitedevticket.local` → Portaria e validação de ingressos (manual e QR Code)

---

## 🛠️ Pré-requisitos e Execução

### Opção A: Execução Local com Docker (Recomendada)

- **Docker & Docker Compose** instalados e em execução.

```sh
docker compose up --build
```

O compose inicializa três containers interligados:
- `postgres`: PostgreSQL 17 na porta `5432` com banco `elitedevticket`.
- `backend`: Spring Boot 4 / Java 21 na porta `8080` (executa Flyway migrations e seeds).
- `frontend`: React 19 + TypeScript + Vite na porta `5173` (encaminha `/api` para o backend na mesma origem lógica).

---

### Opção B: Execução Fora do Docker

- **Java 21** (JDK)
- **Node.js 22.12+ LTS** (verificado via `frontend/.nvmrc`)
- **Docker** (para o PostgreSQL)

```sh
# 1. Subir o banco de dados PostgreSQL
docker compose up -d postgres

# 2. Inicializar o Backend (Spring Boot)
# Linux / macOS
./backend/mvnw -f backend/pom.xml spring-boot:run
# Windows (PowerShell)
.\backend\mvnw.cmd -f backend/pom.xml spring-boot:run

# 3. Inicializar o Frontend (Vite Dev Server)
npm --prefix frontend install
npm --prefix frontend run dev
```

Acesse a aplicação em `http://localhost:5173`.

---

## ⚙️ Variáveis de Ambiente e Configuração

Consulte o arquivo `.env.example` para obter os nomes e propósitos de todas as variáveis suportadas. Para desenvolvimento e avaliação local, nenhum segredo externo é obrigatório:

| Variável | Propósito | Padrão Local / Demo |
| --- | --- | --- |
| `DATABASE_URL` | JDBC URL do PostgreSQL | `jdbc:postgresql://localhost:5432/elitedevticket` |
| `DATABASE_USERNAME` | Usuário do PostgreSQL | `elitedevticket` |
| `DATABASE_PASSWORD` | Senha do PostgreSQL | *(vazio no perfil local)* |
| `AUTH_JWT_SECRET` | Segredo para assinatura de tokens JWT | Chave default para ambiente local |
| `TICKETMASTER_API_KEY` | Chave da API Ticketmaster Discovery | *Opcional* (busca externa falha de forma clara caso ausente ou indisponível) |

---

## 👥 Credenciais Demo Provisionadas

Todas as contas abaixo são provisionadas automaticamente nos perfis `local`, `test` e `demo` com a senha universal **`password`**:

| Papel | E-mail | Propósito na Avaliação |
| --- | --- | --- |
| **ORGANIZER** | `organizer@demo.elitedevticket.local` | Pesquisar eventos no Ticketmaster, importar rascunhos, configurar setores e publicar eventos. |
| **CUSTOMER** | `customer.one@demo.elitedevticket.local` | Navegar no catálogo, selecionar ingressos, testar hold de 10 min, pagar e gerenciar ingressos próprios. |
| **CUSTOMER** | `customer.two@demo.elitedevticket.local` | Segundo comprador independente com seus próprios ingressos emitidos. |
| **GATE** | `gate@demo.elitedevticket.local` | Operador de portaria para validação de ingressos via leitor de câmera QR ou digitação de código alfanumérico. |

### Perfis de Execução (Spring + Flyway)

- `local` e `test`: aplicam `classpath:db/migration` (schema) e `classpath:db/seed/demo` (contas, eventos e ingressos de teste).
- `demo`: aplica as mesmas migrations e seeds, mas exige variáveis de ambiente externas configuradas.
- `prod`: aplica exclusivamente `classpath:db/migration`. Nenhum dado ou conta demo é semeado em produção.

---

## 🧭 Roteiro de Avaliação das Jornadas Principais

### Jornada 1: Organizador (`ORGANIZER`)

1. **Login:** Acesse a tela de login e autentique-se com `organizer@demo.elitedevticket.local` / `password`.
2. **Meus Eventos:** Visualize a lista de eventos criados.
3. **Criar Evento:**
   - Clique em **"Novo Evento"**.
   - Digite um termo de busca (ex: `Rock`, `Indie`, `Show`) para pesquisar eventos reais via API Ticketmaster.
   - Clique em **"Importar como Rascunho"** em um dos resultados.
4. **Editar Rascunho (*Draft*):**
   - Altere título, descrição, local, endereço ou data/hora de realização.
5. **Configurar Setores (*TicketSectors*):**
   - Na seção de setores, adicione ou ajuste setores com nome, capacidade de ingressos e preço unitário (ex: *Pista Premium* - 100 vagas - R$ 150,00).
6. **Revisar e Publicar:**
   - Clique em **"Publicar Evento"**. O sistema valida as invariantes de negócio (data futura obrigatória e ao menos um setor configurado com capacidade e preço válidos). O evento passa ao estado `PUBLISHED`.
7. **Catálogo Público:** Clique em "Ver Catálogo Público de Eventos" para inspecionar a visualização do cliente.

---

### Jornada 2: Cliente Comprador (`CUSTOMER`)

1. **Descoberta:** Acesse a página inicial (pública). Pesquise por texto no catálogo de eventos `PUBLISHED`. Os cards exibem preços a partir do menor valor de setor disponível.
2. **Detalhes e Intenção de Compra:** Clique em um evento (ex: *Show Acústico de Demonstração (Event A)*). Selecione o setor desejado e a quantidade de ingressos.
3. **Login e Restauração de Intenção:**
   - Se não estiver logado, o sistema solicita autenticação.
   - Faça login com `customer.one@demo.elitedevticket.local` / `password`. A intenção de compra previamente selecionada é automaticamente restaurada e o *Hold* é gerado no backend.
4. **Reserva Temporária (*Hold* de 10 minutos):**
   - O checkout exibe o **Timer Autoritativo** decrescente sincronizado com o relógio do servidor (`serverNow`).
   - Os ingressos ficam temporariamente bloqueados contra concorrência (*overselling*).
   - Se o usuário navegar para o catálogo, um banner fixo permite retornar ao checkout a qualquer momento antes do término do tempo.
5. **Simulação de Pagamento:**
   - **Opção Aprovado (APPROVED):** Clique em *Simular Pagamento Aprovado*. A reserva é confirmada atomicamente (`CONFIRMED`), o pagamento é registrado e os ingressos digitais são emitidos.
   - **Opção Recusado (DECLINED):** Clique em *Simular Pagamento Recusado*. O sistema exibe o feedback de recusa sem debitar estoque indevidamente e permite re-tentativa com nova chave de idempotência.
6. **Meus Ingressos:**
   - No menu superior, clique em **"Meus Ingressos"**.
   - Abra um ingresso para visualizar os detalhes, o **QR Code seguro** e o código alfanumérico legível de 10 caracteres.
   - Clique em **"Compartilhar Ingressos"** para obter o link público permanente (`/t/:shareToken`) acessível por qualquer pessoa mesmo sem autenticação.

---

### Jornada 3: Validador de Portaria (`GATE`)

1. **Login:** Autentique-se com `gate@demo.elitedevticket.local` / `password`.
2. **Seleção de Evento:** Selecione o evento de trabalho na lista (ex: *Show Acústico Demo (Event A)*).
3. **Validação:** Utilize a câmera integrada para ler o QR Code ou insira o código alfanumérico manualmente.
4. **Demonstração dos 4 Desfechos da Portaria:**

| Teste | Evento Selecionado | Código Manual / QR | Resultado Esperado | Comportamento do Sistema |
| --- | --- | --- | --- | --- |
| **1. Ingresso Válido** | `Show Acústico Demo (Event A)` | `DEM0A1C0DE` (ou `DEM0-A1C0-DE`) | **VALID** | Ingresso validado com sucesso (alerta verde), marcado atomicamente como utilizado com registro de data/hora e operador. |
| **2. Ingresso Já Utilizado** | `Show Acústico Demo (Event A)` | `DEM0A1C0DE` *(segunda tentativa)* | **ALREADY_USED** | Alerta de re-uso informando data/hora e operador que efetuou a primeira validação. |
| **3. Ingresso Pré-Utilizado** | `Show Acústico Demo (Event A)` | `DEM0A2C0DE` | **ALREADY_USED** | Ingresso já registrado como usado na carga de seed inicial. |
| **4. Evento Incorreto** | `Show Acústico Demo (Event A)` | `DEM0B1C0DE` *(pertence ao Event B)* | **WRONG_EVENT** | Alerta informando que o ingresso pertence a outro evento; o ingresso **não** é consumido. |
| **5. Código Inexistente** | Qualquer Evento | `INVALID999` | **INVALID** | Alerta de ingresso inválido ou não encontrado no sistema. |

---

## 📊 Matriz de Cobertura dos Requisitos do Desafio

| Requisito do Desafio | Classificação | Implementação / Evidência | Verificação Automatizada |
| --- | :---: | --- | --- |
| **Stack Base (Java 21 + Spring Boot + React + TS)** | `MUST` | `backend/pom.xml`, `frontend/package.json` | Testes de compilação e execução |
| **Persistência Relacional com Migrations** | `MUST` | PostgreSQL 17 + Flyway (`V1` a `V11`) com Hibernate `ddl-auto=validate` | `FlywayMigrationIntegrationTest` |
| **Integração Ticketmaster Discovery** | `MUST` | `TicketmasterAdapter` com timeout de 5s, 1 retry para falhas transitórias/5xx/timeout, sem retry para 4xx/429 e feedback claro de erro | `TicketmasterIntegrationTest` |
| **Autenticação e RBAC (ORGANIZER, CUSTOMER, GATE)** | `MUST` | Cookies `HttpOnly`, Spring Security, `@PreAuthorize` e handlers 401/403 | `AuthControllerTest`, `SecurityConfigTest` |
| **Criação e Gestão de Eventos** | `MUST` | Ciclo `DRAFT` → `PUBLISHED` com validação de invariantes e campos imutáveis | `EventControllerTest`, `EventServiceTest` |
| **Setores de Ingressos por Quantidade** | `MUST` | `TicketSector` com capacidade total, preço unitário e disponibilidade dinâmica | `TicketSectorIntegrationTest` |
| **Reserva com Hold de 10 minutos** | `MUST` | `Reservation` em `HOLDING` com expiração autoritativa pelo backend | `ReservationHoldConcurrencyTest` |
| **Proteção contra Overselling** | `MUST` | Lock pessimista (`PESSIMISTIC_WRITE`) no estoque do setor | `OversellingProtectionIntegrationTest` |
| **Simulação de Pagamento (APPROVED / DECLINED)** | `MUST` | Pagamento idempotente com UUID, confirmação atômica e emissão de ingressos | `PaymentProcessingTest` |
| **Emissão de Ingressos e Meus Ingressos** | `MUST` | Ingressos com código de 10 caracteres legível, QR Code e consulta própria | `TicketIssuanceTest`, `MyTicketsTest` |
| **Compartilhamento de Ingressos** | `MUST` | Rota pública `/t/:shareToken` sem exigência de autenticação | `TicketShareControllerTest` |
| **Portaria (Gate) com 4 Desfechos** | `MUST` | Validação atômica anti-replay para `VALID`, `ALREADY_USED`, `WRONG_EVENT`, `INVALID` | `GateValidationIntegrationTest` |
| **Leitor de Câmera com Fallback Manual** | `MUST` | Componente com HTML5 Canvas/Web API e campo de texto com normalização | `GateScannerComponentTest` |
| **Dados Seedados e Perfis Isolados** | `MUST` | Seeds Flyway separados (`db/seed/demo`), `prod` estritamente limpo | `SeedIsolationIntegrationTest` |
| **Ambiente Reprodutível em Docker** | `SHOULD` | `docker-compose.yml` integrando SPA, API e PostgreSQL em mesma origem lógica | Docker compose health checks |

---

## 🏛️ Decisões de Arquitetura e Trade-offs

1. **Monólito Modular com Fronteiras Claras:**
   - O backend foi organizado em módulos por capacidade de negócio (`auth`, `events`, `reservations`, `payments`, `tickets`, `gate`, `shared`), garantindo alta coesão e baixo acoplamento sem a sobrecarga operacional de microsserviços.
2. **Autoridade Estrita do Backend para Tempo e Regras Críticas:**
   - O frontend nunca calcula se uma reserva expirou, nem define o preço a ser cobrado. O backend responde com `serverNow` e `expiresAt`, garantindo que relógios descalibrados de clientes não afetem a integridade das compras.
3. **Controle de Concorrência e Integridade de Estoque:**
   - Utilização de `PESSIMISTIC_WRITE` e transações atômicas no PostgreSQL para reserva de ingressos e check-in na portaria. Isso elimina condições de corrida (*race conditions*) em compras concorrentes simultâneas no esgotamento de lotes.
4. **Sessão Baseada em Cookies `HttpOnly` com SameSite Lax e CSRF Token:**
   - Autenticação protegida contra ataques XSS (tokens não acessíveis via JavaScript) e proteção CSRF via token sincronizado para requisições mutantes (`POST`, `PUT`, `DELETE`).
5. **Vite Proxy para Preservação da Mesma Origem Lógica:**
   - O ambiente de desenvolvimento utiliza o proxy interno do Vite (`/api` → `http://localhost:8080/api`), eliminando atritos de CORS e viabilizando o tráfego seguro de cookies em `localhost`.

---

## ⚠️ Limitações Conscientes do MVP

- **Pagamento Simulado:** O processamento financeiro é simulado de forma controlada (`APPROVED` / `DECLINED`). Não há integração com adquirentes ou gateways bancários reais de produção (ex: Stripe, Mercado Pago) nesta fase.
- **Dependência da API Externa do Ticketmaster:** A criação de eventos depende da disponibilidade e cota da API Discovery da Ticketmaster (não existe criação manual como fallback). Em caso de indisponibilidade externa, a integração aplica timeout de até 5s e até 1 retry exclusivo para falhas transitórias/5xx/timeout (sem retry para 4xx/429), retornando mensagem de erro clara para que o usuário possa tentar novamente.
- **Portaria Requer Conectividade:** A validação de ingressos na Gate requer conexão com o backend para garantir auditoria em tempo real e proteção atômica contra uso duplicado concorrente.
- **Câmera Web e Contexto Seguro:** O leitor de QR Code via câmera depende de permissões do navegador e exige contexto seguro (`localhost` ou `HTTPS`). Em caso de restrição do dispositivo, o fallback manual com normalização de código (`DEM0-A1C0-DE` ou `DEM0A1C0DE`) está sempre disponível.
- **Sem Suporte Offline / PWA:** A aplicação não foi desenhada como Progressive Web App com sincronização offline no escopo deste MVP.
- **Funcionalidades Fora do Escopo Oficial:** Recursos como mapa de assentos interativo SVG com marcação de poltronas numeradas, múltiplos organizadores por evento, estorno/refund automatizado e recuperação de senha por e-mail não foram implementados por pertencerem a escopos futuros (`COULD`/`WON'T`).
- **Compatibilidade Alvo de Navegadores:** Projetado para navegadores modernos (Google Chrome, Mozilla Firefox e Microsoft Edge em desktop; Safari e Chrome em dispositivos móveis). O uso de câmera depende de permissões do navegador e contexto seguro (HTTPS ou localhost), com fallback manual disponível.

---

## 🧪 Execução dos Testes Automatizados

### Backend (Testes Unitários, de Integração, Contratos e Arquitetura)

```sh
# Executar todos os testes do backend
backend/mvnw -f backend/pom.xml test
```

A suíte inclui:
- Testes com PostgreSQL real (Testcontainers);
- Validações de concorrência e corrida (*Overselling* e *Double-use*);
- Testes de contratos OpenAPI e conformidade arquitetural (ArchUnit).

### Frontend (Testes Unitários e de Componentes)

```sh
npm --prefix frontend test
```

A suíte em Vitest + React Testing Library valida o ciclo de vida das sessões, restauração de intenção, renderização do timer autoritativo e componentes da interface.

---

## 🤖 Uso Responsável e Transparente de Inteligência Artificial

O desenvolvimento deste projeto adotou IA generativa de forma estratégica e responsável, seguindo o framework de governança **BMAD (Benchmark-driven Multi-Agent Development)**:

### 1. Ferramentas e Agentes Utilizados
- **BMAD Orchestrator & Specialized Subagents:** Agentes com papéis especializados (Arquiteto, Engenheiro de Domínio, Test Architect e Reviewer Adversarial) para elaboração de especificações executáveis (*Story Specs*) e análise de cenários de borda.
- **Modelos de Linguagem Avançados:** Utilizados como copilotos de desenvolvimento para aceleração de código tipado, geração de scaffolding de testes e análise comparativa de documentação técnica.

### 2. Onde a IA Acelerou o Desenvolvimento
- Geração de código boilerplate e DTOs tipados no Spring Boot e React;
- Scaffolding de casos de teste unitários e de integração baseados em tabelas de decisão;
- Mapeamento e documentação de contratos OpenAPI 3.0;
- Diagnóstico inicial e investigação de mensagens de log e erros em pipelines.

### 3. Validação Humana e Rigor de Engenharia
- **A IA não substituiu a validação final:** Todas as decisões de arquitetura, modelo relacional, invariantes de domínio e políticas de segurança foram definidas por humanos e aprovadas em especificações congeladas.
- **Evidências Determinísticas:** As regras críticas foram cobertas por testes automatizados (unitários, integração e arquitetura), testes com PostgreSQL real e concorrência quando aplicável, contract checks de OpenAPI e smoke tests direcionados. A verificação ponta a ponta consolidada de jornadas e invariantes pertence ao escopo da Story 8.3.
- **Exemplos Reais de Intervenção e Ajuste Fino:**
  - *CSRF & CORS:* Diagnóstico e configuração manual refinada para garantir tráfego de cookies em desenvolvimento com proxy Vite sem desativar proteções de segurança.
  - *Concorrência Crítica:* Implementação deliberada de `PESSIMISTIC_WRITE` e transações isoladas para blindar o sistema contra compras concorrentes simultâneas de último ingresso.
  - *Atomicidade da Portaria:* Refinamento da consulta de validação de ingressos para garantir tratamento atômico com registro de auditoria e rejeição imediata de replay.

### 4. Segurança e Privacidade
- Nenhum segredo real, credencial corporativa ou chave privada foi enviada ou versionada no repositório. O `.env.example` e os arquivos de seed utilizam exclusivamente dados fictícios criados para avaliação.

---

## ✍️ Reflexão Pessoal do Autor

<!-- 
[Preenchimento opcional pelo autor/candidato]:
Espaço reservado para o candidato registrar suas impressões, principais aprendizados, 
desafios enfrentados durante a execução do teste e considerações finais sobre o projeto.
-->
