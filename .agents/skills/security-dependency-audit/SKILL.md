---
name: security-dependency-audit
description: Audita dependencias Maven, plugins, Maven Wrapper e imágenes Docker de Fulfillment Core por vulnerabilidades, procedencia, licencias y versiones riesgosas. Úsala al agregar o actualizar una dependencia/imagen y antes de una release; no actualiza artefactos automáticamente.
---

# Auditoría de dependencias y supply chain

Genera un inventario reproducible y recomendaciones compatibles con Spring Boot antes de proponer upgrades.

## Inventario

Inspecciona, según el alcance:

- dependencias directas y transitivas del `pom.xml` y BOM efectivo;
- plugins Maven y Maven Wrapper;
- imágenes y tags de `compose.yaml` o Dockerfiles;
- repositorios de paquetes configurados, scripts descargables y acciones CI;
- archivos de entorno para detectar secretos trackeados, sin imprimir sus valores.

Usa el Maven Wrapper del repositorio. `dependency:tree` y `help:effective-pom` son evidencia útil cuando correspondan; no agregues un plugin de auditoría sólo para poder auditar.

## Evaluación

Para cada hallazgo determina:

1. coordenada o imagen y versión realmente resuelta;
2. si la dependencia es directa, transitiva, de test, runtime o build;
3. vulnerabilidad/advisory y rango afectado;
4. alcanzabilidad razonable en este proyecto;
5. severidad publicada y severidad contextual;
6. versión corregida y compatibilidad con el BOM de Spring Boot;
7. mitigación temporal si no existe upgrade seguro.

Cuando consultes vulnerabilidades actuales, usa fuentes autoritativas como el advisory del proveedor, CVE/NVD o GitHub Advisory Database y enlaza la evidencia. No concluyas a partir del número de versión recordado por el modelo.

También revisa:

- tags flotantes, imágenes sin mantenimiento y ejecución como root;
- dependencias duplicadas o versiones que eluden el BOM;
- repositorios HTTP/no confiables y downloads sin integridad;
- licencias incompatibles o desconocidas;
- secretos reales incluidos en archivos versionados o historial visible.

Nunca muestres un secreto completo: reporta archivo, tipo y acción de rotación. Un valor local deliberadamente falso no es una credencial comprometida.

## Resultado

Entrega:

1. alcance, fecha y comandos/fuentes usados;
2. inventario resumido;
3. findings ordenados por severidad con evidencia y alcanzabilidad;
4. actualización mínima recomendada y posibles incompatibilidades;
5. verificaciones necesarias después del cambio;
6. herramientas o superficies no disponibles.

La auditoría es report-first. No actualices dependencias, imágenes, wrapper ni lockfiles sin solicitud explícita. Nunca ejecutes `dependency:purge-local-repository`, borres cachés ni publiques reportes externamente como parte de la auditoría.
