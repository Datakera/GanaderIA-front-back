package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.ganadeia.app.ui.viewmodel.AnimalViewModel
import androidx.compose.ui.graphics.asImageBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalDetailScreen(viewModel: AnimalViewModel, animalId: String, onNavigateBack: () -> Unit,
    onNavigateToUpdateAnalysis: (String) -> Unit = {}
) {
    val animal = viewModel.getAnimal(animalId)
    val history by viewModel.recommendationsHistory.collectAsState()

    LaunchedEffect(animalId) {
        viewModel.loadAllRecommendations(animalId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(animal?.name ?: "Detalle", color = Color.White, style = MaterialTheme.typography.titleMedium) },
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
        if (animal == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Error: Animal no encontrado", color = GrayText)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        if (animal.photoPath != null && java.io.File(animal.photoPath).exists()) {
                            val bitmap = remember(animal.photoPath) {
                                android.graphics.BitmapFactory.decodeFile(animal.photoPath)
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Foto de ${animal.name}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE8F2EC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🐄", fontSize = 32.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(animal.name, color = TextDark, style = MaterialTheme.typography.titleMedium)
                        Text(animal.breed, color = GrayText, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "INFORMACIÓN GENERAL", 
                    color = GrayText, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    DetailRow("ID / UUID", animal.id.take(8).uppercase())
                    Divider(color = CardLight, thickness = 1.dp)
                    DetailRow("Tipo Físico", animal.type.name)
                    Divider(color = CardLight, thickness = 1.dp)
                    DetailRow("Peso de registro", "${animal.currentWeight} kg")
                    Divider(color = CardLight, thickness = 1.dp)
                    DetailRow("Propósito", animal.purpose.name)
                    Divider(color = CardLight, thickness = 1.dp)
                    val statusText = if (history.isNotEmpty()) "Analizado ✅" else "Pendiente"
                    DetailRow("Estado", statusText)
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // ── Sección de Análisis IA guardado ──────────────────────────────
                val firstRec = history.firstOrNull()
                if (firstRec != null) {
                    Text(
                        text = "PRIMER ANÁLISIS DE IA",
                        color = GrayText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AiRecommendationCards(rec = firstRec)
                    
                    // Historial posterior (desplegables)
                    val subsequentRecs = history.drop(1)
                    if (subsequentRecs.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "ACTUALIZACIONES DEL ANÁLISIS",
                            color = GrayText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        subsequentRecs.forEach { updateRec ->
                            ExpandableAiRecommendation(rec = updateRec)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Botón para actualizar el análisis
                    Button(
                        onClick = { onNavigateToUpdateAnalysis(animalId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Actualizar Análisis", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                } else {
                    // Sin análisis guardado
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Sin análisis de IA",
                            color = TextDark,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Analiza este animal desde la sección de IA para ver los resultados aquí.",
                            color = GrayText,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = GrayText, fontSize = 14.sp)
        Text(value, color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun AiRecommendationCards(rec: com.ganadeia.app.domain.model.AiRecommendationRecord) {
    // Fecha del análisis
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy  HH:mm", java.util.Locale.getDefault())
    val dateText = sdf.format(java.util.Date(rec.respondedAt ?: rec.requestedAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryGreen))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Fecha: $dateText", color = GrayText, fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Diagnóstico General
    DetailAnalysisCard(
        title = "DIAGNÓSTICO GENERAL",
        iconColor = PrimaryGreen
    ) {
        Text(
            rec.generalDiagnosis ?: "Sin diagnóstico",
            color = TextDark,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        if (rec.priorityAction != null) {
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
                            append(rec.priorityAction!!)
                        },
                        color = TextDark,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
                Box(modifier = Modifier.width(4.dp).matchParentSize().background(AccentOrange))
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Recomendación Nutricional
    DetailAnalysisCard(
        title = "RECOMENDACIÓN NUTRICIONAL",
        iconColor = AccentOrange
    ) {
        Text(
            rec.nutritionalRecommendation ?: "Sin recomendación",
            color = TextDark,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
        val score = rec.confidenceScore ?: 0f
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Confianza del modelo", color = GrayText, fontSize = 12.sp)
            Text("${score.toInt()}%", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = PrimaryGreen,
            trackColor = Color(0xFFE8E8E8)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Recomendación Médica
    DetailAnalysisCard(
        title = "RECOMENDACIÓN MÉDICA",
        iconColor = Color(0xFF1976D2)
    ) {
        Text(
            rec.medicalRecommendation ?: "Sin recomendación médica",
            color = TextDark,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Plan de Vacunación
    DetailAnalysisCard(
        title = "PLAN DE VACUNACIÓN",
        iconColor = Color(0xFF9C27B0)
    ) {
        Text(
            rec.vaccineRecommendation ?: "Sin recomendación de vacunas",
            color = TextDark,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun ExpandableAiRecommendation(rec: com.ganadeia.app.domain.model.AiRecommendationRecord) {
    var expanded by remember { mutableStateOf(false) }
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    val dateText = sdf.format(java.util.Date(rec.respondedAt ?: rec.requestedAt))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryGreen))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Actualización: $dateText", color = TextDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                if (expanded) "▲" else "▼",
                color = GrayText,
                fontSize = 12.sp
            )
        }
        
        if (expanded) {
            Spacer(modifier = Modifier.height(16.dp))
            AiRecommendationCards(rec = rec)
        }
    }
}

@Composable
fun DetailAnalysisCard(
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
