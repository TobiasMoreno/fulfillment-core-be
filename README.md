# Fulfillment Core

Plataforma educativa para diseñar, implementar y operar el backend de fulfillment de un marketplace con estándares profesionales de backend. Este repositorio contiene el monolito modular; la lógica de órdenes, inventario, pagos y envíos todavía no está implementada.

## Estado

La fundación técnica y el proceso de Specification-Driven Development (SDD) están preparados para revisión humana. Ningún módulo funcional puede comenzar hasta que sus especificaciones y decisiones arquitectónicas estén marcadas como `APPROVED`.

## Stack base

- Java 21.
- Spring Boot 4.1.1 y Maven.
- PostgreSQL 17 y Redis 7 para desarrollo local.
- Flyway para migraciones futuras.
- Actuator y métricas Prometheus.
- JUnit y Testcontainers.
- Docker Compose sin servicios cloud.

RabbitMQ, WireMock, OpenTelemetry, Grafana y Jaeger se incorporarán en los hitos que realmente los utilicen. La decisión y los límites están documentados; no se agregan dependencias inactivas.

## Requisitos

- JDK 21 en `PATH`.
- Docker Desktop con Docker Compose v2.
- PowerShell 7 o Windows PowerShell 5.1.
- Maven 3.9+ sólo para generar inicialmente el wrapper; luego se usa `mvnw.cmd`.

## Inicio rápido en Windows/PowerShell

```powershell
Set-Location 'C:\ruta\a\fulfillment-core-be'
Copy-Item .env.example .env
docker compose up -d --wait
docker compose ps
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/livez
Invoke-RestMethod http://localhost:8080/readyz
Invoke-WebRequest http://localhost:8080/actuator/prometheus
```

La aplicación escucha sólo en `127.0.0.1` por defecto. Los detalles de salud no se exponen públicamente.

## Validaciones

Pruebas rápidas, sin Docker:

```powershell
.\mvnw.cmd test
```

Build completo y pruebas de integración con PostgreSQL y Redis efímeros:

```powershell
.\mvnw.cmd verify
```

Validación de Compose:

```powershell
docker compose config --quiet
```

Detener el entorno conservando datos:

```powershell
docker compose down
```

Eliminar también los volúmenes locales es destructivo y debe ser una decisión explícita:

```powershell
docker compose down --volumes
```

## Configuración local

| Variable | Default local | Propósito |
|---|---:|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fulfillment` | JDBC de PostgreSQL |
| `DB_USERNAME` | `fulfillment_app` | Usuario de aplicación local |
| `DB_PASSWORD` | `local-development-only` | Secreto descartable local |
| `REDIS_HOST` | `localhost` | Host de Redis |
| `REDIS_PORT` | `6379` | Puerto de Redis |
| `SERVER_ADDRESS` | `127.0.0.1` | Interfaz de escucha |
| `SERVER_PORT` | `8080` | Puerto HTTP |

`.env` está ignorado por Git. `.env.example` contiene únicamente valores descartables; nunca se deben copiar secretos reales al repositorio.

## Documentación

- [Visión de producto](docs/product/product-vision.md)
- [Arquitectura del monolito modular](docs/architecture/modular-monolith.md)
- [Proceso SDD y Spec Kit](docs/architecture/spec-driven-development.md)
- [Estrategia de pruebas](docs/architecture/testing-strategy.md)
- [Plan incremental](docs/architecture/implementation-plan.md)
- [Registro de ADRs](docs/adr/README.md)
- [Runbook de desarrollo local](docs/runbooks/local-development.md)
- [Runbook de dependencias degradadas](docs/runbooks/dependency-failure.md)

## Regla de avance

Los documentos actuales son propuestas (`PROPOSED`). Para iniciar un módulo funcional, una persona responsable debe:

1. resolver las preguntas abiertas;
2. aprobar especificación funcional y plan técnico;
3. registrar decisiones relevantes mediante ADR;
4. cambiar explícitamente su estado a `APPROVED`;
5. revisar el diff generado por cualquier agente antes del merge.

Consultar [AGENTS.md](AGENTS.md) para las reglas completas de contribución asistida por IA.
