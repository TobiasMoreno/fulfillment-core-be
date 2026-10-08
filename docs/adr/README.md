# Architecture Decision Records

Los ADR registran decisiones importantes, alternativas y consecuencias. No reemplazan la especificación funcional.

## Estados

- `PROPOSED`: necesita revisión humana; no autoriza trabajo funcional.
- `ACCEPTED`: aprobado explícitamente por una persona responsable.
- `SUPERSEDED`: reemplazado por otro ADR enlazado.
- `REJECTED`: evaluado y descartado.

## Índice

| ADR | Decisión | Estado |
|---|---|---|
| [0001](0001-modular-monolith.md) | Monolito modular | `ACCEPTED` por restricción inicial |
| [0002](0002-messaging-platform.md) | RabbitMQ detrás de puertos y alternativa SQS/SNS | `PROPOSED` |
| [0003](0003-consistency-strategy.md) | ACID local, outbox/inbox y process manager | `PROPOSED` |

Para crear un ADR: copiar la estructura contexto → drivers → opciones → decisión → consecuencias → validación. Un agente puede proponerlo; sólo una persona cambia su estado a `ACCEPTED`.

