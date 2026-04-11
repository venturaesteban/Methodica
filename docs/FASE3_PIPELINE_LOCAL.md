# Fase 3 real: pipeline IA local (ingestión + chunking + embeddings + retrieval)

## Qué se implementó

- `OnDeviceEmbeddingProvider`: embedding local determinista (hashing vectorial, 256 dimensiones), sin red.
- `RoomBackedRetrievalIndex`: persistencia real de chunks/embeddings y búsqueda top-k con cosine similarity.
- Filtros académicos reales en retrieval: `subjectId`, `assessmentId`, `topicId`, `materialId`, `documentId`.
- `DefaultLocalAiIngestionPipeline`: indexación incremental por `contentHash`, tracking de runs y estado parcial/error.
- Integración con materiales reales:
  - `MaterialRepository.buildAiIndexableContent(...)` para extraer texto utilizable,
  - `UpsertMaterialUseCase` dispara reindexado al guardar,
  - `DeleteMaterialUseCase` limpia índice al borrar.
- Chunking mejorado (`ParagraphChunkingStrategy`) con metadatos y control de tamaño.
- `DefaultAiWorkflowCoordinator` ahora recupera contexto local y lo inyecta en el prompt de análisis.

## Decisiones clave

1. **Embedding local sin dependencias remotas**
   - Se utiliza un encoder local basado en hashing para garantizar ejecución offline y latencia baja.
   - Está preparado como capa de transición compatible para reemplazo futuro por runtime nativo de EmbeddingGemma.

2. **Indexación incremental real**
   - Se comparan chunks nuevos vs existentes por `externalId + contentHash`.
   - Solo se recalculan embeddings de chunks nuevos/cambiados.
   - Se eliminan chunks huérfanos del índice cuando el material cambia.

3. **Observabilidad mínima útil**
   - Runs persistidos en `ai_indexing_runs` con estados: `RUNNING`, `SUCCESS`, `PARTIAL`, `FAILED`.
   - Runtime local refleja `isIndexing` en base a runs en progreso.

## Riesgos abiertos

- El embedding de hashing es robusto para recuperación semántica ligera, pero no iguala calidad de EmbeddingGemma real.
- El rendimiento de retrieval puede requerir optimización adicional para corpus muy grandes (actualmente ranking en memoria tras filtro SQL).
- Faltan pruebas instrumentadas Room end-to-end con base real en Android (se añadieron unit tests con dobles de test).
