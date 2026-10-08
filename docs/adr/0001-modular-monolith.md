# ADR-0001: comenzar con un monolito modular

- Estado: `ACCEPTED` por la restricción humana inicial
- Fecha: 2026-10-08

## Contexto

El producto debe demostrar diseño distribuido y confiabilidad sin pagar el costo accidental de operar microservicios. El equipo/producto es inicial, los límites todavía se validarán y toda la plataforma debe ejecutarse localmente.

## Drivers

- Iteración y debugging sencillos.
- Transacciones locales para invariantes fuertes.
- Límites de dominio visibles y comprobables.
- Bajo costo operativo.
- Camino de evolución sin prometer extracción gratuita.

## Opciones

### A. Monolito modular — elegida

Un proceso y artefacto, paquetes por bounded context, APIs/eventos internos explícitos, tablas con ownership y reglas automatizadas.

### B. Microservicios desde el inicio — rechazada

Aísla despliegues y datos, pero introduce red, discovery, despliegue, observabilidad distribuida y consistencia eventual antes de conocer los límites. Reduciría la señal educativa de negocio en favor de plumbing.

### C. Monolito por capas globales — rechazada

Es simple al principio, pero agrupa controllers/services/repositories de todos los dominios, facilita dependencias cruzadas y hace costosa cualquier evolución.

## Decisión

Construir un monolito modular descrito en `docs/architecture/modular-monolith.md`. Los límites son lógicos y automatizados; no se simula una red dentro del proceso.

## Consecuencias

Positivas:

- despliegue, pruebas y operación locales simples;
- ACID disponible dentro del módulo/agregado;
- refactors de límites más baratos durante aprendizaje;
- trazas completas en un proceso.

Negativas:

- una fuga de encapsulación puede acoplar módulos silenciosamente;
- escala y releases son del artefacto completo;
- aislar fallos requiere disciplina aunque comparta JVM.

Mitigaciones:

- APIs públicas pequeñas;
- ownership de tablas;
- ArchUnit desde el primer módulo;
- métricas por módulo;
- no acceder a repositorios ajenos.

## Señales para reconsiderar

- necesidad comprobada de escalar/operar un módulo de manera independiente;
- ritmos de despliegue incompatibles entre equipos autónomos;
- aislamiento regulatorio o de fallo que el proceso único no puede ofrecer;
- límites estables demostrados por uso, no sólo diagramas.

