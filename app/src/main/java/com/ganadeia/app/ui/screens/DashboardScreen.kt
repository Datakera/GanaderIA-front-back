package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.domain.model.AiRecommendationRecord
import com.ganadeia.app.domain.model.Animal
import com.ganadeia.app.domain.model.User
import com.ganadeia.app.ui.theme.*
import com.ganadeia.app.ui.viewmodel.AnimalViewModel
import com.ganadeia.app.ui.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    animalViewModel: AnimalViewModel,
    onNavigateToProfile: () -> Unit,
    onNavigateToRegisterAnimal: () -> Unit,
    onNavigateToIaAnalysis: () -> Unit,
    onNavigateToAnimals: () -> Unit,
    onNavigateToAnimalDetail: (String) -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val animals by animalViewModel.animalesAgregados.collectAsState()
    val latestGlobalRec by animalViewModel.latestGlobalRecommendation.collectAsState()

    LaunchedEffect(Unit) {
        animalViewModel.refreshForCurrentUser()
    }

    // Reload latest recommendation when animals change (e.g. after navigating back)
    LaunchedEffect(animals) {
        animalViewModel.loadLatestGlobalRecommendation()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        indicatorColor = CardLight
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToAnimals,
                    icon = { Icon(Icons.Default.List, contentDescription = "Animales") },
                    label = { Text("Animales") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToIaAnalysis,
                    icon = { Icon(Icons.Default.Star, contentDescription = "IA") },
                    label = { Text("IA") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRegisterAnimal,
                containerColor = AccentOrange,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(CardLight)
                .padding(paddingValues)
        ) {
            item {
                HeaderSection(user = currentUser, animalCount = animals.size)
            }
            item {
                RecentActivitySection(
                    animals = animals,
                    onNavigateToAnimals = onNavigateToAnimals
                )
            }
            item {
                LatestRecommendationSection(
                    latestRec = latestGlobalRec,
                    onNavigateToAnimalDetail = onNavigateToAnimalDetail
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(user: User?, animalCount: Int) {
    val greeting = getGreeting()
    val userName = user?.name ?: "Ganadero"
    val initials = userName.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(PrimaryGreen)
            .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("$greeting,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(userName, color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 24.sp)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(modifier = Modifier.weight(1f), number = "$animalCount", label = "Animales\nregistrados")
                StatCard(modifier = Modifier.weight(1f), number = "${user?.ranchName?.take(8) ?: "—"}", label = "Finca")
                StatCard(modifier = Modifier.weight(1f), number = "IA", label = "Análisis\ndisponible")
            }
        }
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Buenos días"
        hour < 18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, number: String, label: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SecondaryGreen)
            .padding(12.dp)
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun RecentActivitySection(animals: List<Animal>, onNavigateToAnimals: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Actividad reciente", color = TextDark, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
            TextButton(onClick = onNavigateToAnimals) {
                Text("Ver todo →", color = GrayText, fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (animals.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🐄", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Empieza añadiendo un animal",
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tus animales registrados aparecerán aquí.",
                    color = GrayText,
                    fontSize = 13.sp
                )
            }
        } else {
            // Show last 3 animals
            val recentAnimals = animals.takeLast(3).reversed()
            recentAnimals.forEachIndexed { index, animal ->
                ActivityItem(
                    title = "Res ${animal.name}",
                    subtitle = "${animal.breed} · ${animal.currentWeight} kg · ${animal.type.name}",
                    status = "Registrado",
                    isAnalizado = false
                )
                if (index < recentAnimals.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ActivityItem(title: String, subtitle: String, status: String, isAnalizado: Boolean) {
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
                .background(CardLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = GrayText)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = GrayText, fontSize = 13.sp)
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isAnalizado) StatusAnalyzed else StatusPending)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                status,
                color = if (isAnalizado) PrimaryGreen else AccentOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LatestRecommendationSection(
    latestRec: Pair<AiRecommendationRecord, Animal>?,
    onNavigateToAnimalDetail: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Última recomendación", color = TextDark, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
        
        Spacer(modifier = Modifier.height(16.dp))

        if (latestRec == null) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📊", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Sin análisis de IA aún",
                    color = TextDark,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Empieza a hacer análisis de IA a tus animales agregados.",
                    color = GrayText,
                    fontSize = 13.sp
                )
            }
        } else {
            val (rec, animal) = latestRec
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateText = sdf.format(Date(rec.respondedAt ?: rec.requestedAt))
        
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Text("${animal.name} · $dateText", color = GrayText, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    rec.generalDiagnosis ?: "Análisis completado.",
                    color = TextDark,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { onNavigateToAnimalDetail(animal.id) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Ver análisis completo →", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}
