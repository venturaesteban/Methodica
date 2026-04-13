# Productización pre-fase-5: distribución de modelos y mitigación 16 KB

## 1. Infraestructura de modelos descargables

Methodica resuelve los modelos locales con un catálogo híbrido:

- `LocalAiModelCatalog` fija el contrato que espera el binario de la app (`id`, `modelVersion`, ruta privada, SHA esperada, política de descarga y requisitos mínimos).
- `LocalModelDistributionResolver` mezcla ese contrato con dos fuentes de distribución:
  - descriptor conocido embebido para compatibilidad de EmbeddingGemma,
  - manifest remoto configurable mediante `BuildConfig.LOCAL_MODEL_MANIFEST_URL`.
- `LocalModelManifestConfig` encapsula el endpoint del manifest para poder sustituir más adelante la URL pública de staging por backend propio sin rehacer la lógica.
- `LocalModelArtifactAccessResolver` desacopla el flujo de instalación del origen real del artefacto descargable: hoy devuelve la URL pública del manifest, mañana puede resolver backend, signed URLs o cabeceras efímeras.
- `LocalModelDownloadWorker` descarga a un archivo temporal `.download`, verifica tamaño y `SHA-256`, mueve de forma atómica al destino final y limpia restos obsoletos.
- `local_ai_model_state` persiste `displayName`, `downloadUrl`, `expectedSha256`, metadatos legales (`noticeUrl`, `termsUrl`, `prohibitedUsePolicyUrl`), progreso (`downloadedBytes`, `totalBytes`) y estados de instalación.

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

Además, distingue entre:

- `EmbeddingGemma`: descarga automática y preparación silenciosa cuando falta o se invalida su integridad.
- `Gemma 3n`: descarga guiada y explícita, con consentimiento del usuario, recomendación de Wi-Fi y superficie legal visible.

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
- `noticeUrl`
- `termsUrl`
- `prohibitedUsePolicyUrl`
- `minSdk`

Manifest público de staging validado a fecha 2026-04-13:

- URL: `https://storage.googleapis.com/methodica-bucket/manifests/methodica-models-v1.json`
- Bucket: `methodica-bucket`
- Modo: público, sin login

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
      "noticeUrl": "https://storage.googleapis.com/methodica-bucket/legal/NOTICE_GEMMA.txt",
      "termsUrl": "https://ai.google.dev/gemma/terms",
      "prohibitedUsePolicyUrl": "https://ai.google.dev/gemma/prohibited_use_policy",
      "minSdk": 26
    }
  ]
}
```

Observaciones del staging actual:

- El manifest publicado incluye `type` como campo adicional; la app no depende de ese campo para instalar.
- El manifest no publica `manifestVersion` ni `minSdk`, y la app tolera ambos como opcionales aplicando `manifestVersion = "1"` y `minSdk` por defecto según la build.

## 4. Dependencia de licencia externa

La infraestructura técnica ya está lista para Gemma 3n, pero la publicación de su descriptor remoto depende de confirmar que Methodica puede redistribuir legalmente ese artefacto.

Consecuencia práctica para esta fase:

- EmbeddingGemma puede validarse y descargarse desde staging sin login.
- Gemma 3n se puede distribuir en staging sin login, pero la app lo ofrece siempre bajo consentimiento explícito y no lo descarga al arrancar.
- Para producción, el siguiente paso no será cambiar el flujo de instalación sino sustituir la resolución del artefacto y del manifest por backend/signed URLs.

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
- El manifest público de staging responde `200` y publica tanto `EmbeddingGemma` como `Gemma 3n`.
- `NOTICE_GEMMA.txt`, `Gemma Terms` y `Prohibited Use Policy` responden `200`.

## 7. Límite conocido y explícito

El runtime local que hoy empaqueta Methodica no debe prometer soporte productivo para `x86_64` en el flujo de modelos locales. La mitigación de instalación para el emulador `x86_64` sí queda conseguida al retirar OCR bundled del APK, pero el camino nominal de modelos locales debe tratarse como `arm64-v8a` hasta que el stack nativo correspondiente quede disponible y validado para `x86_64`.
