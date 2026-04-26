package com.ganadeia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganadeia.app.ui.theme.*
import com.ganadeia.app.ui.viewmodel.AuthState
import com.ganadeia.app.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name      by remember { mutableStateOf("") }
    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var ranchName by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    // Navegar al dashboard si el registro fue exitoso
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryGreen)
    ) {
        // Header verde con flecha atrás
        Row(
            modifier = Modifier
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.resetState(); onNavigateBack() },
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text("Crear cuenta", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }

        // Tarjeta blanca inferior con el formulario
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(CardLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    "Regístrate como ganadero",
                    color = TextDark,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Todos los campos marcados son obligatorios", color = GrayText, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(28.dp))

                RegisterField("NOMBRE COMPLETO *", name, { name = it }, "Carlos Rodríguez")
                Spacer(modifier = Modifier.height(20.dp))
                RegisterField("CORREO ELECTRÓNICO *", email, { email = it }, "carlos@finca.co")
                Spacer(modifier = Modifier.height(20.dp))
                RegisterField(
                    "CONTRASEÑA * (mín. 8 caracteres)", password, { password = it },
                    "••••••••", isPassword = true
                )
                Spacer(modifier = Modifier.height(20.dp))
                RegisterField("NOMBRE DE LA FINCA (opcional)", ranchName, { ranchName = it }, "Finca El Valle")

                // Error message
                if (authState is AuthState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEEEE))
                            .padding(16.dp)
                    ) {
                        Text(
                            (authState as AuthState.Error).message,
                            color = Color(0xFFCC0000),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.register(
                            name      = name,
                            email     = email,
                            password  = password,
                            ranchName = ranchName.ifBlank { null }
                        )
                    },
                    enabled  = authState !is AuthState.Loading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        disabledContainerColor = PrimaryGreen.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = if (authState is AuthState.Loading) "Creando cuenta..." else "Crear cuenta",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    Column {
        Text(label, color = GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = GrayText) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = PrimaryGreen
            )
        )
    }
}
