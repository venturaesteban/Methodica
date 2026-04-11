# Methodica — Release Checklist

## 1) Firma y artefacto
- Crear `keystore.properties` desde `keystore.properties.example`.
- Verificar que el archivo `.jks` exista en la ruta de `storeFile`.
- Ejecutar:
  - `./gradlew :app:bundleRelease`
- Confirmar salida en:
  - `app/build/outputs/bundle/release/app-release.aab`

## 2) Quality gates
- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:lint`

## 3) Validación funcional mínima (manual)
- Crear materia, tema y evaluación.
- Generar plan en Planning.
- Marcar sesiones en Today.
- Verificar que Settings controla recordatorios.
- Reiniciar app y validar que los recordatorios se reprograman.

## 4) Publicación en Play Console
- Cargar `app-release.aab`.
- Completar Data Safety y política de privacidad.
- Revisar permisos declarados (`POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`).
- Subir capturas y ficha de store.
- Publicar en Internal Testing antes de producción.

## 5) Criterio de salida a producción
- Build y tests en verde.
- Lint sin issues críticos.
- Sin regresiones en flujo E2E del estudiante.
- Validación de recordatorios en Android 13+.
