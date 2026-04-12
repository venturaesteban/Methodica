# Fase 3.1 correctiva: embeddings semanticos locales reales (Android)

## Resultado

La integracion de embeddings deja de asumir un artefacto `.task` y pasa a un contrato explicito para `MediaPipe TextEmbedder` con modelo `.tflite`. El pipeline existente se mantiene intacto:

- chunking
- indexacion incremental
- retrieval
- `modelVersion`
- reindexado al cambiar de version

## Contrato tecnico correcto

- Provider activo: `MediaPipeTextEmbeddingProvider`
- Runtime: `com.google.mediapipe:tasks-text`
- API de inicializacion: `TextEmbedder.createFromFile(...)`
- Formato esperado: `TFLite` compatible con `MediaPipe TextEmbedder`
- Restriccion importante: si el modelo usa tensores `int32`, debe incluir metadatos/tokenizacion compatibles dentro del artefacto que se entregue al provider

Referencias usadas para fijar este contrato:

- `TextEmbedder` espera un modelo `TFLite` y no un `.task`: [Google AI Edge TextEmbedder](https://ai.google.dev/edge/api/mediapipe/java/com/google/mediapipe/tasks/text/textembedder/TextEmbedder?hl=es-419)
- Archivos LiteRT publicados para EmbeddingGemma: [litert-community/embeddinggemma-300m](https://huggingface.co/litert-community/embeddinggemma-300m/tree/main)

## Artefacto canonico que espera ahora Methodica

Methodica queda preparado para un artefacto canonico unico:

- Archivo exacto: `embeddinggemma-300M_seq1024_mixed-precision.tflite`
- Modelo logico: `EmbeddingGemma 300M`
- Variante fijada en el proyecto: `seq1024`
- Motivo: encaja mejor con el chunking actual de Methodica y evita seguir versionando contra un nombre generico falso

### Rutas exactas

Ruta recomendada para empaquetado en el repo:

- `app/src/main/assets/models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite`

Ruta local canonica en `filesDir` que usa el runtime:

- `files/local_models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite`

Ruta absoluta tipica en dispositivo Android para esta app:

- `/data/user/0/com.methodica.app/files/local_models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite`

## Que cambio en codigo

### `MediaPipeTextEmbeddingProvider`

- Ya no construye `TextEmbedder` con logica heredada de `.task`
- Inicializa el runtime desde archivo con `TextEmbedder.createFromFile(...)`
- Valida que el artefacto entregado sea `.tflite`
- Si la inicializacion falla, persiste un error explicito para `EMBEDDING_GEMMA` con una pista util: el archivo debe ser un `TFLite` compatible con `TextEmbedder`

### `LocalAiModelSpec` de embeddings

El spec queda alineado al artefacto real:

- `id = embeddinggemma-300m-seq1024`
- `version = embeddinggemma-300m-textembedder-tflite-seq1024-v1`
- `assetPath = models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite`
- `localRelativePath = local_models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite`
- `requiredDiskBytes = 256 MiB`
- `requiredRamMb = 256`

Este cambio de `version` fuerza el reindexado automatico de embeddings previos porque el retrieval y la indexacion ya filtran por `modelVersion`.

## Que archivo tienes que descargar ahora

Descarga este archivo concreto:

- `embeddinggemma-300M_seq1024_mixed-precision.tflite`

Fuente recomendada:

- [litert-community/embeddinggemma-300m](https://huggingface.co/litert-community/embeddinggemma-300m/tree/main)

## Hace falta algun artefacto adicional

Para la integracion actual de Methodica, no.

- Methodica solo carga un `.tflite`
- No hay carga de sidecars desde el provider
- Si el proveedor del modelo publica tambien `sentencepiece.model`, ese archivo no lo consume esta integracion

Regla practica:

- Si un paquete necesita un tokenizer externo para arrancar, ese paquete no es el artefacto correcto para este provider tal y como esta implementado ahora

## Hash que debes calcular

Debes calcular el `SHA-256` del archivo exacto que vayas a colocar:

```powershell
Get-FileHash -Algorithm SHA256 .\app\src\main\assets\models\embeddinggemma\embeddinggemma-300M_seq1024_mixed-precision.tflite
```

Despues, copia ese valor en:

- `MediaPipeTextEmbeddingProvider.EMBEDDING_SPEC.expectedSha256`

Si decides no fijarlo todavia, la integridad fuerte quedara desactivada, pero el runtime seguira funcionando.

## Validacion operativa paso a paso

1. Coloca `embeddinggemma-300M_seq1024_mixed-precision.tflite` en `app/src/main/assets/models/embeddinggemma/`.
2. Calcula su `SHA-256`.
3. Pega el hash en `expectedSha256` si quieres validacion estricta.
4. Ejecuta la app y dispara un flujo que necesite embeddings o el warm-up del provider.
5. Confirma que el runtime pasa a `READY`.
6. Confirma que no aparecen `MISSING_MODEL`, `INTEGRITY_ERROR` ni un error de inicializacion de `TextEmbedder`.
7. Confirma que se regeneran embeddings con `modelVersion = embeddinggemma-300m-textembedder-tflite-seq1024-v1`.

## Que se mantiene intacto

- `ParagraphChunkingStrategy`
- persistencia Room de chunks y embeddings
- indexacion incremental
- invalidacion por cambio de `modelVersion`
- retrieval filtrado por `modelVersion`
- runtime manager existente
- razonamiento local con Gemma 3n

## Riesgos abiertos

- Si el archivo no se provee, el estado seguira siendo `MISSING_MODEL`
- Si el `.tflite` que descargues no es compatible con `TextEmbedder`, la inicializacion fallara con error explicito
- Si mas adelante decides usar otra variante de EmbeddingGemma, deberas cambiar nombre/version del spec para no mezclar indices
