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
import com.ganadeia.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToRegisterAnimal: () -> Unit
) {
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
                    onClick = { },
                    icon = { Icon(Icons.Default.List, contentDescription = "Animales") },
                    label = { Text("Animales") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
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
                HeaderSection()
            }
            item {
                RecentActivitySection()
            }
            item {
                LatestRecommendationSection()
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun HeaderSection() {
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
                    Text("Buenas tardes,", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("Carlos Rodríguez", color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 24.sp)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(modifier = Modifier.weight(1f), number = "24", label = "Animales\nregistrados")
                StatCard(modifier = Modifier.weight(1f), number = "3", label = "Registros\nhoy")
                StatCard(modifier = Modifier.weight(1f), number = "18", label = "Análisis\nIA")
            }
        }
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
fun RecentActivitySection() {
    Column(modifier = Modifier.padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Actividad reciente", color = TextDark, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
            Text("Ver todo →", color = GrayText, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ActivityItem(title = "Res #A-042", subtitle = "Cebú · 380 kg · 4 años", status = "Pendiente", isAnalizado = false)
        Spacer(modifier = Modifier.height(12.dp))
        ActivityItem(title = "Res #A-039", subtitle = "Brahman · 420 kg · 5 años", status = "Analizado", isAnalizado = true)
        Spacer(modifier = Modifier.height(12.dp))
        ActivityItem(title = "Res #A-035", subtitle = "Normando · 310 kg · 3 años", status = "Analizado", isAnalizado = true)
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
fun LatestRecommendationSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Última recomendación", color = TextDark, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Text("Res #A-039 · hace 2 horas", color = GrayText, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Revisar alimentación y suplementar con minerales. Condición corporal 3/5 — se recomienda ajuste de dieta forrajera...",
                color = TextDark,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ver análisis completo →", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}
