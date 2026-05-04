package com.ganadeia.app

import android.app.Application
import androidx.room.Room
import com.ganadeia.app.infrastructure.persistence.room.AppDatabase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings

/**
 * Clase Application personalizada de GanadeIA.
 *
 * Su responsabilidad principal es proveer una única instancia de [AppDatabase]
 * compartida entre todos los ViewModels. Esto evita el crash causado por
 * múltiples instancias de Room abriendo el mismo archivo SQLite simultáneamente.
 */
class GanadiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        
        // Configurar Firestore para soporte offline-first nativo
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
        }
    }

    /**
     * Instancia singleton de la base de datos Room.
     * Se inicializa de forma perezosa (lazy) la primera vez que se accede.
     * Al ser lazy + by, Kotlin garantiza que solo se crea una vez, incluso
     * si múltiples ViewModels la solicitan en paralelo.
     */
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "ganadeia_database"
        ).fallbackToDestructiveMigration().build()
    }
}
