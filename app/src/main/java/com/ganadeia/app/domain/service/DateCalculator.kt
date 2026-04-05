package com.ganadeia.app.domain.service

import java.util.Calendar

object DateCalculator {
    fun monthsToBirthDate(months: Int): Long {
        val calendar = Calendar.getInstance()
        // Restamos los meses a la fecha actual
        calendar.add(Calendar.MONTH, -months)
        // Opcional: poner el día en 1 para que sea una fecha "base"
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }
}