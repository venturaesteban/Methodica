# IA Heurística - Política de documentos

Este módulo usa heurísticas locales (sin LLM remoto) para inferir alcance y complejidad.

## Tipos de entrada recomendados

- Guía docente con secciones y temas.
- Temario oficial por unidades/bloques.
- Criterios de evaluación y formato de examen.

## Límites operativos

- Mínimo: 120 palabras.
- Máximo: 12.000 palabras.
- Se requiere al menos una señal estructural:
  - 3+ secciones, o
  - 5+ temas, o
  - 2+ señales de evaluación.

## Casos no recomendados

- Texto muy corto o sin estructura.
- OCR ruidoso de escaneos.
- Mezcla de varias asignaturas en el mismo bloque.

## Resultado de calidad

- `qualityWarnings`: advertencias de baja estructura.
- `blockedReason`: motivo por el cual se bloquea el análisis.


