# Visión de producto

- Estado: `PROPOSED`
- Owner humano: pendiente
- Última revisión: 2026-10-08
- Alcance de este documento: comportamiento objetivo; no autoriza implementación

## Problema y resultado esperado

Fulfillment Core será un entorno educativo profesional para comprender cómo un marketplace procesa una orden desde su aceptación hasta el envío, manteniendo invariantes de stock y dinero frente a concurrencia, duplicados y fallos parciales.

El producto debe permitir demostrar ownership de punta a punta: lenguaje de negocio, decisiones explícitas, contratos, persistencia, consistencia eventual, seguridad, observabilidad, pruebas y operación local. No busca competir con una plataforma comercial ni simular escala ficticia.

## Supuestos

- Un sistema externo ya resolvió catálogo, precios y checkout; entrega una intención de orden con precios congelados.
- Una orden usa una única moneda.
- El pago y el transportista son simulados, pero sus adapters reproducen latencia, rechazo y timeout.
- Hay un único nodo de aplicación al inicio; el diseño no debe impedir ejecutar varios más adelante.
- PostgreSQL es la fuente de verdad. Redis puede perderse y reconstruirse.
- La entrega de mensajes es al menos una vez; duplicados son normales.

## Actores

| Actor | Objetivo | Confianza |
|---|---|---|
| Comprador | Conocer el estado de su compra y evitar cargos/órdenes duplicadas | No confiable; autenticado en una etapa futura |
| Marketplace/Checkout | Crear una orden idempotentemente con datos ya cotizados | Sistema upstream autenticado |
| Operador de fulfillment | Investigar fallos, reintentar acciones seguras y resolver excepciones | Rol privilegiado y auditado |
| Soporte | Consultar estado y timeline sin mutar dinero o stock | Sólo lectura limitada |
| Administrador técnico | Operar infraestructura y observar salud | Privilegio mínimo; sin acceso de negocio implícito |
| Proveedor de pago simulado | Autorizar, capturar, rechazar o quedar indisponible | Sistema externo no confiable |
| Proveedor de envío simulado | Crear y actualizar un envío o fallar temporalmente | Sistema externo no confiable |
| Reloj/scheduler | Expirar reservas y disparar recuperaciones | Actor técnico determinista en pruebas |

## Casos de uso objetivo

1. Crear una orden a partir de un checkout aceptado.
2. Consultar la orden y su timeline de fulfillment.
3. Reservar inventario de todos los ítems con vencimiento.
4. Confirmar o liberar una reserva sin duplicar efectos.
5. Autorizar y capturar un pago simulado.
6. Anular o reembolsar un pago cuando una compensación lo requiera.
7. Crear un envío sólo cuando se satisfacen sus precondiciones.
8. Recibir actualizaciones simuladas del envío de forma idempotente.
9. Coordinar el proceso completo y compensar fallos parciales.
10. Permitir a operaciones inspeccionar mensajes fallidos y ejecutar recuperaciones seguras.

## Requisitos funcionales propuestos

| ID | Requisito |
|---|---|
| FR-01 | Crear una orden devuelve el mismo resultado para la misma idempotency key y payload. |
| FR-02 | Reutilizar una idempotency key con otro payload devuelve conflicto sin nuevo efecto. |
| FR-03 | Cada transición de orden registra actor, instante, causa y correlation ID. |
| FR-04 | Inventory reserva atómicamente todas las unidades solicitadas o ninguna. |
| FR-05 | Una reserva tiene vencimiento explícito y puede confirmarse o liberarse una sola vez. |
| FR-06 | Payments modela autorización, captura, anulación y reembolso con referencias únicas. |
| FR-07 | Shipping no crea un envío hasta confirmar pago e inventario. |
| FR-08 | Fulfillment reacciona a resultados mediante un process manager durable. |
| FR-09 | Todo comando/evento reentregado se procesa idempotentemente. |
| FR-10 | Operaciones puede consultar fallos terminales y reintentar sólo acciones declaradas seguras. |
| FR-11 | Las APIs devuelven errores estables mediante Problem Details y un código de negocio. |
| FR-12 | El sistema expone salud, métricas y trazas correlacionables sin revelar datos sensibles. |

## Requisitos no funcionales propuestos

| ID | Objetivo verificable inicial |
|---|---|
| NFR-01 Confiabilidad | Ningún commit de negocio depende de que RabbitMQ esté disponible; el outbox conserva eventos pendientes. |
| NFR-02 Idempotencia | Repetir el mismo comando 10 veces produce un único efecto observable. |
| NFR-03 Consistencia | Cada agregado mantiene sus invariantes en una transacción PostgreSQL local. |
| NFR-04 Rendimiento | En entorno local de referencia, p95 menor a 300 ms para escrituras sin adapter externo y menor a 150 ms para lecturas simples; se medirá, no se promete hasta fijar hardware. |
| NFR-05 Recuperación | Reiniciar aplicación, RabbitMQ o Redis no pierde estados confirmados en PostgreSQL. |
| NFR-06 Operabilidad | Cada flujo tiene correlation ID, métricas de latencia/error y trazas entre adapters. |
| NFR-07 Seguridad | Secretos fuera de Git, bind local por defecto, mínimo privilegio y logs sin información sensible. |
| NFR-08 Mantenibilidad | Dependencias entre módulos se validan automáticamente; APIs públicas son pequeñas. |
| NFR-09 Portabilidad | La aplicación y dependencias se levantan localmente con Docker Compose, sin cuenta cloud. |
| NFR-10 Testabilidad | Tiempo, IDs y gateways externos son controlables mediante puertos en pruebas. |

Los SLO operativos definitivos requieren una carga y un hardware de referencia aprobados; inventarlos antes sería falsa precisión.

## Invariantes de negocio propuestas

- Una orden no cambia de moneda después de creada.
- El total es igual a la suma de líneas, ajustes e impuestos según una política versionada.
- Una transición de estado sólo ocurre desde estados permitidos y no retrocede implícitamente.
- El stock disponible nunca es negativo.
- La suma de reservas activas no supera el stock utilizable de un SKU/ubicación.
- Una reserva terminal no vuelve a estar activa.
- La captura acumulada nunca supera la autorización ni el total cobrable.
- Un reembolso acumulado nunca supera lo capturado.
- Una referencia externa de pago produce como máximo un efecto financiero interno.
- Un envío no se crea sin orden aceptada, inventario confirmado y pago capturado según la política aprobada.
- Un evento con el mismo `eventId` no produce el efecto de negocio dos veces.
- Los eventos publicados describen hechos confirmados, nunca intenciones no persistidas.

## Flujo feliz de referencia

1. Checkout solicita crear orden con idempotency key.
2. Ordering valida y persiste la orden; registra `OrderPlaced` en outbox.
3. Fulfillment inicia el proceso y solicita reserva.
4. Inventory reserva todo y publica `InventoryReserved`.
5. Fulfillment solicita autorización/captura según la política aprobada.
6. Payments confirma y publica el resultado.
7. Fulfillment confirma la reserva y solicita el envío.
8. Shipping crea el envío; Ordering proyecta el estado visible.

El orden exacto autorización/reserva y captura/confirmación es una decisión de producto todavía abierta.

## Flujos de error y recuperación

| Fallo | Respuesta inmediata | Recuperación esperada |
|---|---|---|
| Payload inválido | `400`, sin persistencia | Corregir solicitud; no reintentar automáticamente |
| Idempotency key con payload diferente | `409` | Cliente investiga/reemplaza la key |
| Orden/recurso inexistente | `404` sin filtrar datos ajenos | No reintentar salvo carrera conocida |
| Conflicto optimista de stock | `409` interno al proceso | Retry acotado con jitter; luego rechazo/compensación |
| Stock insuficiente | Resultado de negocio explícito | Cancelar orden o esperar stock según política futura |
| Proveedor de pago rechaza | Resultado terminal | Liberar reserva y marcar fallo de pago |
| Timeout de pago con resultado desconocido | No asumir rechazo | Consultar por referencia idempotente antes de compensar |
| RabbitMQ caído | Commit local continúa con outbox pendiente | Relay reintenta con backoff; alerta por edad del outbox |
| Consumidor falla transitoriamente | Nack/retry acotado | Dead-letter tras agotar intentos |
| Evento duplicado | Ack sin repetir efecto | Inbox conserva deduplicación y resultado |
| Redis caído | Degradar caché/rate limits según política | PostgreSQL permanece autoritativo; reconstruir al volver |
| Envío falla después del cobro | Estado de excepción visible | Retry seguro o intervención; no reembolsar ciegamente |

## Criterios de aceptación de producto

### AC-01 Creación idempotente

**WHEN** el mismo cliente envía dos veces una creación con igual idempotency key y payload  
**THEN** obtiene la misma orden y existe un único efecto persistido.

### AC-02 Conflicto de idempotencia

**WHEN** una key existente se reutiliza con otro payload  
**THEN** se responde conflicto y no cambia la orden original.

### AC-03 Reserva atómica

**WHEN** al menos una línea no tiene stock suficiente  
**THEN** ninguna línea de esa solicitud queda reservada.

### AC-04 Concurrencia de inventario

**WHEN** dos solicitudes compiten por la última unidad  
**THEN** sólo una confirma la reserva y el disponible nunca resulta negativo.

### AC-05 Pago ambiguo

**WHEN** el gateway agota el timeout después de recibir una operación  
**THEN** el sistema reconcilia por referencia antes de reintentar o compensar.

### AC-06 Caída del broker

**WHEN** RabbitMQ no está disponible al confirmar una transacción  
**THEN** el negocio queda confirmado con evento pendiente y se publica al recuperarse el broker.

### AC-07 Duplicado de evento

**WHEN** un consumidor recibe varias veces el mismo `eventId`  
**THEN** confirma las entregas sin duplicar el efecto de negocio.

### AC-08 Precondiciones de envío

**WHEN** falta pago capturado o inventario confirmado  
**THEN** no se crea un envío.

### AC-09 Auditabilidad

**WHEN** operaciones consulta una orden  
**THEN** puede reconstruir transiciones y correlaciones sin acceder a secretos.

### AC-10 Reinicio

**WHEN** la aplicación reinicia durante mensajes pendientes  
**THEN** continúa el procesamiento sin perder confirmaciones ni duplicar efectos.

## Fuera de alcance inicial

- Catálogo, pricing, promociones y checkout.
- Pagos o transportistas reales.
- Multi-región, multi-tenant y multi-moneda por orden.
- Optimización de rutas o warehouse management.
- Frontend productivo.
- Microservicios y despliegue cloud.
- Exactly-once distribuido.

## Preguntas que requieren aprobación humana

1. ¿Se reserva antes de autorizar pago o al revés, y qué costo de negocio se prioriza?
2. ¿La captura ocurre antes de preparar envío o al despachar?
3. ¿Una orden puede dividirse en varios envíos y ubicaciones?
4. ¿Qué política aplica cuando una reserva vence durante un pago ambiguo?
5. ¿Qué acciones puede ejecutar operaciones y cuáles requieren doble aprobación?
