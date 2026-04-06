package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun AnimalsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tus animales", color = Color.White, style = MaterialTheme.typography.titleMedium) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            item { AnimalListItem(title = "Res #A-042", subtitle = "Cebú · 380 kg · 4 años", status = "Pendiente", isAnalizado = false) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item { AnimalListItem(title = "Res #A-039", subtitle = "Brahman · 420 kg · 5 años", status = "Analizado", isAnalizado = true) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item { AnimalListItem(title = "Res #A-035", subtitle = "Normando · 310 kg · 3 años", status = "Analizado", isAnalizado = true) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item { AnimalListItem(title = "Res #A-030", subtitle = "Holstein · 450 kg · 4 años", status = "Pendiente", isAnalizado = false) }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            
            item { AnimalListItem(title = "Res #A-028", subtitle = "Cebú · 370 kg · 3 años", status = "Analizado", isAnalizado = true) }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun AnimalListItem(title: String, subtitle: String, status: String, isAnalizado: Boolean) {
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
