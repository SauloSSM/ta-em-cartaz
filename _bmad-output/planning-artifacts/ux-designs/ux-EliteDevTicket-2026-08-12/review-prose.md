# Revisão editorial — prosa

Este conjunto existe para ajudar designers, engenharia e revisores a implementar e verificar a experiência do EliteDevTicket sem alterar as decisões de produto e Domain.

Guia aplicado: **Microsoft Writing Style Guide**, calibrado para leitores humanos. Foram preservados os nomes canônicos de entidades, estados, componentes, atributos ARIA e termos técnicos necessários.

| Pass | Original Text | Revised Text | Changes |
|---|---|---|---|
| prose | “estados interativos, disabled e focus” | “estados interativos, desabilitados e de foco” | Eliminou mistura de idiomas sem valor terminológico. |
| prose | “Do's and Don'ts” | “Boas práticas e antipadrões” | Tornou o heading escaneável em português. |
| prose | “Apresentar ticket bearer-like” | “Apresentar ingresso ao portador” | Substituiu anglicismo ambíguo. |
| prose | `EventDetail` | “a S02” | Removeu nome de componente que não pertence ao catálogo canônico, preservando a mesma superfície e regra. |
| prose | `PaymentForm` | `PaymentSimulationControl` | Alinhou a passagem ao nome canônico sem mudar o comportamento. |
| prose | “Durante processamento, mantém resumo e impede reenvio” | “Durante o processamento, mantém o resumo e impede o reenvio” | Melhorou fluidez e precisão gramatical. |
| prose | `aria-current=page` | `aria-current="page"` | Corrigiu a notação do atributo. |
| prose | “Cold-load estrutural” | “Carregamento inicial estrutural” | Eliminou anglicismo desnecessário. |
| prose | “Web Share é melhoria progressiva e copy é fallback” | “Web Share é melhoria progressiva e a cópia do link é o fallback” | Esclareceu a alternativa sem mudar a decisão. |
| prose | “Share API indisponível” | “Web Share API indisponível” | Usou o nome técnico preciso. |
| prose | “anatomy”, “live-region” e “responsive” | “anatomia”, “live region” e “responsivo” | Normalizou a prosa; preservou os estados técnicos enumerados. |

Resumo: 11 correções seguras foram aplicadas. Nenhuma altera Domain, escopo MVP, regras de pagamento, hold, Ticketmaster, roles, handoffs de Architecture, bloqueio de tokens do Design System, IDs, jornadas ou links de wireframe.
