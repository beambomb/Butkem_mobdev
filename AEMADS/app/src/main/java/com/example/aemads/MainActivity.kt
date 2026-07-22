package com.example.aemads

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import java.io.File

// --- THEME COLORS ---
val DarkNavy = Color(0xFF0B0F19)
val SurfaceNavy = Color(0xFF151C2C)
val NeonAmber = Color(0xFFFFB800)
val CriticalRed = Color(0xFFFF4D4D)
val NeonCyan = Color(0xFF00E5FF)
val TextGray = Color(0xFF8A95A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setupNotificationChannels()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = DarkNavy,
                    surface = SurfaceNavy,
                    primary = NeonCyan,
                    error = CriticalRed
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AemadsApp(context = this)
                }
            }
        }
    }

    private fun setupNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val highChannel = NotificationChannel("spike_alert", "Spike Alerts", NotificationManager.IMPORTANCE_HIGH)
        highChannel.enableVibration(true)
        val silentChannel = NotificationChannel("drift_alert", "Drift Alerts", NotificationManager.IMPORTANCE_DEFAULT)
        silentChannel.setSound(null, null)
        silentChannel.enableVibration(false)
        manager.createNotificationChannel(highChannel)
        manager.createNotificationChannel(silentChannel)
    }
}

@Composable
fun AemadsApp(context: MainActivity, viewModel: DashboardViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("main") { MainScreen(context, viewModel) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var plant by remember { mutableStateOf("Plant 1 - Body & Paint") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkNavy).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Factory, contentDescription = "Logo", tint = NeonCyan, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("AEMADS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Energy Anomaly Early Warning System", color = TextGray, fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Enterprise ID / Email") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, 
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, 
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = plant, onValueChange = {}, readOnly = true,
                label = { Text("Select Plant Location") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, 
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan
                ),
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Plant 1 - Body & Paint") }, onClick = { plant = "Plant 1 - Body & Paint"; expanded = false })
                DropdownMenuItem(text = { Text("Plant 2 - Assembly") }, onClick = { plant = "Plant 2 - Assembly"; expanded = false })
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("main") { popUpTo("login") { inclusive = true } } },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Sign In to Control Room", color = DarkNavy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun MainScreen(context: MainActivity, viewModel: DashboardViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // Camera Logic
    var showReportDialog by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var reportNotes by remember { mutableStateOf("") }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            showReportDialog = true
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Submit Damage Report") },
            text = {
                Column {
                    capturedImage?.let { 
                        Image(bitmap = it.asImageBitmap(), contentDescription = "Captured", modifier = Modifier.height(150.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reportNotes, 
                        onValueChange = { reportNotes = it },
                        label = { Text("Damage Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addReport(DamageReport(notes = reportNotes, imageBitmap = capturedImage))
                    showReportDialog = false
                    reportNotes = ""
                    selectedTab = 3 // Go to history tab
                }) { Text("Submit to Database") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { cameraLauncher.launch() },
                containerColor = NeonAmber,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).offset(y = 40.dp) // Offset to dock into BottomAppBar nicely
            ) {
                Icon(Icons.Default.CameraAlt, "Report Damage", tint = DarkNavy, modifier = Modifier.size(32.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        bottomBar = {
            BottomAppBar(
                containerColor = SurfaceNavy,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, "Home", tint = if (selectedTab == 0) NeonCyan else TextGray) }
                    IconButton(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Icon(if (selectedTab == 1) Icons.Filled.Analytics else Icons.Outlined.Analytics, "Analytics", tint = if (selectedTab == 1) NeonCyan else TextGray) }
                    Spacer(modifier = Modifier.weight(1f)) // Empty space for FAB
                    IconButton(onClick = { selectedTab = 3 }, modifier = Modifier.weight(1f)) { Icon(if (selectedTab == 3) Icons.Filled.History else Icons.Outlined.History, "History", tint = if (selectedTab == 3) NeonCyan else TextGray) }
                    IconButton(onClick = { selectedTab = 4 }, modifier = Modifier.weight(1f)) { Icon(if (selectedTab == 4) Icons.Filled.Person else Icons.Outlined.Person, "Profile", tint = if (selectedTab == 4) NeonCyan else TextGray) }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(DarkNavy)) {
            when (selectedTab) {
                0 -> DashboardTab(viewModel)
                1 -> AnalyticsTab(viewModel)
                3 -> HistoryTab(viewModel)
                4 -> ProfileTab()
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: DashboardViewModel) {
    val dataList by viewModel.dataList.collectAsState()
    val alertState by viewModel.alertState.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // TOP HEADER - Simplified
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Technician Portal", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Plant 1 - Body & Paint", color = TextGray, fontSize = 12.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // CLEAN METRICS
        val latestData = dataList.lastOrNull()
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("POWER (kW)", latestData?.power_kw?.toString() ?: "0.0", NeonCyan, Modifier.weight(1f))
            MetricCard("OEE SCORE", latestData?.oee_score?.toString()?.plus("%") ?: "0%", Color.White, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("COS PHI", latestData?.power_factor?.toString() ?: "0.0", Color.White, Modifier.weight(1f))
            MetricCard("THD", latestData?.thd_value?.toString()?.plus("%") ?: "0%", if ((latestData?.thd_value ?: 0.0) > 10.0) CriticalRed else Color.White, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("ACTIVE SHOP HEATMAP", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ShopHeatmapCard("Stamping", Color(0xFF4CAF50))
            ShopHeatmapCard("Body", Color(0xFF4CAF50))
            ShopHeatmapCard("Paint", if (alertState == AlertState.SPIKE) CriticalRed else Color(0xFF4CAF50))
            ShopHeatmapCard("Assembly", Color(0xFF4CAF50))
            ShopHeatmapCard("Utils", if (alertState == AlertState.DRIFT) NeonAmber else Color(0xFF4CAF50))
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = SurfaceNavy)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
    }
}

@Composable
fun ShopHeatmapCard(name: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceNavy), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(64.dp)) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(16.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, color = Color.White, fontSize = 10.sp)
        }
    }
}

@Composable
fun AnalyticsTab(viewModel: DashboardViewModel) {
    val dataList by viewModel.dataList.collectAsState()
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    
    LaunchedEffect(dataList) {
        val entries = dataList.mapIndexed { index, data -> FloatEntry(index.toFloat(), data.power_kw.toFloat()) }
        if (entries.isNotEmpty()) {
            chartEntryModelProducer.setEntries(entries)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Realtime Power Trend (kW)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Waiting for generator.py data...", color = TextGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().height(300.dp), colors = CardDefaults.cardColors(containerColor = SurfaceNavy)) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (dataList.isNotEmpty()) {
                    Chart(chart = lineChart(), chartModelProducer = chartEntryModelProducer, startAxis = rememberStartAxis(), bottomAxis = rememberBottomAxis())
                } else {
                    Text("NO DATA (Check Python Script)", color = CriticalRed, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun HistoryTab(viewModel: DashboardViewModel) {
    val reports by viewModel.reportList.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Damage Report History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Syncing to Supabase (Pending)", color = TextGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No reports yet. Use the Camera to report.", color = TextGray)
            }
        } else {
            LazyColumn {
                items(reports) { report ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = SurfaceNavy)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(report.timestamp, color = TextGray, fontSize = 12.sp)
                            Spacer(Modifier.height(8.dp))
                            report.imageBitmap?.let { 
                                Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.height(120.dp).fillMaxWidth(), contentScale = ContentScale.Crop)
                                Spacer(Modifier.height(8.dp))
                            }
                            Text(report.notes, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTab() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White, modifier = Modifier.size(100.dp))
        Spacer(Modifier.height(16.dp))
        Text("John Doe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Senior Maintenance Technician", color = NeonCyan)
        Spacer(Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceNavy)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Employee ID: EMP-98234", color = Color.White)
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = DarkNavy)
                Text("Assigned Plant: Plant 1 - Body & Paint", color = Color.White)
            }
        }
    }
}
