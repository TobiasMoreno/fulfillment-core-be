# Proceso SDD e integración con GitHub Spec Kit

- Estado: `PROPOSED`
- Última revisión: 2026-10-08

## Principio

La especificación es el contrato de intención; el código es una implementación verificable de ese contrato. Spec Kit ayuda a producir y relacionar artefactos, pero no reemplaza decisiones de producto, ADRs ni aprobación humana.

## Flujo por feature

```text
idea
  -> spec funcional PROPOSED
  -> preguntas/decisiones humanas
  -> spec APPROVED
  -> plan técnico + ADRs
  -> aprobación humana
  -> tareas verificables
  -> implementación
  -> análisis de consistencia
  -> evidencia y cierre
```

Artefactos mínimos:

- `spec.md`: actores, escenarios WHEN/THEN, invariantes y fuera de alcance.
- `plan.md`: límites, contratos, datos, seguridad, fallos, observabilidad y pruebas.
- `tasks.md`: tareas pequeñas, dependencias y verificación.
- ADRs: decisiones costosas o difíciles de revertir.
- evidencia: comandos ejecutados y resultados.

## Integración de Spec Kit

No se ejecutó `specify init` durante la fundación. Es una mutación amplia sobre un repositorio no vacío y debe revisarse como cambio separado.

Fuentes verificadas el 2026-10-08:

- instalación oficial: <https://github.com/github/spec-kit/blob/main/docs/installation.md>
- comandos core: <https://github.github.io/spec-kit/reference/core.html>
- integración Codex: <https://github.github.io/spec-kit/reference/integrations.html>

La documentación oficial vigente indica Python 3.11+, `uv` o `pipx`, soporte PowerShell y la clave de integración `codex`. Como la CLI cambia, no se debe copiar una versión “latest” ni ejecutar comandos destructivos desde este documento sin inspeccionar la instalación local.

### Adopción controlada propuesta

1. Confirmar herramientas sin modificar el repo:

   ```powershell
   specify version
   specify version --features
   specify check
   ```

2. Elegir una release desde la página oficial y registrar `<VERSION_APROBADA>` en el PR. No sustituir el placeholder automáticamente.

3. Instalar de forma reproducible según la guía oficial vigente. Ejemplo documentado oficialmente:

   ```powershell
   uv tool install specify-cli --from "git+https://github.com/github/spec-kit.git@v<VERSION_APROBADA>"
   specify version
   ```

4. Crear commit limpio antes de inicializar y ejecutar en una rama dedicada. Para este repositorio existente, la documentación oficial actual propone:

   ```powershell
   specify init --here --force --non-interactive --integration codex --integration-options="--skills" --script ps
   ```

   `--force` reconoce que el directorio no está vacío; no equivale a permiso para aceptar todo el diff.

5. Revisar cada archivo generado, especialmente `AGENTS.md`, `.agents/`, `.specify/`, scripts y templates. Fusionar las reglas sin debilitar este documento.

6. Ejecutar nuevamente `specify version`, `specify check` y las pruebas del repositorio.

7. Registrar versión, checksum/commit de origen, diff aceptado y procedimiento de actualización.

## Uso esperado después de adoptar

La integración Codex vigente expone skills con forma `$speckit-<command>`. Las capacidades documentadas incluyen specify, clarify, plan, tasks, analyze e implement, pero el agente debe consultar los comandos realmente instalados; no asumir nombres por memoria.

Secuencia recomendada:

1. crear spec;
2. aclarar ambigüedades;
3. obtener aprobación humana;
4. generar plan;
5. registrar/aprobar ADRs;
6. generar tasks;
7. analizar consistencia entre artefactos;
8. implementar sólo tareas aprobadas;
9. converger y documentar evidencia.

## Mapeo con este repositorio

- `docs/product/`: visión transversal de producto.
- `docs/architecture/`: arquitectura y proceso transversal.
- `docs/adr/`: decisiones durables.
- `specs/<NNN-feature>/`: artefactos por capacidad que generará Spec Kit.
- `.specify/`: templates, scripts y memoria administrados por Spec Kit.
- `AGENTS.md`: reglas obligatorias del repositorio; cualquier template debe preservarlas.

## Gates humanos

Requieren confirmación explícita:

- pasar una spec o ADR a `APPROVED`;
- elegir semántica de negocio o consistencia;
- introducir dependencia, infraestructura o contrato público;
- ejecutar implementación funcional;
- publicar, desplegar o comunicarse con un sistema externo.

Un agente puede señalar alternativas y preparar un diff, pero no decidir unilateralmente.

## Riesgos y controles

| Riesgo | Control |
|---|---|
| Templates sobrescriben reglas | Rama/commit limpio, revisar diff y manifest |
| Versión flotante cambia comandos | Pin de release y `specify version --features` |
| Spec extensa pero ambigua | Escenarios observables y preguntas abiertas |
| Plan “aprueba” sus propias decisiones | Gate humano separado antes de tasks/implement |
| Artefactos divergen del código | Analyze/converge, tests y revisión de trazabilidad |
| Agente amplía alcance | AGENTS.md, tareas pequeñas y reporte de diff |

## Criterios de aceptación de la adopción

- La versión está fijada y registrada.
- Los comandos fueron obtenidos de la CLI instalada/documentación oficial.
- La inicialización ocurre sobre rama limpia y su diff es revisado.
- `AGENTS.md` conserva gates y seguridad.
- Una feature de prueba recorre spec → plan → tasks sin implementar negocio.
- Remover/actualizar la integración no elimina specs ni cambios manuales.

