package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryGreen)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "GanadeIA",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Decisiones\ninteligentes\npara tu ganado",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 36.sp,
                lineHeight = 40.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Colombia · Sector ganadero",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(CardLight)
                .padding(top = 32.dp, start = 32.dp, end = 32.dp, bottom = 48.dp)
        ) {
            Column {
                Text(
                    text = "CORREO ELECTRÓNICO",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("carlos@fincaelvalle.co") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.White,
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = PrimaryGreen
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CONTRASEÑA",
                    color = GrayText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.White,
                        unfocusedBorderColor = Color.LightGray,
                        focusedBorderColor = PrimaryGreen
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onLoginSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Ingresar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("¿Olvidaste tu contraseña? ", color = GrayText, fontSize = 14.sp)
                    Text("Recupérala aquí", color = AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
