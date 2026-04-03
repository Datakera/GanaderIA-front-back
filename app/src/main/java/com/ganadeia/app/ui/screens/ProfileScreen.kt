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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onNavigateToHome: () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = onNavigateToHome,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") }
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
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        indicatorColor = CardLight
                    )
                )
            }
        },
        containerColor = CardLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                ProfileHeaderSection()
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "INFORMACIÓN DE FINCA",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    ProfileMenuItem(icon = Icons.Default.Home, title = "Nombre de finca", subtitle = "Finca El Valle")
                    Divider(color = CardLight, thickness = 1.dp, modifier = Modifier.padding(start = 64.dp))
                    ProfileMenuItem(icon = Icons.Default.LocationOn, title = "Ubicación", subtitle = "Huila, Colombia")
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "CUENTA",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                ) {
                    ProfileMenuItem(icon = Icons.Default.Lock, title = "Seguridad", subtitle = "Cambiar contraseña")
                    Divider(color = CardLight, thickness = 1.dp, modifier = Modifier.padding(start = 64.dp))
                    ProfileMenuItem(icon = Icons.Default.Notifications, title = "Notificaciones", subtitle = "Activadas")
                    Divider(color = CardLight, thickness = 1.dp, modifier = Modifier.padding(start = 64.dp))
                    ProfileMenuItem(icon = Icons.Default.ExitToApp, title = "Sesión", subtitle = "Cerrar sesión", isDestructive = true)
                }
            }
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProfileHeaderSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Green background top part
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(PrimaryGreen)
                .padding(top = 48.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text("Mi Perfil", color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 24.sp)
                Text("Editar", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
            }
        }
        
        // Overlapping Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp, start = 24.dp, end = 24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Carlos Rodríguez", color = TextDark, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
                Text("carlos@fincaelvalle.co", color = GrayText, fontSize = 14.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(number = "24", label = "Animales")
                    ProfileStat(number = "18", label = "Análisis")
                    ProfileStat(number = "6", label = "Meses activo")
                }
            }
        }
    }
}

@Composable
fun ProfileStat(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = GrayText, fontSize = 12.sp)
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, subtitle: String, isDestructive: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDestructive) Color(0xFFFDECEA) else CardLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) Color(0xFFD32F2F) else PrimaryGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = GrayText, fontSize = 12.sp)
            Text(
                subtitle,
                color = if (isDestructive) Color(0xFFD32F2F) else TextDark,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = GrayText)
    }
}
