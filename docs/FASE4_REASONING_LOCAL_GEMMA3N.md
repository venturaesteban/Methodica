# Methodica Fase 4.1 (correctiva) — Integración real de runtime Gemma 3n

## Estado final inequívoco de esta fase
**Situación B (explícita):**
- El proyecto **sí** queda integrado con runtime real de MediaPipe GenAI (`tasks-genai`) a nivel de dependencias y API.
- Pero en este repositorio **no se incluye** el archivo de modelo Gemma 3n (`.task`) en `assets`, ni URL de descarga configurada.
- Por tanto, en el estado actual del repo, el camino estable operativo sigue siendo el **fallback heurístico**.

No se presenta Gemma local como camino principal operativo hasta que el modelo compatible esté realmente instalado en dispositivo.

## Runtime/dependencias verificadas
- Runtime de embeddings: `com.google.mediapipe:tasks-text`.
- Runtime de reasoning local: `com.google.mediapipe:tasks-genai`.
- Integración de reasoning actual: **directa** (sin reflexión) contra:
  - `com.google.mediapipe.tasks.genai.llminference.LlmInference`
  - `LlmInference.createFromOptions(context, options)`
  - `generateResponse(prompt)`

## Qué corrige esta fase frente al estado anterior
1. Se elimina la “integración reflectiva ambigua” para reasoning y se usa API tipada de `tasks-genai`.
2. Se clasifica y reporta fallo real por categorías de runtime:
   - runtime ausente en ejecución (`NoClassDefFoundError`),
   - método ausente/incompatible (`NoSuchMethodError`),
   - fallo de inicialización (`IllegalStateException`),
   - incompatibilidad de runtime/JNI (`UnsatisfiedLinkError`),
   - modelo no disponible,
   - error de inferencia.
3. El error se propaga al `LocalModelRuntimeManager` con `markModelError(...)`.
4. El coordinador **no** marca modelos locales como listos si no está instalado `GEMMA_3N_REASONING`.
5. Si Gemma no está realmente disponible/ready, el flujo entra directamente en fallback heurístico con motivo explícito.

## Bloqueo técnico real pendiente
Para que Gemma 3n local pase a estado operativo (Situación A), falta:
1. Proveer modelo `.task` de Gemma 3n compatible con `tasks-genai` en:
   - `assets/models/gemma3n/gemma-3n-e2b-it-int4.task`, o
   - `files/local_models/gemma3n/gemma-3n-e2b-it-int4.task`.
2. (Recomendado) fijar `expectedSha256` en `LocalAiModelSpec` para integridad fuerte.
3. Validar en hardware objetivo (RAM/ABI) que el runtime GenAI inicializa correctamente.

## Cuándo entra fallback heurístico
Fallback entra en cualquiera de estos casos:
- estado runtime no `READY` para `GEMMA_3N_REASONING`,
- modelo ausente,
- incompatibilidad de dispositivo/runtime,
- fallo de inicialización o inferencia,
- incompatibilidad de API de runtime.

## Limitaciones Android reales documentadas
- Requisitos de RAM/espacio pueden bloquear modelos grandes en gama media/baja.
- Dependencia JNI/ABI puede fallar por arquitectura o packaging.
- Sin artefacto de modelo no hay inferencia local real, aunque el runtime esté enlazado en Gradle.

