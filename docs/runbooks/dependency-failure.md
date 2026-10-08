# Runbook: dependencia degradada o mensajería detenida

- Estado: `PROPOSED`; se activa por etapas al incorporar componentes
- Última revisión: 2026-10-08

## Principios

1. No reiniciar repetidamente la aplicación ante una dependencia compartida caída.
2. No editar tablas, outbox, inbox o DLQ manualmente sin procedimiento aprobado.
3. Conservar correlation IDs, timestamps y evidencia.
4. Determinar si el resultado externo es conocido antes de reintentar dinero/envíos.
5. Redis es reconstruible; PostgreSQL no.

## Triage común

```powershell
docker compose ps
docker compose logs --tail 200
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/livez
Invoke-RestMethod http://localhost:8080/readyz
```

Clasificar:

- infraestructura no inició;
- credenciales/configuración inválidas;
- capacidad agotada;
- timeout o red;
- poison message/dato inválido;
- bug de aplicación.

## PostgreSQL

Impacto esperado: readiness `DOWN`; escrituras/lecturas autoritativas no disponibles.

```powershell
docker compose logs postgres --tail 200
docker compose exec postgres pg_isready -U fulfillment_app -d fulfillment
```

Acciones seguras:

- verificar salud y espacio;
- restaurar el contenedor conservando volumen;
- comprobar migraciones y pool al volver;
- ejecutar smoke/readiness.

No hacer:

- borrar volumen para “arreglar” un incidente;
- cambiar checksum de migraciones aplicadas;
- conmutar a Redis como fuente de verdad.

## Redis

Impacto esperado: readiness `DOWN` mientras se lo considere requerido. Los datos de negocio confirmados permanecen en PostgreSQL.

```powershell
docker compose logs redis --tail 200
docker compose exec redis redis-cli ping
```

Después de recuperar, invalidar/reconstruir datos derivados. No dar por válido contenido previo sin conocer su estrategia de persistencia y TTL.

## RabbitMQ (hito M2)

Impacto esperado: APIs pueden confirmar transacciones locales; crecen filas outbox pendientes. Consumidores dejan de avanzar.

Señales:

- edad/cantidad de outbox;
- conexiones/canales;
- publish confirms fallidos;
- profundidad de colas y DLQ;
- procesos Fulfillment vencidos.

Recuperación:

1. restaurar broker/topología declarativa;
2. verificar bindings y consumers antes de liberar backlog;
3. observar tasa de publish/consume, duplicados y errores;
4. limitar consumo si el downstream sigue degradado;
5. reconciliar process managers vencidos.

No republicar manualmente un mensaje sin conservar `eventId`, versión, correlation y causation.

## DLQ/parking

Antes de reintentar un mensaje:

- identificar error y consumer/version;
- determinar si es transitorio o poison message;
- verificar que el handler sea idempotente;
- corregir causa raíz/desplegar fix;
- seleccionar un lote acotado;
- monitorear efectos y detener ante repetición.

Nunca cambiar payload en caliente sin preservar original, auditoría y aprobación.

## Pago o envío ambiguo

Si hubo timeout, no asumir que el proveedor rechazó. Consultar mediante la referencia idempotente. Sólo después:

- registrar el resultado reconciliado;
- completar el paso o ejecutar compensación aprobada;
- conservar evidencia del request/reference, nunca secretos.

## Escalamiento

Escalar a una persona responsable cuando:

- la acción puede duplicar cobro, reembolso o envío;
- hay que alterar estado persistido manualmente;
- el backlog excede el objetivo aún no definido;
- no existe transición/compensación aprobada;
- existe posible exposición de datos.

## Cierre

- health/readiness recuperados;
- backlog y DLQ bajo control;
- invariantes verificadas por consultas/herramientas aprobadas;
- timeline y acciones documentadas;
- test de regresión y actualización del runbook creados;
- secretos y datos sensibles eliminados de la evidencia.
