# Rediseño técnico aterrizado (fase de preparación para IA local)

Fecha: 2026-04-11.

## Objetivo de esta fase
Preparar Methodica para migración local progresiva sin big-bang: contratos nuevos, persistencia para indexado/estado de modelos, coordinador único de workflow IA y wiring inicial en DI.

## Paquetes nuevos introducidos
- `domain/ai/local`: contratos de runtime, embeddings, retrieval, chunking, reasoning y actions.
- `domain/ai/workflow`: contrato único de coordinación del flujo IA.
- `data/localai/runtime`: gestor inicial del ciclo de vida de modelos locales.
- `data/localai/provider`: scaffolding de providers locales (embedding/reasoning/action/chunking/retrieval).
- `data/ai/workflow`: implementación coordinadora para eliminar duplicación entre ViewModels.
- `data/local/entity` + `data/local/dao`: tablas/DAOs para chunks, embeddings, corridas de indexación y estado de modelos.

## Contratos definidos
Se añadieron contratos listos para implementación real:
- `LocalModelRuntimeManager`
- `EmbeddingProvider`
- `ReasoningProvider`
- `ActionProvider`
- `RetrievalIndex`
- `ChunkingStrategy`
- `AiWorkflowCoordinator`

## Estrategia de integración local de modelos (decisión técnica)
1. **Carga de modelos**: fuera del repo de código fuente. El runtime usa rutas locales (`assetPath`) y valida precondiciones antes de marcar modelo como listo.
2. **Ciclo de vida**: `LocalModelRuntimeManager` centraliza disponibilidad, errores y liberación de runtime.
3. **Persistencia de estado**: tabla Room `local_ai_model_state` para estado por tipo de modelo (versión, path, requisitos, error).
4. **Compatibilidad de dispositivo**: chequeo explícito por espacio y RAM antes de inicializar (`StatFs` + `ActivityManager`).
5. **Errores operativos**: si falla compatibilidad o init, se persiste estado `ERROR` con mensaje para UX.
6. **Gestión de peso**: no se versionan binarios pesados en Git; se registran como artefactos locales/versionados externamente y el app sólo guarda metadata/estado.

## Diseño de almacenamiento para embeddings/indexado
Nuevas tablas Room (v9):
- `ai_document_chunks`: chunks normalizados por `subjectId/assessmentId/topicId/materialId/documentId`.
- `ai_chunk_embeddings`: vector por chunk + versión de modelo.
- `ai_indexing_runs`: corridas de indexación con estado y error.
- `local_ai_model_state`: estado operativo del runtime/modelos.

### Reindexado incremental
- Base preparada vía `contentHash` y `updatedAt` en chunks.
- `ai_indexing_runs` registra trigger y resultado por contexto.
- `RetrievalIndex.markSourceDirty(...)` queda definido para invalidación incremental en fase siguiente.

## Eliminación de ambigüedad de flujo IA
- Se introduce `AiWorkflowCoordinator` como orquestador único de:
  - capacidades (externa/local runtime),
  - análisis,
  - lectura de último análisis,
  - guardado de edición,
  - aplicar + regenerar.
- `PlanningViewModel` y `AiAnalysisViewModel` pasan a delegar en este coordinador.

## Compatibilidad y legado
- Se conserva pipeline actual (heurística + modo externo) como compatibilidad temporal.
- Se marca `LlmProvider` y `ExternalLlmProvider` como `@Deprecated` para retirar en fases futuras.
- No se rompe navegación ni pantallas actuales.

## Flujo de producto preparado
- La capa de workflow ya expone capacidad runtime local (`localModelsReady`, `runtimeMessage`) para UI.
- Las pantallas de planificación/análisis ya consumen estado unificado de capacidades.
- Queda preparada la siguiente conexión de UX: `materiales existentes -> indexación -> análisis -> edición -> aplicar -> regenerar`.

## Plan exacto de Fase 3
1. Implementar `EmbeddingProvider` real (EmbeddingGemma) y serialización vectorial compacta.
2. Implementar `RetrievalIndex` real sobre Room (filtro + ranking semántico) y jobs incrementales.
3. Enlazar ingestión desde `MaterialRepository` hacia chunking/indexado automático.
4. Implementar `ReasoningProvider` Gemma 3n con salida JSON validada.
5. Cambiar `AiWorkflowCoordinator.analyzeAssessment` a pipeline RAG local principal.
6. Añadir telemetría estructurada por etapa (ingest/chunk/embed/retrieve/reason/save).
7. Exponer en UX estado de indexación y compatibilidad/modelos con mensajes accionables.
