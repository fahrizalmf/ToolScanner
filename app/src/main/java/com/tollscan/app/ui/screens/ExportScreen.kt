package com.tollscan.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tollscan.app.TollScanApp
import com.tollscan.app.export.PdfExporter
import com.tollscan.app.export.XlsxWriter
import com.tollscan.app.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = (context.applicationContext as TollScanApp).repository
    val scope = rememberCoroutineScope()

    // DatePicker works in UTC millis, so this formatter stays in UTC for consistency.
    val sdf = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    var startDate by remember {
        mutableStateOf(run {
            val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            c.add(Calendar.DAY_OF_MONTH, -30)
            sdf.format(c.time)
        })
    }
    var endDate by remember {
        mutableStateOf(sdf.format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).time))
    }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun shareFile(file: File, mime: String) {
        val uri = FileUtils.getUriForFile(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
    }

    fun exportExcel() {
        scope.launch {
            isExporting = true
            statusMessage = null
            try {
                val data = repository.getByDateRange(startDate, endDate)
                val headers = listOf("Tanggal", "Jam", "Gerbang Tol", "Tarif (Rp)")
                val rows = data.map { listOf(it.tanggal, it.jam, it.gerbangTol, it.tarif.toString()) }
                val outDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
                val file = File(outDir, "Laporan_Tol_${startDate}_sd_${endDate}.xlsx")
                XlsxWriter.write(file, headers, rows, "Laporan Tol")
                statusMessage = "Berhasil export ${data.size} data ke Excel"
                shareFile(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            } catch (e: Exception) {
                statusMessage = "Gagal export: ${e.message}"
            } finally {
                isExporting = false
            }
        }
    }

    fun exportPdf() {
        scope.launch {
            isExporting = true
            statusMessage = null
            try {
                val data = repository.getByDateRange(startDate, endDate)
                val outDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
                val file = File(outDir, "Laporan_Tol_${startDate}_sd_${endDate}.pdf")
                PdfExporter.export(context, file, data, "$startDate s/d $endDate")
                statusMessage = "Berhasil export ${data.size} data ke PDF"
                shareFile(file, "application/pdf")
            } catch (e: Exception) {
                statusMessage = "Gagal export: ${e.message}"
            } finally {
                isExporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Laporan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp)) {
            Text("Pilih Periode Laporan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Dari Tanggal") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { TextButton(onClick = { showStartPicker = true }) { Text("Pilih") } }
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = endDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sampai Tanggal") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { TextButton(onClick = { showEndPicker = true }) { Text("Pilih") } }
            )

            Spacer(Modifier.height(28.dp))

            Button(onClick = { exportExcel() }, enabled = !isExporting, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.TableChart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export ke Excel (.xlsx)")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { exportPdf() }, enabled = !isExporting, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export ke PDF (dengan foto struk)")
            }

            if (isExporting) {
                Spacer(Modifier.height(20.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            statusMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showStartPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis -> startDate = sdf.format(Date(millis)) }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis -> endDate = sdf.format(Date(millis)) }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Batal") } }
        ) { DatePicker(state = state) }
    }
}
