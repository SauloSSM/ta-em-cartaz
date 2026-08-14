---
title: PRD — EliteDevTicket
status: final
approval: APPROVED — implementation-ready
discovery: CLOSED
created: 2026-08-11
updated: 2026-08-12
---

# PRD — EliteDevTicket

## 1. Visão do produto

O EliteDevTicket é uma plataforma web de eventos pequena, coerente e completa. Seu propósito é permitir que organizadores, clientes e operadores de portaria percorram todo o ciclo de criação, venda, emissão e validação de ingressos sem atalhos manuais no banco de dados ou no código.

O produto privilegia clareza, segurança e explicabilidade. Para manter um escopo executável, utiliza a Ticketmaster como ponto de partida para a criação de eventos, inventário por setores e quantidade em vez de mapa de assentos, reserva temporária de dez minutos, pagamento simulado, ingresso compartilhável e validação de uso único.

Como entrega de avaliação, a experiência deve evidenciar escolhas autorais de produto e UX, evitando uma aparência genérica ou simplesmente derivada de ferramentas de IA. Isso não prescreve uma estética específica: as decisões visuais devem servir às necessidades distintas de Organizer, Customer e Gate e ser explicadas junto das alternativas relevantes descartadas.

As três experiências possuem intenções distintas e complementares:

- **Organizer:** controle e produtividade para configurar e administrar eventos.
- **Customer:** descoberta, confiança e empolgação durante a compra.
- **Gate:** velocidade e certeza em uma ferramenta operacional sem distrações.

## 2. Objetivo do MVP

O MVP será bem-sucedido quando o fluxo principal puder ser realizado integralmente pela interface e pelos dados seedados, sem intervenção manual no banco ou no código:

1. O Organizer entra em sua área, pesquisa uma referência na Ticketmaster, cria um evento em rascunho, configura data, local e setores com capacidade e preço, e publica o evento.
2. O Customer encontra o evento publicado, seleciona setor e quantidade, cria uma reserva com hold de dez minutos e realiza um pagamento simulado aprovado ou recusado.
3. Quando o pagamento é aprovado, o sistema emite exatamente a quantidade comprada de ingressos e os disponibiliza em Meus Ingressos com QR, código manual e link de compartilhamento.
4. A Gate seleciona o evento, valida um ingresso por QR ou código manual e recebe corretamente um dos resultados: `VALID`, `INVALID`, `ALREADY_USED` ou `WRONG_EVENT`.

O MVP também deve proteger os casos que comprometem a confiança no sistema: overselling, duplicação de reserva por nova tentativa, restituição incorreta de estoque, confirmação após expiração e dupla utilização de ingresso.

## 3. Métricas e critérios de sucesso

### 3.1 Integridade do inventário e das reservas

- **SM-01 — Overselling:** zero vendas excedentes em testes concorrentes; requisições disputando o último estoque nunca deixam a disponibilidade negativa.
- **SM-02 — Repetição segura:** zero holds duplicados quando a mesma intenção de reserva é repetida por nova tentativa ou clique duplo.
- **SM-03 — Expiração:** uma reserva expirada devolve seu estoque exatamente uma vez.
- **SM-04 — Recusa:** um pagamento recusado mantém a reserva em `HOLDING` enquanto o hold ainda estiver vigente.
- **SM-05 — Limite temporal:** uma reserva expirada nunca pode ser confirmada posteriormente.

### 3.2 Emissão e validação

- **SM-06 — Emissão exata:** um pagamento aprovado gera exatamente `reservation.quantity` ingressos.
- **SM-07 — Uso único:** duas validações concorrentes do mesmo ingresso resultam em uma `VALID` e uma `ALREADY_USED`, nunca em duas `VALID`.
- **SM-08 — Evento incorreto:** validar um ingresso em outro evento retorna `WRONG_EVENT` sem consumir o ingresso.
- **SM-09 — Clareza operacional:** a Gate apresenta resultado visual inequívoco após a leitura, sem exigir interpretação adicional do operador.

### 3.3 Fluxo ponta a ponta e qualidade da entrega

- **SM-10 — Autossuficiência:** o fluxo principal pode ser executado somente pela interface e com os dados seedados.
- **SM-11 — Estabilidade:** os happy paths de Organizer, Customer e Gate são concluídos sem erro inesperado, incluindo zero respostas HTTP 500.
- **SM-12 — Verificação:** todos os testes críticos definidos para domínio e concorrência passam antes da entrega, conforme a estratégia de testes do addendum técnico.
- **SM-13 — Reprodutibilidade:** um avaliador consegue iniciar o projeto seguindo apenas o README, preferencialmente com `docker compose up --build`, e encontra credenciais e dados de teste claramente documentados.

### 3.4 Contramétricas e guardrails

- A rapidez da Gate não pode enfraquecer a validação atômica nem permitir reutilização do ingresso.
- A recuperação após pagamento recusado não pode prolongar nem recriar o hold original.
- A conclusão aparente do checkout não pode confirmar reservas expiradas ou emitir quantidade incorreta de ingressos.
- A facilidade de avaliação não pode depender de credenciais, segredos ou alterações manuais não documentadas.

## 4. Jornadas dos usuários

### UJ-01 — Ana, Organizer, cria e publica um evento

Ana acessa sua área, pesquisa o catálogo da Ticketmaster e escolhe um show como ponto de partida para um Event próprio. Define data, local e setores, revisa livremente o rascunho com indicação clara das pendências e publica quando estiver segura. Depois, continua gerenciando apenas os campos e setores permitidos pelas regras de publicação.

**Resultado emocional esperado:** produtividade e orientação no início, controle durante a configuração e segurança ao publicar.

### UJ-02 — Bruno, Customer, descobre, reserva e recebe ingressos

Bruno navega pelos eventos publicados, pesquisa, abre detalhes e escolhe setor e quantidade com preço e disponibilidade claros. A reserva retém o estoque por dez minutos e o checkout mostra o tempo restante. Após uma recusa, ele pode tentar novamente durante o hold; após aprovação, recebe os ingressos em Meus Ingressos com QR, código manual e link individual. O evento continua consultável quando as vendas se encerram.

**Resultado emocional esperado:** interesse e empolgação na descoberta, confiança na escolha, urgência controlada no checkout, tranquilização após uma recusa e sucesso claro após a compra.

### UJ-03 — Carla apresenta e Diego, operador Gate, valida o ingresso

Carla abre um ingresso próprio ou recebido por link e apresenta seu QR. Diego seleciona o evento e lê o código pela câmera ou, como fallback, digita o código manual. O sistema responde de forma inequívoca com `VALID`, `INVALID`, `ALREADY_USED` ou `WRONG_EVENT`, consumindo uma única vez o ingresso aceito.

**Resultado emocional esperado:** para o participante, uma entrada simples; para a Gate, velocidade e certeza, com mínima exploração ou tomada de decisão.

## 5. Glossário de domínio

- **Organizer, Customer e Gate:** papéis de acesso. **Gate** também nomeia a superfície operacional usada por quem possui o papel `GATE`.
- **Event:** evento interno pertencente a um Organizer, com identidade própria e referência a um snapshot da Ticketmaster. Seus estados no MVP são `DRAFT` e `PUBLISHED`.
- **TicketSector:** setor de venda pertencente a um Event, definido por nome, capacidade, preço e disponibilidade.
- **Disponibilidade (`availableQuantity`):** quantidade do TicketSector ainda disponível para novos holds. Deve permanecer entre zero e a capacidade.
- **Quantidade comprometida:** parcela da capacidade já retirada da disponibilidade, calculada como `capacity - availableQuantity`; a capacidade nunca pode ser reduzida abaixo desse valor.
- **Reservation:** intenção de compra de 1 a 6 ingressos de um único TicketSector por um Customer. Seus estados são `HOLDING`, `CONFIRMED` e `EXPIRED`.
- **Hold vigente:** Reservation em `HOLDING` para a qual `serverNow < expiresAt`. Um estado `HOLDING` persistido após o vencimento não torna o hold vigente.
- **Payment:** tentativa individual de pagamento simulado associada a uma Reservation, com resultado `APPROVED` ou `DECLINED`.
- **Ticket:** ingresso emitido somente após a confirmação da Reservation. Seus estados de uso são `VALID` e `USED`.
- **Backend como autoridade temporal:** decisões de expiração e fechamento usam o relógio do servidor (`serverNow`), independentemente do relógio exibido no dispositivo do usuário.

## 6. Decisões operacionais do MVP

- **Acesso:** somente usuários provisionados com papéis `ORGANIZER`, `CUSTOMER` e `GATE`; não há cadastro, recuperação de senha nem administração de papéis. A autorização é aplicada pelo backend. Consulte FR-01 a FR-04.
- **Eventos:** todo Event nasce de um snapshot da Ticketmaster, cuja indisponibilidade permite nova tentativa, mas não criação manual. A referência pode ser reutilizada. Publicação exige referência, título, data futura, local completo e ao menos um setor válido; conteúdo opcional recebe fallback. Consulte FR-05 a FR-19.
- **Reservas e pagamento:** cada Reservation reúne de 1 a 6 ingressos de um setor. Há no máximo um hold vigente por Customer e Event; repetição não retém estoque novamente. O pagamento é simulado de forma determinística e o backend determina o valor. Consulte FR-24 a FR-37.
- **Compartilhamento:** o link permanente e não revogável expõe somente o necessário para apresentar o Ticket, não transfere propriedade e continua mostrando `USED` após o consumo. Consulte FR-38 a FR-42.
- **Gate:** qualquer `GATE` seleciona um Event `PUBLISHED` e valida online; a câmera é o fluxo móvel principal, e a entrada manual é o fallback obrigatório. Não há associação Gate–Event nem modo offline. Consulte FR-43 a FR-52 e NFR-11 a NFR-14.

Essas decisões priorizam uma demonstração reproduzível e mantêm fora do MVP capacidades que não contribuem diretamente para a avaliação.

## 7. Requisitos não funcionais

### 7.1 Tempo, moeda e autoridade temporal

- **NFR-01:** O MVP deve operar em BRL e exibir valores monetários em reais (`R$`).
- **NFR-02:** Datas e horários apresentados ao usuário devem usar o timezone `America/Sao_Paulo`; instantes devem ser persistidos de forma não ambígua.
- **NFR-03:** O backend deve ser a autoridade temporal para holds, expirações e fechamento de vendas.
- **NFR-04:** Quando `serverNow >= startsAt`, inclusive no instante exato de início, novas reservas devem ser recusadas como vendas encerradas.

### 7.2 Desempenho percebido

As metas abaixo são objetivos em condições normais do ambiente de avaliação, não SLAs de produção:

- **NFR-05:** Após um QR ser decodificado ou um código manual ser submetido, a Gate deve apresentar o resultado da validação em até 1 segundo.
- **NFR-06:** Ações comuns — login, listagens, busca, abertura de detalhes, criação/edição de rascunho, criação de reserva, pagamento e abertura de Meus Ingressos — devem concluir em até 2 segundos em condições normais do ambiente de avaliação.
- **NFR-07:** Toda operação assíncrona iniciada pelo usuário deve apresentar feedback visual assim que o processamento começar e mantê-lo até sucesso ou falha, evitando ações sem confirmação de processamento. Um limiar temporal específico para o início desse feedback não é requisito do MVP.

### 7.3 Acessibilidade

- **NFR-08:** O MVP deve buscar conformidade WCAG 2.1 nível AA.
- **NFR-09:** Fluxos essenciais devem oferecer navegação por teclado, foco visível, labels associados, semântica adequada, contraste suficiente e mensagens compreensíveis.
- **NFR-10:** Resultados da Gate nunca devem depender somente de cor; devem combinar texto, ícone e tratamento visual inequívoco.

### 7.4 Compatibilidade e responsividade

- **NFR-11:** A aplicação deve suportar as versões estáveis mais recentes disponíveis na data da entrega de Chrome, Edge e Firefox em desktop e Chrome no Android; as versões efetivamente verificadas devem ser registradas no README.
- **NFR-12:** Safari no iOS é alvo móvel em regime de best effort: seus fluxos principais devem ser verificados e limitações encontradas devem ser documentadas no README, sem bloquear a entrega quando a digitação manual preservar a operação da Gate.
- **NFR-13:** Navegadores legados estão fora do escopo.
- **NFR-14:** O scanner pode depender das APIs de câmera e de contexto seguro do navegador, mas a digitação manual deve permanecer disponível como fallback obrigatório.

### 7.5 Auditoria, privacidade e segurança operacional

- **NFR-15:** Cada tentativa de pagamento deve persistir horário e resultado.
- **NFR-16:** Cada tentativa de validação na Gate deve registrar horário, operador, evento, ticket quando identificável, método e resultado.
- **NFR-17:** Tokens, códigos completos, JWTs, senhas e outros segredos nunca devem ser gravados em logs ou registros de auditoria.
- **NFR-18:** Erros apresentados ao cliente não devem expor stack traces, detalhes internos ou segredos.

### 7.6 Documentação e avaliabilidade

- **NFR-19:** O README deve documentar a configuração e o uso do banco de dados, as variáveis de ambiente, as credenciais seedadas, como executar a aplicação e os passos para reproduzir criação/publicação, pagamento aprovado/recusado, compartilhamento e os quatro resultados da Gate. Este requisito atende diretamente à exigência do desafio oficial de explicar a configuração e o uso do banco.
- **NFR-20:** O README deve declarar claramente qualquer funcionalidade incompleta, limitação conhecida ou comportamento diferente do esperado.

## 8. Requisitos funcionais

**Navegação:** autenticação e autorização; catálogo e criação; configuração e publicação; descoberta pública; reservas; pagamentos; ingressos; Gate; preparação para avaliação.

### 8.1 Autenticação e autorização

**Rastreabilidade:** UJ-01, UJ-02 e UJ-03; SM-10 e SM-11.

#### FR-01 — Login com usuário provisionado

O sistema deve permitir que um usuário provisionado autentique-se com suas credenciais e receba acesso correspondente ao papel `ORGANIZER`, `CUSTOMER` ou `GATE`.

#### FR-02 — Autorização por papel

O backend deve restringir cada operação ao papel autorizado e rejeitar acessos incompatíveis, independentemente dos controles exibidos no frontend.

#### FR-03 — Isolamento de propriedade

O Organizer deve administrar somente seus próprios Events, e o Customer deve acessar somente suas próprias Reservations e seus próprios Tickets pelas áreas autenticadas.

#### FR-04 — Logout

Um usuário autenticado deve poder encerrar sua sessão e retornar ao estado não autenticado, permitindo a troca segura entre as contas provisionadas durante a avaliação.

### 8.2 Catálogo Ticketmaster e criação do Event

**Rastreabilidade:** UJ-01; SM-10 e SM-11.

#### FR-05 — Pesquisa no catálogo

O Organizer deve poder pesquisar referências de eventos na Ticketmaster e visualizar, quando fornecidos pelo catálogo, título, imagem, descrição e categoria para escolher uma referência. Realiza UJ-01.

#### FR-06 — Tratamento de indisponibilidade

Quando a Ticketmaster não puder responder, o sistema deve informar que o catálogo está indisponível, oferecer uma ação de nova tentativa e não oferecer criação manual no MVP.

#### FR-07 — Criação a partir de snapshot

O Organizer deve poder criar um Event `DRAFT` a partir de uma referência Ticketmaster, copiando os dados disponíveis como snapshot e atribuindo uma identidade interna independente.

#### FR-08 — Reutilização de referência

O sistema deve permitir que o mesmo `externalId` origine mais de um Event interno, inclusive para o mesmo Organizer.

### 8.3 Configuração, publicação e gerenciamento

**Rastreabilidade:** UJ-01; SM-10 e SM-11.

#### FR-09 — Edição livre do rascunho

Enquanto um Event estiver `DRAFT`, seu Organizer deve poder alterar os dados do Event e configurar seus TicketSectors.

#### FR-10 — Listagem dos próprios eventos

O Organizer deve poder listar seus próprios Events, incluindo os estados `DRAFT` e `PUBLISHED`, e identificar claramente o estado de cada um.

#### FR-11 — Exclusão de rascunho

O Organizer deve poder excluir um Event próprio enquanto estiver `DRAFT`; Events `PUBLISHED` não podem ser excluídos nem cancelados no MVP.

#### FR-12 — Gerenciamento de setores

O Organizer deve poder adicionar, editar e remover TicketSectors com nome, capacidade e preço enquanto as regras do estado do Event permitirem.

#### FR-13 — Indicação de pendências

Antes da publicação, a interface deve listar os campos e condições obrigatórios ainda não satisfeitos e impedir a ação de publicação enquanto houver qualquer pendência.

#### FR-14 — Validação de publicação

O sistema deve permitir publicar somente um Event que possua referência Ticketmaster, título, data futura, nome e endereço do local e ao menos um TicketSector com nome, capacidade maior que zero e preço maior ou igual a zero.

#### FR-15 — Conteúdo opcional com fallback

A ausência de descrição, imagem ou categoria não deve impedir a publicação; as superfícies públicas devem oferecer uma apresentação válida para cada conteúdo ausente, sem exibir imagem quebrada, valor nulo ou campo técnico. A definição visual exata permanece destinada à etapa de UX conforme §11.

#### FR-16 — Imutabilidade estrutural

Depois da publicação, o sistema deve impedir alterações em `title`, `venueName`, `venueAddress`, `startsAt`, `externalSource` e `externalId`.

#### FR-17 — Conteúdo editável após publicação

Depois da publicação, o Organizer deve poder alterar `description`, `imageUrl` e `category` de seu Event.

#### FR-18 — Proteção de setor comprometido

Depois da publicação, um TicketSector com qualquer Reservation ou Ticket associado não pode ser removido. Sua capacidade pode ser reduzida somente até o limite da quantidade atualmente comprometida.

#### FR-19 — Alteração segura de capacidade e preço

Depois da publicação, o Organizer pode aumentar ou reduzir a capacidade dentro do limite permitido e alterar o preço; reservas existentes preservam quantidade e preço capturados no momento da criação.

### 8.4 Descoberta pública

**Rastreabilidade:** UJ-02; SM-10 e SM-11.

#### FR-20 — Listagem pública de eventos publicados

Um visitante autenticado ou não deve poder navegar por Events `PUBLISHED`, com cards exibindo imagem ou fallback, título, data, local e preço inicial. O preço inicial corresponde ao menor preço entre os TicketSectors do Event: `startingPrice = MIN(TicketSector.price)`. Realiza UJ-02.

#### FR-21 — Busca pública simples

Um visitante autenticado ou não deve poder pesquisar Events publicados por título; filtros avançados não fazem parte do escopo obrigatório do MVP.

#### FR-22 — Detalhes públicos e disponibilidade

Um visitante autenticado ou não deve poder abrir um Event publicado e consultar seus dados, TicketSectors, preços e disponibilidade atual.

#### FR-23 — Fechamento derivado de vendas

O Event publicado deve continuar consultável após seu início, mas o sistema deve bloquear novas Reservations quando `serverNow >= startsAt`.

### 8.5 Reserva, hold e expiração

**Rastreabilidade:** UJ-02; SM-01, SM-02, SM-03, SM-04, SM-05, SM-10 e SM-11.

#### FR-24 — Criação autenticada de reserva

Um visitante deve autenticar-se como `CUSTOMER` para iniciar uma Reservation. O Customer autenticado deve poder reservar um único TicketSector e uma quantidade inteira entre 1 e 6, desde que haja disponibilidade e as vendas estejam abertas.

#### FR-25 — Hold temporário

Uma Reservation criada deve entrar em `HOLDING`, reduzir atomicamente a disponibilidade e expirar dez minutos após sua criação segundo o relógio do servidor.

#### FR-26 — Preço capturado

A Reservation deve capturar o preço unitário vigente e o valor total no momento da criação; o frontend não pode definir esses valores.

#### FR-27 — Uma reserva ativa vigente por evento

Um Customer pode ter no máximo uma Reservation `HOLDING` ainda vigente (`serverNow < expiresAt`) por Event. Uma nova tentativa deve retornar ou direcionar à Reservation vigente sem reduzir estoque novamente. Uma Reservation vencida não pode bloquear um novo hold apenas porque seu estado ainda não foi atualizado pelo processo de expiração.

#### FR-28 — Repetição segura da intenção de reserva

Repetir a mesma intenção de criação por nova tentativa ou clique duplo deve retornar a mesma Reservation, sem criar outro hold nem reduzir o estoque novamente.

#### FR-29 — Concorrência de estoque

Quando solicitações concorrentes disputarem estoque insuficiente, somente as que couberem na disponibilidade podem criar hold, e a disponibilidade nunca pode ficar negativa.

#### FR-30 — Timer do checkout

O checkout deve exibir claramente o tempo restante do hold e atualizar sua apresentação conforme a expiração se aproxima.

#### FR-31 — Expiração idempotente

Ao expirar, uma Reservation deve mudar para `EXPIRED` e devolver seu estoque exatamente uma vez, mesmo se a expiração for processada repetidamente ou durante outra operação.

### 8.6 Pagamento e emissão

**Rastreabilidade:** UJ-02; SM-04, SM-05, SM-06, SM-10 e SM-11.

#### FR-32 — Simulação determinística

O Customer deve poder provocar explicitamente um resultado `APPROVED` ou `DECLINED` no pagamento simulado para reproduzir ambos os fluxos. Realiza UJ-02.

#### FR-33 — Múltiplas tentativas

Enquanto a Reservation permanecer `HOLDING` e vigente, o Customer pode realizar múltiplas tentativas de Payment, e cada tentativa deve ser registrada separadamente.

#### FR-34 — Recusa sem perda do hold

Um Payment recusado deve permanecer `DECLINED` sem confirmar a Reservation nem devolver o estoque antes do vencimento do hold.

#### FR-35 — Aprovação atômica

Um Payment aprovado deve confirmar uma Reservation vigente exatamente uma vez; aprovação e expiração concorrentes devem produzir uma única transição final válida.

#### FR-36 — Bloqueio após expiração

O sistema deve recusar tentativas de Payment para uma Reservation expirada e nunca confirmá-la posteriormente.

#### FR-37 — Emissão exata e idempotente

Ao confirmar uma Reservation, o sistema deve emitir exatamente um Ticket por unidade comprada, sem duplicar Tickets em novas tentativas ou reprocessamentos.

### 8.7 Meus Ingressos e compartilhamento

**Rastreabilidade:** UJ-02 e UJ-03; SM-06, SM-08, SM-10 e SM-11.

#### FR-38 — Listagem de ingressos próprios

O Customer deve poder acessar Meus Ingressos. Cada Ticket deve exibir, no mínimo, título do Event, TicketSector, data e hora, local, estado atual, QR, código manual e ação de compartilhamento. Realiza UJ-02.

#### FR-39 — Identificadores seguros

Cada Ticket deve possuir identificadores únicos e não previsíveis para validação, código manual e compartilhamento; o token de compartilhamento deve ser distinto do token de validação.

#### FR-40 — Link público do ingresso

O Customer deve poder obter e copiar o link público permanente de cada Ticket, sem regeneração, rotação, revogação ou transferência de propriedade no MVP.

#### FR-41 — Conteúdo mínimo compartilhado

O link compartilhado deve apresentar, no mínimo, título do Event, data e hora, local, TicketSector, estado do Ticket, QR e código manual, sem revelar dados pessoais do Customer.

#### FR-42 — Ingresso compartilhado utilizado

Depois que o Ticket for utilizado, seu link deve continuar acessível, exibir claramente o estado `USED` e não permitir nova entrada.

### 8.8 Validação na Gate

**Rastreabilidade:** UJ-03; SM-07, SM-08, SM-09, SM-10 e SM-11.

#### FR-43 — Seleção do evento

Um usuário `GATE` deve poder listar todos os Events `PUBLISHED` e selecionar o Event de trabalho antes de validar Tickets. Realiza UJ-03.

#### FR-44 — Leitura por câmera

A Gate deve poder capturar o QR pela câmera quando o dispositivo e o contexto seguro oferecerem suporte.

#### FR-45 — Entrada manual

A Gate deve permitir a digitação do código manual como fallback obrigatório à câmera.

#### FR-46 — Resultado válido e consumo

Quando um Ticket `VALID` pertencer ao Event selecionado, a primeira validação deve retornar `VALID` e marcar o Ticket como `USED` de forma atômica.

#### FR-47 — Resultado já utilizado

Quando um Ticket já estiver `USED`, a validação deve retornar `ALREADY_USED` e não alterar novamente seu estado.

#### FR-48 — Resultado de evento errado

Quando o Ticket existir mas pertencer a outro Event, a validação deve retornar `WRONG_EVENT` sem consumir o Ticket.

#### FR-49 — Resultado inválido

Quando nenhum Ticket for reconhecido pelo QR ou código informado, inclusive quando o QR ou código tiver sido falsificado, adulterado ou não tiver sido emitido pelo sistema, a validação deve retornar `INVALID` sem revelar detalhes sensíveis.

#### FR-50 — Concorrência de validação

Duas validações concorrentes do mesmo Ticket devem produzir no máximo um resultado `VALID`; as demais devem retornar `ALREADY_USED`.

#### FR-51 — Feedback inequívoco

A Gate deve comunicar cada resultado por texto, ícone e tratamento visual distinto, sem depender somente de cor.

#### FR-52 — Auditoria da validação

Cada tentativa deve registrar horário, operador, Event, Ticket quando identificável, método (`QR` ou manual) e resultado, sem persistir tokens ou códigos completos.

### 8.9 Preparação para avaliação

**Rastreabilidade:** SM-10, SM-12 e SM-13.

#### FR-53 — Dados de demonstração

O projeto deve fornecer dados seedados com ao menos um Organizer, dois Customers, um usuário `GATE` e um Event `PUBLISHED` com TicketSectors e estoque disponível.

## 9. Fora do escopo do MVP

- Cadastro público, recuperação de senha e administração de papéis.
- Filmes e integração com TMDb.
- Criação manual de Event sem referência Ticketmaster.
- Mapa de assentos e seleção de assento individual.
- Gateway de pagamento real, cobrança financeira e conformidade PCI.
- Cancelamento de Event, cancelamento de compra, reembolso e devolução voluntária de estoque.
- Revenda, transferência de propriedade e envio de ingresso por e-mail.
- Revogação ou expiração do link compartilhado.
- Associação entre Gate e Event.
- Operação offline da Gate e sincronização posterior.
- Aplicativos nativos, navegadores legados, microsserviços, filas distribuídas e cache distribuído.
- Analytics avançado, dashboards financeiros e atualização em tempo real por SSE/WebSocket.

## 10. Restrições da avaliação

- Entrega até 18 de agosto de 2026.
- Repositório público no GitHub e submissão pelo formulário indicado no desafio.
- Commits descritivos distribuídos ao longo do período de desenvolvimento.
- README suficiente para configuração, execução e avaliação autônoma.
- Artefatos de PRD, BMAD e contexto de IA, quando produzidos durante o projeto, versionados no repositório; sua produção não é obrigatória pelo desafio.
- Declaração das ferramentas de IA utilizadas, das partes assistidas, das partes feitas sem IA e do processo de decisão e revisão humana.
- README explicando as principais escolhas autorais de produto e UX e as alternativas relevantes descartadas, de modo a tornar o raciocínio do candidato verificável.
- Deploy é opcional e vale bônus de 1 ponto.

## 11. Questões abertas não bloqueantes

- Definir durante UX os fallbacks visuais exatos para imagem, descrição e categoria ausentes.
- Definir durante UX os limiares visuais dos estados normal, atenção e crítico do timer, sem alterar a duração total do hold.
- Validar em implementação o suporte real à câmera nos navegadores móveis-alvo e preservar entrada manual em todos os casos.
