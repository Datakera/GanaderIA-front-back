package com.ganadeia.app.infrastructure.persistence.firebase

import com.ganadeia.app.domain.model.Session
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.domain.port.driven.repository.SessionRepository
import com.ganadeia.app.domain.port.driven.repository.UserRepository
import com.ganadeia.app.infrastructure.monitoring.CrashReporter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Decorador que intercepta la autenticación para inyectar Firebase Auth,
 * manteniendo las firmas originales y delegando la persistencia a Room (offline-first).
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val localUserRepository: UserRepository,
    private val localSessionRepository: SessionRepository
) : UserRepository, SessionRepository {

    private val usersCollection = firestore.collection("users")

    // ── SessionRepository (Login y Logout) ──────────────────────────────────
    
    override suspend fun validateCredentials(email: String, passwordHash: String): User? {
        return try {
            // 1. Intentar iniciar sesión en Firebase usando el hash como contraseña
            auth.signInWithEmailAndPassword(email, passwordHash).await()
            val user = localSessionRepository.validateCredentials(email, passwordHash)
            user?.let { CrashReporter.setUser(it.id) }
            user
        } catch (e: Exception) {
            CrashReporter.logError("FirebaseAuth", e)
            // Si Firebase falla (ej. sin internet o credenciales malas), 
            // intentamos hacer fallback a Room para soporte offline
            val localUser = localSessionRepository.validateCredentials(email, passwordHash)
            localUser?.let { CrashReporter.setUser(it.id) }
            localUser
        }
    }

    override suspend fun clearSession(): Boolean {
        // Cierra sesión en Firebase
        auth.signOut()
        CrashReporter.clearUser()
        // Cierra sesión localmente
        return localSessionRepository.clearSession()
    }

    override suspend fun saveSession(session: Session): Boolean =
        localSessionRepository.saveSession(session)

    override suspend fun getActiveSession(): Session? =
        localSessionRepository.getActiveSession()


    // ── UserRepository (Registro de Usuarios) ────────────────────────────────

    override suspend fun saveUser(user: User, passwordHash: String): Boolean {
        return try {
            // 1. Crear el usuario en Firebase Auth usando el email y el hash
            auth.createUserWithEmailAndPassword(user.email, passwordHash).await()
            // 2. Si Firebase tiene éxito, guardar en Room como fuente de verdad
            val localSuccess = localUserRepository.saveUser(user, passwordHash)
            // 3. Sincronizar el perfil del usuario a Firestore
            if (localSuccess) {
                try {
                    usersCollection.document(user.id).set(user).await()
                } catch (e: Exception) {
                    CrashReporter.logError("Firestore_SaveUser", e)
                }
            }
            localSuccess
        } catch (e: Exception) {
            CrashReporter.logError("FirebaseAuth_SaveUser", e)
            // Si Firebase falla (ej. sin internet), no registramos al usuario
            // porque la creación de cuentas requiere nube.
            false
        }
    }

    override suspend fun getUserById(userId: String): User? =
        localUserRepository.getUserById(userId)

    override suspend fun updateProfile(user: User): Boolean {
        val localSuccess = localUserRepository.updateProfile(user)
        if (localSuccess) {
            try {
                usersCollection.document(user.id).set(user).await()
            } catch (e: Exception) {
                CrashReporter.logError("Firestore_UpdateProfile", e)
            }
        }
        return localSuccess
    }

    override suspend fun existsByEmail(email: String): Boolean =
        localUserRepository.existsByEmail(email)
}
