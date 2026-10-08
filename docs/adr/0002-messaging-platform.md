# ADR-0002: mensajería local con RabbitMQ detrás de contratos propios

- Estado: `PROPOSED`
- Fecha: 2026-10-08

## Contexto

Fulfillment necesita desacoplar efectos, tolerar fallos y enseñar entrega al menos una vez. La restricción inicial exige RabbitMQ local y evaluar una alternativa SQS/SNS. No se debe contaminar dominio/aplicación con tipos del broker ni fingir que los productos tienen semántica idéntica.

## Drivers

- Ejecución local gratuita y reproducible.
- Routing de eventos y comandos.
- Publisher confirms, redelivery y dead-lettering observables.
- Testcontainers.
- Posibilidad de evaluar AWS sin rediseñar casos de uso.
- Operación comprensible para una sola aplicación.

## Alternativas

| Criterio | RabbitMQ local | SNS + SQS | PostgreSQL/in-process |
|---|---|---|---|
| Local | Imagen oficial liviana | Requiere LocalStack/emulación o AWS | Muy simple |
| Routing | Exchanges/bindings flexibles | Fanout SNS a colas SQS | Debe construirse |
| Entrega | Ack/nack, prefetch, redelivery | Visibility timeout, receive/delete | Polling/callback |
| Orden | Por cola con matices | FIFO sólo en recursos específicos | Diseñable |
| DLQ | Dead-letter exchange/queue | Redrive policy | Tabla/proceso propio |
| Backpressure | Prefetch/consumidores | Polling y visibility | Limitado/manual |
| Portabilidad | Broker neutral a nivel aplicación | Cloud administrado, IAM/costos | Acopla mensajería a DB/proceso |
| Valor educativo | Alto y visible | Alto para AWS, más setup | Oculta semántica distribuida |

### Alternativa A: RabbitMQ — recomendada

Usar exchanges topic, colas durables, mensajes persistentes, publisher confirms, consumers manual-ack, retry acotado y DLQ. En local, management UI facilita inspección.

### Alternativa B: SNS + SQS — evaluada, no inicial

Ventajas: operación administrada, escalado e integración AWS. Diferencias relevantes: SNS publica/fanout y SQS hace polling; visibility timeout reemplaza ack/nack; FIFO, deduplicación, tamaño, IAM y DLQ tienen configuración propia. LocalStack agrega peso y puede no reproducir todos los comportamientos.

Se implementaría un adapter distinto y pruebas contractuales del puerto. No se pretende reutilizar topología Rabbit literalmente.

### Alternativa C: eventos en memoria o polling PostgreSQL — rechazada como transporte principal

En memoria pierde eventos al reiniciar y no enseña recuperación distribuida. Una cola propia en PostgreSQL puede ser válida a baja escala, pero duplica features de broker y mezcla carga operacional con la fuente de verdad. PostgreSQL sí aloja el outbox transaccional, no el transporte final.

## Decisión propuesta

1. RabbitMQ será el primer transporte local.
2. Aplicación dependerá de contratos mínimos como `EventPublisher`/handlers y de un envelope propio; no de `RabbitTemplate`, AMQP headers o nombres de exchange.
3. El adapter Rabbit es dueño de topology, serialización y ack.
4. Outbox evita dual write; publisher confirms confirman al relay, no a la transacción de negocio.
5. Consumidores son idempotentes y asumen duplicados.
6. Se construirá un spike de adapter SNS/SQS sólo si se aprueba una evaluación concreta; no se agrega LocalStack ahora.

El puerto no ocultará capacidades específicas. Orden estricto, delay, priority o FIFO se modelan como requisitos explícitos y pueden invalidar la sustitución.

## Topología inicial a validar

- exchange de eventos por entorno, tipo `topic` y durable;
- routing key `<context>.<aggregate>.<event>.v<version>`;
- una cola por consumidor lógico, nunca una cola compartida accidentalmente por módulos;
- DLX y cola de parking por consumidor;
- retry con TTL/colas o scheduler sólo después de medir complejidad;
- payload JSON versionado y content type explícito.

## Consecuencias

- Se aprende y prueba la semántica real de redelivery.
- Existe operación adicional local y riesgo de topologías inconsistentes.
- El adapter alternativo requiere trabajo real; la interfaz reduce acoplamiento, no elimina diferencias.
- El broker caído no bloquea commits gracias al outbox, pero aumenta backlog.

## Validación antes de aceptar

- Probar publish confirm, caída del broker y recuperación del outbox.
- Probar redelivery, duplicados, prefetch y DLQ con Testcontainers.
- Definir límites de payload y compatibilidad.
- Comparar un caso representativo en SNS/SQS por semántica, costo y operación.
- Aprobar nombres, retries y política de parking.

