package com.ganadeia.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ganadeia.app.GanadiaApplication
import com.ganadeia.app.application.CheckActiveSessionUseCase
import com.ganadeia.app.application.LoginUseCase
import com.ganadeia.app.application.LogoutUseCase
import com.ganadeia.app.application.RegisterUseCase
import com.ganadeia.app.application.RegisterRequest
import com.ganadeia.app.data.repository.RoomSessionRepository
import com.ganadeia.app.data.repository.RoomUserRepository
import com.ganadeia.app.domain.model.LoginCredentials
import com.ganadeia.app.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estados posibles de la pantalla de auth */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

/** Estado del arranque de la app: ¿hay sesión activa? */
sealed class SplashState {
    object Checking : SplashState()
    object SessionActive : SplashState()
    object NoSession : SplashState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    // ── Room setup — usa el singleton de GanadiaApplication ───────────────────
    private val database = (application as GanadiaApplication).database

    private val userRepository    = RoomUserRepository(database.userDao())
    private val sessionRepository = RoomSessionRepository(database.userDao(), database.sessionDao())

    // ── Casos de uso (carpeta application/ — única fuente de lógica) ───────────
    private val loginUseCase          = LoginUseCase(sessionRepository)
    private val registerUseCase       = RegisterUseCase(userRepository, sessionRepository)
    private val logoutUseCase         = LogoutUseCase(sessionRepository)
    private val checkSessionUseCase   = CheckActiveSessionUseCase(sessionRepository)

    // ── Estado expuesto a la UI ────────────────────────────────────────────────
    private val _authState   = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Checking)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()

    // Usuario activo de la sesión actual (para pasar a AnimalViewModel)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ── Al crear el ViewModel, revisar sesión activa ───────────────────────────
    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _splashState.value = SplashState.Checking
            val session = withContext(Dispatchers.IO) {
                checkSessionUseCase.execute()
            }
            if (session != null) {
                val user = withContext(Dispatchers.IO) {
                    userRepository.getUserById(session.userId)
                }
                _currentUser.value = user
                _splashState.value = SplashState.SessionActive
            } else {
                _splashState.value = SplashState.NoSession
            }
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                loginUseCase.execute(LoginCredentials(email, password))
            }
            result.fold(
                onSuccess = { loginResult ->
                    _currentUser.value = loginResult.user
                    _authState.value   = AuthState.Success(loginResult.user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Error al iniciar sesión")
                }
            )
        }
    }

    fun register(name: String, email: String, password: String, ranchName: String?) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                registerUseCase.execute(
                    RegisterRequest(name = name, email = email, password = password, ranchName = ranchName)
                )
            }
            result.fold(
                onSuccess = { registerResult ->
                    _currentUser.value = registerResult.user
                    _authState.value   = AuthState.Success(registerResult.user)
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Error al registrarse")
                }
            )
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                logoutUseCase.execute()
            }
            _currentUser.value = null
            _authState.value   = AuthState.Idle
            onDone()
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
