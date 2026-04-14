package com.ganadeia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.ganadeia.app.application.AddAnimalRequest
import com.ganadeia.app.application.AddAnimalUseCase
import com.ganadeia.app.data.repository.RoomAnimalRepository
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import com.ganadeia.app.infrastructure.persistence.room.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnimalViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Configuración manual de Room y Repositorio
    // fallbackToDestructiveMigration: permite que Room reconstruya la BD cuando
    // el equipo de backend sube la versión (actualmente v2). Apropiado para desarrollo.
    private val database = Room.databaseBuilder(
        application.applicationContext,
        AppDatabase::class.java,
        "ganadeia_database"
    ).fallbackToDestructiveMigration().build()

    private val animalRepository = RoomAnimalRepository(database.animalDao())
    
    // 2. Inicialización estricta del único caso de uso
    private val addAnimalUseCase = AddAnimalUseCase(animalRepository)

    // 3. Estado en memoria reactiva de los animales guardados 
    //    en esta sesión para cumplir con el requerimiento de no aislar otros GetUseCases
    private val _animalesAgregados = MutableStateFlow<List<Animal>>(emptyList())
    val animalesAgregados: StateFlow<List<Animal>> = _animalesAgregados.asStateFlow()

    // 4. Usuario mock para poder salvar en el modelo de dominio
    val currentUser = User(
        id = "mock-user-123",
        name = "Carlos Rodríguez",
        email = "carlos@fincaelvalle.co",
        role = UserRole.RANCHER,
        ranchName = "Finca El Valle",
        location = "Huila, Colombia",
        permissions = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    fun guardarAnimal(request: AddAnimalRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = addAnimalUseCase.execute(request)
            
            result.onSuccess { newAnimal ->
                // Actualizar la lista en memoria con el nuevo animal agregado a la BD
                _animalesAgregados.value = _animalesAgregados.value + newAnimal
                onSuccess()
            }.onFailure { exception ->
                onError(exception.message ?: "Error desconocido")
            }
        }
    }

    fun getAnimal(id: String): Animal? {
        return _animalesAgregados.value.find { it.id == id }
    }
}
