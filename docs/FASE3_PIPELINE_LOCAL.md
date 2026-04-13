# Fase 3 cerrada: EmbeddingGemma local productizada

## Estado real

EmbeddingGemma sigue integrada con `MediaPipeTextEmbeddingProvider` usando `MediaPipe TextEmbedder` sobre un artefacto `.tflite`, pero ya no depende de empaquetarse en el APK ni de copiarse a mano.

- Provider activo: `MediaPipeTextEmbeddingProvider`
- Runtime: `com.google.mediapipe:tasks-text`
- API real: `TextEmbedder.createFromFile(...)`
- Artefacto esperado: `embeddinggemma-300M_seq1024_mixed-precision.tflite`
- Ruta runtime en almacenamiento privado: `files/local_models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite`
- Distribución actual soportada: descarga automática bajo demanda con verificación y persistencia de estado
- Empaquetado actual: el `.tflite` se excluye del APK mediante `ignoreAssetsPatterns`

## Contrato del modelo

- `id`: `embeddinggemma-300m-seq1024`
- `modelVersion`: `embeddinggemma-300m-textembedder-tflite-seq1024-v1`
- `expectedSha256`: `8b0b8bbd0aa95f9f747c25a6c87cd05a8286933282660f6a50da877662917e31`
- `requiredDiskBytes`: `268435456`
- `requiredRamMb`: `256`
- ABI declarada por Methodica para flujo productizado: `arm64-v8a`

Methodica incluye un descriptor de descarga conocido para EmbeddingGemma y lo puede sobrescribir con el manifest remoto configurable si el equipo decide mover la distribución a un catálogo externo común.

## Reindexado e integridad

El pipeline mantiene estos invariantes:

- `AiChunkEmbeddingEntity` guarda `modelVersion` por embedding.
- `DefaultLocalAiIngestionPipeline` detecta cambio de `modelVersion` y fuerza reindexado de chunks incompatibles.
- `RoomBackedRetrievalIndex` consulta solo embeddings de la versión activa.
- `RoomBackedLocalModelRuntimeManager` valida `SHA-256` antes de marcar el modelo como `READY`.
- Si el archivo desaparece o queda corrupto, el runtime invalida el estado y vuelve a programar descarga.

No se mezclan embeddings de versiones distintas en retrieval.

## Comportamiento esperado

### Nominal

- Si el `.tflite` no existe en `filesDir`, Methodica programa una descarga con `WorkManager`.
- El worker descarga a un `.download` temporal, verifica tamaño y `SHA-256`, y mueve el archivo con rename atómico.
- El estado persistido pasa por `DOWNLOADING`, `VERIFYING`, `INSTALLING` y termina en `READY`.

### Degradado

- Si no hay red, espacio, ABI compatible o la distribución remota falla, el estado persiste `ERROR`, `NO_SPACE` o `INCOMPATIBLE_DEVICE`.
- El indexado no se rompe a nivel de build; simplemente no puede generar embeddings hasta que el modelo quede listo.
- La pantalla de Ajustes expone descarga, progreso, reintento, cancelación y borrado/reinstalación.

## Validación operativa

1. Ejecutar `:app:compileDebugKotlin`, `:app:testDebugUnitTest` y `:app:assembleDebug`.
2. Confirmar que el APK no contiene `*.tflite` del modelo.
3. Abrir Ajustes y verificar que EmbeddingGemma aparece con estado persistido y acciones de descarga.
4. Confirmar que `ai_chunk_embeddings.modelVersion` queda en `embeddinggemma-300m-textembedder-tflite-seq1024-v1`.
5. Confirmar que un segundo reindexado sin cambios no regenera embeddings y que un cambio de `modelVersion` sí lo hace.
