package com.ganadeia.app.domain.model

enum class VisibleSymptom(val label: String, val isCritical: Boolean = false) {
    NONE("Ninguno"),
    DECAIMIENTO("Decaimiento general"),
    FALTA_DE_APETITO("Falta de apetito"),
    COJERA("Cojera"),
    DIARREA("Diarrea"),
    DESCARGA_NASAL("Descarga nasal"),
    DESCARGA_OCULAR("Descarga ocular"),
    PELAJE_OPACO("Pelaje opaco / sin brillo"),
    PERDIDA_DE_PESO("Pérdida de peso visible"),
    DISTENSION_ABDOMINAL("Distensión abdominal", isCritical = true),
    TOS("Tos frecuente"),
    FIEBRE("Fiebre / Temperatura elevada"), // Moderado (7 días según tu test)
    AISLAMIENTO("Aislamiento del hato"),
    BAJA_PRODUCCION_LECHE("Baja producción de leche"),
    TEMBLORES("Temblores musculares", isCritical = true),
    DIFICULTAD_RESPIRATORIA("Dificultad respiratoria", isCritical = true) // Crítico (3 días)
}

data class HealthRecord(
    val id: String,
    val animalId: String,                    // FK rígida al animal
    val date: Long,                          // epoch ms — cuándo se realizó el chequeo

    // ── Datos clínicos ────────────────────────────────────────────────────────
    val weightKg: Double,
    val bodyConditionScore: Int,             // escala 1–5
    val symptoms: Set<VisibleSymptom>,       // conjunto de síntomas observados
    val notes: String?,                      // observaciones libres del ganadero

    // ── Resultado IA ──────────────────────────────────────────────────────────
    val aiRecommendation: String?,           // null hasta que llega respuesta de la IA

    // ── Sincronización offline-first ──────────────────────────────────────────
    val syncStatus: SyncStatus,

    // ── Seguimiento ───────────────────────────────────────────────────────────
    val confirmedFollowUpDate: Long?         // null hasta que el ganadero confirma/edita
)

/**
 * Estado de sincronización de un registro con el servidor remoto.
 *
 * Máquina de estados:
 *   PENDING_SYNC  ──(hay red)──►  SYNCED
 *   PENDING_SYNC  ──(error)───►  SYNC_ERROR  ──(retry)──► PENDING_SYNC
 */
enum class SyncStatus {
    /** Guardado localmente, aún no enviado a la nube. Estado inicial sin red. */
    PENDING_SYNC,

    /** Enviado y confirmado por el servidor remoto. */
    SYNCED,

    /** El último intento de sincronización falló; se reintentará. */
    SYNC_ERROR
}
