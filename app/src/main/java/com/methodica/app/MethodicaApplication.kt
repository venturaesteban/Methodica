package com.methodica.app

import android.app.Application

class MethodicaApplication : Application() {

    /**
     * Punto de acceso global al contenedor de dependencias.
     * Los ViewModels que necesiten un repositorio (Fase 1+) obtendrán
     * la referencia a través de este contenedor vía ViewModelProvider.Factory.
     */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
