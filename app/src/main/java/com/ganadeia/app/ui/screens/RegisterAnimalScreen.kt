package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.ui.theme.*
import com.ganadeia.app.ui.viewmodel.AnimalViewModel
import com.ganadeia.app.application.AddAnimalRequest
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.BreedHardiness

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAnimalScreen(viewModel: AnimalViewModel, onNavigateBack: () -> Unit) {
    var id by remember { mutableStateOf("A-043") }
    var raza by remember { mutableStateOf("Cebú") }
    var peso by remember { mutableStateOf("350") }
    var edad by remember { mutableStateOf("36") }
    var condicion by remember { mutableStateOf("3 — Normal") }
    var sintomas by remember { mutableStateOf("Pérdida leve de apetito, pelaje sin brillo") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Animal", color = Color.White, style = MaterialTheme.typography.titleMedium) },
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
            
            // Photo placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEBDDCB)), // Soft orange from design
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Placeholder for a camera icon
                    Box(modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                       Text("📷", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tomar foto del animal", color = AccentOrange, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Form Rows
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField(modifier = Modifier.weight(1f), label = "ID / ARETE", value = id, onValueChange = { id = it })
                FormField(modifier = Modifier.weight(1f), label = "RAZA", value = raza, onValueChange = { raza = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField(modifier = Modifier.weight(1f), label = "PESO (KG)", value = peso, onValueChange = { peso = it })
                FormField(modifier = Modifier.weight(1f), label = "EDAD (MESES)", value = edad, onValueChange = { edad = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            FormField(modifier = Modifier.fillMaxWidth(), label = "CONDICIÓN CORPORAL", value = condicion, onValueChange = { condicion = it })

            Spacer(modifier = Modifier.height(16.dp))

            FormField(
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                label = "SÍNTOMAS VISIBLES",
                value = sintomas,
                onValueChange = { sintomas = it },
                singleLine = false
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* IA analysis not implemented yet */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analizar con IA", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    try {
                        viewModel.guardarAnimal(
                            requestBuilder = { user ->
                                AddAnimalRequest(
                                    owner = user,
                                    name = id,
                                    type = AnimalType.BOVINE,
                                    breed = raza,
                                    hardiness = BreedHardiness.HIGH,
                                    weight = peso.toDoubleOrNull() ?: 0.0,
                                    ageInMonths = edad.toIntOrNull() ?: 0,
                                    purpose = AnimalPurpose.MEAT
                                )
                            },
                            onSuccess = {
                                id = ""
                                raza = ""
                                peso = ""
                                edad = ""
                            },
                            onError = { /* Error handling */ }
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Guardar animal", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun FormField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(text = label, color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = PrimaryGreen,
                unfocusedIndicatorColor = Color.LightGray
            ),
            singleLine = singleLine
        )
    }
}
