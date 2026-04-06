package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.HealthRecord

interface HealthRepository {
    suspend fun getLastRecordForAnimal(id: String): HealthRecord?

    // Para mostrar la lista completa en la pantalla de detalles del animal
    suspend fun getHealthHistory(animalId: String): List<HealthRecord>

    //Para guardar nuevos chequeos
    suspend fun addHealthRecord(record: HealthRecord): Boolean
}