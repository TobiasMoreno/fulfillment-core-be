# Arquitectura del monolito modular

- Estado: `PROPOSED`
- Decisión vinculada: [ADR-0001](../adr/0001-modular-monolith.md)
- Última revisión: 2026-10-08

## Contexto observado

Al iniciar esta fundación, `fulfillment-core-be` era un repositorio Git vacío. No existían restricciones de compatibilidad ni modelo previo que preservar. El frontend vive en un repositorio hermano y no forma parte de este cambio.

## Forma del sistema

Un único proceso Spring Boot y un único artefacto desplegable contienen módulos de negocio con límites verificables. Se usa una instancia PostgreSQL, pero cada contexto será dueño de su esquema/tablas. Compartir servidor no autoriza joins, entidades o repositorios entre módulos.

```text
HTTP / scheduler / messages
            |
            v
   adapters de entrada
            |
            v
   casos de uso / puertos  <---- contratos públicos entre módulos
            |
            v
      modelo de dominio
            ^
            |
 adapters de salida ------> PostgreSQL / RabbitMQ / Redis / HTTP simulado
```

La hexagonalidad se aplica en fronteras con volatilidad o efectos externos. No se crearán interfaces para cada clase ni capas vacías.

## Bounded contexts propuestos

| Contexto/módulo | Responsabilidad y datos propios | No posee |
|---|---|---|
| `ordering` | Orden comercial, líneas congeladas, totales, estado visible e idempotencia de creación | Stock, movimientos financieros o etiquetas de envío |
| `inventory` | Stock por SKU/ubicación, reservas, expiración y confirmación/liberación | Estado de orden o pago |
| `payments` | Intentos, autorización, captura, anulación, reembolso y referencias del proveedor simulado | Total recalculado de orden o stock |
| `shipping` | Solicitud de envío, bultos, tracking y estados del carrier simulado | Decidir si pago/stock son válidos |
| `fulfillment` | Process manager durable, pasos, deadlines y compensaciones del flujo | Reimplementar invariantes de otros contextos |
| `operations` | Vistas de diagnóstico y comandos operativos explícitamente autorizados | Mutar tablas de otros módulos directamente |
| `platform` | Outbox/inbox, transporte, observabilidad, seguridad técnica y reloj | Reglas de negocio; no es un bounded context |

`operations` puede posponerse hasta existir un flujo que operar. No se crea un contexto `shared`; el shared kernel se evalúa sólo cuando aparezca duplicación semántica real.

## Dependencias permitidas

Leyenda: `API` llamada sincrónica a contrato público; `EVENT` consumo de hechos; `—` no depende.

| Consumidor ↓ / Proveedor → | ordering | inventory | payments | shipping | fulfillment | platform |
|---|---|---|---|---|---|---|
| `ordering` | — | — | — | — | EVENT de proyección, si se aprueba | puertos técnicos |
| `inventory` | — | — | — | — | — | puertos técnicos |
| `payments` | — | — | — | — | — | puertos técnicos |
| `shipping` | — | — | — | — | — | puertos técnicos |
| `fulfillment` | API/EVENT | API/EVENT | API/EVENT | API/EVENT | — | puertos técnicos |
| `operations` | API de lectura | API de lectura | API de lectura | API de lectura | API de lectura | observabilidad |

Reglas:

- Un módulo publica tipos deliberados desde `<context>.api`; todo lo demás es interno.
- Ningún módulo importa `<otro>.domain`, `<otro>.adapter` o `<otro>.internal`.
- Llamadas síncronas sólo cuando el caller necesita una respuesta inmediata y ambos módulos comparten proceso.
- Eventos para propagar hechos y desacoplar efectos que toleran consistencia eventual.
- Fulfillment conoce contratos, no representaciones persistentes.
- Platform implementa puertos definidos hacia adentro; el dominio no importa platform.
- No hay ciclos. Si aparece uno, se corrigen responsabilidades en vez de agregar callbacks.

## Estructura de paquetes objetivo

La estructura se crea cuando cada especificación sea aprobada:

```text
com.fulfillmentcore
  ordering
    api
    application
      port.in
      port.out
    domain
    adapter.in.web
    adapter.out.persistence
  inventory
  payments
  shipping
  fulfillment
  operations
  platform
```

La raíz actual contiene únicamente `FulfillmentCoreApplication`; crear paquetes vacíos daría una falsa sensación de avance.

## Propiedad de datos

- Cada tabla tiene un único owner.
- Integridad dentro de un agregado usa constraints y transacción local.
- Referencias a otro contexto son IDs opacos, no foreign keys navegables por ORM.
- Un módulo no usa joins contra tablas ajenas en código operativo.
- Reporting cross-context se alimenta con eventos hacia proyecciones propias.
- Redis nunca contiene el único ejemplar de un hecho de negocio.
- Migraciones se nombrarán `V<timestamp>__<context>_<change>.sql` y no se editarán después de aplicar.

## Consistencia y coordinación

- ACID local por comando y agregado.
- Outbox escrito en la misma transacción que el cambio de negocio.
- Relay asíncrono publica con publisher confirms.
- Inbox o clave natural impide aplicar dos veces el mismo mensaje.
- Fulfillment mantiene un process manager explícito para pasos y compensaciones.
- No se promete exactly-once; se demuestra effectively-once para efectos controlados mediante idempotencia.

Ver [ADR-0003](../adr/0003-consistency-strategy.md).

## Concurrencia

La estrategia por agregado se decide con evidencia:

- optimistic locking para contención baja y conflictos recuperables;
- actualización condicional/locking pesimista acotado para la última unidad de stock;
- constraints únicos para idempotencia y referencias externas;
- nunca locks distribuidos de Redis para proteger la fuente de verdad PostgreSQL.

La decisión concreta de reservas requiere una prueba de concurrencia y aprobación funcional.

## Integración y contratos

- HTTP usa Problem Details, validación en el borde y DTOs independientes del dominio.
- Eventos usan envelope versionado; cambios incompatibles crean nueva versión/tipo.
- El puerto de publicación no expone clases RabbitMQ.
- Los adapters HTTP externos reciben timeout, retry y circuit breaker sólo con política documentada.
- Autenticación/autorización se diseña antes del primer endpoint funcional; Actuator local no implica política productiva.

## Observabilidad

- Métricas base: latencia/error HTTP, pools, JVM y outbox pendiente.
- Trazas OpenTelemetry: entrada, caso de uso, persistencia, publicación y consumo.
- Propagar `traceparent`, correlation y causation IDs sin convertirlos en IDs de negocio.
- Logs estructurados con campos estables y sin payloads sensibles.
- Prometheus, Grafana y Jaeger se agregan en el hito de observabilidad con dashboards/runbooks versionados.

## Enforcement planificado

Cuando exista el primer módulo se agregarán pruebas ArchUnit para:

1. dominio sin dependencias Spring/adapters;
2. acceso cross-context sólo a `.api`;
3. ausencia de ciclos;
4. adapters de salida implementando puertos;
5. clases web fuera del dominio.

Spring Modulith puede evaluarse entonces para verificación y documentación. No se incorpora aún porque no hay módulos que validar.

## Consecuencias

- Menor costo operativo y transacciones locales simples.
- Límites pueden erosionarse si sólo se documentan; de ahí las pruebas futuras.
- Un process manager suma estado, pero hace visibles recuperación y compensaciones.
- Extraer un módulo será posible, no gratuito; no es un objetivo inicial.

