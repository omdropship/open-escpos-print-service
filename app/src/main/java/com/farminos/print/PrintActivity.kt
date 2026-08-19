package com.farminos.print

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farminos.print.ui.theme.OpenESCPOSPrintServiceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PrintActivity : ComponentActivity() {

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectedUri = intent?.data ?: if (intent?.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else null

        setContent {
            OpenESCPOSPrintServiceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (selectedUri != null) {
                        PdfPreviewAndPrintScreen(
                            onBack = { finish() }
                        )
                    } else {
                        SettingsScreen(this)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfPreviewAndPrintScreen(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // State Pengaturan Cetak
    var printDirection by remember { mutableStateOf(0) } // 0, 90, 180, 270
    var isCustomSize by remember { mutableStateOf(true) }
    var widthMm by remember { mutableStateOf("80") }
    var heightMm by remember { mutableStateOf("100") }
    
    var startPage by remember { mutableStateOf("1") }
    var endPage by remember { mutableStateOf("100") }
    
    // State Progress Cetak
    var isPrinting by remember { mutableStateOf(false) }
    var printedCount by remember { mutableStateOf(0) }
    var totalToPrint by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2196F3))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !isPrinting) {
                Text("← Kembali", color = Color.White, fontSize = 16.sp)
            }
            Text("Preview & Cetak", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- AREA PREVIEW RESI ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📄 Preview File Resi PDF", color = Color.Gray, fontSize = 16.sp)
                }
            }

            // --- PRINT DIRECTION (ROTASI) ---
            Text("Print direction", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0, 90, 180, 270).forEach { angle ->
                    Button(
                        onClick = { printDirection = angle },
                        enabled = !isPrinting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (printDirection == angle) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                            contentColor = if (printDirection == angle) Color.White else Color.Black
                        )
                    ) {
                        Text("$angle°")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- OPSI UKURAN KERTAS (STANDARD / CUSTOM) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ukuran Kertas", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isCustomSize = false
                            widthMm = "80"
                            heightMm = "100"
                        },
                        enabled = !isPrinting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isCustomSize) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                            contentColor = if (!isCustomSize) Color.White else Color.Black
                        )
                    ) {
                        Text("Standard")
                    }
                    Button(
                        onClick = { isCustomSize = true },
                        enabled = !isPrinting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCustomSize) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                            contentColor = if (isCustomSize) Color.White else Color.Black
                        )
                    ) {
                        Text("Custom")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // INPUT CUSTOM LEBAR & TINGGI (MM)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = widthMm,
                    onValueChange = { widthMm = it },
                    label = { Text("Width (mm)") },
                    enabled = isCustomSize && !isPrinting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = heightMm,
                    onValueChange = { heightMm = it },
                    label = { Text("Height (mm)") },
                    enabled = isCustomSize && !isPrinting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- RANGE HALAMAN ---
            Text("Range Halaman yang Dicetak", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = startPage,
                    onValueChange = { startPage = it },
                    label = { Text("Mulai") },
                    enabled = !isPrinting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text("--", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = endPage,
                    onValueChange = { endPage = it },
                    label = { Text("Sampai") },
                    enabled = !isPrinting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // --- INDIKATOR STATUS PROGRES MENCETAK ---
            if (isPrinting) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔄 Memproses Cetak...",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Berhasil mencetak: $printedCount dari $totalToPrint resi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D47A1)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val progressFloat = if (totalToPrint > 0) printedCount.toFloat() / totalToPrint.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = progressFloat,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // --- TOMBOL UTAMA PRINT ---
        Button(
            onClick = {
                if (!isPrinting) {
                    val start = startPage.toIntOrNull() ?: 1
                    val end = endPage.toIntOrNull() ?: 1
                    totalToPrint = (end - start + 1).coerceAtLeast(1)
                    printedCount = 0
                    isPrinting = true

                    coroutineScope.launch {
                        for (i in 1..totalToPrint) {
                            delay(300)
                            printedCount = i
                        }
                        isPrinting = false
                    }
                }
            },
            enabled = !isPrinting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPrinting) Color.Gray else Color(0xFF2196F3)
            ),
            shape = RectangleShape
        ) {
            Text(
                text = if (isPrinting) "SEDANG MENCETAK ($printedCount/$totalToPrint)..." else "PRINT DOKUMEN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
