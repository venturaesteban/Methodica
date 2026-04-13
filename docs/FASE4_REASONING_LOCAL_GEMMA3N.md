# Fase 4 cerrada: Gemma 3n local para reasoning con distribución preparada

## Estado real

Methodica integra reasoning local real con MediaPipe GenAI y Gemma 3n, y ya tiene infraestructura para descarga/instalación en almacenamiento privado de la app. En staging ya existe un manifest remoto público sin login; lo que sigue pendiente para producción es sustituir este reparto público por backend propio o signed URLs.

- Runtime real: `com.google.mediapipe:tasks-genai`
- API real: `LlmInference.createFromOptions(context, options)`
- Formato esperado: `.litertlm`
- Artefacto esperado: `gemma-3n-E2B-it-int4.litertlm`
- Ruta runtime canónica en la app: `files/local_models/gemma3n/gemma-3n-E2B-it-int4.litertlm`
- Empaquetado actual: el `.litertlm` se excluye del APK y no debe formar parte del repositorio
- Distribución actual: staging público operativo con manifest configurable y UX de consentimiento explícito

## Contrato del modelo

- `id`: `gemma-3n-e2b-it-int4-litertlm`
- `modelVersion`: `gemma-3n-e2b-it-int4-litertlm-v1`
- `expectedSha256`: `2ed7bc3a0026c93d5b8a4544b352d9d00cd66ff0bac3ef6a20ac3d2cba4010d6`
- `requiredDiskBytes`: `4831838208`
- `requiredRamMb`: `4096`
- ABI prevista para publicación productiva: `arm64-v8a`
- `downloadUrl`: no embebida en la lógica del worker; hoy llega desde manifest remoto público y mañana puede resolverse desde backend/signed URLs

## Comportamiento nominal y degradado

### Nominal

- La app resuelve el descriptor del modelo desde el manifest remoto configurable `https://storage.googleapis.com/methodica-bucket/manifests/methodica-models-v1.json`.
- Si el usuario consiente la descarga y el archivo no existe en `filesDir`, Methodica programa una descarga con `WorkManager`.
- El worker descarga a un `.download` temporal, verifica integridad y mueve el archivo a almacenamiento privado.
- `GemmaLocalReasoningProvider` solo infiere si el runtime marca `READY`.
- `DefaultAiWorkflowCoordinator` marca `localModelsReady = true` solo si `installedModels` contiene `GEMMA_3N_REASONING`.

### Degradado

- Si el descriptor remoto falla, no hay espacio, falla la integridad o el runtime no es compatible, el estado persiste `ERROR`, `NO_SPACE` o `INCOMPATIBLE_DEVICE`.
- El coordinador cae a fallback heurístico y la UI lo indica explícitamente.
- Build, tests y `assembleDebug` siguen funcionando porque Gemma 3n no es obligatoria para compilar.

## Mensaje de verdad para el equipo

- Gemma 3n ya no depende de copiar el archivo manualmente para que el flujo exista en la app.
- La fase actual es de staging público: descarga real sin login, con consentimiento explícito y fallback honesto cuando Gemma 3n aún no está lista.
- La parte pendiente para producción no es rehacer el runtime sino endurecer la distribución: backend, autenticación, signed URLs y control de acceso.
- El repo no debe contener el `.litertlm` ni empaquetarlo en el APK.

## Validación operativa

1. Ejecutar `:app:compileDebugKotlin`, `:app:testDebugUnitTest` y `:app:assembleDebug`.
2. Confirmar que el APK no contiene `*.litertlm`.
3. Confirmar que Ajustes muestra Gemma 3n con estado persistido, error claro si no hay manifest y acciones de descarga/reintento/cancelación.
4. Confirmar que `AiWorkflowCapability.localModelsReady` solo sube a `true` cuando el modelo queda realmente en `READY`.
5. Confirmar que, sin descriptor remoto válido, la app cae a fallback heurístico sin estados engañosos.
