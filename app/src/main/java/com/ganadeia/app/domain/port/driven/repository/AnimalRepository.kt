package com.ganadeia.app.domain.port.driven.repository

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalStatus

interface AnimalRepository {

    /** Inserta un animal nuevo. Si el ID ya existe, lo reemplaza (REPLACE). */
    suspend fun addAnimal(ownerId: String, animal: Animal): Boolean

    /** Devuelve todos los animales de un dueño, sin importar su status. */
    suspend fun getAnimalsByOwner(ownerId: String): List<Animal>

    /** Devuelve solo los animales con status ACTIVE de el usuario. */
    suspend fun getAllActiveAnimals(): List<Animal>

    /** Actualiza la fecha del próximo seguimiento de un animal. */
    suspend fun updateFollowUpDate(id: String, newDate: Long): Boolean

    /**
     * Actualiza los datos mutables de un animal ya registrado.
     * Los campos inmutables (type, breed, hardiness, birthDate) no se tocan.
     *
     * @param ownerId  ID del dueño — requerido por la FK en Room.
     * @param animal   Animal con los datos ya actualizados.
     * @return true si la operación fue exitosa.
     */
    suspend fun updateAnimal(ownerId: String, animal: Animal): Boolean

    /**
     * Cambia el status de un animal a SOLD o INACTIVE.
     * No elimina el animal ni su historial (offline-first, trazabilidad).
     *
     * @param animalId  ID del animal a desactivar.
     * @param newStatus Nuevo status: SOLD o INACTIVE.
     * @return true si la operación fue exitosa.
     */
    suspend fun updateAnimalStatus(animalId: String, newStatus: AnimalStatus): Boolean
}