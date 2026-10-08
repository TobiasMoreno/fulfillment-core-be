# ADR-0003: consistencia local fuerte y coordinación eventual confiable

- Estado: `PROPOSED`
- Fecha: 2026-10-08

## Contexto

Órdenes, stock, pagos y envíos tienen propietarios e invariantes diferentes. Aun dentro de un monolito, una transacción global entre todos los conceptos aumenta acoplamiento y no incluye de forma segura broker ni proveedores externos. El flujo debe sobrevivir reinicios, duplicados y resultados ambiguos.

## Drivers

- Proteger dinero y stock.
- Evitar pérdida de eventos entre DB y broker.
- Recuperar procesos largos.
- Hacer fallos y compensaciones observables.
- Mantener diseño localmente ejecutable.

## Alternativas

### A. Transacción local + outbox/inbox + process manager — recomendada

Cada comando confirma su agregado y una fila outbox en una transacción PostgreSQL. Un relay publica; consumidores deduplican y confirman su estado local. Fulfillment persiste la coordinación y emite compensaciones explícitas.

Ventajas:

- no hay ventana DB/broker de dual write;
- recovery y auditoría visibles;
- usa capacidades maduras de PostgreSQL/RabbitMQ;
- se parece a una futura separación sin exigir microservicios.

Costos:

- consistencia eventual entre módulos;
- duplicados y eventos fuera de orden;
- tablas/relay/process manager requieren limpieza, métricas y runbook.

### B. Una transacción ACID para todos los módulos — no elegida para el proceso completo

Dentro del mismo PostgreSQL simplifica varios cambios, pero acopla schemas y no abarca RabbitMQ, pagos o carriers. Puede usarse dentro de un único módulo/agregado, no como mecanismo de coordinación cross-context.

### C. Dual write directo DB + broker — rechazada

Publicar después del commit puede perder el evento; publicar antes puede anunciar un cambio que luego hace rollback. Reintentar sin estado durable no cierra la ventana.

### D. XA/2PC — rechazada

RabbitMQ/JDBC podrían coordinarse parcialmente con infraestructura adicional, pero proveedores HTTP no participan. Añade bloqueo, recuperación compleja y acoplamiento sin resolver el proceso end-to-end.

### E. Event sourcing total — pospuesta/rechazada inicialmente

Brinda historial y reconstrucción potentes, pero obliga a diseñar versionado/proyecciones de todo el dominio antes de validar necesidades. Un timeline/audit log más outbox cubre el aprendizaje inicial con menor complejidad.

## Decisión propuesta

- ACID local protege cada agregado e invariantes bajo su owner.
- Transactional outbox registra eventos en el mismo commit.
- Relay usa claim/lock acotado, publish confirm y marca publicación de forma idempotente.
- Inbox o constraint equivalente deduplica `eventId` dentro de la misma transacción del efecto.
- Process manager de Fulfillment persiste estado, pasos, deadline y último error.
- Compensaciones son comandos de negocio idempotentes, no rollback mágico.
- Efectos externos usan referencia/idempotency key estable y reconciliación ante timeout ambiguo.
- Redis no coordina consistencia y su pérdida no afecta invariantes.

## Estados y orden

- Cada agregado usa versión para optimistic concurrency cuando corresponda.
- Eventos llevan versión del contrato y del agregado si resulta necesaria.
- Un consumidor define qué hacer con eventos tardíos; no confía sólo en orden de entrega.
- Exactly-once no se declara. “Effectively once” se prueba por efecto e idempotency key.

## Operación

Métricas mínimas:

- cantidad y edad máxima de outbox pendiente;
- publicaciones fallidas y confirm latency;
- duplicados de inbox;
- retries/DLQ por consumidor;
- procesos en cada estado y vencidos;
- compensaciones fallidas.

Retención y limpieza de outbox/inbox necesitan job controlado, índices y runbook antes de producción.

## Consecuencias

- El estado intermedio es visible y debe ser aceptado por producto.
- Los handlers requieren disciplina idempotente.
- Los tests deben cubrir crash points, no sólo happy path.
- Operaciones obtiene herramientas claras para reconciliar sin editar tablas manualmente.

## Validación antes de aceptar

1. Aprobar orden de reserva/pago/captura.
2. Prototipar outbox con caída antes/después del publish confirm.
3. Demostrar consumidor duplicado y crash antes/después del commit.
4. Probar al menos una compensación y un resultado externo ambiguo.
5. Definir retención, alertas y acción segura sobre DLQ.
