# Plan incremental de implementación

- Estado: `PROPOSED`
- Última revisión: 2026-10-08
- Regla: cada hito funcional necesita especificación `APPROVED`

## Definition of Ready para un módulo funcional

- Owner humano identificado.
- Objetivo, actores, fuera de alcance y escenarios WHEN/THEN aprobados.
- Invariantes y estados acordados.
- Contratos HTTP/eventos y política de idempotencia definidos.
- Modelo de datos y ownership revisados.
- Fallos, timeouts, reintentos y compensaciones decididos.
- Riesgos de seguridad/privacidad evaluados.
- ADRs relevantes aceptados.
- Plan de pruebas trazado a aceptación.

Sin todos estos puntos el módulo permanece en discovery.

## M0 — Fundación y SDD

Entregables actuales:

- visión de producto propuesta;
- bounded contexts y reglas de dependencias;
- ADRs de arquitectura, mensajería y consistencia;
- AGENTS.md y proceso Spec Kit;
- Spring Boot/Actuator;
- PostgreSQL y Redis en Compose;
- pruebas base con Testcontainers;
- runbooks locales.

Verificación:

- `mvnw.cmd test`;
- `mvnw.cmd verify` con Docker;
- `docker compose config --quiet`;
- endpoints health/liveness/readiness.

Gate de salida: revisión humana de todos los documentos `PROPOSED`. Este hito no cambia su estado por terminar el código.

## M1 — Plataforma de persistencia y contratos técnicos

Alcance:

- convención de schemas y migraciones;
- Problem Details y correlation IDs;
- puertos técnicos mínimos;
- primer test ArchUnit;
- baseline de seguridad para endpoints.

No incluye entidades de negocio.

Gate: ADRs y modelo de errores aprobados.

## M2 — Mensajería confiable y observabilidad

Alcance:

- RabbitMQ en Compose;
- puerto `EventPublisher` y adapter AMQP;
- outbox relay, inbox/deduplicación y DLQ;
- OpenTelemetry Collector, Prometheus, Grafana y Jaeger;
- dashboards/alertas locales y runbooks;
- Testcontainers para broker y fallos.

Gate: ADR-0002/0003 aceptados y contratos de envelope aprobados.

## M3 — Ordering

Alcance después de spec aprobada:

- agregado y estados de orden;
- creación idempotente y consulta;
- total congelado y timeline;
- outbox `OrderPlaced`;
- API/contrato y pruebas.

Demostración: duplicados, conflicto de key, transición inválida y restart.

## M4 — Inventory

Alcance después de spec aprobada:

- stock y reservas;
- estrategia de concurrencia medida;
- reserva atómica, expiración, confirmación/liberación;
- eventos y pruebas de carrera.

Demostración: competencia por última unidad y recuperación post-reinicio.

## M5 — Payments simulado

Alcance después de spec aprobada:

- modelo de intentos y movimientos;
- gateway HTTP simulado mediante puerto;
- autorización/captura/anulación/reembolso;
- reconciliación de timeout ambiguo;
- WireMock y propiedades monetarias.

Demostración: rechazo, timeout luego de aceptar, reintento y duplicado.

## M6 — Shipping simulado

Alcance después de spec aprobada:

- solicitud, tracking y estados;
- adapter de carrier simulado;
- webhook/eventos idempotentes;
- WireMock para errores del proveedor.

Demostración: update duplicado/fuera de orden y fallo transitorio.

## M7 — Fulfillment process manager

Alcance después de decisión del orden de pasos:

- máquina de estados durable;
- deadlines, retries y compensaciones;
- integración de módulos sólo mediante APIs/eventos públicos;
- pruebas end-to-end de happy path y fallos parciales.

Demostración: broker caído, pago ambiguo, stock perdido, shipping fallido y recuperación manual.

## M8 — Operaciones y hardening

Alcance:

- vistas operativas y acciones privilegiadas;
- RBAC y auditoría;
- performance baseline y fault injection;
- dependency/secret scanning;
- documentación de recuperación y demo reproducible.

## Definition of Done por hito

- Criterios de aceptación vinculados a tests verdes.
- Código respeta límites de módulos automatizados.
- Migraciones probadas desde base vacía.
- Errores, métricas, logs y trazas útiles para el flujo.
- Amenazas y secretos revisados.
- Runbook actualizado.
- `test`, `verify`, Compose y lint/checks verdes.
- Sin warnings nuevos ignorados.
- Evidencia de comandos adjunta al PR.
- Documentos y ADRs reflejan el resultado real.
- Revisión humana completada; el agente no se autoaprueba.

## Estrategia de commits y revisión

Cada hito se divide en vertical slices demostrables. Un cambio de infraestructura puede preceder a negocio sólo si tiene consumidor/test concreto. Los PRs separan cambios mecánicos de semántica para facilitar revisión.
