# Productización pre-fase-5: distribución de modelos y mitigación 16 KB

## 1. Infraestructura de modelos descargables

Methodica resuelve los modelos locales con un catálogo híbrido:

- `LocalAiModelCatalog` fija el contrato que espera el binario de la app (`id`, `modelVersion`, ruta privada, SHA esperada y requisitos mínimos).
- `LocalModelDistributionResolver` mezcla ese contrato con dos fuentes de distribución:
  - descriptor conocido embebido para EmbeddingGemma,
  - manifest remoto configurable mediante `BuildConfig.LOCAL_MODEL_MANIFEST_URL`.
- `LocalModelDownloadWorker` descarga a un archivo temporal `.download`, verifica tamaño y `SHA-256`, mueve de forma atómica al destino final y limpia restos obsoletos.
- `local_ai_model_state` persiste `displayName`, `downloadUrl`, `expectedSha256`, progreso (`downloadedBytes`, `totalBytes`) y estados de instalación.

## 2. Estados persistidos

Estados soportados por la app:

- `NOT_INSTALLED`
- `DOWNLOADING`
- `VERIFYING`
- `INSTALLING`
- `READY`
- `ERROR`
- `INCOMPATIBLE_DEVICE`
- `NO_SPACE`

La UI de Ajustes se limita a representar estos estados y a ofrecer acciones de descarga, cancelación, reintento, borrado y reinstalación.

## 3. Manifest remoto esperado

El manifest remoto debe exponer por modelo:

- `id`
- `version`
- `downloadUrl`
- `sha256`
- `sizeBytes`
- `requiredRamMb`
- `requiredDiskBytes`
- `supportedAbis`
- `minSdk`

Ejemplo esquemático:

```json
{
  "manifestVersion": "1",
  "models": [
    {
      "id": "gemma-3n-e2b-it-int4-litertlm",
      "version": "gemma-3n-e2b-it-int4-litertlm-v1",
      "downloadUrl": "<REAL_DOWNLOAD_URL>",
      "sha256": "<REAL_SHA256>",
      "sizeBytes": 3655827456,
      "requiredRamMb": 4096,
      "requiredDiskBytes": 4831838208,
      "supportedAbis": ["arm64-v8a"],
      "minSdk": 26
    }
  ]
}
```

## 4. Dependencia de licencia externa

La infraestructura técnica ya está lista para Gemma 3n, pero la publicación de su descriptor remoto depende de confirmar que Methodica puede redistribuir legalmente ese artefacto.

Consecuencia práctica:

- si la licencia queda aprobada, basta con publicar el manifest real y rellenar `METHODICA_LOCAL_MODEL_MANIFEST_URL`;
- si la licencia no queda aprobada, Gemma 3n debe seguir fuera del canal de distribución automático y la app debe permanecer en flujo degradado honesto.

## 5. Decisión tomada para OCR y 16 KB

Se sustituyó `com.google.mlkit:text-recognition` por `com.google.android.gms:play-services-mlkit-text-recognition`.

Motivos:

- evita empaquetar `libmlkit_google_ocr_pipeline.so` dentro del APK;
- reduce tamaño del APK;
- elimina el binario nativo concreto que estaba bloqueando instalación/compatibilidad en el escenario reportado;
- deja el reconocimiento OCR como módulo dinámico gestionado por Google Play Services.

Además se añadió en `AndroidManifest.xml`:

- `com.google.mlkit.vision.DEPENDENCIES = ocr`

Esto permite predescarga cuando la app se instala desde Play Store. En instalaciones por `adb` o entornos de desarrollo, la descarga puede producirse en el primer uso del OCR.

## 6. Resultado técnico actual

- `assembleDebug` pasa.
- `testDebugUnitTest` pasa.
- `compileDebugAndroidTestKotlin` pasa.
- `zipalign -c -P 16 -v 4` sobre el APK generado pasa.
- El APK ya no contiene `lib/x86_64/libmlkit_google_ocr_pipeline.so`.
- El APK ya no contiene `*.tflite` ni `*.litertlm` de modelos locales.

## 7. Límite conocido y explícito

El runtime local que hoy empaqueta Methodica no debe prometer soporte productivo para `x86_64` en el flujo de modelos locales. La mitigación de instalación para el emulador `x86_64` sí queda conseguida al retirar OCR bundled del APK, pero el camino nominal de modelos locales debe tratarse como `arm64-v8a` hasta que el stack nativo correspondiente quede disponible y validado para `x86_64`.
