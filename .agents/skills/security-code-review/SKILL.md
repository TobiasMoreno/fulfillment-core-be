---
name: security-code-review
description: Revisa cambios de Fulfillment Core buscando vulnerabilidades explotables y controles faltantes. Úsala cuando el diff afecta entradas HTTP, autorización, persistencia, mensajería, integraciones externas, secretos o infraestructura; no reemplaza una revisión funcional general.
---

# Revisión de seguridad

Realiza una revisión basada en evidencia y reporta antes de modificar.

## Alcance

1. Lee `AGENTS.md`, especificaciones y ADR relevantes.
2. Determina el diff solicitado. Si no se indica base, revisa el working tree contra `HEAD`, incluyendo archivos no trackeados.
3. Inspecciona callers, configuración y pruebas suficientes para entender si una ruta es alcanzable.
4. No subas código, payloads ni secretos a scanners externos.

## Orden de revisión

Busca primero impactos de mayor severidad:

1. autenticación, autorización por objeto/tenant y separación de roles;
2. validación de bordes, SQL/SpEL/command injection, path traversal, SSRF y deserialización;
3. secretos, PII, datos de pago, logs, errores y respuestas HTTP;
4. CORS/CSRF, headers confiables, límites de payload y endpoints Actuator;
5. queries, migraciones, privilegios de base y aislamiento entre bounded contexts;
6. idempotencia, replay, eventos falsificados, redelivery, DLQ y timeout ambiguo;
7. configuración Docker, puertos publicados, usuarios, imágenes y permisos de archivos;
8. degradación segura, rate limits, timeouts, circuit breaking y agotamiento de recursos.

No marques como vulnerabilidad una posibilidad abstracta. Cada finding debe incluir:

- severidad `CRITICAL`, `HIGH`, `MEDIUM` o `LOW`;
- archivo y línea o símbolo;
- ruta de ataque y precondiciones;
- impacto técnico o de negocio;
- corrección mínima sugerida;
- prueba de regresión o verificación esperada.

Clasifica como crítico sólo cuando exista impacto severo y una ruta razonablemente explotable, por ejemplo ejecución remota, bypass de autenticación generalizado o exposición directa de secretos reales.

## Verificación

Ejecuta sólo comprobaciones locales y proporcionales al cambio. Prefiere las órdenes documentadas en el repositorio. Si una comprobación depende de Docker, red o una herramienta ausente, informa la omisión y su impacto; nunca supongas que pasó.

## Resultado

Presenta findings primero, ordenados por severidad. Después incluye:

- preguntas o supuestos que cambien la conclusión;
- verificaciones ejecutadas y omitidas;
- riesgo residual.

Si no encuentras vulnerabilidades, dilo explícitamente sin afirmar seguridad absoluta e identifica las superficies no verificadas. No apliques correcciones salvo solicitud explícita; cambios de autenticación, autorización, contratos, esquemas o infraestructura conservan el gate humano de `AGENTS.md`.
