package com.ganadeia.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ganadeia.app.infrastructure.persistence.room.AppDatabase
import com.ganadeia.app.infrastructure.persistence.room.dao.AnimalDao
import com.ganadeia.app.infrastructure.persistence.room.entity.AnimalEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnimalDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var animalDao: AnimalDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Usamos una base de datos en MEMORIA para que los tests sean limpios
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        animalDao = db.animalDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadAnimal() = runBlocking {
        // Given: Una entidad de animal
        val animal = AnimalEntity(
            id = "test-1",
            ownerId = "owner-99",
            name = "Lola",
            breed = "Jersey",
            weight = 250.0,
            birthDate = 1000L,
            purpose = "MEAT",
            status = "ACTIVE",
            nextFollowUpDate = null
        )

        // When: Insertamos en la DB real del emulador
        animalDao.insertAnimal(animal)

        // Then: Consultamos y verificamos que los datos sean idénticos
        val list = animalDao.getAnimalsByOwner("owner-99")
        assertEquals(1, list.size)
        assertEquals("Lola", list[0].name)
    }
}