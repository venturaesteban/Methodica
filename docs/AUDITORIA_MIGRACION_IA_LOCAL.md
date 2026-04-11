# Auditoría integral del proyecto Methodica y plan de migración a IA local

Fecha de auditoría: 2026-04-11.

## 1) Mapa del proyecto

## Stack y plataforma
- **App Android nativa** con **Kotlin + Jetpack Compose**.
- **Arquitectura por capas** (feature/ui, domain/usecase, data/repository/local/preferences, di).
- **Persistencia local principal** con **Room** (AppDatabase v8).
- **Preferencias/configuración** con **DataStore Preferences**.
- **Ejecución diferida/recordatorios** con **WorkManager**.
- **Procesamiento documental local** para recursos con **PDFBox-Android + PdfRenderer + ML Kit OCR**.
- **DI** con **Hilt**.

## Estructura funcional (alto nivel)
- `app/src/main/java/com/methodica/app/feature/*`: pantallas + ViewModels por feature.
- `app/src/main/java/com/methodica/app/domain/*`: modelos de dominio, puertos y casos de uso.
- `app/src/main/java/com/methodica/app/data/*`: repositorios, persistencia, heurísticas IA y proveedor externo.
- `app/src/main/java/com/methodica/app/core/*`: navegación, políticas de procesamiento IA y design system.
- `app/src/main/java/com/methodica/app/di/*`: wiring de dependencias.

## Módulos/servicios clave
- Gestión académica: grados, cursos, asignaturas, temas, evaluaciones.
- Planificación: generación de sesiones de estudio y repaso.
- Materiales: alta de links/archivos y extracción de resumen para IA.
- IA: análisis de temario (heurístico local o proveedor remoto configurable).
- Recordatorios: planificación diaria de notificaciones.

---

## 2) Arquitectura actual (real)

## Frontend / UX
- Single-Activity (`MainActivity`) + `MethodicaApp()` + `MethodicaNavHost`.
- Rutas top-level: `home`, `today`, `degrees`, `planning`, `materials`, `settings`.
- Rutas internas: forms de degree/subject/topic/assessment/material + `aiAnalysis/{assessmentId}`.

## Estado
- Estado por pantalla mediante `StateFlow` en ViewModels (`MutableStateFlow` + `collectAsStateWithLifecycle`).
- No se observa un store global unificado; el estado está **distribuido por feature**.

## Datos y almacenamiento
- Room como **fuente única de verdad** para entidades académicas + entidades IA:
  - `AiDocumentEntity`, `AiAnalysisEntity`, `ExamScopeAnalysisEntity`, `TopicComplexityAnalysisEntity`.
- DataStore para:
  - `PlanningSettings` (horas, buffer, repaso, perfil estudiante, etc.).
  - `AiProviderSettings` (enabled, providerName, baseUrl, model, apiKey).

## Integración IA actual
- Inyección de interfaces de dominio (`DocumentParser`, `SyllabusAnalyzer`, `ExamScopeInferenceService`, `ComplexityEstimator`, `StudyPlanningAdvisor`, `LlmProvider`).
- Implementación por defecto:
  - Heurística local para parser/análisis/inferencia/recomendación.
  - `LlmProvider` conectado a `ExternalLlmProvider` (HTTP remoto) para modo EXTERNAL.

---

## 3) Flujos actuales E2E

## Flujo académico base
1. Crear titulación (`Degrees`/`DegreeForm`).
2. Crear curso académico (`AcademicYears`).
3. Crear asignaturas (`Subjects`/`SubjectForm`).
4. Dentro de asignatura, crear temas (`TopicForm`) y evaluaciones (`AssessmentForm`).

## Flujo de planificación clásica
1. Seleccionar evaluación en `PlanningScreen`.
2. (Opcional) pegar texto y ejecutar análisis IA.
3. Generar plan con `GenerateAssessmentPlanUseCase`.
4. Se reemplazan sesiones `PLANNED` auto-generadas de la evaluación y se insertan nuevas si el resultado no es `INFEASIBLE`.

## Flujo de análisis IA detallado (pantalla dedicada)
1. Entrar a `AiAnalysisScreen` con assessmentId.
2. Escribir texto fuente y elegir modo (`HEURISTIC`/`EXTERNAL`).
3. Ejecutar análisis; se guarda en DB:
   - documento procesado,
   - análisis resumen/confianza,
   - alcance inferido,
   - complejidad por tema.
4. Usuario puede editar alcance y complejidades.
5. Guardar edición y aplicar cambios a temas + regenerar plan.

## Flujo de materiales/importación
1. Crear/editar material en `MaterialForm`.
2. Tipo: `FILE_URI`, `WEB_LINK` o `VIDEO_LINK`.
3. Para archivos locales:
   - validación MIME/extensión,
   - extracción de metadata,
   - para PDF: texto por PDFBox y fallback OCR por ML Kit,
   - limpieza y truncado de texto según política.
4. `MaterialRepository.buildAiResourceSummary()` devuelve resumen textual apto para IA.

---

## 4) Uso actual de IA remota y heurísticas (exacto)

## Dónde se usan endpoints remotos de IA
- `ExternalLlmProvider` realiza `HttpURLConnection` POST y soporta:
  - OpenAI-compatible (`/chat/completions`),
  - Anthropic (`/v1/messages`),
  - Gemini (`:generateContent`).
- Se usa desde:
  - `AnalyzeAssessmentWithAiUseCase` cuando `executionMode == EXTERNAL`.
  - `VerifyAiConnectionUseCase` para test de conectividad del proveedor.
- Configuración remota guardada en DataStore (`AiProviderSettingsRepositoryImpl`).

## Dónde se usan heurísticas
- `HeuristicDocumentParser`: normalización, secciones, topics, señales de examen, umbrales de calidad.
- `HeuristicSyllabusAnalyzer`: fallback de topics cuando no detecta suficientes.
- `HeuristicExamScopeInferenceService`: inferencia de alcance + confianza por señales estructurales.
- `HeuristicComplexityEstimator`: complejidad por topic con señales textuales simples.
- `HeuristicStudyPlanningAdvisor`: orden recomendado y advertencias.
- `ApplyAiComplexityToTopicsUseCase`: matching por nombre normalizado + similitud Jaccard-like (heurística de mapeo).

## Cómo se generan hoy los planes de estudio
- `StudyPlanGenerator` (puro) calcula ventana [hoy, examen), bloque de buffer, días de repaso, capacidad diaria, distribución de bloques por tema y sesiones de review.
- `GenerateAssessmentPlanUseCase`:
  - carga evaluación, topics y settings,
  - obtiene sesiones existentes en rango para descontar capacidad,
  - genera resultado,
  - borra sesiones auto-planificadas previas,
  - persiste nuevas sesiones solo si no es `INFEASIBLE`.

## Cómo se importan y almacenan recursos
- `MaterialFormViewModel` acepta URI y valida tipos soportados.
- `MaterialRepositoryImpl` genera resumen AI-ready:
  - PDF text extraction + OCR fallback,
  - lectura de texto plano,
  - descripción de links/video por host.
- Persistencia de materiales en Room (`MaterialEntity`) y almacenamiento del resumen en memoria cache LRU (`summaryCache`) para evitar reprocesado inmediato.

---

## 5) Problemas detectados (deuda técnica / UX / robustez)

## Críticos para objetivo “IA local first”
1. **Acoplamiento funcional a endpoint remoto**: existe modo EXTERNAL directamente integrado en flujos core de IA.
2. **Secrets en DataStore plano**: API key persistida en preferencias sin capa de hardening (Keystore/cifrado).
3. **No hay capa de inferencia local desacoplada de proveedor/model runtime** (faltan adapters/engine interfaces para embeddings, chat y tool-calling por separado).

## Calidad de IA/planificación
4. Heurísticas de parsing e inferencia son frágiles ante OCR ruidoso, formatos mixtos o material escaso.
5. `AnalyzeAssessmentWithAiUseCase` mezcla orquestación de varios pasos y policy de fallback (si falla externo vuelve a rawText) sin telemetría estructurada del motivo.
6. Aplicación de complejidad a topics por nombre/similitud superficial puede producir asignaciones equivocadas en temas parecidos.

## Arquitectura y mantenibilidad
7. **Duplicación de flujo IA** en `PlanningViewModel` y `AiAnalysisViewModel` (selección evaluación, modo ejecución, análisis), con alta probabilidad de divergencia.
8. `NoOpLlmProvider` existe pero no está integrado en DI por entorno/build type; huele a código sin rol operativo.
9. Dependencia de librerías pesadas de OCR/PDF en app principal sin aislamiento por módulo, aumentando superficie de mantenimiento.

## UX / producto
10. `AiAnalysisScreen` depende de input textual manual; no hay flujo integrado para “seleccionar material existente → analizar”.
11. Mensajería de errores y confianza útil pero sin trazabilidad para usuario (“por qué salió esto” detallado por etapa/documento).
12. Falta explicitud de presupuesto local (latencia, memoria, tamaño de modelos, tiempo de indexación) en UX y configuración.

---

## 6) Arquitectura objetivo propuesta (EmbeddingGemma + Gemma 3n + FunctionGemma)

## Principios
- **Offline-first real**: sin dependencia de endpoints remotos para casos nominales.
- **Motores separados por responsabilidad**:
  1) embeddings/indexado,
  2) razonamiento/generación,
  3) acciones/tool-calling.
- **Puertos limpios en dominio** para evitar acoplar casos de uso a framework/model runtime específico.

## Componentes objetivo

### A. LocalEmbeddingEngine (EmbeddingGemma)
Responsabilidades:
- Chunking de documentos (materiales y texto manual).
- Generación de embeddings locales.
- Persistencia de vectores y metadata por `subjectId/topicId/assessmentId/materialId`.
- Búsqueda semántica local (top-k + filtros por contexto académico).

Implementación sugerida:
- Nuevo módulo `data/localai/embedding`.
- Índice local persistente (SQLite + tabla vectores o índice ANN local embebido).
- Política de reindex incremental al cambiar materiales/temas.

### B. LocalReasoningEngine (Gemma 3n)
Responsabilidades:
- Síntesis estructurada para:
  - alcance evaluable,
  - complejidad por tema,
  - recomendaciones de secuenciación.
- Recibir contexto recuperado (RAG local) y devolver salida estructurada (JSON schema interno).

Implementación sugerida:
- Nuevo puerto `ReasoningProvider` (dominio IA) separado de `LlmProvider` legacy.
- Prompting y parsing robusto con validación de esquema (rechazo/reintento local).

### C. LocalActionEngine (FunctionGemma)
Responsabilidades:
- Tool-calling local para acciones seguras:
  - aplicar complejidades a temas,
  - regenerar plan,
  - sugerir reprogramación,
  - proponer faltantes de material.
- Gobernanza por permisos y reglas de negocio (sin writes directos desde modelo).

Implementación sugerida:
- Catálogo de herramientas internas tipadas (`sealed interface ToolCommand`).
- Executor transaccional en capa application/usecase.

### D. Model Runtime Manager
Responsabilidades:
- Descarga/verificación local de modelos (hash + versión).
- Gestión de almacenamiento, cuotas y limpieza.
- Selección de variante según capacidad del dispositivo.
- Estado operacional para UX (descargando/listo/error/sin espacio).

### E. AI Orchestrator v2
Responsabilidades:
- Pipeline único: ingestión → indexado → recuperación → inferencia → validación → persistencia.
- Eliminación de duplicación entre pantallas de planificación y análisis IA.

---

## 7) Encaje exacto y sustituciones necesarias

## Encaje directo
- Reemplazar implementación de `DocumentParser/SyllabusAnalyzer/ExamScopeInferenceService/ComplexityEstimator/StudyPlanningAdvisor` por adapters que deleguen a pipeline local con Gemma.
- Mantener contratos de dominio inicialmente para minimizar ruptura.
- `AiAnalysisRepository` y entidades actuales sirven como base de persistencia de resultados (aprovechable).

## Rediseño necesario
1. **Sustituir `LlmProvider` centrado en HTTP** por abstracciones locales separadas:
   - `EmbeddingProvider`,
   - `ReasoningProvider`,
   - `ActionProvider`.
2. Introducir almacenamiento vectorial local y tablas de chunk/index.
3. Unificar flujos IA de `PlanningViewModel` y `AiAnalysisViewModel` en un `AiWorkflowCoordinator`.
4. Rediseñar UX de análisis para elegir fuentes desde materiales existentes y no solo texto pegado.
5. Mantener modo remoto solo como **fallback explícito no preferente** (si se decide conservar), con feature flag.

---

## 8) Plan de migración por fases (sin big-bang)

## Fase 0 — Auditoría y observabilidad (actual)
- Congelar contratos críticos y documentar arquitectura.
- Añadir trazas estructuradas por etapa IA (parse/retrieve/reason/apply) sin exponer PII.

## Fase 1 — Cimientos de IA local
- Crear módulo `localai-runtime` con interfaces y manager de modelos.
- Integrar Model Runtime Manager y verificación de estado en Settings.
- Mantener heurísticas actuales como fallback temporal.

## Fase 2 — Embeddings e indexado local
- Ingesta de materiales + texto manual a chunks.
- EmbeddingGemma local + índice semántico persistente.
- Endpoint interno de retrieval para evaluación/tema.

## Fase 3 — Razonamiento local (Gemma 3n)
- Sustituir inferencia heurística principal por RAG local.
- Salida estructurada validada + guardado en `AiAnalysisRepository`.
- Métricas de calidad comparativa contra heurística baseline.

## Fase 4 — Acciones/tool-calling local (FunctionGemma)
- Implementar catálogo de herramientas internas (aplicar complejidad, regenerar plan, etc.).
- Guardrails de negocio + ejecución transaccional.

## Fase 5 — UX y consolidación
- Flujo “seleccionar materiales → analizar → editar → aplicar” integrado.
- Pantalla de estado de modelos (descarga, tamaño, espacio, rendimiento esperado).
- Reducir duplicación de ViewModels IA con coordinador único.

## Fase 6 — Endurecimiento y retirada gradual de remoto
- Marcar EXTERNAL como opcional avanzado o deprecado.
- Cifrado robusto de secretos si se mantiene fallback remoto.
- Test de regresión funcional completo (planificación, materiales, análisis IA).

---

## 9) Riesgos y trade-offs

- **Rendimiento en dispositivo**: Gemma local puede elevar latencia/consumo; mitigación con perfiles por gama de hardware.
- **Espacio en disco**: modelos + índice vectorial; se requiere gestor de cuotas y limpieza.
- **Complejidad operacional**: más piezas locales (runtime, indexado, lifecycle de modelos).
- **Calidad variable por OCR**: la calidad del texto fuente seguirá condicionando resultados; conviene pipeline de limpieza y scoring de calidad.
- **Compatibilidad**: mantener APIs/entidades actuales reduce riesgo de ruptura, pero puede requerir capas adaptadoras temporales.

---

## 10) Recomendación ejecutiva

Orden recomendado de implementación:
1. **Infra local (runtime manager + interfaces)**.
2. **Embedding/indexado y retrieval**.
3. **Razonamiento Gemma 3n reemplazando heurística en flujo AI principal**.
4. **FunctionGemma para acciones seguras y reducción de fricción UX**.
5. **Consolidación de UX/arquitectura (coordinador único + eliminación de duplicidades)**.

Este orden maximiza impacto de producto con riesgo controlado y evita migración big-bang.
