package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.ui.theme.*
import com.ganadeia.app.ui.viewmodel.IaAnalysisState
import com.ganadeia.app.ui.viewmodel.IaAnalysisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IaAnalysisScreen(
    viewModel: IaAnalysisViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAnimalDetail: (String) -> Unit
) {
    val animals by viewModel.userAnimals.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()

    var selectedAnimal by remember { mutableStateOf<Animal?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadUserAnimals()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis IA", color = Color.White, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.resetState()
                            onNavigateBack()
                        },
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
        containerColor = CardLight,
        bottomBar = {
            if (analysisState is IaAnalysisState.Success) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                    ) {
                        Text("Compartir", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val successState = analysisState as? IaAnalysisState.Success
                            if (successState != null) {
                                viewModel.resetState()
                                onNavigateToAnimalDetail(successState.animal.id)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Guardar en historial", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Selector de animal
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Animal a analizar",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedAnimal?.name ?: "Seleccione un animal",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            containerColor = Color.White,
                            unfocusedBorderColor = PrimaryGreen,
                            focusedBorderColor = PrimaryGreen
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        animals.forEach { animal ->
                            DropdownMenuItem(
                                text = { Text("${animal.id.take(4).uppercase()} - ${animal.name}") },
                                onClick = {
                                    selectedAnimal = animal
                                    expanded = false
                                    viewModel.resetState()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        selectedAnimal?.let { viewModel.analyzeAnimal(it.id) }
                    },
                    enabled = selectedAnimal != null && analysisState !is IaAnalysisState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        disabledContainerColor = PrimaryGreen.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = if (analysisState is IaAnalysisState.Loading) "Analizando..." else "Analizar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (analysisState is IaAnalysisState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (analysisState as IaAnalysisState.Error).message,
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Resultados del análisis
            if (analysisState is IaAnalysisState.Success) {
                val successState = analysisState as IaAnalysisState.Success
                val animal = successState.animal
                val record = successState.result.record

                Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(modifier = Modifier.height(32.dp))

                AnimalAnalysisItem(
                    title = "${animal.id.take(4).uppercase()} — ${animal.name}",
                    tags = listOf("${animal.currentWeight} kg", animal.type.name, animal.purpose.name),
                    diaText = record.generalDiagnosis ?: "Análisis en proceso o guardado offline.",
                    prioritariaText = record.priorityAction ?: "N/A",
                    nutricionalText = record.nutritionalRecommendation ?: "N/A",
                    medicoText = record.medicalRecommendation ?: "Sin recomendación médica.",
                    vacunasText = record.vaccineRecommendation ?: "Sin recomendación de vacunas.",
                    confianza = record.confidenceScore ?: 0f,
                    seguimientoText = " (Sugerido por Groq API)",
                    seguimientoBoldText = "Acción Recomendada"
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun AnimalAnalysisItem(
    title: String,
    tags: List<String>,
    diaText: String,
    prioritariaText: String,
    nutricionalText: String,
    medicoText: String,
    vacunasText: String,
    confianza: Float,
    seguimientoText: String,
    seguimientoBoldText: String
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        // Animal Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8F2EC)),
                contentAlignment = Alignment.Center
            ) {
                Text("🐮", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardLight)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(tag, fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Diagnóstico General
        AnalysisCard(
            title = "DIAGNÓSTICO GENERAL",
            iconColor = PrimaryGreen
        ) {
            Text(diaText, color = TextDark, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF9EAE1))
                    .padding(start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9EAE1))
                        .padding(12.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append("Acción prioritaria: ")
                            }
                            append(prioritariaText)
                        },
                        color = TextDark,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                Box(modifier = Modifier.width(4.dp).matchParentSize().background(AccentOrange))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recomendación Nutricional
        AnalysisCard(
            title = "RECOMENDACIÓN NUTRICIONAL",
            iconColor = AccentOrange
        ) {
            Text(nutricionalText, color = TextDark, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Confianza del modelo", color = GrayText, fontSize = 12.sp)
                Text("${confianza.toInt()}%", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = confianza / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryGreen,
                trackColor = Color(0xFFE8E8E8)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recomendación Médica (generada por IA)
        AnalysisCard(
            title = "RECOMENDACIÓN MÉDICA",
            iconColor = Color(0xFF1976D2)
        ) {
            Text(medicoText, color = TextDark, fontSize = 14.sp, lineHeight = 22.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Plan de Vacunación (generado por IA)
        AnalysisCard(
            title = "PLAN DE VACUNACIÓN",
            iconColor = Color(0xFF9C27B0)
        ) {
            Text(vacunasText, color = TextDark, fontSize = 14.sp, lineHeight = 22.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seguimiento Sugerido
        AnalysisCard(
            title = "SEGUIMIENTO SUGERIDO",
            iconColor = Color(0xFF5E4B40)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(seguimientoBoldText)
                    }
                    append(seguimientoText)
                },
                color = TextDark,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun AnalysisCard(
    title: String,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(iconColor))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = GrayText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}
