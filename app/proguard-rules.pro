# Reglas ProGuard para la versión release.
# En Fase 0 no hay ofuscación; se activará cuando el producto esté maduro.

# Mantener anotaciones de Room para que el compilador KSP genere DAOs correctamente
-keepattributes *Annotation*

# Evitar advertencias de Kotlin stdlib en builds release
-dontwarn kotlin.**
