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
import androidx.compose.runtime.Composable
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
import com.ganadeia.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IaAnalysisScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Análisis IA", color = Color.White, style = MaterialTheme.typography.titleMedium) },
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
        containerColor = CardLight,
        bottomBar = {
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
                    onClick = { },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // First Animal Analysis
            AnimalAnalysisItem(
                title = "Res #A-042 — Cebú",
                tags = listOf("350 kg", "36 meses", "CC: 2/5", "Huila"),
                diaText = "El animal presenta una condición corporal baja (2/5) con pérdida de apetito y pelaje opaco. Estos síntomas son consistentes con deficiencia de minerales y posible parasitosis gastrointestinal.",
                prioritariaText = "Realizar examen coproparasitológico en los próximos 3 días y aplicar desparasitación preventiva si se confirma carga parasitaria elevada.",
                nutricionalText = "Suplementar con bloque mineral (sal mineralizada al 8% de fósforo) durante 30 días. Aumentar aporte de forraje de alta calidad (Maralfalfa o King Grass) mínimo 3% del peso vivo/día.",
                confianza = 0.87f,
                seguimientoText = "Si la condición corporal no mejora a 3/5, considerar consulta con médico veterinario especializado en nutrición bovina.",
                seguimientoBoldText = "Re-evaluar en 15 días. "
            )

            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(modifier = Modifier.height(32.dp))

            // Second Animal Analysis (To enable scrolling as requested)
            AnimalAnalysisItem(
                title = "Res #A-039 — Brahman",
                tags = listOf("420 kg", "5 años", "CC: 3/5", "Antioquia"),
                diaText = "El animal se encuentra en buen estado general. La ganancia de peso diaria es aceptable para su edad y raza. No se observan signos de enfermedades activas.",
                prioritariaText = "Mantener esquema de vacunación actualizado y proveer control de parásitos externos rutinariamente.",
                nutricionalText = "Continuar con pastoreo rotacional. Mantener acceso a agua limpia a voluntad y oferta continua de sal mineralizada base.",
                confianza = 0.95f,
                seguimientoText = "para evaluar la ganancia de peso mensual de forma regular.",
                seguimientoBoldText = "Control de rutina en 30 días "
            )
            
            Spacer(modifier = Modifier.height(48.dp))
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
                    .background(Color(0xFFE8F2EC)), // Light green
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
                    .background(Color(0xFFF9EAE1)) // Light orange background
                    .padding(start = 4.dp) // Left border simulation
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
                // Custom Left Border
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
                Text("${(confianza * 100).toInt()}%", color = TextDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = confianza,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryGreen,
                trackColor = Color(0xFFE8E8E8)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seguimiento Sugerido
        AnalysisCard(
            title = "SEGUIMIENTO SUGERIDO",
            iconColor = Color(0xFF5E4B40) // Dark brown
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
