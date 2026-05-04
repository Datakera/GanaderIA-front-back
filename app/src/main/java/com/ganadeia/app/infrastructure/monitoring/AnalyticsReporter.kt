package com.ganadeia.app.infrastructure.monitoring

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsReporter {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    fun logEvent(eventName: String, params: Bundle? = null) {
        analytics.logEvent(eventName, params)
    }

    // Funciones auxiliares tipadas para evitar errores de tipeo
    fun logLogin() {
        logEvent(FirebaseAnalytics.Event.LOGIN)
    }

    fun logSignUp() {
        logEvent(FirebaseAnalytics.Event.SIGN_UP)
    }

    fun logAddAnimal(animalType: String) {
        val bundle = Bundle().apply {
            putString("animal_type", animalType)
        }
        logEvent("add_animal", bundle)
    }

    fun logAiAnalysisRequested(animalType: String) {
        val bundle = Bundle().apply {
            putString("animal_type", animalType)
        }
        logEvent("ai_analysis_requested", bundle)
    }
}
