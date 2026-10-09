# AGENTS.md

## Misión y alcance

Fulfillment Core demuestra competencias senior de backend mediante un sistema ejecutable localmente y explicable. La simplicidad evolutiva tiene prioridad sobre incorporar tecnología por moda.

Este archivo aplica a todo el repositorio.

## Autoridad y gates

Orden de autoridad:

1. instrucciones humanas explícitas;
2. especificación funcional con estado `APPROVED`;
3. ADR aceptado y plan técnico aprobado;
4. código y pruebas existentes;
5. propuesta del agente.

`PROPOSED` significa discutible y no autoriza implementación funcional. Sólo una persona puede cambiar una especificación o ADR a `APPROVED`. Un agente nunca se autoaprueba ni interpreta silencio como aprobación.

Hasta esa aprobación está permitido trabajar únicamente en fundación, tooling, documentación y pruebas de esa fundación. No crear agregados, endpoints ni flujos de órdenes, inventario, pagos o envíos.

## Skills obligatorias

- Usar `$security-threat-model` antes de proponer o implementar autenticación/autorización, endpoints externos, datos sensibles, integraciones HTTP/mensajería o un nuevo límite de confianza.
- Usar `$security-code-review` al revisar cambios que afecten entradas HTTP, autorización, persistencia, mensajería, integraciones externas, secretos o infraestructura.
- Usar `$security-dependency-audit` al agregar o actualizar dependencias, plugins, Maven Wrapper o imágenes, y antes de preparar una release.
- Estas skills producen evidencia y recomendaciones; no sustituyen los gates de aprobación humana ni autorizan cambios fuera de alcance.

## Flujo SDD obligatorio

Para cada capacidad:

1. inspeccionar código, ADRs y especificaciones vigentes;
2. redactar objetivo, actores, escenarios WHEN/THEN, invariantes y fuera de alcance;
3. resolver ambigüedades con una persona responsable;
4. diseñar contratos, datos, fallos, observabilidad y estrategia de pruebas;
5. obtener aprobación humana explícita;
6. dividir en tareas pequeñas y verificables;
7. implementar una tarea por vez;
8. ejecutar verificaciones proporcionales al riesgo;
9. actualizar documentación y evidencia.

Todo cambio de alcance, contrato público, consistencia, seguridad o dependencia entre módulos vuelve al gate humano.

## Arquitectura y dependencias

- Organizar por bounded context bajo `com.fulfillmentcore.<context>`; no por capas globales.
- Dentro de un contexto usar `domain`, `application`, `adapter.in` y `adapter.out` cuando aporten separación real.
- El dominio no depende de Spring, JPA, RabbitMQ, HTTP ni Redis.
- Los adapters dependen hacia los puertos; los casos de uso coordinan, no contienen detalles de transporte.
- Entre contextos sólo se permiten APIs públicas de aplicación o eventos versionados.
- Prohibido acceder al repositorio, tablas, entidades o paquetes `internal` de otro contexto.
- No compartir entidades JPA. El shared kernel debe ser mínimo, estable y aprobado.
- PostgreSQL es fuente de verdad. Redis almacena sólo datos derivados, efímeros o reconstruibles.
- No introducir microservicios. Una extracción futura exige evidencia operativa y un ADR nuevo.
- No crear abstracciones anticipadas salvo el puerto de mensajería requerido para comparar RabbitMQ con SQS/SNS.

## Convenciones Java

- Java 21, cuatro espacios, UTF-8 y una clase pública por archivo.
- Nombres de negocio en inglés y consistentes con el lenguaje ubicuo aprobado.
- Inyección por constructor; evitar field injection.
- Preferir objetos inmutables y records para valores/DTOs cuando corresponda.
- Evitar `null` en contratos; expresar ausencia de forma explícita.
- No usar Lombok inicialmente: el ahorro no compensa ocultar semántica en un proyecto educativo.
- No exponer entidades persistentes desde HTTP o eventos.
- Dinero siempre incluye moneda y usa precisión decimal; nunca `double`.
- Tiempo de negocio mediante `Clock` inyectable y timestamps UTC.
- IDs e idempotency keys se validan en el borde.
- Excepciones no controlan flujos normales y nunca filtran secretos o detalles internos.
- Migraciones Flyway son forward-only; una migración aplicada no se reescribe.

## Contratos y errores

- APIs HTTP versionadas y problem details consistentes.
- Las mutaciones externas requieren idempotency key cuando puedan reintentarse.
- Eventos llevan `eventId`, `eventType`, `eventVersion`, `occurredAt`, `correlationId` y `causationId`.
- Consumidores idempotentes; no se asume exactly-once.
- Timeouts, reintentos y backoff deben ser explícitos. No reintentar errores de validación.
- Logs estructurados con correlation ID; nunca PAN, tokens, passwords ni PII innecesaria.

## Calidad y pruebas

Pirámide requerida:

- unitarias rápidas para invariantes y casos de uso;
- integración con PostgreSQL/Redis/RabbitMQ reales mediante Testcontainers;
- WireMock para adapters HTTP salientes y sus fallos;
- pruebas de contrato para APIs/eventos públicos;
- pocas pruebas end-to-end para recorridos críticos.

Reglas:

- No mockear value objects ni agregados.
- No usar H2 como sustituto de PostgreSQL.
- Tests deterministas, independientes y sin depender del orden.
- Cada bug corregido agrega una prueba de regresión.
- Los tests `*Test` corren con `mvnw.cmd test`; los `*IT` también corren con `mvnw.cmd verify`.
- Un cambio de arquitectura debe agregar o ajustar reglas automatizadas con ArchUnit cuando existan módulos.
- Cobertura es señal, no objetivo; los caminos de dinero, stock e idempotencia requieren cobertura de ramas significativa.

Antes de entregar:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
docker compose config --quiet
git diff --check
```

Si una validación no puede ejecutarse, informar comando, motivo e impacto. Nunca afirmar que pasó.

## Seguridad y secretos

- Aplicar mínimo privilegio a usuarios, endpoints y datos.
- No commitear `.env`, credenciales, tokens, dumps ni datos personales.
- Los valores por defecto son sólo locales y deben estar enlazados a loopback.
- Dependencias nuevas requieren necesidad concreta, revisión de licencia y vulnerabilidades.
- No habilitar Actuator sensible, stack traces ni detalles de health públicamente.
- No ejecutar comandos destructivos, publicar artefactos o contactar sistemas externos sin autorización explícita.

## Revisión de trabajo generado por agentes

Cada entrega asistida por IA debe declarar:

- objetivo y alcance;
- archivos modificados;
- supuestos y decisiones tomadas;
- pruebas ejecutadas con resultado;
- pruebas omitidas y motivo;
- riesgos y preguntas abiertas;
- evidencia de que no amplió el alcance.

Revisión humana obligatoria para:

- estados e invariantes de dominio;
- contratos HTTP y eventos;
- esquemas y migraciones;
- autenticación/autorización;
- consistencia, reintentos y compensaciones;
- dependencias nuevas;
- cambios en infraestructura y observabilidad.

Un agente debe preservar cambios ajenos, mantener el diff pequeño y detenerse ante contradicciones de especificación. No debe modificar una prueba para ocultar una regresión ni reducir controles de seguridad para hacer pasar el build.
