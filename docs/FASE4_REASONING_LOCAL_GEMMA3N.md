# Methodica Fase 4 — Reasoning local real con Gemma 3n

## Objetivo implementado
En esta fase el camino **nominal** de análisis IA pasa a ser:

1. Retrieval local persistente (índice local + filtros académicos).
2. Reasoning local con Gemma 3n (sin endpoints remotos).
3. Salida estructurada validada.
4. Persistencia en `AiAnalysisRepository` para alimentar análisis y planificación.

El camino heurístico queda como **fallback secundario**, activado solo ante fallo real del runtime local de reasoning.

## Runtime/modelo de reasoning usado
- Modelo esperado: `gemma-3n-e2b-it-int4`.
- Tipo de runtime: `LocalAiModelType.GEMMA_3N_REASONING`.
- Ruta local esperada: `local_models/gemma3n/gemma-3n-e2b-it-int4.task`.
- Integración de inferencia: intento local vía `com.google.mediapipe.tasks.genai.llminference.LlmInference` (reflectivo para no falsear compatibilidad en build).

## Qué está automatizado
- Verificación de compatibilidad/disco/RAM con `LocalModelRuntimeManager`.
- Instalación desde assets locales si el archivo está empaquetado.
- Validación de integridad (SHA256 si se configura).
- Cambio de estado de runtime (`UNINITIALIZED`, `DOWNLOADING`, `INITIALIZING`, `READY`, `ERROR`).
- Ejecución de reasoning local sobre evidencia recuperada.
- Parseo robusto de JSON estructurado con validación y reintento de reparación de salida.
- Persistencia de resultados estructurados para flujo de producto.

## Intervención manual mínima pendiente
- Colocar el artefacto del modelo Gemma 3n compatible en assets o en la ruta local esperada.
- Si se requiere hash estricto en producción, definir `expectedSha256` del modelo en el `LocalAiModelSpec`.
- Validar en dispositivo objetivo que la librería de inferencia local elegida (MediaPipe GenAI) esté disponible en el packaging final.

## Limitaciones reales (Android)
- En algunos dispositivos, el runtime de Gemma 3n puede no estar disponible por RAM/ABI.
- La API exacta de inferencia local puede variar según versión del runtime; por eso se implementa invocación reflectiva y fallback controlado.
- Si el runtime local falla, Methodica degrada a heurístico para no romper pantalla/flujo.

## Diferencia explícita: principal vs fallback
- **Principal**: `ReasoningProvider` local (Gemma 3n) + retrieval local.
- **Fallback**: `AnalyzeAssessmentWithAiUseCase` heurístico, solo si falla `ReasoningProvider`.

