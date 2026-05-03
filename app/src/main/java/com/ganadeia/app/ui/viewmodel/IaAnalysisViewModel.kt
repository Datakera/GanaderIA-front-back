package com.ganadeia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganadeia.app.GanadiaApplication
import com.ganadeia.app.application.GetAiRecommendationResult
import com.ganadeia.app.application.GetAiRecommendationUseCase
import com.ganadeia.app.data.repository.*
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.infrastructure.remote.GroqAiService
import com.ganadeia.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class IaAnalysisState {
    object Idle : IaAnalysisState()
    object Loading : IaAnalysisState()
    data class Success(
        val result: GetAiRecommendationResult,
        val animal: Animal
    ) : IaAnalysisState()
    data class Error(val message: String) : IaAnalysisState()
}

class IaAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as GanadiaApplication).database

    // ── Repositorios ──────────────────────────────────────────────────────────
    private val animalRepository = RoomAnimalRepository(database.animalDao())
    private val healthCheckRepository = RoomHealthCheckRepository(database.healthCheckDao())
    private val vaccinationRepository = RoomVaccinationRepository(database.vaccinationDao())
    private val aiRecommendationRepository = RoomAiRecommendationRepository(database.aiRecommendationDao())
    private val sessionRepository = RoomSessionRepository(database.userDao(), database.sessionDao())
    private val userRepository = RoomUserRepository(database.userDao())

    // ── Servicio de IA (Groq) ─────────────────────────────────────────────────
    // Usamos la API Key inyectada desde local.properties vía BuildConfig
    private val groqAiService = GroqAiService.create(BuildConfig.GROQ_API_KEY)

    // ── Caso de Uso ───────────────────────────────────────────────────────────
    private val getAiRecommendationUseCase = GetAiRecommendationUseCase(
        animalRepository = animalRepository,
        healthCheckRepository = healthCheckRepository,
        vaccinationRepository = vaccinationRepository,
        aiRecommendationRepository = aiRecommendationRepository,
        aiServicePort = groqAiService,
        isNetworkAvailable = { true } // Para simplificar, asumimos que siempre hay red en la demo
    )

    // ── Estado ────────────────────────────────────────────────────────────────
    private val _analysisState = MutableStateFlow<IaAnalysisState>(IaAnalysisState.Idle)
    val analysisState: StateFlow<IaAnalysisState> = _analysisState.asStateFlow()

    private val _userAnimals = MutableStateFlow<List<Animal>>(emptyList())
    val userAnimals: StateFlow<List<Animal>> = _userAnimals.asStateFlow()

    init {
        loadUserAnimals()
    }

    fun loadUserAnimals() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val session = sessionRepository.getActiveSession()
                if (session != null) {
                    val animals = animalRepository.getAnimalsByOwner(session.userId)
                    _userAnimals.value = animals
                } else {
                    _userAnimals.value = emptyList()
                }
            }
        }
    }

    fun analyzeAnimal(animalId: String) {
        _analysisState.value = IaAnalysisState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val session = sessionRepository.getActiveSession()
                        ?: return@withContext Result.failure(Exception("No hay sesión activa"))
                    
                    val user = userRepository.getUserById(session.userId)
                        ?: return@withContext Result.failure(Exception("Usuario no encontrado"))
                    
                    val animal = animalRepository.getAnimalsByOwner(user.id).firstOrNull { it.id == animalId }
                        ?: return@withContext Result.failure(Exception("Animal no encontrado"))

                    getAiRecommendationUseCase.execute(user, animalId)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { recommendationResult ->
                        val animal = _userAnimals.value.firstOrNull { it.id == animalId }
                        if (animal != null) {
                            _analysisState.value = IaAnalysisState.Success(
                                result = recommendationResult, 
                                animal = animal
                            )
                        } else {
                            _analysisState.value = IaAnalysisState.Error("Animal no encontrado localmente")
                        }
                    },
                    onFailure = { error ->
                        _analysisState.value = IaAnalysisState.Error(error.message ?: "Error desconocido al analizar")
                    }
                )
            }
        }
    }

    fun resetState() {
        _analysisState.value = IaAnalysisState.Idle
    }
}
