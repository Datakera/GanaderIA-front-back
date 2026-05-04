package com.ganadeia.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.domain.model.VisibleSymptom
import com.ganadeia.app.ui.theme.*
import com.ganadeia.app.ui.viewmodel.IaAnalysisViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UpdateAnalysisScreen(
    viewModel: IaAnalysisViewModel,
    animalId: String,
    onNavigateBack: () -> Unit,
    onNavigateToIaAnalysis: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var weightStr by remember { mutableStateOf("") }
    var ageInMonthsStr by remember { mutableStateOf("") }
    var selectedSymptoms by remember { mutableStateOf(setOf<VisibleSymptom>()) }

    // Formateador para mostrar la fecha seleccionada
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Actualizar Análisis", color = Color.White, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryGreen)
            )
        },
        containerColor = CardLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text("Nuevos datos del animal", style = MaterialTheme.typography.titleMedium, color = TextDark)
            Text("Ingresa la información actual para un diagnóstico preciso.", color = GrayText, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(24.dp))

            // Campo Fecha
            OutlinedTextField(
                value = dateFormat.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha del análisis") },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha", tint = PrimaryGreen)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Peso
            OutlinedTextField(
                value = weightStr,
                onValueChange = { weightStr = it },
                label = { Text("Peso actual (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Edad (meses)
            OutlinedTextField(
                value = ageInMonthsStr,
                onValueChange = { ageInMonthsStr = it },
                label = { Text("Edad en meses") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Síntomas
            Text("Síntomas visibles", style = MaterialTheme.typography.titleSmall, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisibleSymptom.values().forEach { symptom ->
                    val isSelected = selectedSymptoms.contains(symptom)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (symptom == VisibleSymptom.NONE) {
                                selectedSymptoms = setOf(VisibleSymptom.NONE)
                            } else {
                                val current = selectedSymptoms.toMutableSet()
                                current.remove(VisibleSymptom.NONE) // Quitar "Ninguno" si selecciona otro
                                if (isSelected) {
                                    current.remove(symptom)
                                } else {
                                    current.add(symptom)
                                }
                                selectedSymptoms = current
                            }
                        },
                        label = { Text(symptom.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val weight = weightStr.toDoubleOrNull() ?: 0.0
                    // ageInMonthsStr se guarda si decides actualizar la BD del animal, 
                    // por ahora el ViewModel usa weight y symptoms.
                    viewModel.analyzeAnimalWithUpdate(
                        animalId = animalId,
                        newDate = selectedDate,
                        newWeight = weight,
                        newSymptoms = if (selectedSymptoms.isEmpty()) setOf(VisibleSymptom.NONE) else selectedSymptoms
                    )
                    onNavigateToIaAnalysis()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = weightStr.isNotBlank() && ageInMonthsStr.isNotBlank()
            ) {
                Text("Hacer análisis de IA", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
