package com.ganadeia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganadeia.app.GanadiaApplication
import com.ganadeia.app.application.AddAnimalRequest
import com.ganadeia.app.application.AddAnimalUseCase
import com.ganadeia.app.data.repository.RoomAnimalRepository
import com.ganadeia.app.data.repository.RoomAiRecommendationRepository
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.AiRecommendationRecord
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

    private val localAnimalRepository = RoomAnimalRepository(database.animalDao())
    private val animalRepository = com.ganadeia.app.infrastructure.persistence.firebase.FirestoreAnimalRepository(
        firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(),
        localAnimalRepository = localAnimalRepository
    )
    private val localAiRecommendationRepository = RoomAiRecommendationRepository(database.aiRecommendationDao())
    private val aiRecommendationRepository = com.ganadeia.app.infrastructure.persistence.firebase.FirestoreAiRecommendationRepository(
        firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance(),
        localAiRecommendationRepository = localAiRecommendationRepository
    )
    
    // 2. Inicialización estricta del único caso de uso
    private val addAnimalUseCase = AddAnimalUseCase(animalRepository)

    private val sessionRepository = RoomSessionRepository(database.userDao(), database.sessionDao())
    private val userRepository = RoomUserRepository(database.userDao())

    private val _animalesAgregados = MutableStateFlow<List<Animal>>(emptyList())
    val animalesAgregados: StateFlow<List<Animal>> = _animalesAgregados.asStateFlow()

    private val _recommendationsHistory = MutableStateFlow<List<AiRecommendationRecord>>(emptyList())
    val recommendationsHistory: StateFlow<List<AiRecommendationRecord>> = _recommendationsHistory.asStateFlow()

    init {
        loadUserAnimals()
    }

    fun refreshForCurrentUser() {
        _animalesAgregados.value = emptyList()
        _recommendationsHistory.value = emptyList()
        _latestGlobalRecommendation.value = null
        loadUserAnimals()
    }

    fun loadUserAnimals() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val session = sessionRepository.getActiveSession()
                if (session != null) {
                    val animals = animalRepository.getAnimalsByOwner(session.userId)
                    _animalesAgregados.value = animals
                } else {
                    _animalesAgregados.value = emptyList()
                }
            }
        }
    }

    fun loadAllRecommendations(animalId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // Obtenemos todas y las ordenamos por fecha ascendente (la primera al inicio)
                val recs = aiRecommendationRepository.getRecommendationsByAnimal(animalId)
                    .sortedBy { it.requestedAt }
                _recommendationsHistory.value = recs
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

    fun deleteAnimal(animalId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                animalRepository.deleteAnimal(animalId)
            }
            withContext(Dispatchers.Main) {
                if (success) {
                    // Update state directly to reflect deletion
                    _animalesAgregados.value = _animalesAgregados.value.filter { it.id != animalId }
                    // Also clear global recommendation if the deleted animal was the latest
                    if (_latestGlobalRecommendation.value?.second?.id == animalId) {
                        _latestGlobalRecommendation.value = null
                        loadLatestGlobalRecommendation()
                    }
                    onSuccess()
                } else {
                    onError("No se pudo eliminar el animal")
                }
            }
        }
    }

    // ── Dashboard: última recomendación global ──────────────────────────────
    private val _latestGlobalRecommendation = MutableStateFlow<Pair<AiRecommendationRecord, Animal>?>(null)
    val latestGlobalRecommendation: StateFlow<Pair<AiRecommendationRecord, Animal>?> = _latestGlobalRecommendation.asStateFlow()

    fun loadLatestGlobalRecommendation() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val animals = _animalesAgregados.value
                var latest: Pair<AiRecommendationRecord, Animal>? = null
                for (animal in animals) {
                    val rec = aiRecommendationRepository.getLastCompletedRecommendation(animal.id)
                    if (rec != null) {
                        if (latest == null || rec.requestedAt > latest.first.requestedAt) {
                            latest = Pair(rec, animal)
                        }
                    }
                }
                _latestGlobalRecommendation.value = latest
            }
        }
    }
}
