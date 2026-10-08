# Runbook: desarrollo local

- Estado: `ACTIVE` para la fundación
- Última revisión: 2026-10-08

## Propósito

Levantar y verificar la fundación en Windows/PowerShell sin servicios pagos.

## Preflight

```powershell
java -version
.\mvnw.cmd --version
docker version
docker compose version
```

Esperado: Java 21, Maven 3.9+ provisto por el wrapper y Docker operativo.

## Configurar

```powershell
Set-Location 'C:\ruta\a\fulfillment-core-be'
Copy-Item .env.example .env
```

`.env` contiene credenciales descartables locales y no debe commitearse. Si los puertos `5432` o `6379` están ocupados, cambiar `POSTGRES_PORT`/`REDIS_PORT` en `.env` y alinear `DB_URL`/`REDIS_PORT` de la aplicación.

## Levantar dependencias

```powershell
docker compose pull
docker compose up -d --wait
docker compose ps
```

Ambos servicios deben figurar `healthy`. Diagnóstico:

```powershell
docker compose logs postgres --tail 100
docker compose logs redis --tail 100
docker compose exec postgres pg_isready -U fulfillment_app -d fulfillment
docker compose exec redis redis-cli ping
```

## Ejecutar aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación debe iniciar Flyway, conectar PostgreSQL/Redis y escuchar en `127.0.0.1:8080`.

## Smoke checks

En otra terminal:

```powershell
$health = Invoke-RestMethod http://localhost:8080/actuator/health
$live = Invoke-RestMethod http://localhost:8080/livez
$ready = Invoke-RestMethod http://localhost:8080/readyz
$health
$live
$ready
```

Esperado: `status` igual a `UP`. `/readyz` incluye la aptitud para recibir tráfico y depende de PostgreSQL/Redis en esta fase; `/livez` no depende de servicios externos.

Métricas:

```powershell
(Invoke-WebRequest http://localhost:8080/actuator/prometheus).StatusCode
```

Esperado: HTTP `200`.

## Pruebas

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

`verify` necesita Docker porque crea PostgreSQL/Redis aislados con Testcontainers. No necesita los servicios de Compose levantados.

## Detener

```powershell
docker compose down
```

Los volúmenes se conservan. Para un reset explícito y destructivo:

```powershell
docker compose down --volumes
```

Antes de borrar, confirmar que el proyecto mostrado por `docker compose ls` sea `fulfillment-core`.

## Problemas frecuentes

| Síntoma | Diagnóstico | Acción |
|---|---|---|
| Puerto ocupado | `Get-NetTCPConnection -LocalPort 5432,6379,8080` | Cambiar puertos o detener el proceso correcto |
| Docker no responde | `docker version` | Iniciar Docker Desktop |
| Java incorrecto | `java -version` | Seleccionar JDK 21; no desactivar Enforcer |
| PostgreSQL unhealthy | logs + `pg_isready` | Revisar `.env`, volumen y puerto |
| Redis unhealthy | logs + `redis-cli ping` | Revisar comando, volumen y puerto |
| `readyz` DOWN | `actuator/health` + logs | Restaurar dependencia; no reiniciar en loop |
| Testcontainers falla | `docker info` | Verificar daemon, permisos e imágenes |

## Evidencia para reportar

- versiones de Java/Maven/Docker;
- salida de `docker compose ps`;
- resultados de `test` y `verify`;
- códigos de health checks;
- logs relevantes sin secretos.
