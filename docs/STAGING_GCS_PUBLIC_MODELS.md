# Staging GCS público sin login

Fecha de validación: 2026-04-13.

## Objetivo de esta fase

Validar descarga e instalación real de modelos locales en Methodica usando Google Cloud Storage público, sin login y sin backend intermedio todavía.

- Bucket: `methodica-bucket`
- Manifest remoto de staging: `https://storage.googleapis.com/methodica-bucket/manifests/methodica-models-v1.json`
- Modo actual: público, sin login, solo para staging/prueba

## Qué hace hoy la app

- `EmbeddingGemma` se intenta preparar automáticamente cuando falta.
- `Gemma 3n` no se descarga al arrancar y requiere una acción explícita del usuario.
- Ambos modelos se validan con `SHA-256` antes de marcarse `READY`.
- Los archivos se instalan en `filesDir/local_models/...`, fuera del APK.
- La pantalla de Ajustes muestra estado, tamaño, requisitos, errores, acciones de descarga/cancelación/borrado/reinstalación y enlaces legales.

## Contrato remoto consumido

La app consume por modelo:

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
- `minSdk` opcional

## Flujo por modelo

### EmbeddingGemma

- Política: automática.
- Caso nominal: si falta o está corrupto, Methodica programa la descarga en segundo plano.
- Caso sano: si el archivo ya existe y pasa integridad, se mantiene `READY` y no se redescarga.
- Finalidad: embeddings locales para indexado y retrieval semántico.

### Gemma 3n

- Política: descarga explícita.
- Caso nominal: la UI informa tamaño aproximado, espacio requerido, ABI esperada (`arm64-v8a`), recomendación de Wi-Fi y uso local avanzado.
- La descarga solo empieza al pulsar la acción correspondiente.
- Si todavía no está disponible, los flujos de reasoning caen a fallback heurístico sin romper la app.

## Legal mínimo visible

La UI expone directamente:

- `NOTICE_GEMMA.txt`
- Gemma Terms
- Gemma Prohibited Use Policy

En staging se validó que los tres enlaces responden `200`.

## Preparación para producción

La arquitectura ya no depende de forma rígida del bucket público:

- `LocalModelManifestConfig` concentra la URL del manifest.
- `LocalModelArtifactAccessResolver` separa el flujo de instalación del origen real del artefacto descargable.

Eso permite que en producción el `downloadUrl` se obtenga desde:

- backend propio,
- signed URL temporal,
- o un resolvedor distinto con cabeceras/token efímero,

sin rehacer el scheduler, el worker, la verificación de integridad, la persistencia de estados ni la UI.

## Qué cambiar más adelante

- Sustituir el manifest público por un endpoint autenticado o firmado.
- Resolver `downloadUrl` con backend/signed URLs en lugar de acceso público directo.
- Añadir login, autorización y expiración/control de acceso.
- Afinar observabilidad y políticas de red para producción.
