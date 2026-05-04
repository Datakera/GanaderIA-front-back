package com.ganadeia.app.infrastructure.persistence.firebase

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AnimalStatus
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.infrastructure.monitoring.CrashReporter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Decorador que intercepta las escrituras de animales para respaldarlas en Firebase Firestore.
 * - Las lecturas siempre provienen de Room (fuente de verdad local).
 * - Las escrituras se guardan primero en Room y luego se envían a Firestore.
 * - Soporte Offline-First: Si no hay internet, el SDK de Firestore guardará el registro
 *   en su caché local automáticamente y lo sincronizará cuando la red vuelva.
 */
class FirestoreAnimalRepository(
    private val firestore: FirebaseFirestore,
    private val localAnimalRepository: AnimalRepository
) : AnimalRepository {

    private val animalsCollection = firestore.collection("animals")

    override suspend fun addAnimal(ownerId: String, animal: Animal): Boolean {
        // 1. Guardar localmente
        val localSuccess = localAnimalRepository.addAnimal(ownerId, animal)
        // 2. Respaldar en Firestore si el guardado local fue exitoso
        if (localSuccess) {
            try {
                animalsCollection.document(animal.id).set(animal).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAnimal", e)
                // Falla silenciosa permitida por arquitectura offline-first de Firebase
            }
        }
        return localSuccess
    }

    override suspend fun updateAnimal(ownerId: String, animal: Animal): Boolean {
        val localSuccess = localAnimalRepository.updateAnimal(ownerId, animal)
        if (localSuccess) {
            try {
                animalsCollection.document(animal.id).set(animal).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAnimal_Update", e)
                // Falla silenciosa (caché Firestore)
            }
        }
        return localSuccess
    }

    override suspend fun updateAnimalStatus(animalId: String, newStatus: AnimalStatus): Boolean {
        val localSuccess = localAnimalRepository.updateAnimalStatus(animalId, newStatus)
        if (localSuccess) {
            try {
                animalsCollection.document(animalId).update("status", newStatus.name).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAnimal_Status", e)
                // Falla silenciosa (caché Firestore)
            }
        }
        return localSuccess
    }

    override suspend fun updateFollowUpDate(id: String, newDate: Long): Boolean {
        val localSuccess = localAnimalRepository.updateFollowUpDate(id, newDate)
        if (localSuccess) {
            try {
                animalsCollection.document(id).update("nextFollowUpDate", newDate).await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAnimal_FollowUp", e)
                // Falla silenciosa (caché Firestore)
            }
        }
        return localSuccess
    }

    override suspend fun deleteAnimal(animalId: String): Boolean {
        val localSuccess = localAnimalRepository.deleteAnimal(animalId)
        if (localSuccess) {
            try {
                animalsCollection.document(animalId).delete().await()
            } catch (e: Exception) {
                CrashReporter.logError("FirestoreAnimal_Delete", e)
                // Falla silenciosa (caché Firestore)
            }
        }
        return localSuccess
    }

    // ── LECTURAS: Siempre de Room ──────────────────────────────────────────

    override suspend fun getAnimalsByOwner(ownerId: String): List<Animal> {
        return localAnimalRepository.getAnimalsByOwner(ownerId)
    }

    override suspend fun getAllActiveAnimals(): List<Animal> {
        return localAnimalRepository.getAllActiveAnimals()
    }
}
