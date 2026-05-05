package com.ganadeia.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ganadeia.app.ui.theme.*
import com.ganadeia.app.ui.viewmodel.AnimalViewModel
import com.ganadeia.app.application.AddAnimalRequest
import com.ganadeia.app.domain.model.AnimalType
import com.ganadeia.app.domain.model.AnimalPurpose
import com.ganadeia.app.domain.model.BreedHardiness
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterAnimalScreen(viewModel: AnimalViewModel, onNavigateBack: () -> Unit) {
    var id by remember { mutableStateOf("A-043") }
    var raza by remember { mutableStateOf("Cebú") }
    var peso by remember { mutableStateOf("350") }
    var edad by remember { mutableStateOf("36") }
    var condicion by remember { mutableStateOf("3 — Normal") }
    var sintomas by remember { mutableStateOf("Pérdida leve de apetito, pelaje sin brillo") }

    // ── Estado de la foto ──────────────────────────────────────────────────
    val context = LocalContext.current
    var photoPath by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Función para crear un archivo temporal para la foto
    fun createImageFile(): Pair<File, Uri> {
        val photosDir = File(context.filesDir, "animal_photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val file = File(photosDir, "animal_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(file, uri)
    }

    // Launcher para tomar la foto
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoPath != null) {
            // La foto se guardó correctamente en el archivo
            // Forzar recomposición manteniendo el path
            val savedPath = photoPath
            photoPath = null
            photoPath = savedPath
        } else {
            // Si falló o canceló, limpiamos
            photoPath = null
            photoUri = null
        }
    }

    // Launcher para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (file, uri) = createImageFile()
            photoPath = file.absolutePath
            photoUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    // Función para iniciar la captura de foto
    fun takePhoto() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            val (file, uri) = createImageFile()
            photoPath = file.absolutePath
            photoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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
            
            // Photo capture area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEBDDCB))
                    .clickable { takePhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (photoPath != null && File(photoPath!!).exists()) {
                    // Mostrar la foto capturada
                    val bitmap = remember(photoPath) {
                        BitmapFactory.decodeFile(photoPath)
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto del animal",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Placeholder: sin foto aún
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tomar foto del animal", color = AccentOrange, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Toca aquí para abrir la cámara", color = GrayText, fontSize = 12.sp)
                    }
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
                                    purpose = AnimalPurpose.MEAT,
                                    photoPath = photoPath
                                )
                            },
                            onSuccess = {
                                id = ""
                                raza = ""
                                peso = ""
                                edad = ""
                                photoPath = null
                                photoUri = null
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
