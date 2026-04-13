# Checklist final de readiness pre-fase-5

## Alcance cerrado antes de fase 5

Queda cerrado y verificado:

- EmbeddingGemma local con `MediaPipe TextEmbedder` sobre `.tflite` y descarga automática preparada.
- Gemma 3n local con `tasks-genai`, descarga/instalación on-device preparada y bloqueo legal documentado para su publicación remota.
- Persistencia Room del estado de instalación de modelos con progreso y errores.
- Integración en UI para descargar, reintentar, cancelar, borrar y reinstalar modelos.
- Fallback heurístico honesto cuando Gemma 3n no está lista.
- Mitigación de instalación/16 KB mediante OCR en Google Play Services y exclusión de artefactos pesados del APK.

## Artefactos locales requeridos

### En el APK

- Ningún artefacto pesado de modelo local (`*.tflite`, `*.litertlm`, `*.task`) debe viajar empaquetado.

### En almacenamiento privado de la app

- `files/local_models/embeddinggemma/embeddinggemma-300M_seq1024_mixed-precision.tflite` cuando EmbeddingGemma está lista.
- `files/local_models/gemma3n/gemma-3n-E2B-it-int4.litertlm` cuando Gemma 3n está lista.

## Pasos manuales mínimos del desarrollador

1. Definir `METHODICA_LOCAL_MODEL_MANIFEST_URL` en `gradle.properties` local o en CI cuando exista un manifest remoto real.
2. Confirmar la licencia de redistribución de Gemma 3n antes de publicar su entrada en el manifest.
3. Ejecutar `./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
4. Validar el APK con `zipalign -c -P 16 -v 4` si se va a revisar compatibilidad de 16 KB.

## Cómo validar que embeddings están listas

- Ajustes debe mostrar `EmbeddingGemma` en `READY`.
- El hash efectivo debe ser `8b0b8bbd0aa95f9f747c25a6c87cd05a8286933282660f6a50da877662917e31`.
- El runtime no debe registrar `INTEGRITY_ERROR`, `NO_SPACE` ni `INCOMPATIBLE_DEVICE`.
- Un reindexado inicial debe poblar `ai_chunk_embeddings`.
- Un segundo reindexado sin cambios no debe reembeder chunks ya válidos.

## Cómo validar que Gemma 3n está lista

- Ajustes debe mostrar `Gemma 3n` en `READY`.
- El hash efectivo debe ser `2ed7bc3a0026c93d5b8a4544b352d9d00cd66ff0bac3ef6a20ac3d2cba4010d6`.
- `local_ai_model_state` para `GEMMA_3N_REASONING` debe quedar en `READY`.
- `AiWorkflowCapability.localModelsReady` debe ser `true`.
- Un análisis debe guardar salida con `sourceLabel` de Gemma local, no de fallback.

## Síntomas de fallback degradado

- Ajustes muestra `ERROR`, `NO_SPACE` o `INCOMPATIBLE_DEVICE` para Gemma 3n.
- El mensaje del runtime indica ausencia de manifest, licencia pendiente, incompatibilidad o falta de espacio.
- `localModelsReady = false`.
- `DefaultAiWorkflowCoordinator` guarda `sourceLabel = Fallback heurístico (Gemma local no operativa)`.
- La UI de análisis indica explícitamente que sigue en flujo degradado.

## Validación de packaging y 16 KB

- `app-debug.apk` pesa alrededor de `62.9 MB` tras excluir modelos pesados.
- El APK ya no contiene `lib/x86_64/libmlkit_google_ocr_pipeline.so`.
- El APK ya no contiene `*.tflite` ni `*.litertlm`.
- `zipalign -c -P 16 -v 4 app-debug.apk` debe devolver verificación correcta.

## Condiciones para arrancar fase 5 sin sorpresas

Todas estas deben cumplirse:

- `:app:testDebugUnitTest` en verde.
- `:app:assembleDebug` en verde.
- El APK no empaqueta OCR bundled ni modelos pesados.
- Room migra hasta v10 sin fallback destructivo.
- La documentación de fases 3 y 4 refleja el estado real del código.
- El equipo sabe que la redistribución remota de Gemma 3n depende todavía de licencia externa.
- El flujo nominal y el degradado son distinguibles en runtime y en UI.

## Decisión de readiness

Si se cumplen los puntos anteriores, Methodica queda lista para iniciar fase 5 sin deuda oculta relevante en distribución local de modelos ni en el frente principal de instalación/16 KB.
