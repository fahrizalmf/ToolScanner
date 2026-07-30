package com.tollscan.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tollscan.app.TollScanApp
import com.tollscan.app.data.TollReceipt
import com.tollscan.app.ocr.ReceiptParser
import com.tollscan.app.ocr.TextRecognizerHelper
import com.tollscan.app.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val repository = (context.applicationContext as TollScanApp).repository
    val scope = rememberCoroutineScope()

    var photoFile by remember { mutableStateOf<File?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    var gerbang by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf("") }
    var jam by remember { mutableStateOf("") }
    var tarif by remember { mutableStateOf("") }

    fun processImage(file: File) {
        isProcessing = true
        showForm = false
        scope.launch {
            try {
                val fixed = FileUtils.fixOrientation(context, file)
                FileUtils.saveBitmap(fixed, file)
                bitmap = fixed
                val text = TextRecognizerHelper.recognize(fixed)
                rawText = text
                val parsed = ReceiptParser.parse(text)
                gerbang = parsed.gerbangTol
                tanggal = parsed.tanggal
                jam = parsed.jam
                tarif = parsed.tarif.toString()
            } catch (e: Exception) {
                rawText = "Gagal membaca struk: ${e.message}"
                gerbang = ""
                tanggal = ""
                jam = ""
                tarif = "0"
            } finally {
                isProcessing = false
                showForm = true
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoFile != null) {
            processImage(photoFile!!)
        }
    }

    fun launchCameraWithNewFile() {
        val file = FileUtils.createImageFile(context)
        photoFile = file
        val uri = FileUtils.getUriForFile(context, file)
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCameraWithNewFile()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = FileUtils.createImageFile(context)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            photoFile = file
            processImage(file)
        }
    }

    fun requestCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            launchCameraWithNewFile()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Struk Tol") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val currentBitmap = bitmap
            if (currentBitmap == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Ambil foto struk tol dengan pencahayaan cukup,\nrata, dan tidak buram",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { requestCamera() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Kamera")
                    }
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Galeri")
                    }
                }
            } else {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { requestCamera() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ambil Ulang Foto")
                }
            }

            if (isProcessing) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Membaca struk...")
                }
            }

            if (showForm) {
                Spacer(Modifier.height(20.dp))
                Text("Periksa & Perbaiki Data", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = gerbang, onValueChange = { gerbang = it },
                    label = { Text("Nama Gerbang Tol") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = tanggal, onValueChange = { tanggal = it },
                    label = { Text("Tanggal (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = jam, onValueChange = { jam = it },
                    label = { Text("Jam (HH:mm)") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = tarif,
                    onValueChange = { input -> tarif = input.filter { it.isDigit() } },
                    label = { Text("Tarif (Rp)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val file = photoFile
                        if (file != null) {
                            scope.launch {
                                repository.insert(
                                    TollReceipt(
                                        gerbangTol = gerbang.ifBlank { "TIDAK DIKETAHUI" },
                                        tanggal = tanggal.ifBlank { "-" },
                                        jam = jam.ifBlank { "-" },
                                        tarif = tarif.toLongOrNull() ?: 0L,
                                        imagePath = file.absolutePath,
                                        rawOcrText = rawText
                                    )
                                )
                                onDone()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Struk")
                }
            }
        }
    }
}
