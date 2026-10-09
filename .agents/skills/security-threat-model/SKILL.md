---
name: security-threat-model
description: Modela amenazas de cambios en Fulfillment Core antes de aprobar endpoints, autenticación/autorización, datos sensibles, integraciones HTTP, mensajería o nuevos límites de confianza. No usar para revisiones rutinarias de estilo.
---

# Modelado de amenazas

Produce un análisis defendible y accionable sin convertir una checklist genérica en arquitectura.

## Preparación

1. Lee `AGENTS.md`, la especificación, los ADR aplicables y sólo el código/configuración dentro del alcance.
2. Separa hechos observados, diseño propuesto y supuestos. No inventes controles, actores ni requisitos regulatorios.
3. Identifica el cambio, los activos que protege y qué decisión humana debe habilitar.

## Análisis

Modela únicamente los flujos afectados:

- actores humanos, procesos, almacenamientos y sistemas externos;
- límites de confianza y cambios de identidad o privilegio;
- datos entrantes, persistidos, publicados y registrados;
- estados degradados, reintentos, recuperación y operación manual.

Usa STRIDE como ayuda de cobertura, no como salida mecánica. Prioriza amenazas plausibles para este sistema:

- bypass de autorización, escalamiento horizontal/vertical y enumeración de recursos;
- inyección, deserialización insegura, SSRF y manipulación de parámetros;
- exposición de PII, tokens, secretos, payloads o detalles internos en logs y errores;
- replay de webhooks/eventos, abuso de idempotency keys y mensajes falsificados;
- duplicación, reordenamiento, poison messages y agotamiento de DLQ/outbox;
- carreras de stock, timeout ambiguo de pagos y compensaciones abusables;
- abuso de endpoints operativos, Actuator, métricas o configuración local;
- denegación de servicio por payloads, cardinalidad, locks, reintentos o recursos sin límite.

Para cada amenaza registra:

1. escenario y precondiciones;
2. activo e impacto;
3. probabilidad e impacto como `LOW`, `MEDIUM`, `HIGH` o `CRITICAL`;
4. controles preventivos, detectivos y de recuperación;
5. prueba o evidencia necesaria;
6. riesgo residual y decisión pendiente.

No eleves severidad sin una ruta de ataque concreta. No declares "mitigado" sin evidencia verificable.

## Resultado

Entrega, en este orden:

1. alcance, supuestos y límites de confianza;
2. diagrama pequeño sólo si aclara tres o más relaciones;
3. tabla de amenazas ordenada por severidad;
4. controles y pruebas recomendadas;
5. riesgos aceptados o abiertos;
6. decisiones que requieren aprobación humana.

El análisis es report-first. No cambies autenticación, contratos, esquemas, infraestructura ni secretos salvo que la persona lo solicite expresamente y exista la aprobación arquitectónica exigida por `AGENTS.md`.
