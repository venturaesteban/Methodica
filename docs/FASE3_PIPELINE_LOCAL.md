# Fase 3.1 correctiva: embeddings semánticos locales reales (Android)

## Resultado

Se sustituyó el proveedor de hashing por un proveedor semántico real local en Android usando **MediaPipe Tasks Text Embedder** (inferencia offline) y una integración operativa de modelo para `EMBEDDING_GEMMA`. La carga de modelo se hace desde **archivo local real** (storage privado) mediante `TextEmbedder.createFromFile(...)`. El pipeline de chunking, persistencia Room, indexación incremental, filtros académicos y tracking de runs se mantiene.

## Proveedor definitivo

- **Proveedor activo**: `MediaPipeTextEmbeddingProvider`.
- **Runtime**: `com.google.mediapipe:tasks-text`.
- **Inferencia**: totalmente local, sin red durante embedding/query.
- **Carga de modelo en runtime**: `TextEmbedder.createFromFile(context, absolutePath)` sobre archivo local en `filesDir`.
- **Estado por defecto**: ya no existe hashing como camino principal.

## Gestión operativa del modelo (implementada)

### Estrategia elegida

1. **Ruta local canónica** (persistente en app private storage):
   - `files/local_models/embeddinggemma/embeddinggemma-300m.task`
2. **Inicialización en primer uso**:
   - `ensureModelReady(spec)` valida compatibilidad (ABI 64-bit, RAM, espacio) y estado.
3. **Obtención del modelo**:
   - Intento 1: copiar desde `assets/models/embeddinggemma/embeddinggemma-300m.task` a `filesDir`.
   - Intento 2: descarga HTTP a `filesDir` si `downloadUrl` está configurada en el `spec`.
   - Si no hay asset ni URL, se marca estado `MISSING_MODEL` y falla explícitamente.
4. **Carga del runtime MediaPipe (validado)**:
   - `setModelAssetPath(...)` se reserva para assets APK.
   - Para archivo local descargado/copiado se usa `TextEmbedder.createFromFile(...)` con ruta absoluta, que es la vía operativa estable del flujo actual.
5. **Integridad**:
   - En **debug**: `expectedSha256` opcional.
   - En **release**: `expectedSha256` obligatorio (si no, estado `INTEGRITY_ERROR` y no se inicializa).
6. **Versionado**:
   - `modelVersion = embeddinggemma-300m-task-v1` (provider).
   - Se persiste en `ai_chunk_embeddings.modelVersion`.
7. **No mezcla de índices incompatibles**:
   - Retrieval SQL filtra por `modelVersion` del proveedor activo.
   - La indexación incremental fuerza re-embedding cuando detecta versiones antiguas en chunks existentes.

### Estados expuestos a UI/capabilities

Se persisten y propagan al runtime:
- `MISSING_MODEL`
- `DOWNLOADING`
- `INITIALIZING`
- `READY`
- `INCOMPATIBLE_DEVICE`
- `NO_SPACE`
- `INTEGRITY_ERROR`
- `ERROR`

## Bloqueo real sobre EmbeddingGemma y cómo queda resuelto

### Bloqueo técnico real

En el estado actual del repo, **no se incluye** el artefacto `.task` de EmbeddingGemma (pesado y normalmente distribuido fuera del repo). Sin ese archivo no puede crearse `TextEmbedder`.

Tipo de bloqueo: **provisión/artefacto de modelo** (no de arquitectura del pipeline).

### Integración máxima viable implementada

- Runtime y provider reales listos para EmbeddingGemma.
- Flujo operativo listo para:
  - asset empaquetado, o
  - descarga bajo configuración.
- Si el artefacto no está, se informa estado explícito (`MISSING_MODEL`) en lugar de fallback falso.

## Pasos manuales mínimos inevitables (claros)

> Necesarios solo si no se configura descarga automática y no se empaqueta en assets.

1. Obtener un **modelo Text Embedder compatible con MediaPipe** para EmbeddingGemma en formato `.task`.
2. Colocarlo en:
   - `app/src/main/assets/models/embeddinggemma/embeddinggemma-300m.task` (para empaquetado),
   - o proveerlo a `files/local_models/embeddinggemma/embeddinggemma-300m.task` en dispositivo.
3. (Release obligatorio) Fijar SHA-256 en `MediaPipeTextEmbeddingProvider.EMBEDDING_SPEC.expectedSha256`.
4. Verificar instalación:
   - runtime pasa a `READY`,
   - no aparece `MISSING_MODEL` ni `INTEGRITY_ERROR`,
   - indexing/retrieval generan resultados.

## Qué queda automatizado por código

- Evaluación de compatibilidad del dispositivo.
- Copia desde assets si existe modelo empaquetado.
- Descarga HTTP si hay URL configurada.
- Verificación de integridad SHA-256 (obligatoria en release).
- Inicialización del runtime de embeddings.
- Persistencia de estado del modelo para UI.
- Reindexado incremental con invalidación automática por cambio de `modelVersion`.
- Retrieval filtrado por versión del embedding para evitar contaminación con vectores legacy.

## Validación y tests

Se actualizaron pruebas de pipeline para cubrir:
- retrieval con filtros académicos,
- indexación incremental sin recalcular cuando no cambia contenido,
- reindexación al cambiar versión de embeddings.

## Riesgos abiertos

- Si el artefacto EmbeddingGemma no se provee, el estado quedará en `MISSING_MODEL` (esperado).
- La descarga automática requiere URL estable y distribución permitida del modelo.
- Ajustes de RAM/espacio (`requiredRamMb`, `requiredDiskBytes`) pueden necesitar tuning por dispositivo real.
