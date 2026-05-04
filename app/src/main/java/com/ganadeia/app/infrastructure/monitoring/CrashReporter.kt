package com.ganadeia.app.infrastructure.monitoring

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {

    // Registra errores silenciosos (no crashean la app)
    fun logError(tag: String, error: Throwable) {
        FirebaseCrashlytics.getInstance().apply {
            log("ERROR [$tag]: ${error.message}")
            recordException(error)
        }
    }

    // Registra mensajes de estado importantes
    fun log(tag: String, message: String) {
        FirebaseCrashlytics.getInstance().log("[$tag] $message")
    }

    // Asocia el error al usuario autenticado
    fun setUser(userId: String) {
        FirebaseCrashlytics.getInstance().setUserId(userId)
    }

    // Limpia el usuario al hacer logout
    fun clearUser() {
        FirebaseCrashlytics.getInstance().setUserId("")
    }
}
