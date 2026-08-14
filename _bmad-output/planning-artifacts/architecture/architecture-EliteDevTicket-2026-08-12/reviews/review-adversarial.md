# Revisão adversarial final — Architecture Spine

**Artefato:** `ARCHITECTURE-SPINE.md` pós-Checkpoint 7  
**Lentes:** versões/fit oficial; divergência entre unidades downstream; integridade; segurança  
**Modo:** revisão somente  
**Veredito:** **PASS — nenhum finding material aberto.**

## Resultado

Não foi possível construir duas unidades downstream que, cumprindo integralmente os ADs atuais, divergiriam materialmente em ownership, mutações, contrato HTTP, replay/idempotência, locks, ambientes ou segurança do MVP.

| Área reavaliada | Resultado |
| --- | --- |
| Fingerprints de Payment e Gate | **Fechado.** AD-23 fixa versão, campos, ordem, normalização, digest, tratamento de credencial e conflito para payload incompatível. |
| Claim/replay concorrente | **Fechado.** Unicidade no banco e unidade atômica de claim/processamento/resultado impedem efeito duplicado; AD-14 mantém consumo do Ticket e resultado Gate no mesmo commit. |
| Contrato SPA ↔ API | **Fechado.** AD-12 torna OpenAPI versionado a autoridade e AD-21 exige checks automatizados de drift. |
| Evidência por testes | **Fechado.** AD-21 exige replay compatível, conflito incompatível, efeito único concorrente, replay de `VALID`, `WRONG_EVENT` sem consumo e PostgreSQL real para concorrência. |
| JWT e CSRF | **Fechado.** HS256, segredo externo CSPRNG ≥256 bits por ambiente, assinatura/`exp`, cookie, TTL, renovação CSRF e fail-fast estão normativos. |
| BCrypt | **Fechado no nível apropriado.** Algoritmo, configuração por ambiente, default explícito no cold-start e teste dos parâmetros do hash convergem a implementação sem congelar tuning dependente do runtime no spine. |
| `manualCode` | **Fechado.** Formato, alfabeto, comprimento, CSPRNG, normalização e unicidade estão definidos. |
| Fronteiras modulares | **Fechado.** AD-1 define a fronteira e AD-21 exige verificação arquitetural automática do grafo e imports proibidos. |
| Node/runtime frontend | **Fechado.** Node.js 22.12+ LTS é compatível com Vite 7.3; patches passam corretamente ao lockfile/cold-start. |

## Teste adversarial de duas unidades

| Dimensão | Unidade A | Unidade B | Resultado |
| --- | --- | --- | --- |
| Estoque/capacidade | Usa ports e locks na ordem canônica. | Implementação interna diferente, mesma ordem, lazy expiry e invariantes. | Efeitos convergem. |
| Payment | Claim por constraint/lock e fingerprint v1. | Upsert/lock equivalente e mesmo fingerprint v1. | Um efeito e mesmo replay. |
| Gate | Resolve attempt, bloqueia Ticket e faz commit conjunto. | Estratégia JPA/SQL distinta, preservando a mesma unidade atômica. | Mesmo outcome, inclusive replay de `VALID`. |
| API | DTOs derivados do OpenAPI. | DTOs manuais validados contra o OpenAPI. | Wire contract converge. |
| Módulos | ArchUnit. | Verificador equivalente. | Grafo e proibições convergem. |
| Ambientes | Node 22.x e patches pinados. | Outro patch 22.x compatível e pinado. | Build reproduzível após cold-start. |

## Verificação oficial de stack

| Tecnologia | Situação em 2026-08-12 |
| --- | --- |
| Java 21 / Spring Boot 4.0.7 | Compatíveis; documentação oficial da linha 4.0.7 permanece disponível. [Spring Boot 4.0.7](https://docs.spring.io/spring-boot/4.0/reference/using/index.html) |
| React 19.2.x | Linha válida documentada oficialmente. [React versions](https://react.dev/versions) |
| TypeScript 5.x | Escolha conservadora válida; patch será fixado no lockfile. [TypeScript 5.9](https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-9.html) |
| Vite 7.3.x | Linha suportada com backports importantes/de segurança. [Vite releases](https://vite.dev/releases) |
| Node.js 22.12+ LTS | Linha LTS oficial e satisfaz o requisito do Vite 7. [Node releases](https://nodejs.org/en/about/previous-releases), [Vite guide](https://vite.dev/guide/) |
| PostgreSQL 17.x | Suportado oficialmente até novembro de 2029; usar minor corrente no cold-start. [PostgreSQL versioning](https://www.postgresql.org/support/versioning/) |
| Docker Compose Specification | Formato atual e recomendado. [Compose reference](https://docs.docker.com/reference/compose-file/) |

As linhas não mais recentes (Boot 4.0.x, TypeScript 5.x e Vite 7.3.x) são escolhas deliberadas e ainda compatíveis/suportadas; não constituem conflito.

## Conflitos autoritativos

**Nenhum.** O spine permanece consistente com o PDF oficial, PRD aprovado, Domain v1.2, addendum técnico e UX Behavioral Contract. Nenhuma correção de produto ou domínio é necessária.

## Disposição

**Aprovado para finalização e handoff.** Não há finding adversarial material que justifique novo checkpoint ou refinamento do spine.
