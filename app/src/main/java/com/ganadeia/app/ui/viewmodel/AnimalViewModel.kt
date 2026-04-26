package com.ganadeia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganadeia.app.GanadiaApplication
import com.ganadeia.app.application.AddAnimalRequest
import com.ganadeia.app.application.AddAnimalUseCase
import com.ganadeia.app.data.repository.RoomAnimalRepository
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ganadeia.app.data.repository.RoomSessionRepository
import com.ganadeia.app.data.repository.RoomUserRepository

class AnimalViewModel(application: Application) : AndroidViewModel(application) {

    // Room comparte la misma instancia de BD con AuthViewModel mediante GanadiaApplication
    private val database = (application as GanadiaApplication).database

    private val animalRepository = RoomAnimalRepository(database.animalDao())
    
    // 2. Inicialización estricta del único caso de uso
    private val addAnimalUseCase = AddAnimalUseCase(animalRepository)

    private val sessionRepository = RoomSessionRepository(database.userDao(), database.sessionDao())
    private val userRepository = RoomUserRepository(database.userDao())

    private val _animalesAgregados = MutableStateFlow<List<Animal>>(emptyList())
    val animalesAgregados: StateFlow<List<Animal>> = _animalesAgregados.asStateFlow()

    init {
        loadUserAnimals()
    }

    private fun loadUserAnimals() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val session = sessionRepository.getActiveSession()
                if (session != null) {
                    val animals = animalRepository.getAnimalsByOwner(session.userId)
                    _animalesAgregados.value = animals
                }
            }
        }
    }

    fun guardarAnimal(requestBuilder: (User) -> AddAnimalRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val session = sessionRepository.getActiveSession()
                        ?: throw Exception("No hay sesión activa. Inicie sesión primero.")
                    val user = userRepository.getUserById(session.userId)
                        ?: throw Exception("Usuario no encontrado.")
                    
                    val request = requestBuilder(user)
                    addAnimalUseCase.execute(request)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { newAnimal ->
                        _animalesAgregados.value = _animalesAgregados.value + newAnimal
                        onSuccess()
                    },
                    onFailure = { exception ->
                        onError(exception.message ?: "Error desconocido al guardar animal")
                    }
                )
            }
        }
    }

    fun getAnimal(id: String): Animal? {
        return _animalesAgregados.value.find { it.id == id }
    }
}
