# Tá em Cartaz — EliteDevTicket

Plataforma de eventos e ingressos desenvolvida para o **Desafio Elite Dev**, com criação e publicação de eventos, reserva temporária (*hold*), pagamento simulado, emissão de ingressos digitais com QR Code, compartilhamento público e validação em portaria.

O objetivo não foi construir o maior sistema possível, mas entregar um MVP **completo, coerente, testável, explicável e visualmente intencional**.

```text
Organizer cria e publica
        ↓
Customer descobre e reserva
        ↓
Checkout + pagamento simulado
        ↓
Ingresso digital + compartilhamento
        ↓
Gate valida QR / código manual
```

---

## 🚀 Demo Online — Comece por Aqui

**Aplicação publicada:**  
https://ta-em-cartaz.vercel.app

A forma mais rápida de avaliar o projeto é pelo deploy público.

A própria tela de login permite selecionar uma das contas de demonstração e preencher as credenciais automaticamente.

### Credenciais de Avaliação

Todas usam a senha:

```text
password
```

| Papel | E-mail | Uso sugerido |
| --- | --- | --- |
| **ORGANIZER** | `organizer@demo.elitedevticket.local` | Criar, editar, configurar setores e publicar eventos |
| **CUSTOMER 1** | `customer.one@demo.elitedevticket.local` | Fluxo completo de compra e ingressos |
| **CUSTOMER 2** | `customer.two@demo.elitedevticket.local` | Segundo comprador independente |
| **GATE** | `gate@demo.elitedevticket.local` | Validação por câmera QR e código manual |

---

## ⚡ Guia Rápido de Avaliação

### Organizer

```text
Meus Eventos
→ + Novo Evento do Catálogo
→ buscar referência Ticketmaster
→ Usar como Referência
→ completar data/local
→ configurar setores
→ revisar checklist
→ publicar
```

### Customer

```text
Eventos em Cartaz
→ abrir evento
→ escolher setor e quantidade
→ Garantir Ingressos
→ Checkout
→ Finalizar Pagamento ou Simular Recusa
→ Meus Ingressos
→ abrir ingresso
→ QR / código manual / compartilhamento
```

### Gate

```text
selecionar evento
→ câmera QR ou código manual
→ validar
```

Resultados suportados:

```text
VALID
INVALID
ALREADY_USED
WRONG_EVENT
```

---

# 🛠️ Execução Local

## Opção A — Docker Compose

Pré-requisito:

- Docker + Docker Compose

Na raiz do projeto:

```sh
docker compose up --build
```

Serviços principais:

- `postgres` → PostgreSQL
- `backend` → Java 21 + Spring Boot
- `frontend` → React + TypeScript + Vite

Acesse:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Readiness: `http://localhost:8080/actuator/health/readiness`

> O tempo de inicialização pode variar conforme máquina, cache de imagens e download de dependências.

---

## Opção B — Execução Fora do Docker

Pré-requisitos:

- Java 21
- Node.js compatível com `frontend/.nvmrc`
- Docker para o PostgreSQL

```sh
# 1. PostgreSQL
docker compose up -d postgres

# 2. Backend — Linux/macOS
./backend/mvnw -f backend/pom.xml spring-boot:run

# Backend — Windows PowerShell
.\backend\mvnw.cmd -f backend/pom.xml spring-boot:run

# 3. Frontend
npm --prefix frontend install
npm --prefix frontend run dev
```

Acesse:

```text
http://localhost:5173
```

---

# ⚙️ Variáveis de Ambiente

Consulte `.env.example` para a lista completa.

| Variável | Propósito | Observação |
| --- | --- | --- |
| `DATABASE_URL` | JDBC URL do PostgreSQL | Configurada conforme o ambiente |
| `DATABASE_USERNAME` | Usuário do PostgreSQL | Configurada conforme o ambiente |
| `DATABASE_PASSWORD` | Senha do PostgreSQL | Configurada conforme o ambiente |
| `AUTH_JWT_SECRET` | Assinatura dos tokens JWT | Usar valor seguro fora de ambiente local |
| `TICKETMASTER_API_KEY` | Ticketmaster Discovery API | Opcional para os fluxos seedados; necessária para pesquisar referências reais |

A ausência de `TICKETMASTER_API_KEY` não impede a avaliação dos fluxos já seedados, mas impede a busca externa real no fluxo de criação de evento.

---

# 👥 Contas e Perfis Demo

As contas são seedadas para facilitar a demonstração das três experiências do produto:

```text
CUSTOMER
ORGANIZER
GATE
```

Não existe cadastro público neste MVP.

### Perfis de Execução

- `local` / `test` → schema + dados de demonstração
- `demo` → ambiente demonstrativo com configuração externa
- `prod` → migrations de produção sem semear automaticamente as contas demo

---

# 🧭 Jornadas Principais

## Jornada 1 — Organizer

### Login

```text
organizer@demo.elitedevticket.local
password
```

### Fluxo

1. Acesse **Meus Eventos**.
2. Clique em **+ Novo Evento do Catálogo**.
3. Pesquise uma referência real da Ticketmaster.
4. Clique em **Usar como Referência**.
5. Complete os dados internos do evento:
   - data e hora;
   - local;
   - endereço;
   - categoria;
   - descrição, se desejado.
6. A data/hora usa um seletor amigável no frontend e é convertida para o formato esperado pela API.
7. Configure um ou mais setores:
   - nome;
   - capacidade;
   - preço.
8. Revise o checklist de publicação.
9. Publique quando todos os requisitos obrigatórios estiverem válidos.

Depois de `PUBLISHED`, campos estruturais protegidos pelo domínio permanecem bloqueados conforme as regras do MVP.

---

## Jornada 2 — Customer

### Descoberta

O catálogo público permite:

- visualizar eventos publicados;
- pesquisar eventos;
- consultar data/local;
- visualizar preço inicial;
- abrir detalhes.

### Reserva

No detalhe:

```text
Escolher setor
→ quantidade
→ Garantir Ingressos
```

Se autenticação for necessária durante a compra, a intenção é restaurada após o login.

### Hold

A reserva entra em:

```text
HOLDING
```

por aproximadamente:

```text
10 minutos
```

O backend é a autoridade para:

- `serverNow`;
- `expiresAt`;
- expiração;
- estoque disponível.

### Pagamento

No checkout:

- **Finalizar Pagamento** → simula aprovação;
- **Simular Recusa** → demonstra o estado recusado.

Em caso de recusa:

- o hold continua ativo enquanto não estiver expirado;
- o cliente pode tentar novamente.

### Ingressos

Após aprovação:

```text
Reservation CONFIRMED
→ tickets emitidos
```

Em **Meus Ingressos**, o cliente acessa:

- evento;
- setor;
- data;
- local;
- status;
- QR Code;
- código manual;
- link público de compartilhamento.

Os tickets receberam uma direção editorial inspirada em ingressos físicos, mas continuam totalmente dinâmicos e funcionais.

---

## Jornada 3 — Gate

### Login

```text
gate@demo.elitedevticket.local
password
```

### Operação

1. Selecione o evento de trabalho.
2. Utilize a câmera para leitura do QR Code.
3. Se a câmera não estiver disponível, utilize o código manual.
4. O backend retorna um dos quatro resultados oficiais:

| Resultado | Significado |
| --- | --- |
| `VALID` | Ingresso correto e consumido com sucesso |
| `INVALID` | Código inexistente ou inválido |
| `ALREADY_USED` | Ingresso já consumido |
| `WRONG_EVENT` | Ingresso válido, mas pertencente a outro evento |

A transição:

```text
VALID → USED
```

é protegida de forma atômica para impedir double-use concorrente.

---

# 📊 Cobertura dos Requisitos

| Requisito | Status | Implementação |
| --- | :---: | --- |
| React + TypeScript | ✅ | SPA em Vite |
| Java 21 + Spring Boot | ✅ | Backend REST |
| PostgreSQL | ✅ | Persistência relacional |
| Flyway | ✅ | Migrations + seeds |
| Ticketmaster Discovery | ✅ | Pesquisa e snapshot para criação |
| Auth + RBAC | ✅ | CUSTOMER / ORGANIZER / GATE |
| Gestão de eventos | ✅ | DRAFT → PUBLISHED |
| Setores + quantidade | ✅ | Capacidade, disponibilidade e preço |
| Hold de 10 minutos | ✅ | Reservation `HOLDING` |
| Idempotência na reserva | ✅ | Proteção contra retry/double-click |
| Overselling | ✅ | Lock transacional no setor |
| Pagamento aprovado/recusado | ✅ | FakePaymentGateway |
| Emissão de tickets | ✅ | Após confirmação |
| My Tickets | ✅ | Área autenticada |
| QR seguro | ✅ | Credencial de validação não previsível |
| Código manual | ✅ | Fallback obrigatório |
| Compartilhamento | ✅ | `/t/:shareToken` |
| Gate via câmera | ✅ | Leitor web |
| 4 desfechos de Gate | ✅ | VALID / INVALID / ALREADY_USED / WRONG_EVENT |
| Double-use protection | ✅ | Consumo atômico |
| Dados seedados | ✅ | 1 organizer, 2 customers, 1 gate |
| Docker Compose | ✅ | Ambiente reproduzível |
| Deploy público | ✅ | Aplicação publicada |
| Testes automatizados | ✅ | Frontend + Backend |

---

# 🏛️ Decisões de Arquitetura e Trade-offs

## Monólito Modular

O backend foi organizado por capacidades de negócio:

```text
auth
catalog
events
reservations
payments
tickets
gate
```

A escolha por um monólito modular permitiu manter fronteiras claras sem introduzir a complexidade operacional de microsserviços para um MVP de sete dias.

## Backend como autoridade

O frontend não decide:

- validade da reserva;
- preço final;
- expiração;
- disponibilidade real;
- uso do ticket.

Essas decisões pertencem ao backend e ao PostgreSQL.

## Concorrência e Overselling

A reserva de estoque utiliza controle transacional sobre `TicketSector`.

Objetivo:

```text
availableQuantity nunca pode ficar < 0
```

Requisições concorrentes são serializadas no ponto crítico para impedir venda duplicada do último ingresso.

## Idempotência

A criação de Reservation utiliza uma chave de idempotência para proteger:

- double-click;
- retry após timeout;
- perda da resposta HTTP.

A mesma intenção não deve criar múltiplos holds.

## Segurança

A aplicação utiliza:

- Spring Security;
- JWT;
- cookies `HttpOnly`;
- `SameSite=Lax`;
- CSRF para operações mutantes;
- BCrypt;
- autorização no backend;
- ownership de recursos.

Esconder um botão no frontend nunca substitui autorização no backend.

## Ticket Security

Cada ingresso possui responsabilidades separadas:

```text
validationToken
manualCode
shareToken
```

O link público de compartilhamento não expõe diretamente a credencial usada pela portaria.

---

# 🎨 Decisões de UX/UI

A identidade visual do **Tá em Cartaz** foi construída como uma combinação de referências editoriais de festivais com influências **Neo-Swiss / Swiss Punk**.

A ideia principal foi:

> **festival culture organized by product thinking.**

A interface deveria ter personalidade sem prejudicar leitura, navegação ou clareza dos fluxos.

## Customer

A experiência Customer recebe maior expressão visual:

- hero editorial;
- imagens reais dos eventos;
- cores da marca;
- colagens e grafismos;
- tickets inspirados em ingressos físicos;
- composição mais emocional.

O evento continua sendo o protagonista.

As imagens dinâmicas são exibidas sem filtros pesados para preservar a identidade visual de cada artista/evento.

## Organizer

O Organizer é mais contido e orientado a produtividade:

- status visíveis;
- etapas claras;
- formulários diretos;
- inventário de setores;
- checklist de publicação;
- menos ornamentação.

## Gate

A portaria utiliza uma superfície escura e de alto contraste.

Prioridades:

```text
speed
contrast
certainty
zero distraction
```

A câmera é o fluxo principal e o código manual permanece disponível como fallback.

## Processo Visual

A UI foi refinada ao longo do desenvolvimento através de:

- referências visuais;
- mockups;
- testes em desktop/mobile;
- revisão das telas reais;
- criação e adaptação de assets;
- múltiplas rodadas de polish.

A intenção foi evitar uma interface genérica gerada automaticamente e criar uma identidade coerente entre Customer, Organizer e Gate.

---

# ⚠️ Limitações Conscientes do MVP

- **Pagamento simulado:** não existe integração financeira real nesta versão.
- **Ticketmaster:** novas buscas externas dependem da disponibilidade e da chave da API.
- **Gate online:** a portaria depende do backend para preservar atomicidade e auditoria.
- **Câmera:** exige permissão do navegador e contexto seguro; código manual é o fallback.
- **Sem suporte offline/PWA completo:** não existe sincronização offline no MVP.
- **Sem mapa de assentos:** o modelo escolhido foi setores + quantidade.
- **Sem recuperação de senha, OAuth, revenda, nota fiscal ou envio de ingresso por e-mail.**

---

# 🚀 Ideias Futuras e Evolução do Produto

Durante o desenvolvimento surgiram várias possibilidades de evolução que foram conscientemente deixadas fora do MVP para priorizar um fluxo completo, testado e confiável.

## Mapa de assentos numerados

Para eventos compatíveis, permitir seleção visual de lugares individuais além do modelo atual:

```text
setor + quantidade
```

Isso exigiria um modelo de inventário diferente e regras adicionais de concorrência.

## Disponibilidade em tempo real

Adicionar SSE ou WebSocket para atualizar visualmente a disponibilidade enquanto diferentes clientes compram ingressos.

A consistência continuaria no backend; realtime seria uma melhoria de experiência.

## Dashboard do Organizer

Adicionar métricas úteis, como:

- ingressos vendidos;
- ocupação por setor;
- receita estimada;
- evolução das vendas.

A ideia seria evitar dashboards decorativos e mostrar apenas informações acionáveis.

## Cancelamento e Reembolso

Adicionar suporte para:

- cancelamento de compra;
- cancelamento de evento;
- devolução de estoque;
- histórico;
- política de reembolso;
- integração financeira correspondente.

## Gateway de Pagamento Real

A abstração atual permite futuramente trocar o `FakePaymentGateway` por uma implementação real, por exemplo Mercado Pago ou Stripe, sem acoplar as regras de domínio diretamente ao provedor.

## Notificações

Enviar comunicações para:

- confirmação de compra;
- ingresso emitido;
- alteração importante no evento;
- lembretes próximos à data.

## Gate Offline

Estudar uma estratégia segura para eventos com conectividade instável.

O principal desafio seria manter proteção contra double-use mesmo com múltiplos dispositivos temporariamente desconectados.

## Descoberta Avançada

Expandir o catálogo com filtros por:

- cidade;
- data;
- categoria;
- faixa de preço.

## PWA / Mobile

Transformar a aplicação em PWA para:

- instalação;
- acesso rápido ao ingresso;
- experiência mobile mais integrada.

## Tickets ainda mais colecionáveis

Expandir o sistema visual atual para permitir identidade específica por evento mantendo:

- QR;
- acessibilidade;
- informações obrigatórias;
- consistência da plataforma.

Essas ideias não foram removidas por falta de interesse. A decisão durante o desafio foi priorizar:

```text
fluxo completo
→ regras críticas
→ testes
→ deploy
→ experiência visual
```

antes de adicionar funcionalidades que aumentariam significativamente a complexidade.

---

# 🧪 Testes e Hardening

## Estado validado mais recente

### Frontend

```text
46 test files passing
290 tests passing
TypeScript: 0 errors
OpenAPI contract check: PASS
Vite production build: PASS
```

### Backend

```text
362 tests passing
0 failures
0 errors
BUILD SUCCESS
```

> Estes números representam o último estado validado durante o hardening. Caso novos testes sejam adicionados após esta atualização do README, a contagem pode aumentar.

## Backend

```sh
backend/mvnw -f backend/pom.xml test
```

No Windows:

```powershell
.\backend\mvnw.cmd -f backend/pom.xml test
```

A suíte cobre, entre outros:

- regras de domínio;
- persistência;
- concorrência;
- overselling;
- double-use;
- autenticação/autorização;
- migrations;
- arquitetura.

## Frontend

```sh
npm --prefix frontend test -- --run
```

Build completo:

```sh
npm --prefix frontend run build
```

---

# 🤖 Uso de Inteligência Artificial

A IA foi utilizada como ferramenta de desenvolvimento, documentação, revisão e aceleração — não como substituta da validação final.

A filosofia adotada foi:

> **Human-led, AI-implemented, test-protected.**

## Ferramentas utilizadas

### ChatGPT

Utilizado principalmente para:

- discovery;
- refinamento de requisitos;
- arquitetura;
- UX;
- Design System;
- mockups e assets;
- revisão;
- documentação;
- análise de bugs e trade-offs.

### BMAD

Utilizado para estruturar:

- PRD;
- jornadas;
- arquitetura;
- Stories;
- acceptance criteria;
- edge cases;
- reviews.

### Antigravity

Utilizado como agente de implementação e estabilização para:

- alterações controladas;
- typecheck;
- testes;
- builds;
- investigação de regressões;
- validação técnica de lotes de UI.

### Codex

Utilizado em tarefas específicas de implementação/refactor quando apropriado.

## O que ficou sob decisão e validação manual

Mesmo utilizando IA intensamente, várias partes do processo continuaram deliberadamente sob controle do candidato:

- priorização do que entraria ou não no MVP;
- aprovação das regras de domínio e dos trade-offs;
- escolha das referências visuais;
- aprovação e rejeição de mockups;
- revisão das telas reais no navegador;
- testes manuais no deploy;
- decisões sobre o que congelar e o que refazer;
- versionamento, commits e pushes finais;
- decisão de não remover proteções de segurança apenas para “fazer funcionar”.

Alterações importantes seguiam, sempre que possível, um fluxo de:

```text
decisão
→ implementação
→ testes
→ estabilização
→ build
→ revisão
```

Não foi adotada a estratégia de simplesmente aceitar código gerado sem validação.

## Segurança

Nenhum segredo real ou chave privada foi versionado no repositório.

Credenciais presentes na aplicação são exclusivamente dados fictícios de demonstração.

---

# ✍️ Reflexão Pessoal

Esse projeto foi, sem dúvida, o mais difícil que já enfrentei. Foram dias de muito aprendizado, madrugada tentando resolver bugs, bastante estresse para acertar a UI e a todo momento achando que não conseguiria entregar a tempo.

Uma das partes que mais marcou o processo e a mais complicada foi ter que aguentar uma madrugada inteira tentando resolver problemas de **CORS e CSRF**. O sistema já tinha várias partes funcionando, mas autenticação, cookies, origem do frontend e proteção das requisições começaram a se cruzar de um jeito que tornou o diagnóstico bem mais difícil do que eu esperava. Tive que ficar analisando NetWork, logs, HTTP, XFCS e com muitas outras coisas, o frontend não respondia com um clique apenas, ficava dando erro de HTTP 403, mas se eu clicasse depois funcionava, isso me deixou muito intrigado do porque não estar indo de primeira, nisso passei desde às 00:00 até 4:53 da manhã quando foi que consegui resolver esse bug, se quiserem saber mais do que se tratava está nos meus commits do dia 16 pro dia 17. Mas com certeza foi o dia que fiquei mais feliz de poder arrumar um bug tão insuportavel como esse.

A UI também deu bastante trabalho. Em vários momentos eu não estava satisfeito com o resultado e preferi refazer, ajustar assets, testar outras composições e revisar as telas até sentir que o projeto tinha uma identidade própria e não apenas uma interface funcional.

Mesmo assim, tentei cuidar de cada pedaço para entregar algo que realmente mostrasse meu esforço, mesmo não conseguindo colocar tudo que desejava.

No fim, mais do que simplesmente concluir o desafio, minha maior preocupação foi entregar aos avaliadores o melhor projeto que eu conseguia fazer dentro do tempo disponível.

---

# 👤 Autor

**Saulo da Silva Stuque Menegucci**

- GitHub: https://github.com/SauloSSM
- LinkedIn: https://www.linkedin.com/in/saulo-da-silva-stuque-menegucci/

---

# 📌 Resumo Final

O objetivo do Tá em Cartaz não foi construir o maior sistema possível.

Foi construir um MVP:

- completo;
- coerente;
- testável;
- explicável;
- seguro;
- visualmente intencional;
- simples de avaliar.

```text
Human-led
AI-implemented
Test-protected
Domain-driven
MVP-first
```
