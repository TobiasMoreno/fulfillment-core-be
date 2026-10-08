# Estrategia de pruebas

- Estado: `PROPOSED`
- Última revisión: 2026-10-08

## Objetivo

Obtener confianza proporcional al riesgo usando la prueba más barata que pueda detectar el fallo. La suite no debe depender de infraestructura compartida, orden de ejecución ni datos manuales.

## Capas

| Capa | Alcance | Herramientas | Ejecuta |
|---|---|---|---|
| Unit | Value objects, agregados, políticas y casos de uso aislados | JUnit, AssertJ, fakes escritos a mano | `mvnw.cmd test` |
| Arquitectura | Dependencias y ciclos de módulos | ArchUnit cuando exista el primer módulo | `mvnw.cmd test` |
| Integración | Persistencia, migraciones, Redis y RabbitMQ reales | Spring Boot Test, Testcontainers | `mvnw.cmd verify` |
| Adapter HTTP | Requests, respuestas, timeout y errores de proveedores | WireMock | `mvnw.cmd verify` |
| Contrato | OpenAPI y schemas de eventos compatibles | Validación de artefactos/versiones | CI/`verify` |
| End-to-end | Pocos recorridos críticos en Compose | HTTP real y dependencias locales | suite explícita futura |

## Fundación actual

- `FulfillmentCoreApplicationTest` valida el bootstrap mínimo sin requerir Docker.
- `LocalInfrastructureIT` inicia PostgreSQL y Redis efímeros, verifica conectividad y health.
- Surefire excluye `*IT`; Failsafe los ejecuta en `verify`.
- No se usa H2: sus diferencias de locking, tipos y SQL ocultarían defectos.

## Reglas por riesgo

### Dinero

- Tablas de casos para redondeo, moneda y límites.
- Propiedades: capturado ≤ autorizado; reembolsado ≤ capturado.
- Duplicados y timeout ambiguo con WireMock.
- Ningún test usa `double`.

### Inventario

- Concurrencia real contra PostgreSQL, no sólo mocks.
- Barreras/latches controlan carreras reproducibles.
- Propiedades: disponible ≥ 0; una reserva terminal no revive.
- Pruebas de expiración con `Clock`, sin sleeps.

### Mensajería

- RabbitMQ Testcontainer para routing, ack/nack, redelivery y DLQ.
- Publicación caída después del commit para probar outbox.
- Consumidor recibe el mismo evento varias veces y aplica un efecto.
- Poison message termina en DLQ con contexto suficiente y sin secretos.

### Process manager

- Cada transición y compensación tiene tests de tabla.
- Reinicio entre pasos mediante prueba de persistencia.
- Eventos fuera de orden y tardíos no corrompen el estado.

## Dobles de prueba

- Fake para puertos puros en unit tests.
- Testcontainers para tecnologías cuya semántica importa.
- WireMock sólo para fronteras HTTP.
- Mock únicamente para colaboración/interaction difícil de representar con un fake; no mockear el dominio.

## Datos y aislamiento

- Cada prueba crea sus datos; no depende de fixtures globales mutables.
- Rollback cuando sea válido, truncado explícito cuando se prueben commits/eventos.
- Imágenes Docker fijadas a versiones compatibles con Compose.
- Containers compartidos por clase/suite cuando reduzca tiempo sin filtrar estado.
- Hora, IDs y aleatoriedad son inyectables.

## Quality gates propuestos

Antes de merge:

1. compilación con Java 21;
2. tests unitarios verdes;
3. integration tests verdes con Docker;
4. `docker compose config --quiet`;
5. `git diff --check`;
6. migraciones desde base vacía;
7. revisión de dependencias y secretos;
8. escenarios de aceptación trazados a pruebas.

No se fija todavía un porcentaje global de cobertura. Se exigirá mutation testing o revisión de cobertura de ramas en dinero, stock, idempotencia y compensaciones cuando esos módulos existan.

## CI futuro

El pipeline reproducirá comandos locales, no una ruta exclusiva de CI. Orden previsto: build rápido, integración, análisis estático, scan de dependencias/secretos y empaquetado. No se publicará ni desplegará desde esta fase.

## Criterios de aceptación de la estrategia

- **WHEN** Docker no está disponible y se ejecuta `test` **THEN** la suite rápida sigue siendo útil.
- **WHEN** Docker está disponible y se ejecuta `verify` **THEN** PostgreSQL y Redis reales son ejercitados.
- **WHEN** se agrega un adapter HTTP **THEN** sus contratos y fallos se prueban con WireMock.
- **WHEN** se agrega RabbitMQ **THEN** redelivery, idempotencia y DLQ se prueban con container real.
- **WHEN** una prueba no corre **THEN** el reporte declara el motivo y el riesgo residual.
