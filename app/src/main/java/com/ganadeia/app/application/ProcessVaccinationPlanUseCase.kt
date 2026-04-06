package com.ganadeia.app.application

import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.ProcessedVaccinationInput
import com.ganadeia.app.domain.model.VaccinationOption
import com.ganadeia.app.domain.model.VaccinationRecord
import com.ganadeia.app.domain.model.VaccineStatus
import com.ganadeia.app.domain.port.driven.repository.AnimalRepository
import com.ganadeia.app.domain.port.driven.repository.VaccinationRepository
import com.ganadeia.app.domain.service.VaccinationService
import java.util.UUID

/**
 * Caso de Uso: Procesar y Persistir Plan de Vacunación.
 *
 * Historia de usuario:
 * "Como Sistema de Gestión Ganadera, quiero procesar una lista de vacunas
 * seleccionadas y personalizadas por el usuario, para persistir únicamente
 * los registros sanitarios validados y ajustados a la realidad del animal."
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * Criterios de Aceptación implementados:
 *
 * CA-1 Provisión de Catálogo Dinámico:
 *      [getCatalog] recibe un [Animal] y devuelve [VaccinationOption]s con
 *      nombre, descripción educativa y fecha calculada por [VaccinationService].
 *
 * CA-2 Filtrado de Selección:
 *      [execute] solo procesa las entradas donde [ProcessedVaccinationInput.isSelected]
 *      es true. El resto se descarta silenciosamente.
 *
 * CA-3 Procesamiento de Fechas Editadas:
 *      La fecha [ProcessedVaccinationInput.scheduledDate] se valida (> 0) y se
 *      aplica tal como viene, permitiendo que el ganadero sobrescriba la sugerida.
 *
 * CA-4 Diferenciación de Estado Inicial:
 *      Cada registro puede nacer como [VaccineStatus.APPLIED] o [VaccineStatus.PENDING]
 *      según [ProcessedVaccinationInput.initialStatus].
 *
 * CA-5 Integridad de Relación:
 *      Se verifica que el animal existe antes de persistir. Todos los registros
 *      quedan vinculados al [animalId] correcto.
 *
 * CA-6 Cálculo de Aplicación:
 *      Si [initialStatus] == APPLIED, se exige [appliedDate] no nula.
 *      Si [initialStatus] == PENDING, [appliedDate] debe quedar nula.
 * ──────────────────────────────────────────────────────────────────────────────
 */
class ProcessVaccinationPlanUseCase(
    private val animalRepository: AnimalRepository,
    private val vaccinationRepository: VaccinationRepository
) {

    // ── CA-1: Provisión de Catálogo Dinámico ─────────────────────────────────

    /**
     * Genera el catálogo de vacunas recomendadas para un animal específico.
     * La UI usa este catálogo para mostrar opciones seleccionables al ganadero.
     *
     * @param animal El animal para el que se genera el catálogo.
     * @return Lista de [VaccinationOption] con nombre, descripción educativa
     *         y fecha sugerida calculada por [VaccinationService].
     */
    fun getCatalog(animal: Animal): List<VaccinationOption> {
        val vaccinationRecords = VaccinationService.generateInitialVaccinationPlan(animal)
        return vaccinationRecords.map { record ->
            VaccinationOption(
                vaccineName = record.vaccineName,
                educationalDescription = EDUCATIONAL_DESCRIPTIONS[record.vaccineName]
                    ?: "Vacuna recomendada para el control sanitario del hato.",
                suggestedDate = record.scheduledDate
            )
        }
    }

    // ── CA-2 a CA-6: Procesamiento y persistencia ─────────────────────────────

    /**
     * Valida, filtra y persiste las vacunas seleccionadas por el ganadero.
     *
     * @param animalId  ID del animal al que pertenecerán los registros (CA-5).
     * @param inputs    Lista de vacunas procesadas por la UI, incluyendo
     *                  selección, fechas y estados iniciales.
     *
     * @return [Result.success] con la lista de [VaccinationRecord] persistidos, o
     *         [Result.failure] con el primer error de validación encontrado.
     */
    suspend fun execute(
        animalId: String,
        inputs: List<ProcessedVaccinationInput>
    ): Result<List<VaccinationRecord>> {

        // ── CA-5: Verificar existencia del animal ─────────────────────────────
        if (animalId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("El ID del animal no puede estar vacío.")
            )
        }

        val animal = animalRepository.getAnimalsByOwner(animalId)
            .firstOrNull { it.id == animalId }
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró el animal con ID '$animalId'. " +
                            "Los registros de vacunación deben estar vinculados a un animal registrado."
                )
            )

        // ── CA-2: Filtrar solo las seleccionadas ──────────────────────────────
        val selectedInputs = inputs.filter { it.isSelected }

        if (selectedInputs.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Debes seleccionar al menos una vacuna para guardar.")
            )
        }

        // ── CA-3 + CA-4 + CA-6: Validar y construir cada registro ─────────────
        val validatedRecords = mutableListOf<VaccinationRecord>()

        for (input in selectedInputs) {
            val validationError = validate(input)
            if (validationError != null) {
                return Result.failure(IllegalArgumentException(validationError))
            }
            validatedRecords.add(input.toRecord(animalId = animal.id))
        }

        // ── Persistir el lote validado ────────────────────────────────────────
        val saved = vaccinationRepository.saveAll(validatedRecords)
        if (!saved) {
            return Result.failure(
                RuntimeException("No se pudo guardar el plan de vacunación en la base de datos local.")
            )
        }

        return Result.success(validatedRecords)
    }

    // ── Validación privada por entrada ────────────────────────────────────────

    /**
     * Valida una entrada individual.
     * @return Mensaje de error si la validación falla, null si es válida.
     */
    private fun validate(input: ProcessedVaccinationInput): String? {
        if (input.vaccineName.isBlank()) {
            return "El nombre de la vacuna no puede estar vacío."
        }

        // CA-3: Validar que la fecha programada sea un valor positivo
        if (input.scheduledDate <= 0) {
            return "La fecha programada para '${input.vaccineName}' no es válida."
        }

        // CA-6: APPLIED exige appliedDate; PENDING no puede tenerla
        return when (input.initialStatus) {
            VaccineStatus.APPLIED -> {
                if (input.appliedDate == null) {
                    "La vacuna '${input.vaccineName}' está marcada como APLICADA pero no tiene fecha de aplicación."
                } else if (input.appliedDate <= 0) {
                    "La fecha de aplicación para '${input.vaccineName}' no es válida."
                } else {
                    null
                }
            }
            VaccineStatus.PENDING -> {
                // appliedDate debe ser null para no contaminar el registro futuro
                if (input.appliedDate != null) {
                    "La vacuna '${input.vaccineName}' está como PENDIENTE pero tiene fecha de aplicación. Establece el estado como APPLIED o elimina la fecha."
                } else {
                    null
                }
            }
            VaccineStatus.OVERDUE -> {
                // OVERDUE es un estado calculado por el sistema, no por el ganadero
                "El estado OVERDUE es asignado automáticamente por el sistema. Usa PENDING o APPLIED."
            }
        }
    }

    // ── Mapper privado ────────────────────────────────────────────────────────

    /**
     * Convierte un [ProcessedVaccinationInput] validado en un [VaccinationRecord]
     * ya vinculado al [animalId] correcto (CA-5).
     */
    private fun ProcessedVaccinationInput.toRecord(animalId: String) = VaccinationRecord(
        id = UUID.randomUUID().toString(),
        animalId = animalId,
        vaccineName = vaccineName,
        scheduledDate = scheduledDate,  // CA-3: fecha personalizada o sugerida
        appliedDate = appliedDate,      // CA-6: solo presente si APPLIED
        status = initialStatus,         // CA-4: APPLIED o PENDING
        doseMl = doseMl
    )

    // ── Catálogo de descripciones educativas ──────────────────────────────────

    /**
     * Mapa estático de descripciones educativas por nombre de vacuna.
     * En una evolución futura, este contenido podría venir de una API o de
     * una tabla de base de datos para soportar múltiples idiomas.
     */
    private companion object {
        val EDUCATIONAL_DESCRIPTIONS = mapOf(
            "Fiebre Aftosa (Ciclo I)" to
                    "Previene la Fiebre Aftosa, enfermedad viral altamente contagiosa que afecta pezuñas y boca. " +
                    "Obligatoria por el ICA en Colombia. Aplicar a los 4 meses de nacido.",

            "Brucelosis B19" to
                    "Previene la Brucelosis, infección bacteriana que causa abortos y reduce la productividad. " +
                    "Se aplica una sola vez entre los 3 y 8 meses de edad. Solo para hembras bovinas.",

            "Rabia Silvestre" to
                    "Protege contra la Rabia transmitida por murciélagos hematófagos, común en zonas rurales colombianas. " +
                    "Aplicar anualmente en zonas endémicas.",

            "Tétanos" to
                    "Previene el tétanos causado por heridas, castraciones o partos sin higiene. " +
                    "Esencial para équidos expuestos a trabajo y lesiones frecuentes.",

            "Encefalitis Equina" to
                    "Protege contra la Encefalitis del Este y del Oeste, transmitida por mosquitos. " +
                    "Vacuna anual recomendada para todos los équidos.",

            "Clostridiosis" to
                    "Cubre múltiples enfermedades por Clostridium: Enterotoxemia, Edema maligno, Carbón sintomático. " +
                    "Refuerzo anual imprescindible en caprinos y ovinos.",

            "Ectima Contagioso" to
                    "Previene la dermatitis pustular en labios y morros, altamente contagiosa entre cabritos. " +
                    "Aplicar antes del destete.",

            "Newcastle" to
                    "Previene la Enfermedad de Newcastle, viral y devastadora en aves de corral. " +
                    "Primera dosis al mes de edad, revacunar cada 3 meses.",

            "Gumboro" to
                    "Protege la bolsa de Fabricio de los pollos contra la enfermedad infecciosa de la bursa. " +
                    "Aplicar al mes de vida junto con Newcastle.",

            "Peste Porcina Clásica" to
                    "Previene la PPC, enfermedad viral altamente letal en porcinos. " +
                    "Obligatoria en Colombia. Vacunar a los 2 meses y reforzar anualmente."
        )
    }
}